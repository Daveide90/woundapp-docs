package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.domain.scoring.NRS2002Scale
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NRS2002AssessmentScreen(
    patientId: Long,
    onSave: (NRS2002Scale.NRS2002Score) -> Unit,
    onBack: () -> Unit
) {
    val en = LocalLanguage.current != AppLanguage.IT
    var nutritionalScore by remember { mutableIntStateOf(-1) }
    var diseaseSeverityScore by remember { mutableIntStateOf(-1) }
    var ageOver70 by remember { mutableStateOf(false) }

    val currentScore = NRS2002Scale.NRS2002Score(
        nutritionalScore.coerceAtLeast(0),
        diseaseSeverityScore.coerceAtLeast(0),
        ageOver70
    )
    val allFilled = nutritionalScore >= 0 && diseaseSeverityScore >= 0

    // Guardia back: con item già selezionati chiede conferma prima di perdere l'inserimento
    val hasAnswers = nutritionalScore >= 0 || diseaseSeverityScore >= 0 || ageOver70
    var showExitDialog by remember { mutableStateOf(false) }
    val requestBack = { if (hasAnswers) showExitDialog = true else onBack() }
    androidx.activity.compose.BackHandler(enabled = hasAnswers) { showExitDialog = true }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(S.unsavedExitTitle) },
            text = { Text(S.unsavedExitMessage) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) { Text(S.unsavedExitConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text(S.cancel) }
            }
        )
    }

    val patientLabel = com.example.woundmanagement.ui.components.rememberPatientLabel(patientId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(S.nrs2002Scale)
                        com.example.woundmanagement.ui.components.PatientSubtitle(patientLabel)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back)
                    }
                },
                actions = {
                    com.example.woundmanagement.ui.components.InfoIconButton(text = S.infoNRS2002)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Score summary (solo punteggio grezzo /7, nessuna interpretazione)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (allFilled) "${S.score}: ${currentScore.total}/7" else S.fillAllFields,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                loc4(it = "Stato nutrizionale", en = "Nutritional status", fr = "État nutritionnel", es = "Estado nutricional"),
                style = MaterialTheme.typography.titleMedium
            )
            NRS2002Item(nutritionalScore, NRS2002Scale.Descriptions.nutritionalScore(en)) { nutritionalScore = it }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                loc4(it = "Gravità di malattia", en = "Disease severity", fr = "Gravité de la maladie", es = "Gravedad de la enfermedad"),
                style = MaterialTheme.typography.titleMedium
            )
            NRS2002Item(diseaseSeverityScore, NRS2002Scale.Descriptions.diseaseSeverityScore(en)) { diseaseSeverityScore = it }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = ageOver70, onCheckedChange = { ageOver70 = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    NRS2002Scale.Descriptions.ageAdjustment(en),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Guardia anti doppio-tap: evita due punteggi identici (insert asincrono)
            var saving by remember { mutableStateOf(false) }
            Button(
                onClick = { if (!saving) { saving = true; onSave(currentScore) } },
                enabled = allFilled && !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (allFilled) S.saveAssessment else S.fillAllFields)
            }
        }
    }
}

@Composable
private fun NRS2002Item(
    value: Int,
    descriptions: Map<Int, String>,
    onValueChange: (Int) -> Unit
) {
    // Lista radio con descrizione visibile PRIMA della scelta (le chip mostravano
    // la descrizione solo dopo la selezione).
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        for (i in 0..3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onValueChange(i) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = value == i, onClick = { onValueChange(i) })
                Column(modifier = Modifier.weight(1f)) {
                    Text(descriptions[i] ?: "$i", style = MaterialTheme.typography.bodyMedium)
                    Text("$i", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
