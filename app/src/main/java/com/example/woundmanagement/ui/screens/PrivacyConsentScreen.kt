package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4

@Composable
fun PrivacyConsentScreen(onAccept: () -> Unit) {
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            Icons.Default.Security,
            contentDescription = loc4(it = "Privacy", en = "Privacy", fr = "Confidentialité", es = "Privacidad"),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(S.privacyTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (S.isEu) {
            Text(S.privacySubtitle, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (S.isUs) {
            Text(S.privacySubtitleUs, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (S.isEu) {
            // Natura dell'app
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        S.privacyAppNature,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Informativa GDPR completa
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    SectionTitle(S.privacySection1Title)
                    Text(S.privacySection1Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection2Title)
                    Text(S.privacySection2Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection3Title)
                    Text(S.privacySection3Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection4Title)
                    Text(S.privacySection4Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection5Title)
                    Text(S.privacySection5Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection6Title)
                    Text(S.privacySection6Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection7Title)
                    Text(S.privacySection7Text, style = MaterialTheme.typography.bodyMedium)

                    SectionTitle(S.privacySection8Title)
                    Text(S.privacySection8Text, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else if (S.isUs) {
            // Profilo Stati Uniti: natura dell'app (FDA) + dati (HIPAA)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        S.privacyAppNatureUs,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.privacyDataUs, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Profilo Resto del mondo: natura dell'app + dati (testo neutro)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        S.privacyAppNatureIntl,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(S.privacyDataIntl, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // EULA: UE (MDR+GDPR), USA (FDA/HIPAA) oppure neutro (Resto del mondo)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(S.eulaTitle)
                val eula = when {
                    S.isEu -> S.eulaText
                    S.isUs -> S.eulaTextUs
                    else -> S.eulaTextIntl
                }
                Text(eula, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                S.consentCheckbox,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAccept,
            enabled = accepted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(S.acceptAndContinue)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
