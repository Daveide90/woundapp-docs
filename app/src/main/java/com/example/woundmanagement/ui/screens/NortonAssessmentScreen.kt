package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.domain.scoring.NortonScale
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NortonAssessmentScreen(
    patientId: Long,
    onSave: (NortonScale.NortonScore) -> Unit,
    onBack: () -> Unit
) {
    val en = LocalLanguage.current != AppLanguage.IT
    var condizioneFisica by remember { mutableIntStateOf(0) }
    var statoMentale by remember { mutableIntStateOf(0) }
    var attivita by remember { mutableIntStateOf(0) }
    var mobilita by remember { mutableIntStateOf(0) }
    var incontinenza by remember { mutableIntStateOf(0) }

    val currentScore = NortonScale.NortonScore(
        condizioneFisica, statoMentale, attivita, mobilita, incontinenza
    )
    val allFilled = condizioneFisica > 0 && statoMentale > 0 && attivita > 0 &&
        mobilita > 0 && incontinenza > 0

    // Guardia back: con item già selezionati chiede conferma prima di perdere l'inserimento
    val hasAnswers = condizioneFisica > 0 || statoMentale > 0 || attivita > 0 ||
        mobilita > 0 || incontinenza > 0
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
                        Text(S.nortonScale)
                        com.example.woundmanagement.ui.components.PatientSubtitle(patientLabel)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back)
                    }
                },
                actions = {
                    com.example.woundmanagement.ui.components.InfoIconButton(text = S.infoNorton)
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
            // Score summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (allFilled) "${S.score}: ${currentScore.total}/20" else S.fillAllFields,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items
            NortonItem(loc4(it = "Condizione Fisica", en = "Physical Condition", fr = "\u00C9tat physique", es = "Condici\u00F3n f\u00EDsica"), condizioneFisica, NortonScale.Descriptions.condizioneFisica(en)) { condizioneFisica = it }
            NortonItem(loc4(it = "Stato Mentale", en = "Mental State", fr = "\u00C9tat mental", es = "Estado mental"), statoMentale, NortonScale.Descriptions.statoMentale(en)) { statoMentale = it }
            NortonItem(loc4(it = "Attivit\u00E0", en = "Activity", fr = "Activit\u00E9", es = "Actividad"), attivita, NortonScale.Descriptions.attivita(en)) { attivita = it }
            NortonItem(loc4(it = "Mobilit\u00E0", en = "Mobility", fr = "Mobilit\u00E9", es = "Movilidad"), mobilita, NortonScale.Descriptions.mobilita(en)) { mobilita = it }
            NortonItem(loc4(it = "Incontinenza", en = "Incontinence", fr = "Incontinence", es = "Incontinencia"), incontinenza, NortonScale.Descriptions.incontinenza(en)) { incontinenza = it }

            Spacer(modifier = Modifier.height(24.dp))

            // Guardia anti doppio-tap: l'insert è asincrono e la schermata si chiude solo
            // a salvataggio riuscito, quindi due tocchi rapidi creavano due punteggi identici.
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
private fun NortonItem(
    title: String,
    value: Int,
    descriptions: Map<Int, String>,
    onValueChange: (Int) -> Unit
) {
    // Lista radio con descrizione visibile PRIMA della scelta: l'operatore vede
    // tutte le opzioni, non solo il numero (le chip mostravano la descrizione solo dopo).
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        for (i in 1..4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onValueChange(i) },
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
