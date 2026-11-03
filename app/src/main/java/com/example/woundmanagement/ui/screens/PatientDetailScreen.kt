package com.example.woundmanagement.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.domain.export.PatientReportPdf
import com.example.woundmanagement.security.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.woundmanagement.domain.imaging.BitmapUtils
import com.example.woundmanagement.data.db.dao.DressingEventWithWound
import com.example.woundmanagement.ui.components.BodyZone
import com.example.woundmanagement.ui.components.DateSlashTransformation
import com.example.woundmanagement.ui.components.WoundBodyMap
import com.example.woundmanagement.ui.components.ZoneWoundsDialog
import com.example.woundmanagement.ui.components.dateDigitsToSlashed
import com.example.woundmanagement.data.db.entity.PatientEntity
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.woundmanagement.domain.model.ExudateLevel
import com.example.woundmanagement.domain.model.WoundLocation
import com.example.woundmanagement.domain.model.WoundType
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patient: PatientEntity?,
    assessments: List<WoundAssessmentEntity>,
    dressingHistory: List<DressingEventWithWound>,
    scaleHistoryData: ScaleHistoryData? = null,
    onNewAssessment: (String) -> Unit,
    onAssessmentClick: (Long) -> Unit,
    onNortonClick: () -> Unit = {},
    onNRS2002Click: () -> Unit = {},
    onPainNRSClick: () -> Unit = {},
    onPatientUpdate: (PatientEntity) -> Unit = {},
    onDeletePatient: () -> Unit = {},
    onDeleteWound: (Long) -> Unit = {},
    onUpdateWoundNotes: (Long, String) -> Unit = { _, _ -> },
    onDeleteDressing: (Long) -> Unit = {},
    onUpdateDressing: (Long, Long, String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }
    // Tab di default = Lesioni (l'omino): è la vista operativa più usata
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }
    var showPatientInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // ── Export (PDF / single-patient backup) ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportMenu by remember { mutableStateOf(false) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    val patientId = patient?.id ?: -1L

    // Un export lungo (PDF con molte foto, backup del paziente) non produce tocchi sullo
    // schermo: senza questo rinnovo il timeout di inattività scatterebbe A METÀ operazione,
    // chiudendo il database sotto i piedi dell'esportazione.
    LaunchedEffect(isExporting) {
        while (isExporting) {
            com.example.woundmanagement.security.AppPreferences.updateLastActivityTime(context)
            kotlinx.coroutines.delay(30_000)
        }
    }

    // Pre-resolve strings used in non-composable callbacks
    val strExportError = S.exportError
    val authority = "${context.packageName}.fileprovider"
    val reportLang = LocalLanguage.current  // lingua del referto PDF = lingua app (IT/EN/FR/ES)

    fun shareFile(file: File, mime: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // NB: il file resta in cache (l'app ricevente lo legge via FileProvider DOPO il
        // ritorno da questa schermata: cancellarlo qui o in onDispose romperebbe la
        // condivisione, perché il consumer può leggerlo anche minuti dopo).
        // TODO(C9a): valutare una cancellazione differita sicura post-condivisione;
        // per ora i residui vengono ripuliti a ogni avvio (WoundManagementApp.clearExportCache).
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    val sharePdfTitle = S.sharePdf
    fun exportPdf() {
        if (patientId < 0 || isExporting) return
        scope.launch {
            isExporting = true
            try {
                val db = WoundManagementApp.instance.database
                val file = withContext(Dispatchers.IO) {
                    PatientReportPdf.generate(context, db, patientId, reportLang)
                }
                // Audit GDPR: dati che escono dal perimetro cifrato → tracciare l'evento.
                com.example.woundmanagement.data.repository.AuditRepository(db.auditDao())
                    .log("EXPORT_PDF", "Patient", patientId)
                shareFile(file, "application/pdf", sharePdfTitle)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("$strExportError: ${e.message ?: ""}".trim().trimEnd(':'))
            } finally {
                isExporting = false
            }
        }
    }

    val sharePatientBackupTitle = S.sharePatientBackup
    fun exportPatientBackup(password: String) {
        if (patientId < 0 || isExporting) return
        scope.launch {
            isExporting = true
            try {
                val db = WoundManagementApp.instance.database
                val file = withContext(Dispatchers.IO) {
                    BackupManager.createPatientBackup(context, db, password, patientId)
                }
                com.example.woundmanagement.data.repository.AuditRepository(db.auditDao())
                    .log("EXPORT_PATIENT_BACKUP", "Patient", patientId)
                shareFile(file, "application/octet-stream", sharePatientBackupTitle)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("$strExportError: ${e.message ?: ""}".trim().trimEnd(':'))
            } finally {
                isExporting = false
            }
        }
    }

    if (showBackupPasswordDialog) {
        PatientBackupPasswordDialog(
            onDismiss = { showBackupPasswordDialog = false },
            onConfirm = { pwd ->
                showBackupPasswordDialog = false
                exportPatientBackup(pwd)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(S.deleteConfirmTitle) },
            text = { Text(S.deleteConfirmText) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false; onDeletePatient() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(S.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text(S.cancel) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.background(com.example.woundmanagement.ui.components.brandGradientDeep)
        ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            title = { Text(patient?.let { "${it.surname} ${it.name}" } ?: S.patientDetail) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = loc4(it = "Indietro", en = "Back", fr = "Retour", es = "Atrás"))
                }
            },
            actions = {
                IconButton(onClick = { showPatientInfoDialog = true }) {
                    Icon(Icons.Default.Person, contentDescription = loc4(it = "Info paziente", en = "Patient info", fr = "Infos patient", es = "Información del paciente"))
                }
                // Export overflow menu (PDF / single-patient backup)
                Box {
                    IconButton(
                        onClick = { showExportMenu = true },
                        enabled = patient != null && !isExporting
                    ) {
                        Icon(Icons.Default.Share, contentDescription = loc4(it = "Esporta", en = "Export", fr = "Exporter", es = "Exportar"))
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(S.exportPdf) },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                exportPdf()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(S.sendToColleague) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                            onClick = {
                                showExportMenu = false
                                showBackupPasswordDialog = true
                            }
                        )
                    }
                }
                // Elimina paziente: azione distruttiva spostata nel menu overflow
                // (l'icona cestino sempre visibile in barra invitava al tocco accidentale)
                Box {
                    var showOverflow by remember { mutableStateOf(false) }
                    IconButton(onClick = { showOverflow = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = loc4(it = "Altre azioni", en = "More actions", fr = "Autres actions", es = "Más acciones"))
                    }
                    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                        DropdownMenuItem(
                            text = { Text(S.deleteConfirmTitle, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOverflow = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        )
        }

        if (patient == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {

        // Patient info dialog
        if (showPatientInfoDialog) {
            PatientInfoDialog(
                patient = patient,
                onDismiss = { showPatientInfoDialog = false },
                onSave = { updatedPatient ->
                    onPatientUpdate(updatedPatient)
                    showPatientInfoDialog = false
                }
            )
        }

        // Note / presa in carico del paziente (collassabile, matita per modificare)
        PatientNotesCard(
            patient = patient,
            onSaveNotes = { newNotes -> onPatientUpdate(patient.copy(notes = newNotes)) }
        )

        // Tabs a pillole (brand)
        com.example.woundmanagement.ui.components.BrandSegmentedTabs(
            labels = listOf(S.scalesTab, S.woundsTab, S.historyTab),
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (selectedTab) {
            0 -> AssessmentScalesTab(
                scaleHistoryData = scaleHistoryData,
                dateFormat = dateFormat,
                onNortonClick = onNortonClick,
                onNRS2002Click = onNRS2002Click,
                onPainNRSClick = onPainNRSClick
            )
            1 -> AssessmentsTab(
                assessments = assessments,
                dressingCounts = dressingHistory.groupingBy { it.assessmentId }.eachCount(),
                dateFormat = dateFormat,
                onNewAssessment = onNewAssessment,
                onAssessmentClick = onAssessmentClick,
                onDeleteWound = onDeleteWound,
                onUpdateWoundNotes = onUpdateWoundNotes
            )
            2 -> PhotoHistoryTab(
                dressingHistory = dressingHistory,
                dateFormat = dateFormat,
                // Il testo libero del tipo ALTRO vive su WoundAssessmentEntity (non nella
                // proiezione DressingEventWithWound): lookup per assessmentId
                typeOtherByWound = assessments.associate { it.id to it.typeOther },
                onDeleteDressing = onDeleteDressing,
                onUpdateDressing = onUpdateDressing
            )
        }
        } // else (patient != null)
    } // Column

        // Overlay: indicatore di caricamento durante l'esportazione
        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(S.exportInProgress)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    } // Box
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientBackupPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(S.patientBackupPasswordTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Come funziona l'invio a un collega (file cifrato + import merge lato ricevente)
                Text(
                    S.sendToColleagueInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(S.patientBackupPasswordHint, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = password,
                    onValueChange = { if (it.length <= 128) password = it },
                    label = { Text(S.backupPassword) },
                    singleLine = true,
                    // Il motivo per cui Esporta è disabilitato dev'essere visibile qui
                    isError = password.isNotEmpty() && password.length < 12,
                    supportingText = if (password.isNotEmpty() && password.length < 12) {
                        { Text(S.backupPasswordTooShort(password.length)) }
                    } else null,
                    visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) loc4(it = "Nascondi", en = "Hide", fr = "Masquer", es = "Ocultar")
                                    else loc4(it = "Mostra", en = "Show", fr = "Afficher", es = "Mostrar")
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.length >= 12 // C7: stesso minimo di BackupManager
            ) {
                Text(S.export)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(S.cancel) }
        }
    )
}

// Data class for scale history
data class ScaleHistoryData(
    val nortonScores: List<Pair<Long, Int>> = emptyList(),
    val nrs2002Scores: List<Pair<Long, Int>> = emptyList(),
    val painNrsScores: List<Pair<Long, Int>> = emptyList()
)

/**
 * Cronologia punteggi di una scala: ultimo valore + grafico. Nessun delta, freccia o
 * indicazione di tendenza: la lettura del andamento spetta all'operatore (vincolo non-DM).
 * La legenda riporta la direzione dello strumento come pubblicato, con la fonte.
 */
@Composable
private fun ScaleTrendCard(
    title: String,
    subtitle: String,
    scores: List<Pair<Long, Int>>,
    dateFormat: SimpleDateFormat,
    minValue: Int,
    maxValue: Int,
    legend: String
) {
    val neutralColor = com.example.woundmanagement.ui.theme.ScaleNeutral
    val sortedScores = scores.sortedBy { it.first }.takeLast(8)
    val latestScore = sortedScores.lastOrNull()?.second ?: 0

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(neutralColor, RoundedCornerShape(2.dp))
                    )
                    Text(
                        "$latestScore",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Graph with dd/MM dates on X axis
            ScaleTrendGraph(
                scores = sortedScores.map { it.second },
                timestamps = sortedScores.map { it.first },
                minValue = minValue,
                maxValue = maxValue,
                neutralColor = neutralColor
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                legend,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScaleTrendGraph(
    scores: List<Int>,
    timestamps: List<Long> = emptyList(),
    minValue: Int,
    maxValue: Int,
    neutralColor: Color
) {
    if (scores.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textColor = MaterialTheme.colorScheme.outline

    val ddMM = remember { java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()) }
    val dateLabels = remember(timestamps) {
        timestamps.map { ddMM.format(java.util.Date(it)) }
    }

    // C11 (accessibilità): descrizione testuale del grafico per i lettori di schermo.
    val graphDescription = loc4(
        it = "Grafico andamento punteggi: ${scores.size} rilevazioni, ultimo valore ${scores.last()}, scala da $minValue a $maxValue",
        en = "Score trend chart: ${scores.size} readings, latest value ${scores.last()}, scale $minValue to $maxValue",
        fr = "Graphique d'évolution des scores : ${scores.size} mesures, dernière valeur ${scores.last()}, échelle de $minValue à $maxValue",
        es = "Gráfico de evolución de puntuaciones: ${scores.size} mediciones, último valor ${scores.last()}, escala de $minValue a $maxValue"
    )

    // Dimensione testo in sp: rispetta il font scale del dispositivo (accessibilità).
    val labelTextSizePx = with(LocalDensity.current) { 11.sp.toPx() }
    val textPaint = remember(textColor, labelTextSizePx) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (textColor.alpha * 255).toInt(),
                (textColor.red * 255).toInt(),
                (textColor.green * 255).toInt(),
                (textColor.blue * 255).toInt()
            )
            textSize = labelTextSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .semantics { contentDescription = graphDescription }
    ) {
        val width = size.width
        val totalHeight = size.height
        // Margine in funzione del testo (sp→px): il valore fisso 36f in pixel raw faceva
        // invadere le date dd/MM la zona del grafico su schermi ad alta densità/font grandi
        val bottomMargin = labelTextSizePx * 1.4f + 8f
        val chartHeight = totalHeight - bottomMargin
        val padding = 8f
        val range = (maxValue - minValue).toFloat()

        // Grid lines
        for (i in 0..4) {
            val y = padding + (chartHeight - 2 * padding) * i / 4
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(padding, y),
                end = androidx.compose.ui.geometry.Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        if (scores.size == 1) {
            val x = width / 2
            val normalizedScore = (scores[0] - minValue) / range
            val y = padding + (chartHeight - 2 * padding) * (1 - normalizedScore)
            drawCircle(color = neutralColor, radius = 8f, center = androidx.compose.ui.geometry.Offset(x, y))
            if (dateLabels.isNotEmpty()) {
                drawIntoCanvas { it.nativeCanvas.drawText(dateLabels[0], x, totalHeight - 4f, textPaint) }
            }
            return@Canvas
        }

        val points = scores.mapIndexed { index, score ->
            val x = padding + (width - 2 * padding) * index / (scores.size - 1)
            val normalizedScore = (score - minValue) / range
            val y = padding + (chartHeight - 2 * padding) * (1 - normalizedScore)
            androidx.compose.ui.geometry.Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(color = lineColor, start = points[i], end = points[i + 1], strokeWidth = 2f)
        }

        points.forEach { point ->
            drawCircle(color = neutralColor, radius = 6f, center = point)
        }

        points.forEachIndexed { index, point ->
            if (index < dateLabels.size) {
                drawIntoCanvas { it.nativeCanvas.drawText(dateLabels[index], point.x, totalHeight - 4f, textPaint) }
            }
        }
    }
}

@Composable
private fun AssessmentsTab(
    assessments: List<WoundAssessmentEntity>,
    dressingCounts: Map<Long, Int>,
    dateFormat: SimpleDateFormat,
    onNewAssessment: (String) -> Unit,
    onAssessmentClick: (Long) -> Unit,
    onDeleteWound: (Long) -> Unit,
    onUpdateWoundNotes: (Long, String) -> Unit
) {
    var selectedZone by remember { mutableStateOf<BodyZone?>(null) }
    var woundToDelete by remember { mutableStateOf<WoundAssessmentEntity?>(null) }
    var woundToEditNotes by remember { mutableStateOf<WoundAssessmentEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Titolo
        Text(
            S.tapZone,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Mappa corporea
        WoundBodyMap(
            wounds = assessments,
            onZoneClick = { zone, _ -> selectedZone = zone },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Dialog quando si seleziona una zona. Le lesioni della zona vengono ricalcolate
    // dalla lista live: dopo un'eliminazione il dialog si aggiorna da solo.
    selectedZone?.let { zone ->
        ZoneWoundsDialog(
            zone = zone,
            zoneWounds = zone.getWounds(assessments),
            onDismiss = { selectedZone = null },
            onNewWound = {
                selectedZone = null
                onNewAssessment(zone.name)
            },
            onWoundClick = { woundId ->
                selectedZone = null
                onAssessmentClick(woundId)
            },
            onDeleteWound = { wound -> woundToDelete = wound },
            onEditWoundNotes = { wound -> woundToEditNotes = wound }
        )
    }

    // Conferma eliminazione lesione (irreversibile: elimina anche medicazioni e foto)
    woundToDelete?.let { wound ->
        AlertDialog(
            onDismissRequest = { woundToDelete = null },
            title = { Text(S.deleteWoundTitle) },
            text = { Text(S.deleteWoundMessage.format(dressingCounts[wound.id] ?: 0)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWound(wound.id)
                        woundToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(S.delete) }
            },
            dismissButton = {
                TextButton(onClick = { woundToDelete = null }) { Text(S.cancel) }
            }
        )
    }

    // Modifica note/anamnesi della lesione
    woundToEditNotes?.let { wound ->
        var notesText by remember(wound.id) { mutableStateOf(wound.notes) }
        AlertDialog(
            onDismissRequest = { woundToEditNotes = null },
            title = { Text(S.editNotes) },
            text = {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { if (it.length <= 500) notesText = it },
                    label = { Text(S.woundNotesLabel) },
                    placeholder = { Text(S.woundNotesHint) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateWoundNotes(wound.id, notesText.trim())
                    woundToEditNotes = null
                }) { Text(S.save) }
            },
            dismissButton = {
                TextButton(onClick = { woundToEditNotes = null }) { Text(S.cancel) }
            }
        )
    }
}

/** Card collassabile con le note/presa in carico del paziente, modificabili con la matita. */
@Composable
private fun PatientNotesCard(
    patient: PatientEntity,
    onSaveNotes: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = {
            if (patient.notes.isBlank()) showEditDialog = true else expanded = !expanded
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    S.patientNotesTitle,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = S.editNotes,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                patient.notes.ifBlank { S.addNotes },
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                color = if (patient.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showEditDialog) {
        var notesText by remember { mutableStateOf(patient.notes) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(S.patientNotesTitle) },
            text = {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { if (it.length <= 500) notesText = it },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveNotes(notesText.trim())
                    showEditDialog = false
                }) { Text(S.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(S.cancel) }
            }
        )
    }
}

@Composable
private fun AssessmentScalesTab(
    scaleHistoryData: ScaleHistoryData?,
    dateFormat: SimpleDateFormat,
    onNortonClick: () -> Unit,
    onNRS2002Click: () -> Unit,
    onPainNRSClick: () -> Unit
) {
    var showTrends by remember { mutableStateOf(false) }

    // Check if there's any trend data
    val hasTrendData = scaleHistoryData != null && (
        scaleHistoryData.nortonScores.isNotEmpty() ||
        scaleHistoryData.nrs2002Scores.isNotEmpty() ||
        scaleHistoryData.painNrsScores.isNotEmpty()
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // contentPadding (non padding sul modifier): come PhotoHistoryTab, evita il taglio
        // netto delle card a 16dp dal bordo durante lo scroll
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(S.assessmentScales, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                S.scalesSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expandable Trends Section
        if (hasTrendData) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    onClick = { showTrends = !showTrends }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    S.scaleTrends,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Icon(
                                if (showTrends) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        if (!showTrends) {
                            Text(
                                S.tapForCharts,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Show trend cards when expanded
            if (showTrends && scaleHistoryData != null) {
                // Norton
                if (scaleHistoryData.nortonScores.isNotEmpty()) {
                    item {
                        ScaleTrendCard(
                            title = S.nortonScale,
                            subtitle = S.nortonSubtitle,
                            scores = scaleHistoryData.nortonScores,
                            dateFormat = dateFormat,
                            minValue = 5,
                            maxValue = 20,
                            legend = S.nortonLegend
                        )
                    }
                }

                // NRS-2002
                if (scaleHistoryData.nrs2002Scores.isNotEmpty()) {
                    item {
                        ScaleTrendCard(
                            title = S.nrs2002Scale,
                            subtitle = S.nrs2002Subtitle,
                            scores = scaleHistoryData.nrs2002Scores,
                            dateFormat = dateFormat,
                            minValue = 0,
                            maxValue = 7,
                            legend = S.nrs2002Legend
                        )
                    }
                }

                // Pain NRS
                if (scaleHistoryData.painNrsScores.isNotEmpty()) {
                    item {
                        ScaleTrendCard(
                            title = S.nrsPainScaleTitle,
                            subtitle = S.painNrsSubtitle,
                            scores = scaleHistoryData.painNrsScores,
                            dateFormat = dateFormat,
                            minValue = 0,
                            maxValue = 10,
                            legend = S.painNrsLegend
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Rischio Lesioni da Pressione
        item {
            Text(
                S.pressureInjuryRisk,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Norton Scale Card
        item {
            ScaleCard(
                title = S.nortonScale,
                description = S.nortonDesc,
                details = S.nortonDetails,
                icon = Icons.Default.Checklist,
                iconColor = com.example.woundmanagement.ui.theme.ScaleIconNorton,
                onClick = onNortonClick
            )
        }

        // Stato Nutrizionale
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                S.nutritionalStatus,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // NRS-2002 Scale Card
        item {
            ScaleCard(
                title = S.nrs2002Scale,
                description = S.nrs2002Desc,
                details = S.nrs2002Details,
                icon = Icons.Default.Restaurant,
                iconColor = com.example.woundmanagement.ui.theme.ScaleIconNrs2002,
                onClick = onNRS2002Click
            )
        }

        // Dolore
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                S.painAssessment,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // NRS Pain Scale
        item {
            ScaleCard(
                title = S.nrsTitle,
                description = S.nrsDesc,
                details = S.nrsDetails,
                icon = Icons.Default.Healing,
                iconColor = com.example.woundmanagement.ui.theme.ScaleIconPain,
                onClick = onPainNRSClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleCard(
    title: String,
    description: String,
    @Suppress("UNUSED_PARAMETER") details: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) iconColor else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun PhotoHistoryTab(
    dressingHistory: List<DressingEventWithWound>,
    dateFormat: SimpleDateFormat,
    typeOtherByWound: Map<Long, String> = emptyMap(),
    onDeleteDressing: (Long) -> Unit = {},
    onUpdateDressing: (Long, Long, String) -> Unit = { _, _, _ -> }
) {
    if (dressingHistory.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.woundmanagement.R.drawable.empty_photos),
                    contentDescription = null,
                    modifier = Modifier.size(190.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(S.noPhotosRecorded, style = MaterialTheme.typography.bodyLarge)
                Text(S.performAssessmentToSee, style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        // Raggruppa: distretto corporeo → ferita → eventi
        val eventsByWound = dressingHistory.groupBy { it.assessmentId }
        // Lesione di cui è aperto il confronto foto (Dialog in fondo).
        var compareWoundId by remember { mutableStateOf<Long?>(null) }

        // Foto totali per lesione (tabella dressing_photos, tutte le medicazioni): il
        // confronto è possibile anche quando le 2+ foto stanno sulla STESSA medicazione,
        // caso che il solo conteggio delle copertine non vedeva.
        val photoCountByWound by produceState(initialValue = emptyMap<Long, Int>(), dressingHistory) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val dao = WoundManagementApp.instance.database.dressingPhotoDao()
                    val woundByEvent = dressingHistory.associate { it.eventId to it.assessmentId }
                    // chunked: la clausola IN di SQLite ha un tetto ai parametri
                    dressingHistory.map { it.eventId }.chunked(500)
                        .flatMap { dao.getByEvents(it) }
                        .groupingBy { woundByEvent[it.eventId] ?: -1L }
                        .eachCount()
                }.getOrNull() ?: emptyMap()
            }
        }

        // Funzione per trovare il BodyZone da un location string
        fun locationToZone(location: String): com.example.woundmanagement.ui.components.BodyZone? =
            com.example.woundmanagement.ui.components.BodyZone.entries
                .firstOrNull { it.dbLocations.contains(location) || it.name == location }

        // Raggruppa le ferite (assessmentId) per distretto
        val lang = LocalLanguage.current
        val woundsByDistrict = eventsByWound.entries
            .groupBy { (_, events) ->
                val loc = events.firstOrNull()?.woundLocation ?: ""
                locationToZone(loc)?.label(lang) ?: loc.replace("_", " ")
            }
            .toSortedMap()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            woundsByDistrict.forEach { (districtLabel, woundEntries) ->
                // Header distretto
                item(key = "district_$districtLabel") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            "  $districtLabel  ",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }

                woundEntries.forEach { (assessmentId, woundEvents) ->
                    val woundType = woundEvents.firstOrNull()?.woundType ?: ""
                    val woundTypeOther = typeOtherByWound[assessmentId] ?: ""
                    val woundNameRaw = woundEvents.firstOrNull()?.woundName?.ifBlank { null }
                        ?: woundType.ifBlank { null }?.let { woundTypeDisplay(it, woundTypeOther, lang) }

                    // Sub-header ferita
                    item(key = "wound_header_$assessmentId") {
                        // Confronto foto: disponibile se la lesione ha almeno 2 foto in
                        // tutto (anche più foto di una sola medicazione).
                        val woundPhotoCount = photoCountByWound[assessmentId]
                            ?: woundEvents.count { it.photoUri.isNotBlank() }
                        WoundHistoryHeader(
                            woundName = woundNameRaw ?: loc4(it = "Lesione", en = "Wound", fr = "Plaie", es = "Herida"),
                            typeLabel = woundType.ifBlank { null }?.let { woundTypeDisplay(it, woundTypeOther, lang) },
                            dressingCount = woundEvents.size,
                            showCompare = woundPhotoCount >= 2,
                            onCompare = { compareWoundId = assessmentId }
                        )
                    }

                    // Singole medicazioni (più recente prima)
                    woundEvents.forEach { event ->
                        item(key = "event_${event.eventId}") {
                            PhotoHistoryCard(
                                event = event,
                                dateFormat = dateFormat,
                                typeOther = woundTypeOther,
                                onDelete = { onDeleteDressing(event.eventId) },
                                onUpdate = { newDate, newNotes ->
                                    onUpdateDressing(event.eventId, newDate, newNotes)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Dialog di confronto foto (affiancate / sovrapposte) della lesione selezionata
        compareWoundId?.let { wid ->
            PhotoCompareDialog(
                events = eventsByWound[wid].orEmpty(),
                dateFormat = dateFormat,
                onDismiss = { compareWoundId = null }
            )
        }
    }
}

/**
 * Intestazione di una lesione nello Storico: nome, tipo, numero di medicazioni e, con
 * almeno 2 foto, il pulsante «Confronta».
 *
 * Prima la Column dei testi, senza peso, veniva misurata prima del pulsante: un nome o un
 * tipo che va a capo dichiara tutta la larghezza disponibile, al pulsante restavano pochi
 * dp o zero, «Confronta» si spezzava una lettera per riga (o spariva) e la card diventava
 * altissima. Ora il pulsante ha sempre la sua larghezza naturale, su una riga sola, e la
 * disposizione la sceglie [HeaderTextsWithAction].
 */
@Composable
internal fun WoundHistoryHeader(
    woundName: String,
    typeLabel: String?,
    dressingCount: Int,
    showCompare: Boolean,
    onCompare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Col pulsante il margine destro lo dà già il suo padding interno (12dp)
                .padding(start = 12.dp, end = if (showCompare) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Healing,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            val titles: @Composable () -> Unit = {
                Column {
                    Text(woundName, style = MaterialTheme.typography.titleSmall)
                    if (!typeLabel.isNullOrBlank()) {
                        Text(
                            typeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val count: @Composable () -> Unit = {
                Text(
                    loc4(
                        it = "$dressingCount medicazion${if (dressingCount == 1) "e" else "i"}",
                        en = "$dressingCount dressing${if (dressingCount == 1) "" else "s"}",
                        fr = "$dressingCount pansement${if (dressingCount == 1) "" else "s"}",
                        es = "$dressingCount cura${if (dressingCount == 1) "" else "s"}"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showCompare) {
                HeaderTextsWithAction(
                    modifier = Modifier.weight(1f),
                    titles = titles,
                    count = count,
                    action = {
                        TextButton(onClick = onCompare) {
                            Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            // Sempre su una riga: il pulsante non deve mai spezzarsi in verticale
                            Text(
                                loc4(it = "Confronta", en = "Compare", fr = "Comparer", es = "Comparar"),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    titles()
                    count()
                }
            }
        }
    }
}

/**
 * Testi dell'intestazione con un'azione (il pulsante) che ha sempre la sua larghezza
 * naturale. Testi e azione stanno affiancati solo se l'azione non costringe nome e tipo
 * ad andare a capo più di quanto farebbero a tutta larghezza; altrimenti nome e tipo
 * usano tutta la riga e l'azione scende in basso a destra, sulla riga del conteggio.
 * La scelta usa le misure intrinseche (altezza dei testi a una data larghezza), quindi
 * vale per ogni lingua, schermo e dimensione del carattere.
 */
@Composable
private fun HeaderTextsWithAction(
    modifier: Modifier,
    titles: @Composable () -> Unit,
    count: @Composable () -> Unit,
    action: @Composable () -> Unit
) {
    Layout(content = { titles(); count(); action() }, modifier = modifier) { measurables, constraints ->
        val (titlesM, countM, actionM) = measurables
        val full = constraints.maxWidth
        val gap = 4.dp.roundToPx()
        // A tutta riga i testi si fermano 8dp prima: col pulsante il padding destro della
        // card è 4dp invece di 12dp, e così vanno a capo come nella card senza pulsante.
        val textFull = (full - 8.dp.roundToPx()).coerceAtLeast(0)
        val actionW = actionM.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(full)
        val sideW = (full - actionW - gap).coerceAtLeast(0)
        val textsSide = titlesM.minIntrinsicHeight(sideW) + countM.minIntrinsicHeight(sideW)
        val textsFull = titlesM.minIntrinsicHeight(textFull) + countM.minIntrinsicHeight(textFull)
        if (sideW > 0 && textsSide <= textsFull) {
            // Affiancati: testi a sinistra, azione centrata in verticale a destra
            val t = titlesM.measure(Constraints(maxWidth = sideW))
            val c = countM.measure(Constraints(maxWidth = sideW))
            val a = actionM.measure(Constraints(maxWidth = actionW))
            val textH = t.height + c.height
            val h = maxOf(textH, a.height)
            layout(full, h) {
                val top = (h - textH) / 2
                t.placeRelative(0, top)
                c.placeRelative(0, top + t.height)
                a.placeRelative(full - a.width, (h - a.height) / 2)
            }
        } else {
            // In colonna: nome e tipo a tutta larghezza, sotto il conteggio con l'azione a destra
            val t = titlesM.measure(Constraints(maxWidth = textFull))
            val a = actionM.measure(Constraints(maxWidth = actionW))
            val c = countM.measure(Constraints(maxWidth = (full - a.width - gap).coerceAtLeast(0)))
            val rowH = maxOf(c.height, a.height)
            layout(full, t.height + rowH) {
                t.placeRelative(0, 0)
                c.placeRelative(0, t.height + (rowH - c.height) / 2)
                a.placeRelative(full - a.width, t.height + (rowH - a.height) / 2)
            }
        }
    }
}

/** Stato di caricamento della thumbnail: distingue "sto caricando" da "foto mancante". */
private sealed interface PhotoLoad {
    data object Loading : PhotoLoad
    data class Ok(val bitmap: Bitmap) : PhotoLoad
    data object Failed : PhotoLoad
}

/**
 * Carica una foto di medicazione su IO thread: nome file cifrato interno (caso normale)
 * oppure URI legacy con schema. 800px: mostrata fino a 140dp come thumbnail; a 420px era
 * sgranata su schermi ad alta densità, 800px dà margine di nitidezza senza pesare (e resta
 * un tetto prudente anche a schermo intero, dove il pinch-to-zoom lavora sul bitmap).
 */
private suspend fun loadDressingPhoto(
    context: android.content.Context,
    photoRef: String?,
    fullQuality: Boolean = false
): PhotoLoad =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val bmp = when {
                photoRef.isNullOrBlank() -> null
                photoRef.contains("://") -> BitmapUtils.loadFromUri(context, android.net.Uri.parse(photoRef))
                // A schermo intero: piena definizione (fino a 2048px, non cachata) così il
                // pinch-to-zoom resta nitido. La miniatura della card resta a 800px.
                fullQuality -> com.example.woundmanagement.security.PhotoManager.loadBitmap(context, photoRef, 2048)
                else -> com.example.woundmanagement.security.PhotoManager.loadThumbnail(context, photoRef, 800)
            }
            if (bmp != null) PhotoLoad.Ok(bmp) else PhotoLoad.Failed
        } catch (_: Exception) { PhotoLoad.Failed }
    }

/** Una foto confrontabile: riferimento file + data della medicazione. */
private data class ComparePhoto(val ref: String, val date: Long)

/**
 * Confronto VISIVO di due foto della stessa lesione, affiancate o sovrapposte (con
 * opacità regolabile). Solo visualizzazione: nessuna elaborazione/evidenziazione
 * automatica delle differenze (l'app resta di sola documentazione, non-DM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoCompareDialog(
    events: List<DressingEventWithWound>,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalLanguage.current
    // Tutte le foto della lesione, in ordine cronologico (dalla più vecchia).
    // null = caricamento ancora in corso (distinto da "caricato, nessuna foto": senza
    // questa distinzione una lesione senza foto mostrava lo spinner all'infinito).
    val loadedPhotos by produceState<List<ComparePhoto>?>(initialValue = null, events) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val db = WoundManagementApp.instance.database.dressingPhotoDao()
            events.sortedBy { it.eventCreatedAt }.flatMap { ev ->
                val refs = runCatching {
                    db.getByEventOnce(ev.eventId).sortedBy { it.position }.map { it.photoUri }
                }.getOrNull()?.takeIf { it.isNotEmpty() }
                    ?: listOf(ev.photoUri).filter { it.isNotBlank() }
                refs.map { ComparePhoto(it, ev.eventCreatedAt) }
            }
        }
    }

    val photos = loadedPhotos.orEmpty()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            if (photos.size < 2) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (loadedPhotos == null) CircularProgressIndicator(color = Color.White)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            loc4(it = "Servono almeno 2 foto", en = "At least 2 photos needed", fr = "Au moins 2 photos nécessaires", es = "Se necesitan al menos 2 fotos"),
                            color = Color.White
                        )
                        // Senza questo pulsante l'unica uscita era il tasto Indietro
                        TextButton(onClick = onDismiss) {
                            Text(S.close, color = Color.White)
                        }
                    }
                }
                return@Surface
            }
            var idxA by remember(photos.size) { mutableIntStateOf(0) }
            var idxB by remember(photos.size) { mutableIntStateOf(photos.size - 1) }
            var overlay by remember { mutableStateOf(false) }
            var opacity by remember { mutableFloatStateOf(0.5f) }
            // Sovrapposizione: la 2ª foto si può spostare e scalare col dito per allinearla.
            var ovOffset by remember { mutableStateOf(Offset.Zero) }
            var ovScale by remember { mutableFloatStateOf(1f) }
            val bmpA by produceState<PhotoLoad>(PhotoLoad.Loading, photos.getOrNull(idxA)?.ref) {
                value = loadDressingPhoto(context, photos.getOrNull(idxA)?.ref, fullQuality = true)
            }
            val bmpB by produceState<PhotoLoad>(PhotoLoad.Loading, photos.getOrNull(idxB)?.ref) {
                value = loadDressingPhoto(context, photos.getOrNull(idxB)?.ref, fullQuality = true)
            }
            val labelA = photos.getOrNull(idxA)?.let { dateFormat.format(Date(it.date)) } ?: ""
            val labelB = photos.getOrNull(idxB)?.let { dateFormat.format(Date(it.date)) } ?: ""

            Column(Modifier.fillMaxSize()) {
                // Barra alta: titolo + modalità + chiudi
                Row(
                    Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(loc4(it = "Confronto foto", en = "Photo comparison", fr = "Comparaison photos", es = "Comparación de fotos"),
                        color = Color.White, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(selected = !overlay, onClick = { overlay = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                            Text(loc4(it = "Affiancate", en = "Side by side", fr = "Côte à côte", es = "Lado a lado"))
                        }
                        SegmentedButton(selected = overlay, onClick = { overlay = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                            Text(loc4(it = "Sovrapposte", en = "Overlay", fr = "Superposées", es = "Superpuestas"))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = loc4(it = "Chiudi", en = "Close", fr = "Fermer", es = "Cerrar"), tint = Color.White)
                    }
                }

                // Area immagini
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (overlay) {
                        // Reset dell'allineamento quando cambia la 2ª foto
                        LaunchedEffect(idxB) { ovOffset = Offset.Zero; ovScale = 1f }
                        CompareImage(bmpA, Modifier.fillMaxSize())
                        // La 2ª foto: trascina per spostarla, pizzica per scalarla (allineamento)
                        Box(
                            Modifier.fillMaxSize().pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    ovScale = (ovScale * zoom).coerceIn(0.3f, 6f)
                                    ovOffset += pan
                                }
                            }
                        ) {
                            CompareImage(bmpB, Modifier.fillMaxSize().graphicsLayer {
                                this.alpha = opacity
                                translationX = ovOffset.x; translationY = ovOffset.y
                                scaleX = ovScale; scaleY = ovScale
                            })
                        }
                        // Etichette A/B + reset allineamento
                        Row(
                            Modifier.align(Alignment.TopCenter).padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$labelA  ·  $labelB", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            if (ovOffset != Offset.Zero || ovScale != 1f) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    loc4(it = "Reimposta", en = "Reset", fr = "Réinit.", es = "Restablecer"),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clickable { ovOffset = Offset.Zero; ovScale = 1f }
                                )
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                CompareImage(bmpA, Modifier.weight(1f).fillMaxWidth())
                                Text(labelA, color = Color.White, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp))
                            }
                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                CompareImage(bmpB, Modifier.weight(1f).fillMaxWidth())
                                Text(labelB, color = Color.White, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp))
                            }
                        }
                    }
                }

                // Controlli: opacità (solo sovrapposte) + selettori foto
                Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (overlay) {
                        Text(loc4(it = "Opacità 2ª foto · trascina/pizzica per allinearla", en = "2nd photo opacity · drag/pinch to align", fr = "Opacité 2e photo · glisser/pincer pour aligner", es = "Opacidad 2ª foto · arrastra/pellizca para alinear"),
                            color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0f..1f)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComparePhotoSelector(loc4(it = "Foto 1", en = "Photo 1", fr = "Photo 1", es = "Foto 1"),
                            photos, idxA, dateFormat, Modifier.weight(1f)) { idxA = it }
                        ComparePhotoSelector(loc4(it = "Foto 2", en = "Photo 2", fr = "Photo 2", es = "Foto 2"),
                            photos, idxB, dateFormat, Modifier.weight(1f)) { idxB = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareImage(state: PhotoLoad, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when (state) {
            is PhotoLoad.Ok -> Image(bitmap = state.bitmap.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            PhotoLoad.Loading -> CircularProgressIndicator(color = Color.White)
            PhotoLoad.Failed -> Icon(Icons.Default.BrokenImage, contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComparePhotoSelector(
    label: String,
    photos: List<ComparePhoto>,
    selected: Int,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = "${selected + 1} · ${photos.getOrNull(selected)?.let { dateFormat.format(Date(it.date)) } ?: ""}",
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            photos.forEachIndexed { i, p ->
                DropdownMenuItem(text = { Text("${i + 1} · ${dateFormat.format(Date(p.date))}") },
                    onClick = { onSelect(i); expanded = false })
            }
        }
    }
}

/** Etichetta di sezione dentro la card espansa dello storico. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

// ExperimentalFoundationApi: HorizontalPager/rememberPagerState (stabili di fatto nel BOM in uso)
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PhotoHistoryCard(
    event: DressingEventWithWound,
    dateFormat: SimpleDateFormat,
    typeOther: String = "",
    onDelete: () -> Unit = {},
    onUpdate: (Long, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lang = LocalLanguage.current
    var expanded by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Carica thumbnail (= copertina, event.photoUri) in modo asincrono su IO thread
    val photoState by produceState<PhotoLoad>(initialValue = PhotoLoad.Loading, event.photoUri) {
        value = loadDressingPhoto(context, event.photoUri)
    }
    val photoBitmap = (photoState as? PhotoLoad.Ok)?.bitmap

    // Elenco foto dell'evento (dressing_photos, ordinate per position): lazy, si caricano
    // solo i NOMI file (niente bitmap). Eventi legacy senza righe → solo la copertina.
    val photoRefs by produceState(
        initialValue = listOfNotNull(event.photoUri.takeIf { it.isNotBlank() }),
        event.eventId
    ) {
        val fromDb = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                WoundManagementApp.instance.database.dressingPhotoDao()
                    .getByEventOnce(event.eventId)
                    .sortedBy { it.position }
                    .map { it.photoUri }
            } catch (_: Exception) { emptyList() }
        }
        if (fromDb.isNotEmpty()) value = fromDb
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail: caricamento / foto / foto mancante (BrokenImage, non cliccabile)
                Box {
                    when (val ps = photoState) {
                        is PhotoLoad.Ok -> Image(
                            bitmap = ps.bitmap.asImageBitmap(),
                            contentDescription = loc4(it = "Foto lesione", en = "Wound photo", fr = "Photo de la plaie", es = "Foto de la herida"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFullscreenImage = true }
                        )
                        PhotoLoad.Loading -> Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        PhotoLoad.Failed -> Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (event.photoUri.isBlank()) {
                                // Medicazione documentata senza foto: placeholder neutro (non "immagine rotta")
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(34.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        loc4(it = "Senza foto", en = "No photo", fr = "Sans photo", es = "Sin foto"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.BrokenImage,
                                    contentDescription = loc4(it = "Foto non disponibile", en = "Photo unavailable", fr = "Photo indisponible", es = "Foto no disponible"),
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    if (photoState is PhotoLoad.Ok) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = loc4(it = "Ingrandisci", en = "Enlarge", fr = "Agrandir", es = "Ampliar"),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(2.dp),
                            tint = Color.White
                        )
                    }
                    // Badge conteggio foto: solo se l'evento ha più di una foto
                    if (photoRefs.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = loc4(
                                        it = "${photoRefs.size} foto",
                                        en = "${photoRefs.size} photo${if (photoRefs.size == 1) "" else "s"}",
                                        fr = "${photoRefs.size} photo${if (photoRefs.size == 1) "" else "s"}",
                                        es = "${photoRefs.size} foto${if (photoRefs.size == 1) "" else "s"}"
                                    ),
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "${photoRefs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        woundLocationLabel(event.woundLocation, lang),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        woundTypeDisplay(event.woundType, typeOther, lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        dateFormat.format(Date(event.eventCreatedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            // Tissue percentages bar
            Spacer(modifier = Modifier.height(8.dp))
            TissueBar(
                black = event.tissueBlackPercent,
                yellow = event.tissueYellowPercent,
                red = event.tissueRedPercent,
                pink = event.tissuePinkPercent
            )

            // Expanded details — sezioni: TESSUTI / VALUTAZIONE / MEDICAZIONE / NOTE
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                SectionLabel(S.sectionTissues)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailChip(loc4(it = "Necrosi", en = "Necrosis", fr = "Nécrose", es = "Necrosis"), "${event.tissueBlackPercent.toInt()}%", com.example.woundmanagement.ui.theme.TissueBlack)
                    DetailChip(loc4(it = "Fibrina", en = "Slough", fr = "Fibrine", es = "Esfacelo"), "${event.tissueYellowPercent.toInt()}%", com.example.woundmanagement.ui.theme.TissueYellow)
                    DetailChip(loc4(it = "Granul.", en = "Granul.", fr = "Granul.", es = "Granul."), "${event.tissueRedPercent.toInt()}%", com.example.woundmanagement.ui.theme.TissueRed)
                    DetailChip(loc4(it = "Epitel.", en = "Epithel.", fr = "Épithél.", es = "Epitel."), "${event.tissuePinkPercent.toInt()}%", com.example.woundmanagement.ui.theme.TissuePink)
                }

                Spacer(modifier = Modifier.height(8.dp))
                SectionLabel(S.sectionAssessment)
                val exudateText = buildString {
                    append(exudateLabel(event.exudateLevel, lang).ifBlank { "—" })
                    val type = runCatching { com.example.woundmanagement.domain.model.ExudateType.valueOf(event.exudateType) }.getOrNull()
                    if (type != null && type != com.example.woundmanagement.domain.model.ExudateType.NON_SPECIFICATO) {
                        append(" · ${type.label(lang)}")
                    }
                }
                LabelValue(loc4(it = "Essudato", en = "Exudate", fr = "Exsudat", es = "Exudado"), exudateText)
                LabelValue(
                    loc4(it = "Dolore", en = "Pain", fr = "Douleur", es = "Dolor"),
                    event.painNrs?.let { "$it/10" } ?: S.painNotRecorded
                )
                // Misure a questa medicazione (mostrate solo se rilevate): evoluzione nel tempo
                if (event.lengthCm > 0f || event.widthCm > 0f || event.depthCm > 0f) {
                    val depthStr = when {
                        event.depthCm <= 0f -> "—"
                        event.depthCm < 1f -> "%.0f mm".format(event.depthCm * 10)
                        else -> "%.1f cm".format(event.depthCm)
                    }
                    LabelValue(
                        loc4(it = "Misure", en = "Size", fr = "Mesures", es = "Medidas"),
                        "%.1f × %.1f cm · %s %s".format(
                            event.lengthCm, event.widthCm,
                            loc4(it = "prof.", en = "depth", fr = "prof.", es = "prof."), depthStr
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                SectionLabel(S.sectionDressing)
                // Record NUOVO = campi strutturati valorizzati. NB: per "Altro" il campo
                // contiene "ALTRO:<testo libero>", che NON è un valore di enum: usare
                // valueOf() come discriminante nascondeva del tutto la medicazione quando
                // sia la primaria sia la secondaria erano "Altro" con testo.
                val hasStructuredDressing =
                    event.primaryDressing.isNotBlank() || event.secondaryDressing.isNotBlank()
                if (hasStructuredDressing) {
                    // Record nuovi: enum localizzati; per "Altro" il testo libero è in dressingType
                    LabelValue(S.primaryLabel, primaryDressingDisplay(event.primaryDressing, lang))
                    LabelValue(S.secondaryLabel, secondaryDressingDisplay(event.secondaryDressing, lang))
                } else if (event.dressingType.isNotBlank()) {
                    // Record legacy: testo composto salvato nella lingua dell'epoca
                    Text(event.dressingType, style = MaterialTheme.typography.bodySmall)
                }

                if (event.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionLabel(loc4(it = "Note", en = "Notes", fr = "Notes", es = "Notas"))
                    Text(
                        event.notes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                // Azioni sul record: correzione documentale (data/note) o eliminazione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(S.editDressing)
                    }
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(S.delete)
                    }
                }
            }
        }
    }

    // Conferma eliminazione medicazione
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(S.deleteDressingTitle) },
            text = { Text(S.deleteDressingMessage) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(S.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(S.cancel) }
            }
        )
    }

    // Modifica data e note del record (correzione documentale, mai nel futuro)
    if (showEditDialog) {
        var notesText by remember { mutableStateOf(event.notes) }
        var pickedDay by remember { mutableStateOf<Long?>(null) }   // null = data invariata
        var showDatePicker by remember { mutableStateOf(false) }
        val dayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(S.editDressingTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        OutlinedTextField(
                            value = dayFormat.format(Date(pickedDay ?: event.eventCreatedAt)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(S.dressingDate) },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { if (it.length <= 500) notesText = it },
                        label = { Text(loc4(it = "Note", en = "Notes", fr = "Notes", es = "Notas")) },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Giorno nuovo + orario originale dell'evento (ordinamento intra-giorno stabile)
                    val newDate = pickedDay?.let { day ->
                        val utcDay = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = day }
                        Calendar.getInstance().apply {
                            timeInMillis = event.eventCreatedAt
                            set(Calendar.YEAR, utcDay.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcDay.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcDay.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                    } ?: event.eventCreatedAt
                    onUpdate(newDate, notesText.trim())
                    showEditDialog = false
                }) { Text(S.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(S.cancel) }
            }
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = pickedDay ?: event.eventCreatedAt,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) =
                        utcTimeMillis <= System.currentTimeMillis()
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickedDay = datePickerState.selectedDateMillis
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
    }

    // Fullscreen multi-foto: carosello con decifratura on-demand per pagina (anti-OOM),
    // pinch-to-zoom per pagina (reset al cambio pagina), contatore K/N e pallini.
    if (showFullscreenImage && photoRefs.size > 1) {
        Dialog(
            onDismissRequest = { showFullscreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val pagerState = rememberPagerState(pageCount = { photoRefs.size })
            var imgScale by remember { mutableFloatStateOf(1f) }
            var imgOffset by remember { mutableStateOf(Offset.Zero) }
            // Reset zoom/pan quando si cambia pagina
            LaunchedEffect(pagerState.currentPage) {
                imgScale = 1f
                imgOffset = Offset.Zero
            }
            // Gesto zoom "pager-friendly": consuma i tocchi SOLO con 2+ dita (pinch) o
            // quando l'immagine è già ingrandita (pan a 1 dito). A zoom 1x il drag a un
            // dito NON viene consumato e arriva al pager → lo swipe tra le foto funziona.
            // (Modifier.transformable consumava anche il pan a 1 dito bloccando il carosello.)
            val pagerZoomGesture: suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit = {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val ev = awaitPointerEvent()
                        val pressedCount = ev.changes.count { it.pressed }
                        if (pressedCount >= 2 || imgScale > 1f) {
                            val zoomChange = ev.calculateZoom()
                            val panChange = ev.calculatePan()
                            imgScale = (imgScale * zoomChange).coerceIn(1f, 5f)
                            imgOffset = if (imgScale > 1f) {
                                Offset(
                                    x = (imgOffset.x + panChange.x).coerceIn(-500f, 500f),
                                    y = (imgOffset.y + panChange.y).coerceIn(-500f, 500f)
                                )
                            } else Offset.Zero
                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // Bitmap caricato SOLO per la pagina richiesta (niente decodifica in blocco)
                    val pageState by produceState<PhotoLoad>(
                        initialValue = PhotoLoad.Loading, photoRefs[page]
                    ) {
                        value = loadDressingPhoto(context, photoRefs[page], fullQuality = true)
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (val ps = pageState) {
                            is PhotoLoad.Ok -> {
                                val isCurrent = pagerState.currentPage == page
                                Image(
                                    bitmap = ps.bitmap.asImageBitmap(),
                                    contentDescription = loc4(
                                        it = "Foto lesione ${page + 1} di ${photoRefs.size}",
                                        en = "Wound photo ${page + 1} of ${photoRefs.size}",
                                        fr = "Photo de la plaie ${page + 1} sur ${photoRefs.size}",
                                        es = "Foto de la herida ${page + 1} de ${photoRefs.size}"
                                    ),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(page, isCurrent) {
                                            if (isCurrent) pagerZoomGesture()
                                        }
                                        .graphicsLayer(
                                            scaleX = if (isCurrent) imgScale else 1f,
                                            scaleY = if (isCurrent) imgScale else 1f,
                                            translationX = if (isCurrent) imgOffset.x else 0f,
                                            translationY = if (isCurrent) imgOffset.y else 0f
                                        )
                                )
                            }
                            PhotoLoad.Loading -> CircularProgressIndicator(color = Color.White)
                            PhotoLoad.Failed -> Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = loc4(it = "Foto non disponibile", en = "Photo unavailable", fr = "Photo indisponible", es = "Foto no disponible"),
                                modifier = Modifier.size(48.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                // Barra alta: titolo + contatore K/N + chiudi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${woundLocationLabel(event.woundLocation, lang)} - ${dateFormat.format(Date(event.eventCreatedAt))}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${pagerState.currentPage + 1}/${photoRefs.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    IconButton(onClick = { showFullscreenImage = false }) {
                        Icon(Icons.Default.Close, contentDescription = loc4(it = "Chiudi", en = "Close", fr = "Fermer", es = "Cerrar"), tint = Color.White)
                    }
                }
                // Indicatore pallini in basso
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(photoRefs.size) { i ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == pagerState.currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
    // Fullscreen image dialog con pinch-to-zoom (una sola foto: identico a prima)
    else if (showFullscreenImage && photoBitmap != null) {
        var imgScale by remember { mutableFloatStateOf(1f) }
        var imgOffset by remember { mutableStateOf(Offset.Zero) }
        // Versione a piena definizione (2048px): la miniatura 800px fa da anteprima
        // immediata, poi l'immagine si affina quando la piena risoluzione è pronta.
        val fullState by produceState<PhotoLoad>(PhotoLoad.Loading, event.photoUri) {
            value = loadDressingPhoto(context, event.photoUri, fullQuality = true)
        }
        val fsBitmap = (fullState as? PhotoLoad.Ok)?.bitmap ?: photoBitmap!!
        val imgTransformState = rememberTransformableState { zoomChange, offsetChange, _ ->
            imgScale = (imgScale * zoomChange).coerceIn(1f, 5f)
            imgOffset = if (imgScale > 1f) {
                Offset(
                    x = (imgOffset.x + offsetChange.x).coerceIn(-500f, 500f),
                    y = (imgOffset.y + offsetChange.y).coerceIn(-500f, 500f)
                )
            } else Offset.Zero
        }
        Dialog(
            onDismissRequest = { showFullscreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = fsBitmap.asImageBitmap(),
                    contentDescription = loc4(it = "Foto lesione ingrandita", en = "Wound photo enlarged", fr = "Photo de la plaie agrandie", es = "Foto de la herida ampliada"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = imgTransformState)
                        .graphicsLayer(
                            scaleX = imgScale,
                            scaleY = imgScale,
                            translationX = imgOffset.x,
                            translationY = imgOffset.y
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${woundLocationLabel(event.woundLocation, lang)} - ${dateFormat.format(Date(event.eventCreatedAt))}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        // weight(1f) come nella variante carosello: senza, un titolo lungo
                        // schiacciava la X di chiusura
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showFullscreenImage = false }) {
                        Icon(Icons.Default.Close, contentDescription = loc4(it = "Chiudi", en = "Close", fr = "Fermer", es = "Cerrar"), tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TissueBar(black: Float, yellow: Float, red: Float, pink: Float) {
    val total = black + yellow + red + pink
    if (total <= 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        if (black > 0) {
            Box(
                modifier = Modifier
                    .weight(black / total)
                    .fillMaxHeight()
                    .background(com.example.woundmanagement.ui.theme.TissueBlack)
            )
        }
        if (yellow > 0) {
            Box(
                modifier = Modifier
                    .weight(yellow / total)
                    .fillMaxHeight()
                    .background(com.example.woundmanagement.ui.theme.TissueYellow)
            )
        }
        if (red > 0) {
            Box(
                modifier = Modifier
                    .weight(red / total)
                    .fillMaxHeight()
                    .background(com.example.woundmanagement.ui.theme.TissueRed)
            )
        }
        if (pink > 0) {
            Box(
                modifier = Modifier
                    .weight(pink / total)
                    .fillMaxHeight()
                    .background(com.example.woundmanagement.ui.theme.TissuePink)
            )
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(value, style = MaterialTheme.typography.labelMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientInfoDialog(
    patient: PatientEntity,
    onDismiss: () -> Unit,
    onSave: (PatientEntity) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(patient.name) }
    var surname by remember { mutableStateOf(patient.surname) }
    var fiscalCode by remember { mutableStateOf(patient.fiscalCode) }
    // Data di nascita come in NewPatientDialog: lo stato contiene SOLO cifre (max 8),
    // gli slash di GG/MM/AAAA sono visivi (DateSlashTransformation). Il valore esistente
    // arriva con gli slash → si inizializza con le sole cifre.
    // Valori LEGACY non canonici (es. "1/2/1990"): estrarne le cifre produrrebbe un
    // display corrotto ("12/19/90") che l'utente potrebbe "correggere" salvando una data
    // diversa → il campo parte VUOTO e il valore attuale è mostrato nel supporting text;
    // finché non si digita nulla, l'originale viene conservato tale e quale.
    val isLegacyDate = remember { patient.birthDate.isNotBlank() && !DATE_REGEX.matches(patient.birthDate) }
    val originalBirthDigits = remember {
        if (isLegacyDate) "" else patient.birthDate.filter { it.isDigit() }.take(8)
    }
    var birthDate by remember { mutableStateOf(originalBirthDigits) }
    // true dal primo tasto digitato: distingue "mai toccato" (conserva l'originale,
    // anche legacy) da "svuotato apposta" (salva vuoto).
    var dateTouched by remember { mutableStateOf(false) }
    var ward by remember { mutableStateOf(patient.ward) }
    var bed by remember { mutableStateOf(patient.bed) }
    var notes by remember { mutableStateOf(patient.notes) }
    // Al confine (validazione DATE_REGEX / salvataggio) la data si ricompone con gli slash;
    // se il campo non è stato toccato si conserva ESATTAMENTE il valore originale
    // (anche legacy non valido: resta segnalato ma non blocca, come prima).
    val birthDateSlashed =
        if (!dateTouched) patient.birthDate else dateDigitsToSlashed(birthDate)
    // Validazione coerente con la creazione (CF/data): si salva solo se validi (o vuoti).
    val cfError = fiscalCode.isNotBlank() && !CF_REGEX.matches(fiscalCode)
    // Il valore legacy non toccato non è un errore dell'utente: si conserva senza
    // allarmi (l'hint sotto il campo spiega come correggerlo).
    val dateError = dateTouched && birthDateSlashed.isNotBlank() && !DATE_REGEX.matches(birthDateSlashed)
    // Come NewPatientDialog: gli errori sui campi obbligatori compaiono al tap su Salva,
    // il bottone resta sempre attivo (mai disabilitato in silenzio).
    var showErrors by remember { mutableStateOf(false) }

    // Chiusura guardata: in modifica con campi cambiati chiede conferma dello scarto.
    val modified = name != patient.name || surname != patient.surname ||
        fiscalCode != patient.fiscalCode || (dateTouched && birthDateSlashed != patient.birthDate) ||
        ward != patient.ward || bed != patient.bed || notes != patient.notes
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val requestClose = { if (isEditing && modified) showDiscardConfirm = true else onDismiss() }
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(S.unsavedExitTitle) },
            text = { Text(S.unsavedExitMessage) },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onDismiss() }) { Text(S.unsavedExitConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text(S.cancel) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = requestClose,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isEditing) S.editPatientTitle else S.patientInfo)
                if (!isEditing) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = loc4(it = "Modifica", en = "Edit", fr = "Modifier", es = "Editar"))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        label = { Text(loc4(it = "Nome", en = "First name", fr = "Prénom", es = "Nombre")) },
                        isError = showErrors && name.isBlank(),
                        supportingText = if (showErrors && name.isBlank()) {{ Text(S.requiredField) }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { if (it.length <= 50) surname = it },
                        label = { Text(loc4(it = "Cognome", en = "Surname", fr = "Nom", es = "Apellido")) },
                        isError = showErrors && surname.isBlank(),
                        supportingText = if (showErrors && surname.isBlank()) {{ Text(S.requiredField) }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fiscalCode,
                        // Solo lettere/cifre maiuscole, max 16 (formato CF)
                        onValueChange = { fiscalCode = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(16) },
                        label = { Text(S.fiscalCode) },
                        isError = cfError,
                        supportingText = if (cfError) {{ Text(S.fiscalCodeInvalid) }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = birthDate,
                        // Solo cifre (max 8): gli slash li disegna DateSlashTransformation,
                        // così la tastiera numerica basta (niente tasto "/" richiesto)
                        onValueChange = { dateTouched = true; birthDate = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text(loc4(it = "Data di nascita", en = "Date of birth", fr = "Date de naissance", es = "Fecha de nacimiento")) },
                        isError = dateError,
                        supportingText = when {
                            dateError -> {{ Text(loc4(it = "Formato: GG/MM/AAAA", en = "Format: DD/MM/YYYY", fr = "Format : JJ/MM/AAAA", es = "Formato: DD/MM/AAAA")) }}
                            isLegacyDate && !dateTouched -> {{
                                Text(loc4(
                                    it = "Valore attuale: ${patient.birthDate} — digita per correggere",
                                    en = "Current value: ${patient.birthDate} — type to correct",
                                    fr = "Valeur actuelle : ${patient.birthDate} — saisir pour corriger",
                                    es = "Valor actual: ${patient.birthDate} — escriba para corregir"
                                ))
                            }}
                            else -> null
                        },
                        visualTransformation = DateSlashTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ward,
                            onValueChange = { if (it.length <= 50) ward = it },
                            label = { Text(S.ward) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bed,
                            onValueChange = { if (it.length <= 20) bed = it },
                            label = { Text(S.bed) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 500) notes = it },
                        label = { Text(S.notes) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                } else {
                    // View mode
                    InfoRow(loc4(it = "Nome", en = "Name", fr = "Nom", es = "Nombre"), "${patient.name} ${patient.surname}")
                    if (patient.fiscalCode.isNotBlank()) {
                        InfoRow(loc4(it = "Codice Fiscale", en = "Fiscal Code", fr = "Code fiscal", es = "Código fiscal"), patient.fiscalCode)
                    }
                    if (patient.birthDate.isNotBlank()) {
                        InfoRow(loc4(it = "Data di nascita", en = "Date of birth", fr = "Date de naissance", es = "Fecha de nacimiento"), patient.birthDate)
                    }
                    if (patient.ward.isNotBlank()) {
                        InfoRow(loc4(it = "Reparto", en = "Ward", fr = "Service", es = "Unidad"), patient.ward)
                    }
                    if (patient.bed.isNotBlank()) {
                        InfoRow(loc4(it = "Letto", en = "Bed", fr = "Lit", es = "Cama"), patient.bed)
                    }
                    if (patient.notes.isNotBlank()) {
                        InfoRow(loc4(it = "Note", en = "Notes", fr = "Notes", es = "Notas"), patient.notes)
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        // C8c: come NewPatientDialog, niente salvataggio con nome/cognome vuoti
                        // né con CF/data resi non validi in questa modifica; un CF/data già
                        // salvati non validi (import backup) restano segnalati ma non bloccano
                        val canSave = name.isNotBlank() && surname.isNotBlank() &&
                            !invalidEditBlocksSave(fiscalCode, patient.fiscalCode, CF_REGEX) &&
                            !invalidEditBlocksSave(birthDateSlashed, patient.birthDate, DATE_REGEX)
                        if (!canSave) { showErrors = true; return@Button }
                        val updatedPatient = patient.copy(
                            name = name,
                            surname = surname,
                            fiscalCode = fiscalCode,
                            birthDate = birthDateSlashed,
                            ward = ward,
                            bed = bed,
                            notes = notes
                        )
                        onSave(updatedPatient)
                    }
                ) {
                    Text(S.save)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(S.close)
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { if (modified) showDiscardConfirm = true else isEditing = false }) {
                    Text(S.cancel)
                }
            }
        }
    )
}

// ── Etichette localizzate per i codici enum salvati nel DB (B6) ──
// Stesso schema di PatientReportPdf: Enum.valueOf(code).label(lang), con fallback al
// codice leggibile se il valore salvato non è riconosciuto.
private fun woundTypeLabel(code: String, lang: AppLanguage): String =
    runCatching { WoundType.valueOf(code) }.getOrNull()?.label(lang)
        ?: code.replace("_", " ")

/** Tipo lesione da mostrare: per ALTRO con testo libero → "Altro: <specifica>". */
private fun woundTypeDisplay(code: String, typeOther: String, lang: AppLanguage): String {
    val base = woundTypeLabel(code, lang)
    return if (code == "ALTRO" && typeOther.isNotBlank()) "$base: $typeOther" else base
}

private fun exudateLabel(code: String, lang: AppLanguage): String =
    runCatching { ExudateLevel.valueOf(code) }.getOrNull()?.label(lang)
        ?: code.replace("_", " ")

private fun woundLocationLabel(code: String, lang: AppLanguage): String =
    runCatching { WoundLocation.valueOf(code) }.getOrNull()?.label(lang)
        ?: BodyZone.entries.firstOrNull { it.name == code }?.label(lang)
        ?: code.replace("_", " ")

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Campo medicazione strutturato: "ALTRO:testo" → testo libero; altrimenti label enum. */
private fun primaryDressingDisplay(raw: String, lang: com.example.woundmanagement.ui.strings.AppLanguage): String = when {
    raw.startsWith("ALTRO:") -> raw.removePrefix("ALTRO:")
    else -> runCatching { com.example.woundmanagement.domain.model.PrimaryDressing.valueOf(raw) }.getOrNull()?.label(lang) ?: "—"
}

private fun secondaryDressingDisplay(raw: String, lang: com.example.woundmanagement.ui.strings.AppLanguage): String = when {
    raw.startsWith("ALTRO:") -> raw.removePrefix("ALTRO:")
    else -> runCatching { com.example.woundmanagement.domain.model.SecondaryDressing.valueOf(raw) }.getOrNull()?.label(lang) ?: "—"
}
