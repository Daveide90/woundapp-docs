package com.example.woundmanagement

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.security.AppPreferences
import com.example.woundmanagement.security.PhotoManager
import com.example.woundmanagement.ui.components.MdrBanner
import com.example.woundmanagement.ui.screens.*
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.Jurisdiction
import com.example.woundmanagement.ui.strings.LocalJurisdiction
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.viewmodel.DressingFlowViewModel
import com.example.woundmanagement.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minuti

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Country : Screen("country", "Country", { Icon(Icons.Default.Public, contentDescription = null) })
    object Consent : Screen("consent", "Privacy", { Icon(Icons.Default.Security, contentDescription = null) })
    object Login : Screen("login", "Login", { Icon(Icons.Default.Lock, contentDescription = null) })
    object Patients : Screen("patients", "Pazienti", { Icon(Icons.Default.People, contentDescription = null) })
    object Settings : Screen("settings", "Impostazioni", { Icon(Icons.Default.Settings, contentDescription = null) })
}

@Composable
fun WoundManagementNavHost(
    importUri: Uri? = null,
    onImportConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var appLanguage by remember {
        // Mappa la lingua di sistema su una delle lingue supportate.
        // fr->FR, es->ES, en->EN, qualsiasi altra -> IT (fallback storico).
        fun systemLanguage(): AppLanguage = when (java.util.Locale.getDefault().language) {
            "en" -> AppLanguage.EN
            "fr" -> AppLanguage.FR
            "es" -> AppLanguage.ES
            else -> AppLanguage.IT
        }
        val saved = AppPreferences.getLanguage(context)
        val lang = when (saved) {
            "it" -> AppLanguage.IT
            "en" -> AppLanguage.EN
            "fr" -> AppLanguage.FR
            "es" -> AppLanguage.ES
            // "auto" o primo avvio: usa la lingua di sistema
            else -> systemLanguage()
        }
        mutableStateOf(lang)
    }

    // Giurisdizione scelta dall'utente; ri-leggibile quando cambia.
    var jurisdiction by remember {
        mutableStateOf(AppPreferences.getJurisdiction(context))
    }

    CompositionLocalProvider(
        LocalLanguage provides appLanguage,
        LocalJurisdiction provides jurisdiction
    ) {
    WoundManagementNavHostContent(
        onLanguageChanged = { lang ->
            AppPreferences.setLanguage(context, lang.code)
            appLanguage = lang
        },
        onRegionChanged = { code ->
            val j = runCatching { Jurisdiction.valueOf(code) }.getOrDefault(Jurisdiction.EU)
            AppPreferences.setJurisdiction(context, j)
            jurisdiction = j
        },
        importUri = importUri,
        onImportConsumed = onImportConsumed
    )
    }
}

