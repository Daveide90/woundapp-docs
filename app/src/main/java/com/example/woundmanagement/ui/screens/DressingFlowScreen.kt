package com.example.woundmanagement.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.woundmanagement.data.db.entity.DressingEventEntity
import com.example.woundmanagement.domain.imaging.BitmapUtils
import com.example.woundmanagement.domain.model.PrimaryDressing
import com.example.woundmanagement.domain.model.SecondaryDressing
import com.example.woundmanagement.domain.model.ExudateLevel
import com.example.woundmanagement.domain.model.ExudateType
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4
import com.example.woundmanagement.ui.components.*
import com.example.woundmanagement.ui.viewmodel.DressingFlowViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.example.woundmanagement.security.DressingDraftManager
import com.example.woundmanagement.security.PhotoManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DressingFlowScreen(
    assessmentId: Long,
    onSave: (DressingEventEntity, List<String>) -> Unit,
    onBack: () -> Unit,
    saveState: DressingFlowViewModel.SaveState = DressingFlowViewModel.SaveState.Idle
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    // Foto aggiuntive (angolazioni 2..4): la principale resta photoUri (copertina + ROI)
    var extraPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var maskedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Nessun ritaglio: le foto si documentano intere. La colonna roiData del DB resta
    // nello schema solo per compatibilità con i record storici e viene salvata vuota.

    var necroticPercent by remember { mutableFloatStateOf(0f) }
    var sloughPercent by remember { mutableFloatStateOf(0f) }
    var granulationPercent by remember { mutableFloatStateOf(0f) }
    var epithelialPercent by remember { mutableFloatStateOf(0f) }

    // Default "non specificato": nessun valore clinico viene pre-impostato al posto
    // dell'operatore (integrità del dato documentale).
    var exudateLevel by remember { mutableStateOf(ExudateLevel.NON_SPECIFICATO) }
    var exudateType by remember { mutableStateOf(ExudateType.NON_SPECIFICATO) }
    var painDetected by remember { mutableStateOf(false) }
    var painNrs by remember { mutableIntStateOf(0) }
    var primaryDressing by remember { mutableStateOf(PrimaryDressing.NON_SPECIFICATA) }
    var primaryDressingOther by remember { mutableStateOf("") }
    var secondaryDressing by remember { mutableStateOf(SecondaryDressing.NON_SPECIFICATA) }
    var secondaryDressingOther by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf<Long?>(null) }   // null = adesso
    // Misure della lesione a questa medicazione (per seguirne l'evoluzione). Facoltative.
    var measLength by remember { mutableStateOf("") }
    var measWidth by remember { mutableStateOf("") }
    var measDepth by remember { mutableStateOf("") }
    var measDepthUnit by remember { mutableStateOf("mm") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val lang = LocalLanguage.current
    val strPhoto = S.stepPhoto
    val strNotes = S.stepNotes
    val tissueTotal = necroticPercent + sloughPercent + granulationPercent + epithelialPercent
    // 3 tappe: Foto (multi-caricamento) → Tessuto → Note. Il ritaglio della lesione
    // è opzionale e non compare come tappa.
    val steps = listOf(
        StepInfo(strPhoto, completed = photoUri != null),
        StepInfo(loc4(it = "Tessuto", en = "Tissue", fr = "Tissu", es = "Tejido"), completed = tissueTotal > 0f, locked = photoUri == null),
        StepInfo(strNotes, completed = false, locked = tissueTotal <= 0f)
    )

    // Dati non salvati: l'utente ha inserito foto/ROI/valori/note. Se sta salvando
    // (saveState == Saving) lascia procedere il flusso senza chiedere conferma.
    val hasUnsavedData = saveState !is DressingFlowViewModel.SaveState.Saving && (
        photoUri != null ||
        extraPhotoUris.isNotEmpty() ||
        tissueTotal > 0f ||
        notes.isNotBlank() ||
        painDetected ||
        eventDate != null ||
        measLength.isNotBlank() || measWidth.isNotBlank() || measDepth.isNotBlank() ||
        primaryDressingOther.isNotBlank() ||
        secondaryDressingOther.isNotBlank()
    )
    var showExitDialog by remember { mutableStateOf(false) }
    // Indietro guardato: se ci sono dati non salvati chiede conferma, altrimenti esce subito.
    val requestBack = { if (hasUnsavedData) showExitDialog = true else onBack() }

    // Back di sistema SEMPRE intercettato e instradato su requestBack, come la freccia in
    // top-bar: così anche il gesto di sistema (allo step 0, senza dati) chiama onBack →
    // la lesione appena creata e abbandonata viene rimossa dal NavHost.
    BackHandler(enabled = true) {
        if (currentStep > 0) currentStep-- else requestBack()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Messaggi pre-risolti in scope @Composable (usati in callback non-@Composable).
    val galleryErr = loc4(
        it = "Errore nel caricare le foto dalla galleria.",
        en = "Error loading photos from gallery.",
        fr = "Erreur lors du chargement des photos depuis la galerie.",
        es = "Error al cargar las fotos de la galería."
    )
    val photoMaxMsg = loc4(
        it = "Massimo ${MAX_DRESSING_PHOTOS} foto per medicazione",
        en = "Maximum ${MAX_DRESSING_PHOTOS} photos per dressing",
        fr = "Maximum ${MAX_DRESSING_PHOTOS} photos par pansement",
        es = "Máximo ${MAX_DRESSING_PHOTOS} fotos por cura"
    )

    // Caricamento MULTIPLO dalla galleria: la prima foto libera diventa la principale
    // (copertina), le altre le angolazioni, fino a MAX_DRESSING_PHOTOS totali.
    val pickPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_DRESSING_PHOTOS)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            // Copia SOLO le foto che entrano nel tetto: calcolando prima gli slot residui
            // non scriviamo mai su disco file che verrebbero poi scartati (anti-orfani).
            val currentTotal = (if (photoUri != null) 1 else 0) + extraPhotoUris.size
            val slots = (MAX_DRESSING_PHOTOS - currentTotal).coerceAtLeast(0)
            val overflow = uris.size > slots
            val toCopy = uris.take(slots)
            if (toCopy.isEmpty()) {
                if (overflow) snackbarHostState.showSnackbar(photoMaxMsg)
                return@launch
            }
            // 'copied' è condivisa col blocco IO: se la schermata viene abbandonata
            // durante la copia, il finally cancella i file già scritti ma mai referenziati.
            val copied = mutableListOf<Uri>()
            var committed = false
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    for (u in toCopy) BitmapUtils.copyToInternalStorage(context, u)?.let { copied.add(it) }
                }
                if (copied.isEmpty()) { errorMsg = galleryErr; return@launch }
                // La prima foto (se non c'è già una copertina) diventa principale, le altre
                // angolazioni. Il bitmap di riferimento per i tessuti si carica poi in "Avanti".
                var main = photoUri
                val extras = extraPhotoUris.toMutableList()
                for (u in copied) { if (main == null) main = u else extras.add(u) }
                photoUri = main
                extraPhotoUris = extras
                errorMsg = null
                committed = true
                if (overflow) snackbarHostState.showSnackbar(photoMaxMsg)
            } finally {
                // Coroutine cancellata (composable uscita di scena) prima di referenziare
                // i file: le copie orfane vanno cancellate su uno scope che sopravvive.
                if (!committed && copied.isNotEmpty()) {
                    val orphans = copied.map { it.toString() }
                    com.example.woundmanagement.WoundManagementApp.instance.applicationScope
                        .launch(kotlinx.coroutines.Dispatchers.IO) { deleteAbandonedPhotoFiles(orphans) }
                }
            }
        }
    }
    val launchPhotoPicker = {
        pickPhotosLauncher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    // Le foto come UNICA lista ordinata: la posizione 0 è la copertina (principale),
    // le altre sono le angolazioni. Il drag & drop riordina questa lista.
    val photos = listOfNotNull(photoUri) + extraPhotoUris

    // "Avanti" dallo step Foto carica il bitmap della copertina in modo asincrono:
    // finché è in corso disabilitiamo il pulsante ed evitiamo decodifiche doppie.
    var advancingFromPhoto by remember { mutableStateOf(false) }

    // Riordino (drag & drop): la nuova prima foto diventa la copertina.
    val reorderPhotos: (List<Uri>) -> Unit = { newOrder ->
        photoUri = newOrder.firstOrNull()
        extraPhotoUris = newOrder.drop(1)
    }

    // Rimozione di una foto: via dalla lista + cancellazione best-effort del file.
    // Cancellazione incondizionata: questo flusso è solo-creazione (le foto sono copie
    // fresche non ancora referenziate da alcun DressingEventEntity salvato). Se in futuro
    // lo schermo servirà anche a MODIFICARE una medicazione salvata, va aggiunta la guardia
    // per non cancellare un file già referenziato dal record persistito.
    val removePhoto: (Uri) -> Unit = { uri ->
        val newOrder = photos.filter { it != uri }
        photoUri = newOrder.firstOrNull()
        extraPhotoUris = newOrder.drop(1)
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            deleteAbandonedPhotoFiles(listOf(uri.toString()))
        }
    }

    // ── Bozza anti-timeout: si salva da sola e si ripropone al rientro ──
    var pendingDraft by remember { mutableStateOf<DressingDraftManager.Draft?>(null) }
    LaunchedEffect(Unit) {
        pendingDraft = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { DressingDraftManager.load(context, assessmentId) }.getOrNull()
        }
    }
    // Autosalvataggio con debounce: ogni modifica rilevante finisce nella bozza cifrata.
    LaunchedEffect(Unit) {
        snapshotFlow {
            DressingDraftManager.Draft(
                step = currentStep,
                photoUri = photoUri?.toString(),
                necrotic = necroticPercent, slough = sloughPercent,
                granulation = granulationPercent, epithelial = epithelialPercent,
                exudate = exudateLevel.name, exudateType = exudateType.name,
                painDetected = painDetected, painNrs = painNrs,
                primary = primaryDressing.name, primaryOther = primaryDressingOther,
                secondary = secondaryDressing.name, secondaryOther = secondaryDressingOther,
                notes = notes, eventDate = eventDate,
                extraPhotoUris = extraPhotoUris.map { it.toString() },
                measLength = measLength, measWidth = measWidth,
                measDepth = measDepth, measDepthUnit = measDepthUnit
            )
        }.collectLatest { draft ->
            kotlinx.coroutines.delay(800)
            // La foto è l'ancora del flusso: senza foto non c'è nulla da riprendere.
            if (draft.photoUri != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { DressingDraftManager.save(context, assessmentId, draft) }
                }
            }
        }
    }

    val tissueWarning = loc4(
        it = "Attenzione: la somma dei tessuti è ${tissueTotal.toInt()}%, non 100%",
        en = "Note: tissue total is ${tissueTotal.toInt()}%, not 100%",
        fr = "Attention : le total des tissus est de ${tissueTotal.toInt()} %, pas 100 %",
        es = "Atención: la suma de los tejidos es ${tissueTotal.toInt()}%, no 100%"
    )

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(S.dressingTitle)
                    PatientSubtitle(rememberPatientLabelForAssessment(assessmentId))
                }
            },
            navigationIcon = {
                IconButton(onClick = requestBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = loc4(it = "Indietro", en = "Back", fr = "Retour", es = "Atrás"))
                }
            }
        )

        GatedStepIndicator(steps = steps, currentStep = currentStep)

        errorMsg?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        when (currentStep) {
            0 -> PhotoStep(
                photos = photos,
                advancing = advancingFromPhoto,
                onAddPhotos = launchPhotoPicker,
                onReorder = reorderPhotos,
                onRemove = removePhoto,
                onNext = {
                    // Carica la copertina come referenza (foto intera) per lo step tessuti.
                    // Guardia anti doppio-tap: se una decodifica è già in corso, ignora.
                    if (!advancingFromPhoto) {
                        advancingFromPhoto = true
                        scope.launch {
                            try {
                                val bmp = photoUri?.let {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        BitmapUtils.loadFromUri(context, it)
                                    }
                                }
                                bitmap = bmp
                                maskedBitmap = bmp
                                errorMsg = null
                                currentStep = 1
                            } finally {
                                advancingFromPhoto = false
                            }
                        }
                    }
                }
            )
            1 -> ManualTissueStep(
                woundBitmap = maskedBitmap,
                necroticPercent = necroticPercent,
                sloughPercent = sloughPercent,
                granulationPercent = granulationPercent,
                epithelialPercent = epithelialPercent,
                onNecroticChanged = { v ->
                    val max = 100f - sloughPercent - granulationPercent - epithelialPercent
                    necroticPercent = v.coerceIn(0f, max.coerceAtLeast(0f))
                },
                onSloughChanged = { v ->
                    val max = 100f - necroticPercent - granulationPercent - epithelialPercent
                    sloughPercent = v.coerceIn(0f, max.coerceAtLeast(0f))
                },
                onGranulationChanged = { v ->
                    val max = 100f - necroticPercent - sloughPercent - epithelialPercent
                    granulationPercent = v.coerceIn(0f, max.coerceAtLeast(0f))
                },
                onEpithelialChanged = { v ->
                    val max = 100f - necroticPercent - sloughPercent - granulationPercent
                    epithelialPercent = v.coerceIn(0f, max.coerceAtLeast(0f))
                },
                onNext = {
                    // Informativa non bloccante: si procede comunque (la stima può
                    // legittimamente non arrivare a 100).
                    if (tissueTotal.toInt() != 100) {
                        scope.launch { snackbarHostState.showSnackbar(tissueWarning) }
                    }
                    currentStep = 2; errorMsg = null
                },
                onBack = { currentStep = 0 }
            )
            2 -> {
                DressingNotesStep(
                    eventDate = eventDate,
                    onEventDateChanged = { eventDate = it },
                    measLength = measLength,
                    onMeasLengthChanged = { measLength = it },
                    measWidth = measWidth,
                    onMeasWidthChanged = { measWidth = it },
                    measDepth = measDepth,
                    onMeasDepthChanged = { measDepth = it },
                    measDepthUnit = measDepthUnit,
                    onMeasDepthUnitChanged = { u ->
                        // Converte il valore digitato al cambio unità (8 mm resta 8 mm).
                        if (u != measDepthUnit) {
                            parseMeasure(measDepth)?.let {
                                measDepth = formatMeasure(if (u == "mm") it * 10f else it / 10f)
                            }
                            measDepthUnit = u
                        }
                    },
                    exudateLevel = exudateLevel,
                    onExudateChanged = { exudateLevel = it },
                    exudateType = exudateType,
                    onExudateTypeChanged = { exudateType = it },
                    painDetected = painDetected,
                    onPainDetectedChanged = { painDetected = it },
                    painNrs = painNrs,
                    onPainChanged = { painNrs = it },
                    primaryDressing = primaryDressing,
                    onPrimaryDressingChanged = { primaryDressing = it },
                    primaryDressingOther = primaryDressingOther,
                    onPrimaryDressingOtherChanged = { if (it.length <= 100) primaryDressingOther = it },
                    secondaryDressing = secondaryDressing,
                    onSecondaryDressingChanged = { secondaryDressing = it },
                    secondaryDressingOther = secondaryDressingOther,
                    onSecondaryDressingOtherChanged = { if (it.length <= 100) secondaryDressingOther = it },
                    notes = notes,
                    onNotesChanged = {
                        // La digitazione su tastiera non genera pointer event sul Box radice:
                        // rinnoviamo qui il timer di inattività per non scadere mentre si scrive.
                        com.example.woundmanagement.security.AppPreferences.updateLastActivityTime(context)
                        if (it.length <= 500) notes = it
                    },
                    onSave = {
                        // I nuovi record salvano i CODICI enum (render localizzato a valle);
                        // dressingType composto resta solo nei record legacy. "Altro" va nel
                        // testo libero accodato alle note del tipo (campo *Other).
                        val event = DressingEventEntity(
                            assessmentId = assessmentId,
                            // Senza foto resta vuoto (mai la stringa "null").
                            photoUri = photoUri?.lastPathSegment ?: "",
                            roiData = "",
                            tissueBlackPercent = necroticPercent,
                            tissueYellowPercent = sloughPercent,
                            tissueRedPercent = granulationPercent,
                            tissuePinkPercent = epithelialPercent,
                            exudateLevel = exudateLevel.name,
                            exudateType = exudateType.name,
                            painNrs = if (painDetected) painNrs else null,
                            // I nuovi record non usano più il testo composto legacy:
                            // l'eventuale descrizione "Altro" viaggia nel campo strutturato
                            // col prefisso parsabile ("ALTRO:testo"), MAI concatenata.
                            dressingType = "",
                            primaryDressing = if (primaryDressing == PrimaryDressing.ALTRO && primaryDressingOther.isNotBlank())
                                "ALTRO:" + primaryDressingOther.trim() else primaryDressing.name,
                            secondaryDressing = if (secondaryDressing == SecondaryDressing.ALTRO && secondaryDressingOther.isNotBlank())
                                "ALTRO:" + secondaryDressingOther.trim() else secondaryDressing.name,
                            notes = notes,
                            lengthCm = parseMeasure(measLength)?.coerceIn(0f, 100f) ?: 0f,
                            widthCm = parseMeasure(measWidth)?.coerceIn(0f, 100f) ?: 0f,
                            depthCm = (parseMeasure(measDepth)?.let { if (measDepthUnit == "mm") it / 10f else it })?.coerceIn(0f, 30f) ?: 0f,
                            createdAt = eventDate?.let { combineDayWithNowTime(it) }
                                ?: System.currentTimeMillis()
                        )
                        // Le extra viaggiano normalizzate come la principale (solo filename
                        // via lastPathSegment). take(3): il tetto 1+3 vale anche se la UI
                        // è stata aggirata (es. decode principale fallito).
                        onSave(event, extraPhotoUris.take(3).map { it.lastPathSegment ?: it.toString() })
                    },
                    onBack = { currentStep = 1 },
                    saveState = saveState
                )
            }
        }
    } // Column

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

    } // Box

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(S.unsavedExitTitle) },
            text = { Text(S.unsavedExitMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    // Uscita confermata senza salvare: la bozza non serve più e i file
                    // foto copiati in storage interno vanno eliminati (anti-leak).
                    val abandoned = listOf(photoUri?.toString()) + extraPhotoUris.map { it.toString() }
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { DressingDraftManager.clear(context, assessmentId) }
                        deleteAbandonedPhotoFiles(abandoned)
                    }
                    onBack()
                }) {
                    Text(S.unsavedExitConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(S.cancel)
                }
            }
        )
    }

    // Ripresa della bozza trovata al rientro (foto ricaricata; se illeggibile si scarta)
    pendingDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = { pendingDraft = null },
            title = { Text(S.resumeDraftTitle) },
            text = { Text(S.resumeDraftMessage) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDraft = null
                    scope.launch {
                        val restored = draft.photoUri?.let { uriStr ->
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runCatching { BitmapUtils.loadFromUri(context, Uri.parse(uriStr)) }.getOrNull()
                            }
                        }
                        if (restored == null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runCatching { DressingDraftManager.clear(context, assessmentId) }
                                deleteAbandonedPhotoFiles(listOf(draft.photoUri) + draft.extraPhotoUris)
                            }
                            return@launch
                        }
                        photoUri = Uri.parse(draft.photoUri)
                        bitmap = restored
                        // Foto extra: si ripristinano solo quelle il cui file esiste
                        // ancora; le voci orfane si scartano in silenzio.
                        extraPhotoUris = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            draft.extraPhotoUris.mapNotNull { s ->
                                runCatching {
                                    val u = Uri.parse(s)
                                    val p = u.path
                                    if (p != null && java.io.File(p).exists()) u else null
                                }.getOrNull()
                            }.take(3)
                        }
                        // Nessun ritaglio: la referenza tessuti è sempre la foto intera.
                        maskedBitmap = restored
                        necroticPercent = draft.necrotic
                        sloughPercent = draft.slough
                        granulationPercent = draft.granulation
                        epithelialPercent = draft.epithelial
                        exudateLevel = runCatching { ExudateLevel.valueOf(draft.exudate) }.getOrDefault(ExudateLevel.NON_SPECIFICATO)
                        exudateType = runCatching { ExudateType.valueOf(draft.exudateType) }.getOrDefault(ExudateType.NON_SPECIFICATO)
                        painDetected = draft.painDetected
                        painNrs = draft.painNrs
                        primaryDressing = runCatching { PrimaryDressing.valueOf(draft.primary) }.getOrDefault(PrimaryDressing.NON_SPECIFICATA)
                        primaryDressingOther = draft.primaryOther
                        secondaryDressing = runCatching { SecondaryDressing.valueOf(draft.secondary) }.getOrDefault(SecondaryDressing.NON_SPECIFICATA)
                        secondaryDressingOther = draft.secondaryOther
                        notes = draft.notes
                        eventDate = draft.eventDate
                        measLength = draft.measLength
                        measWidth = draft.measWidth
                        measDepth = draft.measDepth
                        measDepthUnit = draft.measDepthUnit
                        // Le bozze della vecchia build a 4 tappe (Foto/ROI/Tessuto/Note,
                        // indici 0..3) vanno riallineate alle 3 nuove (Foto/Tessuto/Note).
                        // Senza un campo di versione usiamo la stessa regola di gating del
                        // flusso live: la tappa Note (2) è raggiungibile solo con tessuto > 0.
                        // Così una vecchia bozza ferma su Tessuto (tessuto ancora a 0) atterra
                        // sulla tappa Tessuto e non salta la composizione tissutale.
                        val tissueSum = draft.necrotic + draft.slough + draft.granulation + draft.epithelial
                        currentStep = draft.step.coerceIn(0, 2).let { s -> if (s >= 2 && tissueSum <= 0f) 1 else s }
                    }
                }) { Text(S.resumeDraftResume) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDraft = null
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { DressingDraftManager.clear(context, assessmentId) }
                        // Bozza scartata: i file foto della bozza non servono più
                        deleteAbandonedPhotoFiles(listOf(draft.photoUri) + draft.extraPhotoUris)
                    }
                }) { Text(S.resumeDraftDiscard) }
            }
        )
    }
}

