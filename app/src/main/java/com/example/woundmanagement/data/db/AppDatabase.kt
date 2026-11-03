package com.example.woundmanagement.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.woundmanagement.data.db.dao.*
import com.example.woundmanagement.data.db.entity.*
import com.example.woundmanagement.security.DatabaseKeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        PatientEntity::class,
        WoundAssessmentEntity::class,
        DressingEventEntity::class,
        DressingPhotoEntity::class,
        AuditLogEntity::class,
        NortonAssessmentEntity::class,
        NRS2002AssessmentEntity::class,
        PainNRSAssessmentEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun woundAssessmentDao(): WoundAssessmentDao
    abstract fun dressingEventDao(): DressingEventDao
    abstract fun dressingPhotoDao(): DressingPhotoDao
    abstract fun auditDao(): AuditDao
    abstract fun nortonDao(): NortonDao
    abstract fun nrs2002Dao(): NRS2002Dao
    abstract fun painNrsDao(): PainNRSDao
    abstract fun backupDao(): BackupDao

    companion object {
        private const val DB_NAME = "wound_management_mdr.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Chiude l'istanza del DB e la azzera: i dati cifrati non restano decifrati in
         * memoria del processo dopo il logout/timeout. Il prossimo getInstance la riapre
         * (solo post-login). Idempotente e tollerante a operazioni in volo.
         */
        fun closeInstance() {
            synchronized(this) {
                runCatching { INSTANCE?.close() }
                INSTANCE = null
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

            // Se esiste un vecchio DB non cifrato, lo elimina per ricominciare cifrato.
            // (fallback sicuro: mai aprire dati in chiaro)
            deleteIfUnencrypted(context, passphrase)

            // Migra i database legacy cifrati con la chiave nulla (bug storico: la
            // passphrase veniva azzerata PRIMA che SupportFactory aprisse il DB, perché
            // la factory ne tiene il riferimento e apre in modo lazy). Re-cifra con la
            // passphrase reale prima che Room apra il database.
            migrateZeroKeyIfNeeded(context, passphrase)

            // NB: NON azzerare qui la passphrase. SupportFactory ne tiene il riferimento
            // e apre il DB in modo lazy: la azzera da sé (clearPassphrase=true) DOPO l'open.
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                // Evita crash su downgrade o su schemi pre-4 (inesistenti in questo fork):
                // distruttivo SOLO in quei casi teorici, mai su un normale mismatch.
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigrationFrom(1, 2, 3)
                .build()
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wound_assessments ADD COLUMN name TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // MNA: campi consumoProteine, liquidi, autovalutazioneSalute, circonferenzaBraccio, totalScore
                // da INTEGER a REAL per supportare mezzi punti (0.5) come da scala validata
                db.execSQL("""CREATE TABLE mna_assessments_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId INTEGER NOT NULL,
                    appetitoRidotto INTEGER NOT NULL,
                    perditaPeso INTEGER NOT NULL,
                    mobilita INTEGER NOT NULL,
                    stressPsicologico INTEGER NOT NULL,
                    problemiNeuropsicologici INTEGER NOT NULL,
                    bmi INTEGER NOT NULL,
                    viveIndipendente INTEGER NOT NULL,
                    farmaciGiorno INTEGER NOT NULL,
                    lesioni INTEGER NOT NULL,
                    pastiCompleti INTEGER NOT NULL,
                    consumoProteine REAL NOT NULL DEFAULT 0,
                    fruttaVerdura INTEGER NOT NULL,
                    liquidi REAL NOT NULL DEFAULT 0,
                    modalitaNutrizione INTEGER NOT NULL,
                    autovalutazioneNutrizionale INTEGER NOT NULL,
                    autovalutazioneSalute REAL NOT NULL DEFAULT 0,
                    circonferenzaBraccio REAL NOT NULL DEFAULT 0,
                    circonferenzaPolpaccio INTEGER NOT NULL,
                    screeningScore INTEGER NOT NULL,
                    totalScore REAL NOT NULL DEFAULT 0,
                    status TEXT NOT NULL,
                    notes TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(patientId) REFERENCES patients(id) ON DELETE CASCADE
                )""")
                db.execSQL("""INSERT INTO mna_assessments_new SELECT
                    id, patientId, appetitoRidotto, perditaPeso, mobilita, stressPsicologico,
                    problemiNeuropsicologici, bmi, viveIndipendente, farmaciGiorno, lesioni,
                    pastiCompleti, CAST(consumoProteine AS REAL), fruttaVerdura,
                    CAST(liquidi AS REAL), modalitaNutrizione, autovalutazioneNutrizionale,
                    CAST(autovalutazioneSalute AS REAL), CAST(circonferenzaBraccio AS REAL),
                    circonferenzaPolpaccio, screeningScore, CAST(totalScore AS REAL),
                    status, notes, createdAt FROM mna_assessments""")
                db.execSQL("DROP TABLE mna_assessments")
                db.execSQL("ALTER TABLE mna_assessments_new RENAME TO mna_assessments")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mna_assessments_patientId ON mna_assessments(patientId)")
            }
        }

        /**
         * MDR compliance: rimuove le colonne interpretative (categoria di rischio/stato)
         * da tutte le scale. Ricrea ogni tabella senza quella colonna, preservando i dati grezzi.
         * Lo schema delle nuove tabelle corrisponde esattamente a quello atteso da Room per la v7.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val recreations = listOf(
                    Triple(
                        "braden_assessments",
                        "CREATE TABLE IF NOT EXISTS `braden_assessments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `percezioneSensoriale` INTEGER NOT NULL, `umidita` INTEGER NOT NULL, `attivita` INTEGER NOT NULL, `mobilita` INTEGER NOT NULL, `nutrizione` INTEGER NOT NULL, `frizioneEScivolamento` INTEGER NOT NULL, `totalScore` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                        "`id`, `patientId`, `percezioneSensoriale`, `umidita`, `attivita`, `mobilita`, `nutrizione`, `frizioneEScivolamento`, `totalScore`, `notes`, `createdAt`"
                    ),
                    Triple(
                        "mna_assessments",
                        "CREATE TABLE IF NOT EXISTS `mna_assessments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `appetitoRidotto` INTEGER NOT NULL, `perditaPeso` INTEGER NOT NULL, `mobilita` INTEGER NOT NULL, `stressPsicologico` INTEGER NOT NULL, `problemiNeuropsicologici` INTEGER NOT NULL, `bmi` INTEGER NOT NULL, `viveIndipendente` INTEGER NOT NULL, `farmaciGiorno` INTEGER NOT NULL, `lesioni` INTEGER NOT NULL, `pastiCompleti` INTEGER NOT NULL, `consumoProteine` REAL NOT NULL, `fruttaVerdura` INTEGER NOT NULL, `liquidi` REAL NOT NULL, `modalitaNutrizione` INTEGER NOT NULL, `autovalutazioneNutrizionale` INTEGER NOT NULL, `autovalutazioneSalute` REAL NOT NULL, `circonferenzaBraccio` REAL NOT NULL, `circonferenzaPolpaccio` INTEGER NOT NULL, `screeningScore` INTEGER NOT NULL, `totalScore` REAL NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                        "`id`, `patientId`, `appetitoRidotto`, `perditaPeso`, `mobilita`, `stressPsicologico`, `problemiNeuropsicologici`, `bmi`, `viveIndipendente`, `farmaciGiorno`, `lesioni`, `pastiCompleti`, `consumoProteine`, `fruttaVerdura`, `liquidi`, `modalitaNutrizione`, `autovalutazioneNutrizionale`, `autovalutazioneSalute`, `circonferenzaBraccio`, `circonferenzaPolpaccio`, `screeningScore`, `totalScore`, `notes`, `createdAt`"
                    ),
                    Triple(
                        "must_assessments",
                        "CREATE TABLE IF NOT EXISTS `must_assessments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `bmiScore` INTEGER NOT NULL, `perditaPesoScore` INTEGER NOT NULL, `effettoMalattiaScore` INTEGER NOT NULL, `totalScore` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                        "`id`, `patientId`, `bmiScore`, `perditaPesoScore`, `effettoMalattiaScore`, `totalScore`, `notes`, `createdAt`"
                    ),
                    Triple(
                        "barthel_assessments",
                        "CREATE TABLE IF NOT EXISTS `barthel_assessments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `alimentazione` INTEGER NOT NULL, `bagno` INTEGER NOT NULL, `igienePersonale` INTEGER NOT NULL, `vestirsi` INTEGER NOT NULL, `continenzaIntestinale` INTEGER NOT NULL, `continenzaUrinaria` INTEGER NOT NULL, `toilette` INTEGER NOT NULL, `trasferimenti` INTEGER NOT NULL, `deambulazione` INTEGER NOT NULL, `scale` INTEGER NOT NULL, `totalScore` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                        "`id`, `patientId`, `alimentazione`, `bagno`, `igienePersonale`, `vestirsi`, `continenzaIntestinale`, `continenzaUrinaria`, `toilette`, `trasferimenti`, `deambulazione`, `scale`, `totalScore`, `notes`, `createdAt`"
                    ),
                    Triple(
                        "conley_assessments",
                        "CREATE TABLE IF NOT EXISTS `conley_assessments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `cadutePrecedenti` INTEGER NOT NULL, `vertigini` INTEGER NOT NULL, `agitazione` INTEGER NOT NULL, `deficitVisivi` INTEGER NOT NULL, `frequenzaMinzione` INTEGER NOT NULL, `debolezzaGenerale` INTEGER NOT NULL, `totalScore` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                        "`id`, `patientId`, `cadutePrecedenti`, `vertigini`, `agitazione`, `deficitVisivi`, `frequenzaMinzione`, `debolezzaGenerale`, `totalScore`, `notes`, `createdAt`"
                    ),
                    Triple(
                        "pain_nrs_assessments",
                        "CREATE TABLE IF NOT EXISTS `pain_nrs_assessments_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `painScore` INTEGER NOT NULL, `painLocation` TEXT NOT NULL, `painType` TEXT NOT NULL, `painCharacter` TEXT NOT NULL, `aggravatingFactors` TEXT NOT NULL, `relievingFactors` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                        "`id`, `patientId`, `painScore`, `painLocation`, `painType`, `painCharacter`, `aggravatingFactors`, `relievingFactors`, `notes`, `createdAt`"
                    )
                )
                for ((table, createNew, cols) in recreations) {
                    db.execSQL(createNew)
                    db.execSQL("INSERT INTO `${table}_new` ($cols) SELECT $cols FROM `$table`")
                    db.execSQL("DROP TABLE `$table`")
                    db.execSQL("ALTER TABLE `${table}_new` RENAME TO `$table`")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_patientId` ON `$table` (`patientId`)")
                }
            }
        }

        /**
         * Aggiunge la scala NRS-2002 (Nutritional Risk Screening, ESPEN/Kondrup 2003).
         * Crea la tabella nrs2002_assessments + indice su patientId.
         * Lo schema della CREATE corrisponde esattamente a quello atteso da Room per la v8
         * (stesse convenzioni di tipi/NOT NULL/FK usate per le altre scale nella v7).
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `nrs2002_assessments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patientId` INTEGER NOT NULL, `nutritionalScore` INTEGER NOT NULL, `diseaseSeverityScore` INTEGER NOT NULL, `ageAdjustment` INTEGER NOT NULL, `totalScore` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_nrs2002_assessments_patientId` ON `nrs2002_assessments` (`patientId`)")
            }
        }

        /**
         * Rimuove le 4 scale coperte da copyright (Braden, BWAT, MNA, MUST): elimina
         * le rispettive tabelle. I dati grezzi non interpretativi delle altre scale
         * (Norton, Conley, Barthel, NRS-2002, PainNRS) restano invariati.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS braden_assessments")
                db.execSQL("DROP TABLE IF EXISTS bwat_assessments")
                db.execSQL("DROP TABLE IF EXISTS mna_assessments")
                db.execSQL("DROP TABLE IF EXISTS must_assessments")
            }
        }

        /**
         * Rimuove i sottosistemi di codice morto: la tabella dei residui di analisi immagine
         * e le tabelle TIME/TIMERS (pseudo-scala non validata). Solo DROP, nessun dato clinico
         * delle scale mantenute viene toccato.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS image_analysis_results")
                db.execSQL("DROP TABLE IF EXISTS time_assessments")
                db.execSQL("DROP TABLE IF EXISTS timers_assessments")
            }
        }

        /**
         * v11 — (1) note/anamnesi sulla lesione; (2) rimozione scale Barthel (copyright
         * MedChi, uso commerciale non consentito senza licenza) e "rischio cadute"
         * (strumento interno non validato, estraneo alla documentazione ferite);
         * (3) dressing_events: painNrs diventa nullable (null = non rilevato, distinto
         * da 0 = nessun dolore) + campi strutturati exudateType/primaryDressing/
         * secondaryDressing. Table-recreate: i dati esistenti sono preservati
         * (painNrs storici restano valorizzati, campi nuovi = '').
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wound_assessments ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP TABLE IF EXISTS barthel_assessments")
                db.execSQL("DROP TABLE IF EXISTS conley_assessments")
                db.execSQL("CREATE TABLE IF NOT EXISTS `dressing_events_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `assessmentId` INTEGER NOT NULL, `photoUri` TEXT NOT NULL, `roiData` TEXT NOT NULL, `tissueBlackPercent` REAL NOT NULL, `tissueYellowPercent` REAL NOT NULL, `tissueRedPercent` REAL NOT NULL, `tissuePinkPercent` REAL NOT NULL, `exudateLevel` TEXT NOT NULL, `exudateType` TEXT NOT NULL, `painNrs` INTEGER, `dressingType` TEXT NOT NULL, `primaryDressing` TEXT NOT NULL, `secondaryDressing` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`assessmentId`) REFERENCES `wound_assessments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `dressing_events_new` (`id`, `assessmentId`, `photoUri`, `roiData`, `tissueBlackPercent`, `tissueYellowPercent`, `tissueRedPercent`, `tissuePinkPercent`, `exudateLevel`, `exudateType`, `painNrs`, `dressingType`, `primaryDressing`, `secondaryDressing`, `notes`, `createdAt`) SELECT `id`, `assessmentId`, `photoUri`, `roiData`, `tissueBlackPercent`, `tissueYellowPercent`, `tissueRedPercent`, `tissuePinkPercent`, `exudateLevel`, '', `painNrs`, `dressingType`, '', '', `notes`, `createdAt` FROM `dressing_events`")
                db.execSQL("DROP TABLE `dressing_events`")
                db.execSQL("ALTER TABLE `dressing_events_new` RENAME TO `dressing_events`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dressing_events_assessmentId` ON `dressing_events` (`assessmentId`)")
            }
        }

        /**
         * v12 — (1) wound_assessments: colonna typeOther (descrizione libera del tipo
         * quando type == "ALTRO"); (2) nuova tabella dressing_photos per le foto
         * multiple delle medicazioni (position 0 = copertina, che resta anche in
         * dressing_events.photoUri). Le foto esistenti vengono copiate nella nuova
         * tabella come position 0: nessun dato viene perso o modificato.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wound_assessments ADD COLUMN typeOther TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS `dressing_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `eventId` INTEGER NOT NULL, `photoUri` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`eventId`) REFERENCES `dressing_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dressing_photos_eventId` ON `dressing_photos` (`eventId`)")
                db.execSQL("INSERT INTO `dressing_photos` (`eventId`, `photoUri`, `position`) SELECT `id`, `photoUri`, 0 FROM `dressing_events` WHERE `photoUri` != ''")
            }
        }

        /**
         * v13 — misure della lesione per ogni medicazione (evoluzione nel tempo):
         * lengthCm/widthCm/depthCm su dressing_events (0 = non misurata). Inserimento
         * manuale, pura documentazione. ALTER additivo, non distruttivo.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dressing_events ADD COLUMN lengthCm REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dressing_events ADD COLUMN widthCm REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dressing_events ADD COLUMN depthCm REAL NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migrazione di sicurezza una-tantum: i database creati prima del fix erano
         * cifrati con una chiave di 32 byte nulli (la passphrase reale veniva azzerata
         * prima dell'apertura). Qui rileviamo quel caso e ri-cifriamo con la passphrase
         * reale tramite PRAGMA rekey, senza perdere i dati.
         *
         * Strategia (non distruttiva, idempotente):
         *  1. prova ad aprire con la passphrase reale → se funziona, nulla da fare;
         *  2. altrimenti prova con la chiave nulla legacy → se funziona, fa il rekey;
         *  3. se nessuna delle due apre, lascia il file intatto (Room gestirà l'errore).
         *
         * Il rekey usa la passphrase come char[] ASCII: poiché la passphrase è la
         * stringa Base64 (solo ASCII), getBytes(char[]) in UTF-8 produce esattamente
         * gli stessi byte passati a SupportFactory(byte[]), garantendo che il DB resti
         * apribile dopo la migrazione.
         */
        private const val MIGRATION_FLAGS = "db_migration_flags"
        private const val FLAG_ZEROKEY_MIGRATED = "zerokey_migrated_v2"

        private fun migrateZeroKeyIfNeeded(context: Context, passphrase: ByteArray) {
            val flags = context.getSharedPreferences(MIGRATION_FLAGS, Context.MODE_PRIVATE)
            if (flags.getBoolean(FLAG_ZEROKEY_MIGRATED, false)) return

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                // Installazione nuova: il DB verrà creato cifrato con la passphrase reale.
                flags.edit().putBoolean(FLAG_ZEROKEY_MIGRATED, true).apply()
                return
            }
            SQLiteDatabase.loadLibs(context)

            // 1. La passphrase reale apre già il DB? (installazioni già corrette)
            if (opensWith(dbFile.path, passphrase)) {
                flags.edit().putBoolean(FLAG_ZEROKEY_MIGRATED, true).apply()
                return
            }

            // 2. Il DB è quello legacy cifrato a zeri?
            val zeroKey = ByteArray(32)
            var db: SQLiteDatabase? = null
            try {
                db = SQLiteDatabase.openOrCreateDatabase(dbFile.path, zeroKey, null)
                // Forza la validazione della chiave: con chiave errata qui lancia.
                db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
                // 3. Rekey alla passphrase reale (char[] ASCII = stessi byte di SupportFactory).
                db.changePassword(String(passphrase, Charsets.US_ASCII).toCharArray())
                flags.edit().putBoolean(FLAG_ZEROKEY_MIGRATED, true).apply()
            } catch (_: Exception) {
                // Né reale né zero: file non riconducibile a questo schema di chiavi.
                // Lo lasciamo intatto (nessun flag): si riproverà al prossimo avvio.
            } finally {
                db?.close()
            }
        }

        /** Apre il file col dato key e tenta una query: true se la chiave è valida. */
        private fun opensWith(path: String, key: ByteArray): Boolean {
            var db: SQLiteDatabase? = null
            return try {
                db = SQLiteDatabase.openOrCreateDatabase(path, key.copyOf(), null)
                db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
                true
            } catch (_: Exception) {
                false
            } finally {
                db?.close()
            }
        }

        /**
         * Controlla se il DB è un SQLite non cifrato leggendo il suo header.
         * I file SQLite in chiaro iniziano con "SQLite format 3\u0000".
         * Se trovato, lo elimina: verrà ricreato cifrato con SQLCipher.
         * Cancella SOLO se l'header in chiaro è stato letto positivamente: un errore
         * I/O transitorio NON deve costare il database (C10c); in quel caso il file
         * resta intatto e saranno Room/SQLCipher a gestire l'apertura.
         */
        private fun deleteIfUnencrypted(context: Context, @Suppress("UNUSED_PARAMETER") passphrase: ByteArray) {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return
            try {
                val header = ByteArray(16)
                val read = dbFile.inputStream().use { it.read(header) }
                if (read >= 15 && String(header, 0, read, Charsets.ISO_8859_1).startsWith("SQLite format 3")) {
                    dbFile.delete()
                    context.getDatabasePath("$DB_NAME-shm").delete()
                    context.getDatabasePath("$DB_NAME-wal").delete()
                }
            } catch (_: Exception) {
                // Errore di lettura (transitorio o no): NON cancellare. Meglio un errore
                // all'apertura che perdere dati per un falso positivo.
            }
        }
    }
}