@Composable
private fun WoundManagementNavHostContent(
    onLanguageChanged: (AppLanguage) -> Unit,
    onRegionChanged: (String) -> Unit,
    importUri: Uri? = null,
    onImportConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.Patients, Screen.Settings)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Pre-risolto qui (contesto @Composable) per l'uso nelle callback di salvataggio.
    val savedMessage = S.saved
    // Mostra una conferma "Salvato" e poi naviga indietro: feedback visibile senza
    // cambiare la destinazione di navigazione.
    fun saveAndPop() {
        coroutineScope.launch { snackbarHostState.showSnackbar(savedMessage) }
        navController.popBackStack()
    }
    // NB: NON accedere al database qui (pre-login). L'audit log si crea al volo nei punti
    // post-login: il DB si apre solo dopo l'autenticazione (vedi WoundManagementApp.database).
    fun auditRepo() = AuditRepository(WoundManagementApp.instance.database.auditDao())
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Autenticazione avvenuta in QUESTA sessione (solo in memoria, NON persistita): un
    // file aperto dal file manager apre il ripristino solo dopo un login effettivo.
    var authenticatedThisSession by remember { mutableStateOf(false) }

    // File aperto/condiviso dal file manager: solo dopo un login effettivo in questa
    // sessione (authenticatedThisSession) e su rotta non protetta, apri la schermata di
    // ripristino con quel file già selezionato.
    LaunchedEffect(importUri, currentRoute) {
        val protectedRoutes = setOf(Screen.Login.route, Screen.Consent.route, Screen.Country.route)
        if (importUri != null && currentRoute != null && authenticatedThisSession &&
            currentRoute !in protectedRoutes && currentRoute != "restore") {
            navController.navigate("restore")
        }
    }

    val showBottomBar = currentRoute in bottomItems.map { it.route }
    val showTopBar = currentRoute != Screen.Login.route &&
        currentRoute != Screen.Consent.route &&
        currentRoute != Screen.Country.route

    // Determina la destinazione iniziale: prima il Paese (primo avvio), poi il consenso, poi il login.
    val startDestination = remember {
        when {
            !AppPreferences.isRegionSet(context) -> Screen.Country.route
            !AppPreferences.isConsentGiven(context) -> Screen.Consent.route
            else -> Screen.Login.route
        }
    }

    // Timeout sessione: controlla inattività quando l'app torna in foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val route = navController.currentBackStackEntry?.destination?.route
                    ?: return@LifecycleEventObserver
                val protectedRoutes = setOf(Screen.Login.route, Screen.Consent.route, Screen.Country.route)
                if (route !in protectedRoutes) {
                    val lastActivity = AppPreferences.getLastActivityTime(context)
                    if (lastActivity > 0 &&
                        System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS) {
                        coroutineScope.launch {
                            auditRepo().log("SESSION_TIMEOUT", "SESSION", details = "Sessione scaduta per inattività")
                            // Dopo il log: chiudi il DB così i dati non restano decifrati in RAM,
                            // e svuota la cache degli export in chiaro.
                            com.example.woundmanagement.data.db.AppDatabase.closeInstance()
                            WoundManagementApp.instance.clearExportCache()
                        }
                        authenticatedThisSession = false
                        // C10b: svuota la cache in memoria delle foto decifrate a fine sessione
                        PhotoManager.clearCache()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Aggiorna il timestamp di attività ad ogni cambio di rotta
    LaunchedEffect(currentRoute) {
        val protectedRoutes = setOf(Screen.Login.route, Screen.Consent.route, Screen.Country.route)
        if (currentRoute !in protectedRoutes) {
            AppPreferences.updateLastActivityTime(context)
        }
    }

    // Controllo periodico inattività anche in foreground
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            val route = navController.currentBackStackEntry?.destination?.route ?: continue
            val protectedRoutes = setOf(Screen.Login.route, Screen.Consent.route, Screen.Country.route)
            if (route !in protectedRoutes) {
                val lastActivity = AppPreferences.getLastActivityTime(context)
                if (lastActivity > 0 &&
                    System.currentTimeMillis() - lastActivity > SESSION_TIMEOUT_MS) {
                    auditRepo().log("SESSION_TIMEOUT", "SESSION",
                        details = "Sessione scaduta per inattività")
                    com.example.woundmanagement.data.db.AppDatabase.closeInstance()
                    WoundManagementApp.instance.clearExportCache()
                    authenticatedThisSession = false
                    // C10b: svuota la cache in memoria delle foto decifrate a fine sessione
                    PhotoManager.clearCache()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var lastUpdate = 0L
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 10_000) {
                            lastUpdate = now
                            AppPreferences.updateLastActivityTime(context)
                        }
                    }
                }
            }
    ) {
    Scaffold(
        topBar = { if (showTopBar) MdrBanner() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(if (screen == Screen.Patients) S.patients else S.infoMdr) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Country.route) {
                CountrySelectionScreen(
                    onCountrySelected = { code ->
                        onRegionChanged(code)
                        val next = if (AppPreferences.isConsentGiven(context)) {
                            Screen.Login.route
                        } else {
                            Screen.Consent.route
                        }
                        navController.navigate(next) {
                            popUpTo(Screen.Country.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Consent.route) {
                PrivacyConsentScreen(
                    onAccept = {
                        AppPreferences.setConsentGiven(context)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Consent.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        AppPreferences.updateLastActivityTime(context)
                        authenticatedThisSession = true
                        coroutineScope.launch {
                            auditRepo().log("LOGIN", "SESSION", details = "Accesso autenticato")
                        }
                        navController.navigate(Screen.Patients.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Patients.route) {
                val vm: PatientListViewModel = viewModel(factory = PatientListViewModel.Factory)
                val patients by vm.patientsWithRisk.collectAsState(initial = emptyList())
                var showDialog by remember { mutableStateOf(false) }

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { showDialog = true },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                            containerColor = com.example.woundmanagement.ui.theme.BrandIndigo,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ) {
                            Icon(Icons.Default.Add, contentDescription = S.newPatient)
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        PatientListScreen(
                            patients = patients,
                            onPatientClick = { navController.navigate("patient/$it") },
                            onAddPatient = { showDialog = true },
                            onSearch = { vm.search(it) }
                        )
                    }
                }

                if (showDialog) {
                    NewPatientDialog(
                        onDismiss = { showDialog = false },
                        onConfirm = { patient ->
                            vm.addPatient(patient)
                            showDialog = false
                        }
                    )
                }
            }

            composable(
                "patient/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: return@composable
                val vm: PatientDetailViewModel = viewModel(factory = PatientDetailViewModel.factory(patientId))
                val patient by vm.patient.collectAsState(initial = null)
                val assessments by vm.assessments.collectAsState(initial = emptyList())
                val dressingHistory by vm.dressingHistory.collectAsState(initial = emptyList())
                val scaleHistoryData by vm.scaleHistoryData.collectAsState(initial = null)
                val db = com.example.woundmanagement.WoundManagementApp.instance.database

                PatientDetailScreen(
                    patient = patient,
                    assessments = assessments,
                    dressingHistory = dressingHistory,
                    scaleHistoryData = scaleHistoryData,
                    onNewAssessment = { location -> navController.navigate("newAssessment/$patientId/$location") },
                    onAssessmentClick = { navController.navigate("dressingFlow/$it") },
                    onNortonClick = { navController.navigate("norton/$patientId") },
                    onNRS2002Click = { navController.navigate("nrs2002/$patientId") },
                    onPainNRSClick = { navController.navigate("painNrs/$patientId") },
                    onPatientUpdate = { vm.updatePatient(it, AuditRepository(db.auditDao())) },
                    onDeletePatient = {
                        vm.deletePatient(AuditRepository(db.auditDao()), context) {
                            // La cancellazione gira su uno scope non cancellabile: al termine
                            // l'utente potrebbe essere già altrove (back manuale, timeout).
                            // Pop solo se siamo ancora sulla scheda del paziente.
                            if (navController.currentDestination?.route?.startsWith("patient/") == true) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onDeleteWound = { woundId ->
                        vm.deleteWound(AuditRepository(db.auditDao()), context, woundId) {}
                    },
                    onUpdateWoundNotes = { woundId, notes ->
                        vm.updateWoundNotes(AuditRepository(db.auditDao()), woundId, notes)
                    },
                    onDeleteDressing = { eventId ->
                        vm.deleteDressing(AuditRepository(db.auditDao()), context, eventId) {}
                    },
                    onUpdateDressing = { eventId, newDate, newNotes ->
                        vm.updateDressingDateNotes(AuditRepository(db.auditDao()), eventId, newDate, newNotes)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "newAssessment/{patientId}/{location}",
                arguments = listOf(
                    navArgument("patientId") { type = NavType.LongType },
                    navArgument("location") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: return@composable
                val location = backStackEntry.arguments?.getString("location") ?: "SACRO"
                val vm: NewAssessmentViewModel = viewModel(factory = NewAssessmentViewModel.Factory)

                NewAssessmentScreen(
                    patientId = patientId,
                    location = location,
                    onSave = { assessment, onCreated ->
                        vm.save(assessment, onCreated)
                    },
                    onWoundCreated = { woundId ->
                        // new=true: lesione appena creata. Se l'assessment viene abbandonato
                        // senza salvare alcuna medicazione, la lesione va rimossa (niente orfani).
                        navController.navigate("dressingFlow/$woundId?new=true") {
                            popUpTo("patient/$patientId") { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "dressingFlow/{assessmentId}?new={new}",
                arguments = listOf(
                    navArgument("assessmentId") { type = NavType.LongType },
                    navArgument("new") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val assessmentId = backStackEntry.arguments?.getLong("assessmentId") ?: return@composable
                val isNewWound = backStackEntry.arguments?.getBoolean("new") ?: false
                val vm: DressingFlowViewModel = viewModel(factory = DressingFlowViewModel.Factory)
                val saveState by vm.saveState.collectAsState()

                LaunchedEffect(saveState) {
                    if (saveState is DressingFlowViewModel.SaveState.Success) {
                        // La bozza è già stata cancellata dentro saveDressing (stesso scope
                        // dell'insert): qui restano solo navigazione e feedback.
                        // Pop PRIMA della snackbar: showSnackbar sospende per ~4s, durante i
                        // quali il pulsante Salva tornerebbe attivo -> doppio INSERT. Navigando
                        // via subito si elimina del tutto quella finestra.
                        navController.popBackStack()
                        // La snackbar va lanciata su uno scope che SOPRAVVIVE al pop: questo
                        // LaunchedEffect appartiene alla schermata che stiamo chiudendo, quindi
                        // la sua coroutine viene cancellata e il messaggio sparirebbe subito
                        // (l'utente non vedeva la conferma del salvataggio).
                        coroutineScope.launch { snackbarHostState.showSnackbar(savedMessage) }
                    }
                }

                DressingFlowScreen(
                    assessmentId = assessmentId,
                    onSave = { event, extraPhotoUris ->
                        vm.saveDressing(event, extraPhotoUris, clearDraft = {
                            com.example.woundmanagement.security.DressingDraftManager.clear(context, assessmentId)
                        })
                    },
                    onBack = {
                        // Uscita senza salvare: se la lesione è appena stata creata e non ha
                        // ancora nessuna medicazione, la rimuoviamo (assessment abbandonato).
                        // NON durante un salvataggio in corso: la SELECT vuota correrebbe con
                        // l'insert (FK CASCADE) e cancellerebbe la medicazione appena inserita.
                        if (isNewWound && saveState !is DressingFlowViewModel.SaveState.Saving) {
                            val db = WoundManagementApp.instance.database
                            WoundManagementApp.instance.applicationScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                runCatching {
                                    if (db.dressingEventDao().getByAssessmentOnce(assessmentId).isEmpty()) {
                                        db.woundAssessmentDao().getById(assessmentId)?.let {
                                            db.woundAssessmentDao().delete(it)
                                        }
                                    }
                                }
                            }
                        }
                        navController.popBackStack()
                    },
                    saveState = saveState
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onAuditLog = { navController.navigate("auditLog") },
                    onBackup = { navController.navigate("backup") },
                    onRestore = { navController.navigate("restore") },
                    onLanguageChanged = onLanguageChanged,
                    onCountryChanged = { code ->
                        // Cambio area/giurisdizione dalle Impostazioni: se cambia la
                        // giurisdizione, il consenso decade (isConsentGiven) e l'utente
                        // deve ri-accettare con il testo legale corretto.
                        val prevJurisdiction = AppPreferences.getJurisdiction(context)
                        onRegionChanged(code)
                        val newJurisdiction = AppPreferences.getJurisdiction(context)
                        if (newJurisdiction != prevJurisdiction) {
                            navController.navigate(Screen.Consent.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Pagina BACKUP (sola creazione)
            composable("backup") {
                val vm: com.example.woundmanagement.ui.viewmodel.BackupViewModel = viewModel(
                    factory = com.example.woundmanagement.ui.viewmodel.BackupViewModel.Factory
                )
                BackupScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    mode = BackupScreenMode.CREATE
                )
            }

            // Pagina RIPRISTINO (separata; riceve anche il file aperto dal file manager)
            composable("restore") {
                val vm: com.example.woundmanagement.ui.viewmodel.BackupViewModel = viewModel(
                    factory = com.example.woundmanagement.ui.viewmodel.BackupViewModel.Factory
                )
                BackupScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    mode = BackupScreenMode.RESTORE,
                    initialUri = importUri,
                    onUriConsumed = onImportConsumed
                )
            }

            composable("auditLog") {
                val vm: AuditLogViewModel = viewModel(factory = AuditLogViewModel.Factory)
                val logs by vm.logs.collectAsState(initial = emptyList())
                AuditLogScreen(
                    auditLogs = logs,
                    onBack = { navController.popBackStack() }
                )
            }

            // Norton Assessment
            composable(
                "norton/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: return@composable
                val vm: AssessmentScalesViewModel = viewModel(factory = AssessmentScalesViewModel.factory(patientId))
                NortonAssessmentScreen(
                    patientId = patientId,
                    onSave = { score ->
                        vm.saveNorton(score) { saveAndPop() }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // NRS-2002 Assessment
            composable(
                "nrs2002/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: return@composable
                val vm: AssessmentScalesViewModel = viewModel(factory = AssessmentScalesViewModel.factory(patientId))
                NRS2002AssessmentScreen(
                    patientId = patientId,
                    onSave = { score ->
                        vm.saveNRS2002(score) { saveAndPop() }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Pain NRS Assessment
            composable(
                "painNrs/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: return@composable
                val vm: AssessmentScalesViewModel = viewModel(factory = AssessmentScalesViewModel.factory(patientId))
                PainNRSAssessmentScreen(
                    patientId = patientId,
                    onSave = { painScore, painLocation, painType, painCharacter, aggravating, relieving ->
                        vm.savePainNRS(painScore, painLocation, painType, painCharacter, aggravating, relieving) { saveAndPop() }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    } // Box
}