/**
 * Prima tappa: caricamento delle foto (una o più in una volta, fino a 4). La prima
 * foto è la copertina. Le foto si documentano intere, senza ritaglio.
 */
@Composable
private fun PhotoStep(
    photos: List<Uri>,
    advancing: Boolean,
    onAddPhotos: () -> Unit,
    onReorder: (List<Uri>) -> Unit,
    onRemove: (Uri) -> Unit,
    onNext: () -> Unit
) {
    val maxReached = photos.size >= MAX_DRESSING_PHOTOS
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            loc4(it = "Foto della lesione", en = "Wound photos", fr = "Photos de la plaie", es = "Fotos de la herida"),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            loc4(
                it = "Carica una o più foto (fino a 4). La prima è la copertina.",
                en = "Load one or more photos (up to 4). The first is the cover.",
                fr = "Chargez une ou plusieurs photos (jusqu'à 4). La première est la couverture.",
                es = "Cargue una o varias fotos (hasta 4). La primera es la portada."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (photos.isEmpty()) {
            // Nessuna foto: riquadro invito grande e cliccabile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onAddPhotos),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(S.addPhoto, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            ReorderablePhotoStrip(photos = photos, onReorder = onReorder, onRemove = onRemove)
            if (photos.size > 1) {
                Text(
                    loc4(
                        it = "Tieni premuto e trascina una foto per riordinarla. La prima è la copertina.",
                        en = "Long-press and drag a photo to reorder. The first is the cover.",
                        fr = "Appuyez longuement et glissez une photo pour réordonner. La première est la couverture.",
                        es = "Mantén pulsada y arrastra una foto para reordenar. La primera es la portada."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAddPhotos, enabled = !maxReached) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(S.addPhoto)
            }
            if (maxReached) {
                Text(S.photoMaxReached, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        // La foto NON è obbligatoria: si può procedere senza, ma con un avviso che ne
        // raccomanda l'uso (la documentazione fotografica aiuta a seguire l'evoluzione).
        var showNoPhotoWarning by remember { mutableStateOf(false) }
        Button(
            onClick = { if (photos.isEmpty()) showNoPhotoWarning = true else onNext() },
            enabled = !advancing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (advancing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(loc4(it = "Avanti", en = "Next", fr = "Suivant", es = "Siguiente"))
        }

        if (showNoPhotoWarning) {
            AlertDialog(
                onDismissRequest = { showNoPhotoWarning = false },
                title = { Text(loc4(it = "Nessuna foto", en = "No photo", fr = "Aucune photo", es = "Sin foto")) },
                text = {
                    Text(loc4(
                        it = "Non hai inserito nessuna foto. La documentazione fotografica rende l'app più utile: la buona pratica di wound care raccomanda di documentare le lesioni con strumenti iconografici (fotografia). Vuoi continuare senza foto?",
                        en = "You haven't added any photo. Photo documentation makes the app more useful: good wound-care practice recommends documenting lesions with iconographic tools (photography). Continue without a photo?",
                        fr = "Vous n'avez ajouté aucune photo. La documentation photographique rend l'app plus utile : les bonnes pratiques en soin des plaies recommandent de documenter les plaies avec des outils iconographiques (photographie). Continuer sans photo ?",
                        es = "No has añadido ninguna foto. La documentación fotográfica hace la app más útil: la buena práctica en el cuidado de heridas recomienda documentar las heridas con herramientas iconográficas (fotografía). ¿Continuar sin foto?"
                    ))
                },
                confirmButton = {
                    TextButton(onClick = { showNoPhotoWarning = false; onNext() }) {
                        Text(loc4(it = "Continua senza foto", en = "Continue without photo", fr = "Continuer sans photo", es = "Continuar sin foto"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoPhotoWarning = false; onAddPhotos() }) {
                        Text(S.addPhoto)
                    }
                }
            )
        }
    }
}

/**
 * Striscia di foto grande, riordinabile con drag & drop (tieni premuto e trascina).
 * La posizione 0 è la copertina. Ogni foto ha la X per rimuoverla.
 */
@Composable
private fun ReorderablePhotoStrip(
    photos: List<Uri>,
    onReorder: (List<Uri>) -> Unit,
    onRemove: (Uri) -> Unit
) {
    val density = LocalDensity.current
    // Copia locale osservabile: si riordina durante il drag, si committa al rilascio.
    // Inizializzata già con le foto correnti così il primo frame mostra le miniature
    // (niente striscia vuota all'ingresso, es. ripresa bozza o ritorno dallo step Tessuto).
    val items = remember { mutableStateListOf<Uri>().apply { addAll(photos) } }
    var dragUri by remember { mutableStateOf<Uri?>(null) }
    // Ri-sincronizza dall'esterno solo quando NON si sta trascinando (evita sfarfallii).
    LaunchedEffect(photos) {
        if (dragUri == null && items.toList() != photos) { items.clear(); items.addAll(photos) }
    }
    val coverLabel = S.mainPhotoLabel

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val count = items.size.coerceAtLeast(1)
        val gapPx = with(density) { 8.dp.toPx() }
        val rowW = constraints.maxWidth.toFloat()
        val slot = ((rowW - gapPx * (count - 1)) / count).coerceAtLeast(1f)
        val stride = slot + gapPx
        val sidePx = minOf(slot, with(density) { 108.dp.toPx() })
        val sideDp = with(density) { sidePx.toDp() }
        var dragDx by remember { mutableStateOf(0f) }

        Box(modifier = Modifier.fillMaxWidth().height(sideDp)) {
            items.forEachIndexed { index, uri ->
                key(uri) {
                    val isDrag = uri == dragUri
                    val baseX = index * stride + (slot - sidePx) / 2f
                    val tx = baseX + if (isDrag) dragDx else 0f
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDrag) 1f else 0f)
                            .offset { IntOffset(tx.roundToInt(), 0) }
                            .size(sideDp)
                            // Ri-aggancia la gesture quando cambiano numero foto o larghezza
                            // (slot/stride cambiano): altrimenti userebbe misure stantie.
                            .pointerInput(uri, items.size, rowW) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragUri = uri; dragDx = 0f },
                                    onDragEnd = { dragUri = null; dragDx = 0f; onReorder(items.toList()) },
                                    onDragCancel = { dragUri = null; dragDx = 0f; onReorder(items.toList()) },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragDx += delta.x
                                        val cur = items.indexOf(uri)
                                        if (cur < 0) return@detectDragGesturesAfterLongPress
                                        val center = cur * stride + (slot - sidePx) / 2f + dragDx + sidePx / 2f
                                        val target = (center / stride).toInt().coerceIn(0, items.size - 1)
                                        if (target != cur) {
                                            items.add(target, items.removeAt(cur))
                                            dragDx += (cur - target) * stride
                                        }
                                    }
                                )
                            }
                    ) {
                        PhotoThumb(uri = uri, elevated = isDrag)
                        if (index == 0 && !isDrag) {
                            // Badge copertina
                            Text(
                                coverLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // X (rimuovi): gestore tap dedicato che CONSUMA il down, così un
                        // tieni-premuto sulla X non fa partire il drag del Box padre
                        // (detectTapGestures consuma il down → il long-press del padre viene
                        // annullato). Tap rapido = rimozione.
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(40.dp)
                                .pointerInput(uri) {
                                    detectTapGestures(onTap = { onRemove(uri) })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = loc4(it = "Rimuovi foto", en = "Remove photo", fr = "Supprimer la photo", es = "Eliminar foto"),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                    .padding(3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Miniatura foto (campionata 256px), riempie il contenitore. */
@Composable
private fun PhotoThumb(uri: Uri, elevated: Boolean) {
    val context = LocalContext.current
    val thumb by produceState<Bitmap?>(initialValue = null, uri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val ref = uri.lastPathSegment ?: uri.toString()
                PhotoManager.loadThumbnail(context, ref, 256)
            }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(10.dp)
    val bmp = thumb
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(if (elevated) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun ManualTissueStep(
    woundBitmap: Bitmap?,
    necroticPercent: Float,
    sloughPercent: Float,
    granulationPercent: Float,
    epithelialPercent: Float,
    onNecroticChanged: (Float) -> Unit,
    onSloughChanged: (Float) -> Unit,
    onGranulationChanged: (Float) -> Unit,
    onEpithelialChanged: (Float) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val total = necroticPercent + sloughPercent + granulationPercent + epithelialPercent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            loc4(it = "Composizione Tissutale", en = "Tissue Composition", fr = "Composition tissulaire", es = "Composición tisular"),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            loc4(
                it = "Osserva la lesione e stima le percentuali di tessuto.",
                en = "Observe the wound and estimate tissue percentages.",
                fr = "Observez la plaie et estimez les pourcentages de tissu.",
                es = "Observe la herida y estime los porcentajes de tejido."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Foto ferita croppata per riferimento visivo
        woundBitmap?.let { bmp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = loc4(it = "Area lesione", en = "Wound area", fr = "Zone de la plaie", es = "Zona de la herida"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        TissuePercentageChart(
            necrotic = necroticPercent,
            slough = sloughPercent,
            granulation = granulationPercent,
            epithelial = epithelialPercent
        )

        Spacer(modifier = Modifier.height(12.dp))

        TissueSlider(
            label = S.necroticBlackBrown,
            value = necroticPercent,
            color = com.example.woundmanagement.ui.theme.TissueBlack,
            onValueChange = onNecroticChanged
        )
        TissueSlider(
            label = S.sloughFibrinYellow,
            value = sloughPercent,
            color = com.example.woundmanagement.ui.theme.TissueYellow,
            onValueChange = onSloughChanged
        )
        TissueSlider(
            label = S.granulationRed,
            value = granulationPercent,
            color = com.example.woundmanagement.ui.theme.TissueRed,
            onValueChange = onGranulationChanged
        )
        TissueSlider(
            label = loc4(it = "Epitelizzazione (rosa)", en = "Epithelialization (pink)", fr = "Épithélialisation (rose)", es = "Epitelización (rosa)"),
            value = epithelialPercent,
            color = com.example.woundmanagement.ui.theme.TissuePink,
            onValueChange = onEpithelialChanged
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "${S.total}: ${"%.0f".format(total)}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${S.remainingPercent}: ${"%.0f".format((100f - total).coerceAtLeast(0f))}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(S.back) }
            Button(
                onClick = onNext,
                enabled = total > 0f,
                modifier = Modifier.weight(1f)
            ) { Text(S.next) }
        }
    }
}

/**
 * Fonde il giorno scelto dal DatePicker (mezzanotte UTC) con l'ora locale corrente,
 * così i record retrodatati mantengono un orario plausibile e l'ordinamento nel giorno.
 */
private fun combineDayWithNowTime(pickedDayUtc: Long): Long {
    val day = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        .apply { timeInMillis = pickedDayUtc }
    return java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, day.get(java.util.Calendar.YEAR))
        set(java.util.Calendar.MONTH, day.get(java.util.Calendar.MONTH))
        set(java.util.Calendar.DAY_OF_MONTH, day.get(java.util.Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

// Parsing misura tollerante alla virgola decimale (gli utenti IT digitano "0,5").
private fun parseMeasure(s: String): Float? = s.trim().replace(',', '.').toFloatOrNull()

// Formatta una misura per il campo: interi senza decimali, altrimenti fino a 2 cifre.
private fun formatMeasure(v: Float): String {
    val r = Math.round(v * 100f) / 100f
    return if (r == r.toLong().toFloat()) r.toLong().toString() else r.toString().trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DressingNotesStep(
    eventDate: Long?,
    onEventDateChanged: (Long?) -> Unit,
    measLength: String,
    onMeasLengthChanged: (String) -> Unit,
    measWidth: String,
    onMeasWidthChanged: (String) -> Unit,
    measDepth: String,
    onMeasDepthChanged: (String) -> Unit,
    measDepthUnit: String,
    onMeasDepthUnitChanged: (String) -> Unit,
    exudateLevel: ExudateLevel,
    onExudateChanged: (ExudateLevel) -> Unit,
    exudateType: ExudateType,
    onExudateTypeChanged: (ExudateType) -> Unit,
    painDetected: Boolean,
    onPainDetectedChanged: (Boolean) -> Unit,
    painNrs: Int,
    onPainChanged: (Int) -> Unit,
    primaryDressing: PrimaryDressing,
    onPrimaryDressingChanged: (PrimaryDressing) -> Unit,
    primaryDressingOther: String,
    onPrimaryDressingOtherChanged: (String) -> Unit,
    secondaryDressing: SecondaryDressing,
    onSecondaryDressingChanged: (SecondaryDressing) -> Unit,
    secondaryDressingOther: String,
    onSecondaryDressingOtherChanged: (String) -> Unit,
    notes: String,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    saveState: DressingFlowViewModel.SaveState = DressingFlowViewModel.SaveState.Idle
) {
    val lang = LocalLanguage.current
    var exudateExpanded by remember { mutableStateOf(false) }
    var exudateTypeExpanded by remember { mutableStateOf(false) }
    var primaryExpanded by remember { mutableStateOf(false) }
    var secondaryExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(S.dressingAndNotes, style = MaterialTheme.typography.titleLarge)

        // Data medicazione (default oggi, retrodatabile — mai nel futuro)
        Box {
            OutlinedTextField(
                value = dateFormat.format(java.util.Date(eventDate ?: System.currentTimeMillis())),
                onValueChange = {},
                readOnly = true,
                label = { Text(S.dressingDate) },
                trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            // I campi readOnly non ricevono click: overlay trasparente cliccabile.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = eventDate ?: System.currentTimeMillis(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis <= System.currentTimeMillis()
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        onEventDateChanged(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text(S.confirm) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(S.cancel) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Misure della lesione a questa medicazione (facoltative): per seguirne l'evoluzione.
        val decKeyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        Text(
            loc4(it = "Misure (facoltative)", en = "Measurements (optional)", fr = "Mesures (facultatif)", es = "Medidas (opcional)"),
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = measLength, onValueChange = onMeasLengthChanged,
                label = { Text("${loc4(it = "Lungh.", en = "Length", fr = "Long.", es = "Long.")} (cm)") },
                modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = decKeyboard)
            OutlinedTextField(value = measWidth, onValueChange = onMeasWidthChanged,
                label = { Text("${loc4(it = "Largh.", en = "Width", fr = "Larg.", es = "Anch.")} (cm)") },
                modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = decKeyboard)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = measDepth, onValueChange = onMeasDepthChanged,
                label = { Text(loc4(it = "Prof.", en = "Depth", fr = "Prof.", es = "Prof.")) },
                placeholder = { Text(if (measDepthUnit == "mm") "5" else "0,5") },
                modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = decKeyboard)
            FilterChip(selected = measDepthUnit == "mm", onClick = { onMeasDepthUnitChanged("mm") }, label = { Text("mm") })
            FilterChip(selected = measDepthUnit == "cm", onClick = { onMeasDepthUnitChanged("cm") }, label = { Text("cm") })
        }

        // Exudate (quantità)
        ExposedDropdownMenuBox(expanded = exudateExpanded, onExpandedChange = { exudateExpanded = it }) {
            OutlinedTextField(
                value = exudateLevel.label(lang),
                onValueChange = {},
                readOnly = true,
                label = { Text(S.exudateLevelLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exudateExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = exudateExpanded, onDismissRequest = { exudateExpanded = false }) {
                ExudateLevel.entries.forEach { level ->
                    DropdownMenuItem(text = { Text(level.label(lang)) }, onClick = { onExudateChanged(level); exudateExpanded = false })
                }
            }
        }

        // Tipo essudato
        ExposedDropdownMenuBox(expanded = exudateTypeExpanded, onExpandedChange = { exudateTypeExpanded = it }) {
            OutlinedTextField(
                value = exudateType.label(lang),
                onValueChange = {},
                readOnly = true,
                label = { Text(S.exudateTypeLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exudateTypeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = exudateTypeExpanded, onDismissRequest = { exudateTypeExpanded = false }) {
                ExudateType.entries.forEach { t ->
                    DropdownMenuItem(text = { Text(t.label(lang)) }, onClick = { onExudateTypeChanged(t); exudateTypeExpanded = false })
                }
            }
        }

        // Dolore durante la procedura: registrato solo se l'operatore lo rileva
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = painDetected, onCheckedChange = onPainDetectedChanged)
            Spacer(modifier = Modifier.width(8.dp))
            Text(S.painProcedural, style = MaterialTheme.typography.labelLarge)
        }
        if (painDetected) {
            Text("${S.painNrsLabel}: $painNrs/10", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = painNrs.toFloat(),
                onValueChange = { onPainChanged(it.toInt()) },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .systemGestureExclusion()
            )
        }

        // NB: la guida alle medicazioni è stata RIMOSSA dall'app (hardening MDR):
        // qui si registra soltanto ciò che l'operatore ha applicato.

        // Primary Dressing
        Text(S.primaryDressingLabel, style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = primaryExpanded, onExpandedChange = { primaryExpanded = it }) {
            OutlinedTextField(
                value = primaryDressing.label(lang),
                onValueChange = {},
                readOnly = true,
                label = { Text(S.contactWithWound) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = primaryExpanded, onDismissRequest = { primaryExpanded = false }) {
                primaryDressingMenu(lang)
                    .forEach { d ->
                        DropdownMenuItem(text = { Text(d.label(lang)) }, onClick = { onPrimaryDressingChanged(d); primaryExpanded = false })
                    }
            }
        }
        if (primaryDressing == PrimaryDressing.ALTRO) {
            OutlinedTextField(
                value = primaryDressingOther,
                onValueChange = onPrimaryDressingOtherChanged,
                label = { Text(S.specifyPrimaryDressing) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Secondary Dressing
        Text(S.secondaryDressingLabel, style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = secondaryExpanded, onExpandedChange = { secondaryExpanded = it }) {
            OutlinedTextField(
                value = secondaryDressing.label(lang),
                onValueChange = {},
                readOnly = true,
                label = { Text(S.coverage) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = secondaryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = secondaryExpanded, onDismissRequest = { secondaryExpanded = false }) {
                secondaryDressingMenu(lang)
                    .forEach { d ->
                        DropdownMenuItem(text = { Text(d.label(lang)) }, onClick = { onSecondaryDressingChanged(d); secondaryExpanded = false })
                    }
            }
        }
        if (secondaryDressing == SecondaryDressing.ALTRO) {
            OutlinedTextField(
                value = secondaryDressingOther,
                onValueChange = onSecondaryDressingOtherChanged,
                label = { Text(S.specifySecondaryDressing) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            label = { Text(S.clinicalNotes) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )

        if (saveState is DressingFlowViewModel.SaveState.Error) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    (saveState as DressingFlowViewModel.SaveState.Error).message,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        val isSaving = saveState is DressingFlowViewModel.SaveState.Saving
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, enabled = !isSaving, modifier = Modifier.weight(1f)) { Text(S.back) }
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                if (isSaving) {
                    // onPrimary: il default (primary) era invisibile sul container primary del Button
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(S.saveDressing)
                }
            }
        }
    }
}

// Tetto foto per medicazione: 1 principale + 3 angolazioni extra
private const val MAX_DRESSING_PHOTOS = 4

/**
 * Cancella (best-effort) i file locali delle foto di un flusso abbandonato: quando
 * l'utente esce senza salvare o scarta la bozza, le copie cifrate in storage interno
 * non sono referenziate da nessun record e resterebbero orfane per sempre.
 */
private suspend fun deleteAbandonedPhotoFiles(uris: List<String?>) {
    val db = com.example.woundmanagement.WoundManagementApp.instance.database
    for (s in uris) {
        if (s.isNullOrBlank()) continue
        runCatching {
            // Mai cancellare un file referenziato da un record già salvato: una bozza
            // "fantasma" sopravvissuta a un salvataggio riuscito punta a file che ormai
            // appartengono al database (lo "Scarta" non deve costare la foto clinica).
            val name = Uri.parse(s).lastPathSegment
            if (name != null) {
                val referenced = db.dressingEventDao().countByPhotoUri(name) > 0 ||
                    db.dressingPhotoDao().countByPhotoUri(name) > 0
                if (!referenced) Uri.parse(s).path?.let { java.io.File(it).delete() }
            }
        }
    }
}

@Composable
private fun TissueSlider(
    label: String,
    value: Float,
    color: androidx.compose.ui.graphics.Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            // Testo in onSurface + pallino colorato: il valore colorato col colore del tessuto
            // (giallo/rosa) su superficie chiara era quasi illeggibile (contrasto ~1.5:1)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    "${"%.0f".format(value)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
            // Il thumb a inizio corsa cade nella zona dello swipe-back gestuale:
            // escludiamo la fascia dello slider dai gesti di sistema.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .systemGestureExclusion()
        )
    }
}

/**
 * Voci della tendina medicazione primaria: in cima "Non specificata" e "Nessuna", da
 * trovare subito invece che in ordine alfabetico sotto la N, poi i prodotti dalla A alla Z
 * nella lingua dell'app.
 */
internal fun primaryDressingMenu(lang: AppLanguage): List<PrimaryDressing> =
    PrimaryDressing.entries.sortedWith(compareBy({
        when (it) {
            PrimaryDressing.NON_SPECIFICATA -> 0
            PrimaryDressing.NESSUNA -> 1
            else -> 2
        }
    }, { it.label(lang).lowercase() }))

/** Come [primaryDressingMenu], per la medicazione secondaria. */
internal fun secondaryDressingMenu(lang: AppLanguage): List<SecondaryDressing> =
    SecondaryDressing.entries.sortedWith(compareBy({
        when (it) {
            SecondaryDressing.NON_SPECIFICATA -> 0
            SecondaryDressing.NESSUNA -> 1
            else -> 2
        }
    }, { it.label(lang).lowercase() }))
