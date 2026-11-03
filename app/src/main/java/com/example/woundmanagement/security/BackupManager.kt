package com.example.woundmanagement.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.woundmanagement.data.db.AppDatabase
import com.example.woundmanagement.data.db.entity.*
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.withTransaction
import java.io.*
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val MAGIC = "WBAK"
    // v3: PBKDF2 600k + header (MAGIC+versione+salt+iv) autenticato come AAD nel GCM.
    // v2: 600k senza AAD; v1: 210k senza AAD. Tutti restano leggibili in lettura.
    private const val VERSION = 3
    private const val PBKDF2_ITERATIONS = 600_000          // OWASP 2024 per PBKDF2-HMAC-SHA256
    private const val PBKDF2_ITERATIONS_LEGACY_V1 = 210_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val MIN_PASSWORD_LENGTH = 12             // creazione backup (C7: minimo robusto)
    // Restore: accetta anche password più corte, per i backup creati quando il minimo era 8.
    private const val MIN_RESTORE_PASSWORD_LENGTH = 8
    private const val MAX_DECOMPRESSED_SIZE = 500L * 1024 * 1024 // 500 MB
    private const val MAX_BACKUP_FILE_SIZE = 600L * 1024 * 1024  // tetto sul file cifrato in ingresso (anti-OOM)

    /** Prefisso delle righe del registro arrivate da un backup: si distinguono da quelle fatte qui. */
    internal const val IMPORTED_AUDIT_PREFIX = "[da backup] "

    /** Una riga di registro del backup, marcata come importata una volta sola (anche da un backup di un backup). */
    internal fun markImported(a: AuditLogEntity): AuditLogEntity = a.copy(
        id = 0,
        details = if (a.details.startsWith(IMPORTED_AUDIT_PREFIX)) a.details else IMPORTED_AUDIT_PREFIX + a.details
    )

    /** La chiave contro i doppioni: la stessa per la riga originale e per quella importata. */
    internal fun auditKey(a: AuditLogEntity): List<Any> =
        listOf(a.timestamp, a.action, a.entityType, a.entityId, a.details.removePrefix(IMPORTED_AUDIT_PREFIX))

    /** Legge al massimo [max] byte: oltre si ferma con un errore, senza caricare il resto (W4). */
    internal fun readAtMost(input: java.io.InputStream, max: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            total += n
            if (total > max) throw IOException("File di backup troppo grande (max ${max / 1024 / 1024} MB)")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    data class BackupSummary(
        val patients: Int,
        val wounds: Int,
        val dressings: Int,
        val photos: Int,
        val scales: Int
    )

    /**
     * Esito dell'import-merge: quanti pazienti del backup erano NUOVI (inseriti)
     * e quanti erano GIÀ PRESENTI in locale (aggiornati con i soli dati mancanti).
     * La UI mostra questi conteggi (S.restoreMergeSummary).
     */
    data class RestoreResult(
        val newPatients: Int,
        val updatedPatients: Int
    )

    /**
     * Crea un backup cifrato di tutto il database e le foto.
     * Ritorna il File del backup nella cache directory.
     */
    suspend fun createBackup(context: Context, db: AppDatabase, password: String): File {
        require(password.length >= MIN_PASSWORD_LENGTH) { "Password deve essere almeno $MIN_PASSWORD_LENGTH caratteri" }
        val dao = db.backupDao()

        // Raccolta dati
        val json = JSONObject()
        json.put("version", VERSION)
        json.put("createdAt", System.currentTimeMillis())
        json.put("appVersion", com.example.woundmanagement.BuildConfig.VERSION_NAME)

        val dressings = dao.getAllDressings()
        val dressingPhotos = dao.getAllDressingPhotos()
        json.put("patients", patientsToJson(dao.getAllPatients()))
        json.put("wounds", woundsToJson(dao.getAllWounds()))
        json.put("dressings", dressingsToJson(dressings))
        json.put("dressingPhotos", dressingPhotosToJson(dressingPhotos))
        json.put("norton", nortonToJson(dao.getAllNorton()))
        json.put("nrs2002", nrs2002ToJson(dao.getAllNRS2002()))
        json.put("painNrs", painNrsToJson(dao.getAllPainNrs()))
        json.put("audit", auditToJson(dao.getAllAudit()))

        // Foto: tutte quelle referenziate (copertina dell'evento + foto aggiuntive della
        // tabella dressing_photos; la position 0 coincide col photoUri dell'evento, quindi
        // il set dedup evita blob duplicati), ridimensionate a max 2048px (stesso
        // downscale dell'import) per evitare OOM.
        val photosJson = JSONObject()
        val seenPhotos = HashSet<String>()
        for (fn in dressings.map { it.photoUri } + dressingPhotos.map { it.photoUri }) {
            if (fn.isBlank() || !seenPhotos.add(fn)) continue
            try {
                // loadBitmap (non loadThumbnail): la cache LRU delle miniature non va
                // riempita con una bitmap da 2048px per OGNI foto dell'archivio, che
                // sfratterebbe le miniature della UI e sprecherebbe memoria.
                val bitmap = PhotoManager.loadBitmap(context, fn, 2048) ?: continue
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                photosJson.put(fn, Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                bitmap.recycle()   // copia non cachata: liberabile subito
            } catch (_: Exception) { /* foto corrotta, skip */ }
        }
        json.put("photos", photosJson)

        // Comprimi + cifra
        val plaintext = json.toString().toByteArray(Charsets.UTF_8)
        val compressed = gzipCompress(plaintext)
        val encrypted = encrypt(compressed, password)

        // Salva in cache
        val backupDir = File(context.cacheDir, "backups")
        backupDir.mkdirs()
        val backupFile = File(backupDir, "woundapp_backup.woundbackup")
        backupFile.writeBytes(encrypted)
        return backupFile
    }

    /**
     * Crea un backup cifrato con i dati di UN SOLO paziente (archivio o trasferimento
     * a un collega). Stesso formato del backup completo: si importa con restoreBackup()
     * che fa il merge non distruttivo (paziente nuovo → aggiunto; già presente →
     * integrato senza doppioni).
     * Il file finisce in cacheDir/exports/ ed e' destinato alla condivisione via FileProvider.
     */
    suspend fun createPatientBackup(context: Context, db: AppDatabase, password: String, patientId: Long): File {
        require(password.length >= MIN_PASSWORD_LENGTH) { "Password deve essere almeno $MIN_PASSWORD_LENGTH caratteri" }
        val dao = db.backupDao()

        val patient = dao.getAllPatients().filter { it.id == patientId }
        require(patient.isNotEmpty()) { "Paziente non trovato" }
        val wounds = dao.getAllWounds().filter { it.patientId == patientId }
        val woundIds = wounds.map { it.id }.toSet()
        val dressings = dao.getAllDressings().filter { it.assessmentId in woundIds }
        val dressingIds = dressings.map { it.id }.toSet()
        val dressingPhotos = dao.getAllDressingPhotos().filter { it.eventId in dressingIds }

        val json = JSONObject()
        json.put("version", VERSION)
        json.put("createdAt", System.currentTimeMillis())
        json.put("appVersion", com.example.woundmanagement.BuildConfig.VERSION_NAME)
        json.put("singlePatient", true)
        json.put("patients", patientsToJson(patient))
        json.put("wounds", woundsToJson(wounds))
        json.put("dressings", dressingsToJson(dressings))
        json.put("dressingPhotos", dressingPhotosToJson(dressingPhotos))
        json.put("norton", nortonToJson(dao.getAllNorton().filter { it.patientId == patientId }))
        json.put("nrs2002", nrs2002ToJson(dao.getAllNRS2002().filter { it.patientId == patientId }))
        json.put("painNrs", painNrsToJson(dao.getAllPainNrs().filter { it.patientId == patientId }))
        json.put("audit", JSONArray()) // audit non incluso nel backup per-paziente

        // Solo le foto referenziate da questo paziente: copertine degli eventi + foto
        // aggiuntive in dressing_photos (position 0 = copertina: il set dedup evita doppioni)
        val photosJson = JSONObject()
        val seenPhotos = HashSet<String>()
        for (fn in dressings.map { it.photoUri } + dressingPhotos.map { it.photoUri }) {
            if (fn.isBlank() || !seenPhotos.add(fn)) continue
            try {
                val bitmap = PhotoManager.loadBitmap(context, fn) ?: continue
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                photosJson.put(fn, Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                bitmap.recycle()
            } catch (_: Exception) { /* foto corrotta, skip */ }
        }
        json.put("photos", photosJson)

        val encrypted = encrypt(gzipCompress(json.toString().toByteArray(Charsets.UTF_8)), password)

        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        // Nome file SENZA PII: solo iniziali + data + id (il nome completo resta dentro al
        // backup cifrato, mai nei metadati del filesystem/chat/cloud).
        val p = patient.first()
        val initials = "${p.surname.firstOrNull()?.uppercaseChar() ?: 'X'}${p.name.firstOrNull()?.uppercaseChar() ?: 'X'}"
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        val safeName = "export_${initials}_${date}_$patientId"
            .replace(Regex("[^A-Za-z0-9_]"), "_").take(48)
        val file = File(exportDir, "$safeName.woundbackup")
        file.writeBytes(encrypted)
        return file
    }

    /**
     * Legge un backup e ritorna il riepilogo senza importare.
     */
    suspend fun peekBackup(context: Context, uri: Uri, password: String): BackupSummary {
        val json = decryptBackup(context, uri, password)
        val dressings = json.jsonArrayOrEmpty("dressings")
        // Conteggio foto: copertine degli eventi + foto aggiuntive (sezione dressingPhotos),
        // dedup per nome file; i blob effettivamente inclusi ("photos") fanno da minimo.
        val photoNames = HashSet<String>()
        for (i in 0 until dressings.length()) {
            val fn = dressings.getJSONObject(i).optString("photoUri", "")
            if (fn.isNotEmpty()) photoNames.add(fn)
        }
        val dressingPhotosArr = json.jsonArrayOrEmpty("dressingPhotos")
        for (i in 0 until dressingPhotosArr.length()) {
            val fn = dressingPhotosArr.getJSONObject(i).optString("photoUri", "")
            if (fn.isNotEmpty()) photoNames.add(fn)
        }
        val scaleCount = json.jsonArrayOrEmpty("norton").length() +
                json.jsonArrayOrEmpty("nrs2002").length() +
                json.jsonArrayOrEmpty("painNrs").length()
        return BackupSummary(
            patients = json.jsonArrayOrEmpty("patients").length(),
            wounds = json.jsonArrayOrEmpty("wounds").length(),
            dressings = dressings.length(),
            photos = maxOf(photoNames.size, json.optJSONObject("photos")?.length() ?: 0),
            scales = scaleCount
        )
    }

    /**
     * Importa un backup con MERGE INTELLIGENTE, non distruttivo e IDEMPOTENTE
     * (importare due volte lo stesso file = zero cambiamenti la seconda volta).
     * Per ogni paziente del backup:
     *  - nessun paziente locale corrispondente → inserito con tutto il suo albero
     *    (ID rimappati, foto rinominate con nomi univoci) → conta nei "nuovi";
     *  - paziente locale corrispondente (per codice fiscale, o per cognome+nome+data
     *    di nascita se il CF manca) → il paziente NON viene reinserito né sovrascritto;
     *    vengono aggiunti solo lesioni/medicazioni/scale non già presenti → conta
     *    negli "aggiornati".
     * Le entita' orfane (FK che non risolve) vengono saltate per non corrompere il DB.
     * Atomica. Ritorna i conteggi per la UI.
     */
    suspend fun restoreBackup(context: Context, db: AppDatabase, uri: Uri, password: String): RestoreResult {
        require(password.length >= MIN_RESTORE_PASSWORD_LENGTH) { "Password deve essere almeno $MIN_RESTORE_PASSWORD_LENGTH caratteri" }
        val json = decryptBackup(context, uri, password)
        val ver = json.optInt("version", 1)
        require(ver in 1..VERSION) { "Versione backup non supportata: $ver" }
        val dao = db.backupDao()

        // Parse TUTTO prima di toccare il DB (se il JSON e' malformato non modifichiamo nulla)
        val patients = patientsFromJson(json.jsonArrayOrEmpty("patients"))
        val wounds = woundsFromJson(json.jsonArrayOrEmpty("wounds"))
        val dressings = dressingsFromJson(json.jsonArrayOrEmpty("dressings"))
        val norton = nortonFromJson(json.jsonArrayOrEmpty("norton"))
        val nrs2002 = nrs2002FromJson(json.jsonArrayOrEmpty("nrs2002"))
        val painNrs = painNrsFromJson(json.jsonArrayOrEmpty("painNrs"))
        val backupAudit = auditFromJson(json.jsonArrayOrEmpty("audit"))
        // Righe dressing_photos del backup (per i backup vecchi senza la sezione viene
        // sintetizzata la riga position 0 dal photoUri di ogni evento).
        val dressingPhotos = dressingPhotosFromJson(json, dressings)

        // Rimappamento nomi foto (C4): ogni import scrive le foto su file NUOVI con nome
        // univoco, così due import dello stesso backup non condividono mai lo stesso file
        // fisico (cancellando un paziente non si perdono le foto del suo "clone").
        // Il nuovo nome è generato qui (UUID.jpg.enc): sicuro per costruzione, nessun
        // nome arbitrario del backup finisce su disco (anti path-traversal).
        // La mappa è popolata LAZY: solo per le foto di record realmente inseriti, così
        // le foto di medicazioni saltate (già presenti) non vengono riscritte su disco.
        val photosJson = json.optJSONObject("photos")
        // Chiave composita (eventId del backup + nome): due eventi del backup che puntano
        // allo STESSO file ottengono file locali DISTINTI — cancellare una medicazione non
        // deve mai orfanare la foto di un'altra.
        val photoNameByEvent = HashMap<String, String>()
        // Elenco (nomeNelBackup -> nomeFileLocale) dei blob da scrivere su disco.
        val photoWrites = mutableListOf<Pair<String, String>>()
        fun remapPhotoName(originalName: String, eventKey: Long): String =
            if (originalName.isBlank() || photosJson?.has(originalName) != true) ""
            else photoNameByEvent.getOrPut("$eventKey|$originalName") {
                "${UUID.randomUUID()}.jpg.enc".also { photoWrites.add(originalName to it) }
            }

        var newPatients = 0
        var updatedPatients = 0
        val photoDir = PhotoManager.getEncryptedDir(context)

        // Merge atomico con rimappamento ID (id del backup -> id locale, nuovo o riusato).
        db.withTransaction {
            // Stato locale per la deduplicazione
            val localPatients = dao.getAllPatients()
            val localByFiscal = HashMap<String, PatientEntity>()
            val localByName = HashMap<Triple<String, String, String>, PatientEntity>()
            // Terne anagrafiche presenti su 2+ pazienti locali: match automatico VIETATO
            // (omonimi: meglio un doppione revisionabile che dati fusi nel paziente sbagliato).
            val ambiguousNameKeys = HashSet<Triple<String, String, String>>()
            val ambiguousFiscalKeys = HashSet<String>()
            for (lp in localPatients) {
                val fc = patientFiscalKey(lp.fiscalCode)
                if (fc.isNotBlank()) {
                    if (fc in localByFiscal) ambiguousFiscalKeys.add(fc) else localByFiscal[fc] = lp
                }
                val nk = patientNameKey(lp)
                if (nk in localByName) ambiguousNameKeys.add(nk) else localByName[nk] = lp
            }
            ambiguousFiscalKeys.forEach { localByFiscal.remove(it) }
            val localWoundsByPatient = dao.getAllWounds().groupBy { it.patientId }
            val localDressingsByWound = dao.getAllDressings().groupBy { it.assessmentId }
            val localDressingTimesByWound = localDressingsByWound
                .mapValues { entry -> entry.value.mapTo(HashSet()) { it.createdAt } }
            val localPhotosByEvent = dao.getAllDressingPhotos().groupBy { it.eventId }
            val backupPhotosByEvent = dressingPhotos.groupBy { it.eventId }

            // ── Pazienti: match → riusa l'id locale SENZA sovrascrivere i campi locali
            val pMap = HashMap<Long, Long>()
            val matchedLocalPatientIds = HashSet<Long>()
            for (p in patients) {
                val match = findMatchingLocalPatient(p, localByFiscal, localByName, ambiguousNameKeys)
                if (match == null) {
                    pMap[p.id] = dao.insertPatient(p.copy(id = 0))
                    newPatients++
                } else {
                    pMap[p.id] = match.id
                    matchedLocalPatientIds.add(match.id)
                    updatedPatients++
                }
            }

            // ── Lesioni: su paziente matchato, una lesione è già presente se ne esiste
            //    una locale con stessi (location, createdAt) → riusa l'id locale
            val wMap = HashMap<Long, Long>()
            val reusedWoundIds = HashSet<Long>() // id locali di lesioni già esistenti (non inserite ora)
            for (w in wounds) {
                val newPid = pMap[w.patientId] ?: continue
                val existing = if (newPid in matchedLocalPatientIds)
                    localWoundsByPatient[newPid]?.firstOrNull { it.location == w.location && it.createdAt == w.createdAt }
                else null
                if (existing != null) {
                    wMap[w.id] = existing.id
                    reusedWoundIds.add(existing.id)
                } else {
                    wMap[w.id] = dao.insertWound(w.copy(id = 0, patientId = newPid))
                }
            }

            // ── Medicazioni: su lesione riusata, stesso createdAt → SKIP (niente doppioni).
            //    Per gli eventi skippati: REPAIR dei file foto mancanti — se un import
            //    precedente è stato interrotto dopo il commit ma prima della scrittura
            //    delle foto, i record locali puntano a file inesistenti; qui li riscriviamo
            //    dai blob del backup (copertina per nome, extra per position).
            val dMap = HashMap<Long, Long>() // solo gli eventi realmente inseriti
            for (d in dressings) {
                val newWid = wMap[d.assessmentId] ?: continue
                if (newWid in reusedWoundIds &&
                    localDressingTimesByWound[newWid]?.contains(d.createdAt) == true) {
                    val localEvent = localDressingsByWound[newWid]?.firstOrNull { it.createdAt == d.createdAt }
                    if (localEvent != null && photosJson != null) {
                        if (localEvent.photoUri.isNotBlank() && d.photoUri.isNotBlank() &&
                            photosJson.has(d.photoUri) && !File(photoDir, localEvent.photoUri).exists()) {
                            photoWrites.add(d.photoUri to localEvent.photoUri)
                        }
                        val localExtra = localPhotosByEvent[localEvent.id].orEmpty()
                        for (bp in backupPhotosByEvent[d.id].orEmpty()) {
                            val lp = localExtra.firstOrNull { it.position == bp.position } ?: continue
                            if (lp.photoUri.isNotBlank() && photosJson.has(bp.photoUri) &&
                                !File(photoDir, lp.photoUri).exists()) {
                                photoWrites.add(bp.photoUri to lp.photoUri)
                            }
                        }
                    }
                    continue
                }
                // photoUri rimappato sul nuovo nome file; se la foto non è inclusa nel
                // backup il riferimento si azzera (mai puntare a file di altri pazienti).
                val newPhoto = remapPhotoName(d.photoUri, d.id)
                dMap[d.id] = dao.insertDressing(d.copy(id = 0, assessmentId = newWid, photoUri = newPhoto))
            }

            // ── dressing_photos: solo per gli eventi inseriti (nuovi pazienti compresi,
            //    position 0 inclusa), con eventId e nome file rimappati. La chiave composita
            //    (eventId|nome) fa coincidere la position 0 con la copertina dell'evento.
            for (dp in dressingPhotos) {
                val newEventId = dMap[dp.eventId] ?: continue
                val newName = remapPhotoName(dp.photoUri, dp.eventId)
                if (newName.isBlank()) continue
                dao.insertDressingPhoto(dp.copy(id = 0, eventId = newEventId, photoUri = newName))
            }

            // ── Scale: inserite solo se (patientId, createdAt) non è già presente in locale
            val nortonSeen = dao.getAllNorton().mapTo(HashSet()) { it.patientId to it.createdAt }
            for (s in norton) {
                val p = pMap[s.patientId] ?: continue
                if (nortonSeen.add(p to s.createdAt)) dao.insertNorton(s.copy(id = 0, patientId = p))
            }
            val nrs2002Seen = dao.getAllNRS2002().mapTo(HashSet()) { it.patientId to it.createdAt }
            for (s in nrs2002) {
                val p = pMap[s.patientId] ?: continue
                if (nrs2002Seen.add(p to s.createdAt)) dao.insertNRS2002(s.copy(id = 0, patientId = p))
            }
            val painNrsSeen = dao.getAllPainNrs().mapTo(HashSet()) { it.patientId to it.createdAt }
            for (s in painNrs) {
                val p = pMap[s.patientId] ?: continue
                if (painNrsSeen.add(p to s.createdAt)) dao.insertPainNrs(s.copy(id = 0, patientId = p))
            }

            // ── Audit del backup: le righe arrivano marcate «[da backup]», così non si confondono
            //    con le operazioni fatte su questo telefono (revisione esterna del 3/11, W3).
            //    Appese solo se non già presenti (reimportare lo stesso file non duplica nulla).
            val auditSeen = dao.getAllAudit().mapTo(HashSet()) { auditKey(it) }
            for (a in backupAudit) {
                val marcata = markImported(a)
                if (auditSeen.add(auditKey(marcata))) dao.insertAudit(marcata)
            }

            // Foto: scritte PRIMA del commit (siamo ancora dentro withTransaction).
            // Se il processo muore durante la scrittura, la transazione non committa e il
            // re-import ripete tutto; se muore dopo il commit restano al più file orfani
            // innocui, MAI record che puntano a foto inesistenti (perdita irreversibile:
            // il re-import li skipperebbe per dedup). Il ramo skip sopra fa comunque da
            // rete di sicurezza riscrivendo i blob dei file mancanti.
            writePhotosFromBackup(context, photosJson, photoDir, photoWrites)
        }
        PhotoManager.clearCache()
        return RestoreResult(newPatients = newPatients, updatedPatients = updatedPatients)
    }

    // ── Helper del merge (matching pazienti) ──

    /** Chiave di confronto del codice fiscale: senza spazi (anche interni) + maiuscolo. */
    private fun patientFiscalKey(fiscalCode: String): String =
        fiscalCode.filterNot { it.isWhitespace() }.uppercase()

    /** Chiave di confronto anagrafica: (cognome, nome, data di nascita) normalizzati. */
    private fun patientNameKey(p: PatientEntity): Triple<String, String, String> =
        Triple(p.surname.trim().lowercase(), p.name.trim().lowercase(), p.birthDate.trim())

    /**
     * Cerca il paziente locale corrispondente a uno del backup: prima per codice
     * fiscale; se il CF non trova nulla (es. il locale non l'ha compilato), fallback
     * sulla terna (cognome, nome, data di nascita). Il match per terna è rifiutato
     * se in locale esistono 2+ omonimi con la stessa terna (ambiguo: meglio un
     * doppione revisionabile che dati clinici fusi nel paziente sbagliato).
     */
    private fun findMatchingLocalPatient(
        backupPatient: PatientEntity,
        localByFiscal: Map<String, PatientEntity>,
        localByName: Map<Triple<String, String, String>, PatientEntity>,
        ambiguousNameKeys: Set<Triple<String, String, String>>
    ): PatientEntity? {
        val fc = patientFiscalKey(backupPatient.fiscalCode)
        if (fc.isNotBlank()) localByFiscal[fc]?.let { return it }
        val nk = patientNameKey(backupPatient)
        if (nk in ambiguousNameKeys) return null
        val candidate = localByName[nk] ?: return null
        // Omonimi con CF DIVERSI sono persone diverse: se sia il backup sia il candidato
        // locale hanno un CF e non coincidono, niente match (il paziente verrà aggiunto).
        val candidateFc = patientFiscalKey(candidate.fiscalCode)
        if (fc.isNotBlank() && candidateFc.isNotBlank() && fc != candidateFc) return null
        return candidate
    }

    /**
     * Scrive (cifrate) le foto contenute in un backup nella cartella foto, senza toccare le
     * esistenti. Ogni foto è salvata col NUOVO nome univoco indicato in [nameMap]: il nome
     * originale del backup è usato solo come chiave di lettura del JSON, mai come nome file
     * su disco (niente path-traversal, niente file condivisi tra import ripetuti).
     * Stesso schema EncryptedFile di PhotoManager: le foto restano decifrabili dall'app.
     */
    private fun writePhotosFromBackup(
        context: Context,
        photos: JSONObject?,
        photoDir: File,
        nameMap: List<Pair<String, String>>
    ) {
        if (photos == null || nameMap.isEmpty()) return
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()

        for ((originalName, newName) in nameMap) {
            try {
                val b64 = photos.getString(originalName)
                val bytes = Base64.decode(b64, Base64.NO_WRAP)

                // Downscale se necessario per evitare OOM (max 2048px)
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (opts.outWidth > 2048 || opts.outHeight > 2048) {
                    var sample = 1
                    while (opts.outWidth / sample > 2048 || opts.outHeight / sample > 2048) sample *= 2
                    opts.inSampleSize = sample
                }
                opts.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: continue
                val destFile = File(photoDir, newName)
                if (destFile.exists()) destFile.delete()  // EncryptedFile non sovrascrive
                val encFile = androidx.security.crypto.EncryptedFile.Builder(
                    context, destFile, masterKey,
                    androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                encFile.openFileOutput().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
            } catch (_: Exception) { /* foto corrotta nel backup, skip */ }
        }
    }

    // ── Crittografia ──

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt, PBKDF2_ITERATIONS)

        // Header in chiaro davanti al ciphertext, autenticato come AAD (v3): un'eventuale
        // manomissione di MAGIC/versione/salt/iv fa fallire il tag GCM (anti-downgrade).
        val header = ByteArrayOutputStream().apply {
            write(MAGIC.toByteArray(Charsets.US_ASCII))
            write(byteArrayOf(VERSION.toByte(), 0, 0, 0)) // version (4 bytes LE)
            write(salt)
            write(iv)
        }.toByteArray()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.updateAAD(header)
        val encrypted = cipher.doFinal(data)

        val output = ByteArrayOutputStream()
        output.write(header)
        output.write(encrypted)
        return output.toByteArray()
    }

    private fun decrypt(data: ByteArray, password: String): ByteArray {
        // Header minimo: MAGIC(4) + version(4) + salt + iv
        if (data.size < 4 + 4 + SALT_LENGTH + IV_LENGTH) {
            throw IllegalArgumentException("File non valido")
        }
        val input = ByteArrayInputStream(data)
        val magic = ByteArray(4).also { input.read(it) }
        if (String(magic, Charsets.US_ASCII) != MAGIC) {
            throw IllegalArgumentException("File non valido")
        }
        val versionBytes = ByteArray(4).also { input.read(it) }
        val version = versionBytes[0].toInt() and 0xFF   // byte senza segno
        val iterations = when (version) {
            1 -> PBKDF2_ITERATIONS_LEGACY_V1
            2 -> PBKDF2_ITERATIONS
            3 -> PBKDF2_ITERATIONS
            else -> throw IllegalArgumentException("Versione backup non supportata: $version")
        }
        val salt = ByteArray(SALT_LENGTH).also { input.read(it) }
        val iv = ByteArray(IV_LENGTH).also { input.read(it) }
        val encrypted = input.readBytes()

        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        // v3: l'header era autenticato come AAD → ricostruiscilo identico e verificalo.
        if (version >= 3) {
            val header = ByteArrayOutputStream().apply {
                write(MAGIC.toByteArray(Charsets.US_ASCII))
                write(byteArrayOf(version.toByte(), 0, 0, 0))
                write(salt)
                write(iv)
            }.toByteArray()
            cipher.updateAAD(header)
        }
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_LENGTH)
        chars.fill('\u0000')
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val keySpec = SecretKeySpec(keyBytes, "AES")
        keyBytes.fill(0)
        return keySpec
    }

    private fun decryptBackup(context: Context, uri: Uri, password: String): JSONObject {
        // Controllo dimensione PRIMA di caricare in memoria (anti-OOM su file scelto dall'utente)
        val declaredSize = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) { -1L }
        if (declaredSize > MAX_BACKUP_FILE_SIZE) {
            throw IOException("File di backup troppo grande (max ${MAX_BACKUP_FILE_SIZE / 1024 / 1024} MB)")
        }
        // Lettura a blocchi con il tetto: vale anche quando Android non dichiara la dimensione (-1).
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { readAtMost(it, MAX_BACKUP_FILE_SIZE) }
            ?: throw IOException("Impossibile leggere il file di backup")
        val compressed = decrypt(rawBytes, password)
        val plaintext = gzipDecompress(compressed)
        return JSONObject(String(plaintext, Charsets.UTF_8))
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(data) }
        return baos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(data)).use { gzis ->
            val buffer = ByteArray(8192)
            var totalRead = 0L
            var bytesRead: Int
            while (gzis.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                if (totalRead > MAX_DECOMPRESSED_SIZE) {
                    throw IOException("Backup troppo grande (max ${MAX_DECOMPRESSED_SIZE / 1024 / 1024} MB)")
                }
                baos.write(buffer, 0, bytesRead)
            }
        }
        return baos.toByteArray()
    }

    // ── Serializzazione JSON ──

    // Legge un JSONArray tollerando chiavi mancanti (retrocompat con backup di versioni precedenti)
    private fun JSONObject.jsonArrayOrEmpty(key: String): JSONArray = optJSONArray(key) ?: JSONArray()

    private fun patientsToJson(list: List<PatientEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("name", e.name); put("surname", e.surname)
                put("fiscalCode", e.fiscalCode); put("birthDate", e.birthDate)
                put("ward", e.ward); put("bed", e.bed); put("notes", e.notes)
                put("createdAt", e.createdAt)
            })
        }
    }

    private fun patientsFromJson(arr: JSONArray): List<PatientEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            PatientEntity(o.getLong("id"), o.getString("name"), o.getString("surname"),
                o.optString("fiscalCode", ""), o.optString("birthDate", ""),
                o.optString("ward", ""), o.optString("bed", ""),
                o.optString("notes", ""), o.getLong("createdAt"))
        }
    }

    internal fun woundsToJson(list: List<WoundAssessmentEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("patientId", e.patientId); put("location", e.location)
                put("name", e.name); put("type", e.type); put("origin", e.origin)
                put("lengthCm", e.lengthCm.toDouble()); put("widthCm", e.widthCm.toDouble())
                put("depthCm", e.depthCm.toDouble()); put("areaCm2", e.areaCm2.toDouble())
                put("notes", e.notes)
                put("typeOther", e.typeOther)
                put("createdAt", e.createdAt)
            })
        }
    }

    internal fun woundsFromJson(arr: JSONArray): List<WoundAssessmentEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            WoundAssessmentEntity(
                id = o.getLong("id"),
                patientId = o.getLong("patientId"),
                location = o.getString("location"),
                name = o.optString("name", ""),
                type = o.getString("type"),
                origin = o.optString("origin", ""),
                lengthCm = o.optDouble("lengthCm", 0.0).toFloat(),
                widthCm = o.optDouble("widthCm", 0.0).toFloat(),
                depthCm = o.optDouble("depthCm", 0.0).toFloat(),
                areaCm2 = o.optDouble("areaCm2", 0.0).toFloat(),
                notes = o.optString("notes", ""),
                typeOther = o.optString("typeOther", ""),
                createdAt = o.getLong("createdAt")
            )
        }
    }

    internal fun dressingPhotosToJson(list: List<DressingPhotoEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("eventId", e.eventId); put("photoUri", e.photoUri); put("position", e.position)
            })
        }
    }

    /**
     * Righe dressing_photos del backup. I backup precedenti non hanno la sezione
     * "dressingPhotos": in quel caso viene sintetizzata la riga position 0 (copertina)
     * dal photoUri di ogni evento, così anche gli import vecchi popolano la tabella.
     */
    internal fun dressingPhotosFromJson(
        json: JSONObject,
        dressings: List<DressingEventEntity>
    ): List<DressingPhotoEntity> {
        val arr = json.optJSONArray("dressingPhotos")
        if (arr != null) {
            return (0 until arr.length()).map { i ->
                arr.getJSONObject(i).let { o ->
                    DressingPhotoEntity(
                        eventId = o.getLong("eventId"),
                        photoUri = o.optString("photoUri", ""),
                        position = o.optInt("position", 0)
                    )
                }
            }
        }
        return dressings.filter { it.photoUri.isNotBlank() }
            .map { DressingPhotoEntity(eventId = it.id, photoUri = it.photoUri, position = 0) }
    }

    internal fun dressingsToJson(list: List<DressingEventEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("assessmentId", e.assessmentId)
                put("photoUri", e.photoUri); put("roiData", e.roiData)
                put("tissueBlackPercent", e.tissueBlackPercent.toDouble())
                put("tissueYellowPercent", e.tissueYellowPercent.toDouble())
                put("tissueRedPercent", e.tissueRedPercent.toDouble())
                put("tissuePinkPercent", e.tissuePinkPercent.toDouble())
                put("exudateLevel", e.exudateLevel); put("exudateType", e.exudateType)
                // painNrs null = dolore non rilevato: nel JSON la chiave viene omessa
                e.painNrs?.let { put("painNrs", it) }
                put("dressingType", e.dressingType)
                put("primaryDressing", e.primaryDressing); put("secondaryDressing", e.secondaryDressing)
                put("notes", e.notes)
                put("lengthCm", e.lengthCm.toDouble()); put("widthCm", e.widthCm.toDouble()); put("depthCm", e.depthCm.toDouble())
                put("createdAt", e.createdAt)
            })
        }
    }

    internal fun dressingsFromJson(arr: JSONArray): List<DressingEventEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            DressingEventEntity(o.getLong("id"), o.getLong("assessmentId"),
                o.optString("photoUri", ""), o.optString("roiData", ""),
                o.optDouble("tissueBlackPercent", 0.0).toFloat(),
                o.optDouble("tissueYellowPercent", 0.0).toFloat(),
                o.optDouble("tissueRedPercent", 0.0).toFloat(),
                o.optDouble("tissuePinkPercent", 0.0).toFloat(),
                o.optString("exudateLevel", ""), o.optString("exudateType", ""),
                // Nei backup pre-v11 painNrs c'era sempre (int); nei nuovi manca se non rilevato.
                if (o.has("painNrs")) o.optInt("painNrs", 0) else null,
                o.optString("dressingType", ""),
                o.optString("primaryDressing", ""), o.optString("secondaryDressing", ""),
                o.optString("notes", ""),
                o.optDouble("lengthCm", 0.0).toFloat(),
                o.optDouble("widthCm", 0.0).toFloat(),
                o.optDouble("depthCm", 0.0).toFloat(),
                o.getLong("createdAt"))
        }
    }

    private fun nortonToJson(list: List<NortonAssessmentEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("patientId", e.patientId)
                put("condizioneFisica", e.condizioneFisica); put("statoMentale", e.statoMentale)
                put("attivita", e.attivita); put("mobilita", e.mobilita)
                put("incontinenza", e.incontinenza); put("totalScore", e.totalScore)
                put("notes", e.notes); put("createdAt", e.createdAt)
            })
        }
    }

    private fun nortonFromJson(arr: JSONArray): List<NortonAssessmentEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            NortonAssessmentEntity(o.getLong("id"), o.getLong("patientId"),
                o.getInt("condizioneFisica"), o.getInt("statoMentale"),
                o.getInt("attivita"), o.getInt("mobilita"), o.getInt("incontinenza"),
                o.getInt("totalScore"), o.optString("notes", ""), o.getLong("createdAt"))
        }
    }





    private fun nrs2002ToJson(list: List<NRS2002AssessmentEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("patientId", e.patientId)
                put("nutritionalScore", e.nutritionalScore)
                put("diseaseSeverityScore", e.diseaseSeverityScore)
                put("ageAdjustment", e.ageAdjustment)
                put("totalScore", e.totalScore)
                put("notes", e.notes); put("createdAt", e.createdAt)
            })
        }
    }

    private fun nrs2002FromJson(arr: JSONArray): List<NRS2002AssessmentEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            NRS2002AssessmentEntity(o.getLong("id"), o.getLong("patientId"),
                o.getInt("nutritionalScore"), o.getInt("diseaseSeverityScore"),
                o.getInt("ageAdjustment"), o.getInt("totalScore"),
                o.optString("notes", ""), o.getLong("createdAt"))
        }
    }

    private fun painNrsToJson(list: List<PainNRSAssessmentEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("patientId", e.patientId)
                put("painScore", e.painScore); put("painLocation", e.painLocation)
                put("painType", e.painType); put("painCharacter", e.painCharacter)
                put("aggravatingFactors", e.aggravatingFactors)
                put("relievingFactors", e.relievingFactors)
                put("notes", e.notes); put("createdAt", e.createdAt)
            })
        }
    }

    private fun painNrsFromJson(arr: JSONArray): List<PainNRSAssessmentEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            PainNRSAssessmentEntity(o.getLong("id"), o.getLong("patientId"),
                o.getInt("painScore"), o.getString("painLocation"),
                o.getString("painType"), o.getString("painCharacter"),
                o.getString("aggravatingFactors"), o.getString("relievingFactors"),
                o.optString("notes", ""), o.getLong("createdAt"))
        }
    }

    private fun auditToJson(list: List<AuditLogEntity>) = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("timestamp", e.timestamp)
                put("action", e.action); put("entityType", e.entityType)
                put("entityId", e.entityId); put("details", e.details)
            })
        }
    }

    private fun auditFromJson(arr: JSONArray): List<AuditLogEntity> = (0 until arr.length()).map { i ->
        arr.getJSONObject(i).let { o ->
            AuditLogEntity(o.getLong("id"), o.getLong("timestamp"),
                o.getString("action"), o.getString("entityType"),
                o.optLong("entityId", 0), o.optString("details", ""))
        }
    }
}
