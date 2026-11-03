package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.ui.strings.Jurisdiction
import com.example.woundmanagement.ui.strings.S

/**
 * Le 3 macro-opzioni di giurisdizione selezionabili dall'utente.
 * Il codice ("EU"/"US"/"ROW") corrisponde a Jurisdiction.name e determina
 * l'informativa legale applicabile (vedi AppPreferences / PrivacyConsentScreen).
 */
internal data class RegionOption(val jurisdiction: Jurisdiction, val label: @Composable () -> String)

internal val REGION_OPTIONS = listOf(
    RegionOption(Jurisdiction.EU) { S.regionEu },
    RegionOption(Jurisdiction.US) { S.regionUs },
    RegionOption(Jurisdiction.ROW) { S.regionRow }
)

@Composable
fun CountrySelectionScreen(onCountrySelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            Icons.Default.Public,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(S.countryTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            S.countrySubtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                REGION_OPTIONS.forEach { option ->
                    RegionRow(
                        name = option.label(),
                        onClick = { onCountrySelected(option.jurisdiction.name) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RegionRow(
    name: String,
    onClick: () -> Unit,
    fontWeight: FontWeight? = null
) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = fontWeight,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

/**
 * Dialog per ri-selezionare l'area geografica/giurisdizione dalle Impostazioni
 * (stesse 3 macro-opzioni della prima scelta). Evidenzia quella corrente.
 */
@Composable
fun CountryPickerDialog(
    current: Jurisdiction,
    onCountrySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(S.countryTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    S.countrySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                REGION_OPTIONS.forEach { option ->
                    RegionRow(
                        name = option.label(),
                        onClick = { onCountrySelected(option.jurisdiction.name) },
                        fontWeight = if (option.jurisdiction == current) FontWeight.Bold else null
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(S.cancel) }
        }
    )
}
