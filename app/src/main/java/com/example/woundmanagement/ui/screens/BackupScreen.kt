package com.example.woundmanagement.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.viewmodel.BackupViewModel
import kotlinx.coroutines.launch
import java.io.File

/** Modalità della schermata: solo creazione backup oppure solo ripristino (pagine separate). */
enum class BackupScreenMode { CREATE, RESTORE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
    mode: BackupScreenMode = BackupScreenMode.CREATE,
    initialUri: android.net.Uri? = null,
    onUriConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var backupPassword by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showRestorePassword by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showBackupInfo by remember { mutableStateOf(false) }
    var showRestoreInfo by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // File arrivato da "apri/condividi dal file manager": pre-seleziona e consuma.
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            restoreUri = initialUri
            onUriConsumed()
        }
    }

    // Pre-resolve composable strings for use in non-composable callbacks
    val strBackupSaved = S.backupSaved
    val strBackupSuccess = S.backupSuccess
    val strRestoreComplete = S.importComplete
    val strSaveError = S.saveError
    val strSendBackup = S.sendBackup
    val strNoFileMgr = S.noFileManager

    // Picker per scegliere dove salvare il file
    var pendingBackupFile by remember { mutableStateOf<File?>(null) }
    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val source = pendingBackupFile
        if (uri != null) {
            if (source == null) {
                // Il file era stato preparato prima che il processo venisse ricreato:
                // meglio dirlo che restare in silenzio (l'utente crederebbe di aver salvato).
                Toast.makeText(context, strSaveError, Toast.LENGTH_LONG).show()
            } else {
                // Copia su thread IO: un archivio da decine di MB copiato sul main thread
                // blocca la UI (ANR). applicationScope: se l'utente lascia la schermata a
                // metà copia il file di destinazione resterebbe troncato.
                val appContext = context.applicationContext
                com.example.woundmanagement.WoundManagementApp.instance.applicationScope.launch {
                    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            appContext.contentResolver.openOutputStream(uri)?.use { out ->
                                source.inputStream().use { inp -> inp.copyTo(out) }
                            } ?: error("stream non disponibile")
                        }.isSuccess
                    }
                    // applicationScope gira su Dispatchers.Default: il Toast va sul Main.
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(
                            appContext,
                            if (ok) strBackupSaved else strSaveError,
                            if (ok) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Strategia 1 — OpenDocument (ACTION_OPEN_DOCUMENT): apre DocumentsUI di sistema (SAF).
    // Memorizza solo il file; il ripristino parte dal pulsante dedicato.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) restoreUri = uri
    }

    // Strategia 2 — GetContent (ACTION_GET_CONTENT) "nudo", SENZA chooser. Su alcune ROM
    // OPEN_DOCUMENT non ha handler ma GET_CONTENT sì (gestori file di terze parti, Galleria…).
    val getContentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) restoreUri = uri
    }

    // Strategia 3 — ACTION_GET_CONTENT esplicito via StartActivityForResult (ultimo tentativo).
    val pickFallback = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) restoreUri = uri
    }

    // Popup snackbar quando backup/restore completato
    LaunchedEffect(state) {
        when (state) {
            is BackupViewModel.BackupState.BackupReady -> {
                snackbarMessage = strBackupSuccess
                showSnackbar = true
            }
            is BackupViewModel.BackupState.RestoreComplete -> {
                snackbarMessage = strRestoreComplete
                showSnackbar = true
                restoreUri = null
                restorePassword = ""
            }
            else -> {}
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(snackbarMessage, duration = SnackbarDuration.Short)
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == BackupScreenMode.CREATE) S.createBackup else S.restoreBackup) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Etichetta versione (per verificare quale build è installato)
            Text(
                "WoundApp ${com.example.woundmanagement.BuildConfig.VERSION_NAME} (build ${com.example.woundmanagement.BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Info collassabile ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier
                    .clickable { showBackupInfo = !showBackupInfo }
                    .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.backupInfo, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        Icon(
                            if (showBackupInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showBackupInfo) S.close else S.open
                        )
                    }
                    AnimatedVisibility(visible = showBackupInfo) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                S.backupInfoText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // ── STATO OPERAZIONE (tra info e crea backup) ──
            when (val s = state) {
                is BackupViewModel.BackupState.Working -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(S.operationInProgress, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                is BackupViewModel.BackupState.BackupReady -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(S.backupSuccess, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    pendingBackupFile = s.file
                                    try {
                                        // Nome proposto con data: più file distinguibili tra loro
                                        savePicker.launch(
                                            "woundapp_backup_" +
                                                java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                                                    .format(java.util.Date()) +
                                                ".woundbackup"
                                        )
                                    } catch (e: Throwable) {
                                        Toast.makeText(context, strSaveError, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(S.saveToPhone)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        s.file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    // NB: il file resta in cache (il consumer lo legge via FileProvider
                                    // DOPO il ritorno: cancellarlo subito romperebbe la condivisione).
                                    // TODO(C9a): valutare una cancellazione differita sicura; per ora i
                                    // residui vengono ripuliti a ogni avvio (WoundManagementApp.clearExportCache).
                                    context.startActivity(Intent.createChooser(shareIntent, strSendBackup))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(S.shareBackup)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.resetState() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(S.close)
                            }
                        }
                    }
                }

                is BackupViewModel.BackupState.PreviewReady -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(S.backupContent, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            BackupInfoRow(S.patientsLabel, "${s.summary.patients}")
                            BackupInfoRow(S.woundsLabel, "${s.summary.wounds}")
                            BackupInfoRow(S.dressingsLabel, "${s.summary.dressings}")
                            BackupInfoRow(S.photosLabel, "${s.summary.photos}")
                            BackupInfoRow(S.scalesLabel, "${s.summary.scales}")
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showRestoreConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(S.restoreMergeButton)
                            }
                        }
                    }
                }

                is BackupViewModel.BackupState.RestoreComplete -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(S.importComplete, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Esito del merge: pazienti nuovi vs già presenti (aggiornati).
                            // Il vecchio "X aggiunti" era fuorviante: l'import ora è un merge.
                            Text(
                                S.restoreMergeSummary
                                    .replace("{new}", "${s.result.newPatients}")
                                    .replace("{upd}", "${s.result.updatedPatients}"),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.resetState() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(S.close)
                            }
                        }
                    }
                }

                is BackupViewModel.BackupState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(S.error, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(backupErrorText(s.error), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.resetState() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(S.retry)
                            }
                        }
                    }
                }

                is BackupViewModel.BackupState.Idle -> { /* niente */ }
            }

            // ── Sezione Crea Backup (solo pagina CREATE) ──
            if (mode == BackupScreenMode.CREATE) Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.createBackup, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { if (it.length <= 128) backupPassword = it },
                        label = { Text(S.backupPassword) },
                        singleLine = true,
                        // Il motivo per cui Genera backup è disabilitato dev'essere visibile qui
                        isError = backupPassword.isNotEmpty() && backupPassword.length < 12,
                        supportingText = if (backupPassword.isNotEmpty() && backupPassword.length < 12) {
                            { Text(S.backupPasswordTooShort(backupPassword.length)) }
                        } else null,
                        visualTransformation = if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) S.hide else S.show
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Conferma password: un typo nella password rende il backup irrecuperabile
                    var confirmPassword by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { if (it.length <= 128) confirmPassword = it },
                        label = { Text(S.backupPasswordConfirm) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && confirmPassword != backupPassword,
                        supportingText = if (confirmPassword.isNotEmpty() && confirmPassword != backupPassword) {
                            { Text(S.backupPasswordMismatch) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.createBackup(context, backupPassword) },
                        // C7: minimo 12 caratteri per la CREAZIONE (il restore resta a 8 per
                        // poter decifrare i backup creati col vecchio minimo)
                        enabled = backupPassword.length >= 12 && confirmPassword == backupPassword &&
                            state !is BackupViewModel.BackupState.Working,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.generateBackup)
                    }
                }
            }

            // ── Sezione Ripristino (solo pagina RESTORE) ──
            if (mode == BackupScreenMode.RESTORE) Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.restoreBackup, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        // Icona "i": spiega backup completo, export singolo paziente, import additivo
                        com.example.woundmanagement.ui.components.InfoIconButton(
                            text = S.backupInfoDialogText,
                            title = S.backupInfoDialogTitle
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Info ripristino collassabile (import = merge non distruttivo)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier
                            .clickable { showRestoreInfo = !showRestoreInfo }
                            .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(S.restoreMergeTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f))
                                Icon(
                                    if (showRestoreInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            AnimatedVisibility(visible = showRestoreInfo) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        S.restoreMergeText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1) Scegli il file — in cima, così resta visibile anche con la tastiera aperta
                    OutlinedButton(
                        onClick = {
                            // Strategia 1: SAF OpenDocument (DocumentsUI di sistema).
                            try {
                                filePicker.launch(arrayOf("*/*"))
                            } catch (e1: Throwable) {
                                // Strategia 2: GetContent "nudo" (ACTION_GET_CONTENT, niente chooser).
                                try {
                                    getContentPicker.launch("*/*")
                                } catch (e2: Throwable) {
                                    // Strategia 3: ACTION_GET_CONTENT esplicito.
                                    try {
                                        val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                        }
                                        pickFallback.launch(pick)
                                    } catch (e3: Throwable) {
                                        if (com.example.woundmanagement.BuildConfig.DEBUG)
                                            android.util.Log.w("BackupPicker", "Nessun selettore file disponibile", e3)
                                        Toast.makeText(context, strNoFileMgr, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = state !is BackupViewModel.BackupState.Working,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.selectBackupFile)
                    }

                    if (restoreUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✓ " + S.fileSelected,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2) Password
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { if (it.length <= 128) restorePassword = it },
                        label = { Text(S.backupPasswordLabel) },
                        singleLine = true,
                        visualTransformation = if (showRestorePassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showRestorePassword = !showRestorePassword }) {
                                Icon(
                                    if (showRestorePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showRestorePassword) S.hide else S.show
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3) Avvia ripristino — servono file + password (l'ordine non conta)
                    Button(
                        onClick = {
                            val u = restoreUri
                            if (u != null && restorePassword.length >= 8) {
                                viewModel.previewRestore(context, u, restorePassword)
                            }
                        },
                        enabled = restoreUri != null && restorePassword.length >= 8 &&
                            state !is BackupViewModel.BackupState.Working,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.restoreBackup)
                    }
                }
            }
        }
    }

    // Dialog di conferma ripristino
    if (showRestoreConfirm) {
        val preview = state as? BackupViewModel.BackupState.PreviewReady
        if (preview != null) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(S.restoreConfirmTitle) },
                text = {
                    Text(S.restoreMergeConfirmText.replace("{n}", "${preview.summary.patients}"))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestoreConfirm = false
                            viewModel.confirmRestore(context, preview.uri, restorePassword)
                        }
                    ) {
                        Text(S.importButton)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) {
                        Text(S.cancel)
                    }
                }
            )
        }
    }
}

@Composable
private fun BackupInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun backupErrorText(error: BackupViewModel.BackupError): String = when (error) {
    BackupViewModel.BackupError.CREATE_FAILED -> S.errBackupCreate
    BackupViewModel.BackupError.WRONG_PASSWORD -> S.errBackupWrongPassword
    BackupViewModel.BackupError.READ_FAILED -> S.errBackupRead
    BackupViewModel.BackupError.RESTORE_FAILED -> S.errBackupRestore
}
