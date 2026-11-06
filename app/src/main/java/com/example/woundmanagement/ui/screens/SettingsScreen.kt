package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.security.AppPreferences
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.Jurisdiction
import com.example.woundmanagement.ui.strings.LocalJurisdiction
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAuditLog: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    onLanguageChanged: (AppLanguage) -> Unit = {},
    onCountryChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = LocalLanguage.current
    val savedPref = remember { AppPreferences.getLanguage(context) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showThirdParty by remember { mutableStateOf(false) }
    // Giurisdizione corrente (fornita dal CompositionLocal: cambia se l'utente la aggiorna)
    val currentJurisdiction = LocalJurisdiction.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(S.settings) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back)
                }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lingua
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.language, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    var currentPref by remember { mutableStateOf(savedPref) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Auto (sistema)
                        FilterChip(
                            selected = currentPref == "auto",
                            onClick = {
                                currentPref = "auto"
                                AppPreferences.setLanguage(context, "auto")
                                val systemLang = java.util.Locale.getDefault().language
                                onLanguageChanged(
                                    when (systemLang) {
                                        "en" -> AppLanguage.EN
                                        "fr" -> AppLanguage.FR
                                        "es" -> AppLanguage.ES
                                        else -> AppLanguage.IT
                                    }
                                )
                            },
                            label = { Text("Auto") },
                            leadingIcon = if (currentPref == "auto") {{
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            }} else null
                        )
                        // Italiano
                        FilterChip(
                            selected = currentPref == "it",
                            onClick = {
                                currentPref = "it"
                                onLanguageChanged(AppLanguage.IT)
                            },
                            label = { Text("Italiano") }
                        )
                        // English
                        FilterChip(
                            selected = currentPref == "en",
                            onClick = {
                                currentPref = "en"
                                onLanguageChanged(AppLanguage.EN)
                            },
                            label = { Text("English") }
                        )
                        // Français
                        FilterChip(
                            selected = currentPref == "fr",
                            onClick = {
                                currentPref = "fr"
                                onLanguageChanged(AppLanguage.FR)
                            },
                            label = { Text("Français") }
                        )
                        // Español
                        FilterChip(
                            selected = currentPref == "es",
                            onClick = {
                                currentPref = "es"
                                onLanguageChanged(AppLanguage.ES)
                            },
                            label = { Text("Español") }
                        )
                    }
                }
            }

            // Paese / Giurisdizione
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCountryDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Public, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(S.countrySettingLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            jurisdictionDisplayValue(currentJurisdiction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Info app
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.appInfo, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(S.appName, "WoundApp")
                    InfoRow(S.version, "${com.example.woundmanagement.BuildConfig.VERSION_NAME} (build ${com.example.woundmanagement.BuildConfig.VERSION_CODE})")
                    InfoRow(S.type, S.appType)
                }
            }

            // Licenza d'uso: gratis per il singolo clinico, vietato l'uso istituzionale
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.licenseTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(S.licenseText, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        S.licenseContact,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Avvisi delle librerie di terzi: MIT, BSD e Apache chiedono che
                    // accompagnino anche l'APK, non solo il codice sorgente
                    OutlinedButton(onClick = { showThirdParty = true }) {
                        Text(S.thirdPartyLicenses)
                    }
                }
            }

            // Segnalazione bug: mailto precompilato con versione/build/dispositivo
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.reportBugText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    val bugContext = androidx.compose.ui.platform.LocalContext.current
                    val bugSubject = "WoundApp Docs ${com.example.woundmanagement.BuildConfig.VERSION_NAME} (build ${com.example.woundmanagement.BuildConfig.VERSION_CODE}) — ${S.reportBugSubject}"
                    val bugBody = S.reportBugBodyIntro + "\n\n\n—\n" +
                        "App: ${com.example.woundmanagement.BuildConfig.VERSION_NAME} (build ${com.example.woundmanagement.BuildConfig.VERSION_CODE})\n" +
                        "Dispositivo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                        "Android: ${android.os.Build.VERSION.RELEASE}\n\n" +
                        S.reportBugNoPatientData
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                bugContext.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:")
                                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("dev.davide@pm.me"))
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, bugSubject)
                                        putExtra(android.content.Intent.EXTRA_TEXT, bugBody)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.reportBugButton)
                    }
                }
            }

            // Disclaimer legale
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.importantWarnings, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(S.settingsDisclaimer, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(S.settingsTissueDisclaimer, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Privacy e sicurezza
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.privacySecurity, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(S.privacySecurityText, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(S.deletePatientInfo, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onAuditLog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.viewAuditLog)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.createBackup)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(S.restoreBackup)
                    }
                }
            }

            // Composizione tissutale
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.tissueCompositionTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(S.tissueCompositionText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showCountryDialog) {
        CountryPickerDialog(
            current = currentJurisdiction,
            onCountrySelected = { code ->
                onCountryChanged(code)
                showCountryDialog = false
            },
            onDismiss = { showCountryDialog = false }
        )
    }

    if (showThirdParty) {
        ThirdPartyLicensesDialog(onDismiss = { showThirdParty = false })
    }
}

/** Percorso negli asset del testo con autori, licenze e NOTICE dei componenti di terzi. */
internal const val THIRD_PARTY_LICENSES_ASSET = "licenze/licenze_di_terzi.txt"

@Composable
private fun ThirdPartyLicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember {
        runCatching {
            context.assets.open(THIRD_PARTY_LICENSES_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(S.thirdPartyLicenses) },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(S.close) }
        }
    )
}

/**
 * Etichetta leggibile per la giurisdizione attualmente selezionata.
 * Es. "Unione Europea / SEE", "Stati Uniti", "Resto del mondo".
 */
@Composable
private fun jurisdictionDisplayValue(j: Jurisdiction): String = when (j) {
    Jurisdiction.EU -> S.regionEu
    Jurisdiction.US -> S.regionUs
    Jurisdiction.ROW -> S.regionRow
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
