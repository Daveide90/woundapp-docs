package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.domain.scoring.PainNRSScale
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainNRSAssessmentScreen(
    patientId: Long,
    onSave: (Int, String, PainNRSScale.PainType, PainNRSScale.PainCharacter, String, String) -> Unit,
    onBack: () -> Unit
) {
    // Etichette brevi localizzate (4 lingue) per tipo e carattere del dolore.
    val painTypeLabels = mapOf(
        PainNRSScale.PainType.CONTINUO to loc4(it = "Continuo", en = "Continuous", fr = "Continue", es = "Continuo"),
        PainNRSScale.PainType.INTERMITTENTE to loc4(it = "Intermittente", en = "Intermittent", fr = "Intermittente", es = "Intermitente"),
        PainNRSScale.PainType.PROCEDURALE to loc4(it = "Procedurale", en = "Procedural", fr = "Procédurale", es = "Procedimental")
    )
    val painCharacterLabels = mapOf(
        PainNRSScale.PainCharacter.PUNGENTE to loc4(it = "Pungente", en = "Stabbing", fr = "Piquante", es = "Punzante"),
        PainNRSScale.PainCharacter.BRUCIANTE to loc4(it = "Bruciante", en = "Burning", fr = "Brûlante", es = "Quemante"),
        PainNRSScale.PainCharacter.PULSANTE to loc4(it = "Pulsante", en = "Throbbing", fr = "Pulsatile", es = "Pulsátil"),
        PainNRSScale.PainCharacter.SORDO to loc4(it = "Sordo", en = "Dull", fr = "Sourde", es = "Sordo"),
        PainNRSScale.PainCharacter.LANCINANTE to loc4(it = "Lancinante", en = "Lancinating", fr = "Lancinante", es = "Lancinante"),
        PainNRSScale.PainCharacter.COMPRESSIVO to loc4(it = "Compressivo", en = "Pressing", fr = "En étau", es = "Opresivo")
    )
    var painScore by remember { mutableIntStateOf(0) }
    var painLocation by remember { mutableStateOf("") }
    var painType by remember { mutableStateOf(PainNRSScale.PainType.CONTINUO) }
    var painCharacter by remember { mutableStateOf(PainNRSScale.PainCharacter.SORDO) }
    var aggravatingFactors by remember { mutableStateOf("") }
    var relievingFactors by remember { mutableStateOf("") }

    // Guardia back: con dati già inseriti chiede conferma prima di perdere l'inserimento
    val hasAnswers = painScore > 0 || painLocation.isNotBlank() ||
        aggravatingFactors.isNotBlank() || relievingFactors.isNotBlank()
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
                        Text(S.painNrsScale)
                        com.example.woundmanagement.ui.components.PatientSubtitle(patientLabel)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back)
                    }
                },
                actions = {
                    com.example.woundmanagement.ui.components.InfoIconButton(text = S.infoPainNrs)
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
                    Text(loc4(it = "Intensità: $painScore/10", en = "Intensity: $painScore/10", fr = "Intensité : $painScore/10", es = "Intensidad: $painScore/10"), style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pain Score Slider
            Text(loc4(it = "Intensità del dolore", en = "Pain Intensity", fr = "Intensité de la douleur", es = "Intensidad del dolor"), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = painScore.toFloat(),
                onValueChange = { painScore = it.toInt() },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .systemGestureExclusion()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0", style = MaterialTheme.typography.labelSmall)
                Text("5", style = MaterialTheme.typography.labelSmall)
                Text("10", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pain location
            OutlinedTextField(
                value = painLocation,
                onValueChange = { if (it.length <= 200) painLocation = it },
                label = { Text(loc4(it = "Localizzazione del dolore", en = "Pain location", fr = "Localisation de la douleur", es = "Localización del dolor")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pain type
            Text(loc4(it = "Tipo di dolore", en = "Pain type", fr = "Type de douleur", es = "Tipo de dolor"), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PainNRSScale.PainType.entries.forEach { type ->
                    FilterChip(
                        selected = painType == type,
                        onClick = { painType = type },
                        label = { Text(painTypeLabels[type] ?: type.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pain character
            Text(loc4(it = "Carattere del dolore", en = "Pain character", fr = "Caractère de la douleur", es = "Carácter del dolor"), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                PainNRSScale.PainCharacter.entries.take(3).forEach { char ->
                    FilterChip(
                        selected = painCharacter == char,
                        onClick = { painCharacter = char },
                        label = { Text(painCharacterLabels[char] ?: char.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                PainNRSScale.PainCharacter.entries.drop(3).forEach { char ->
                    FilterChip(
                        selected = painCharacter == char,
                        onClick = { painCharacter = char },
                        label = { Text(painCharacterLabels[char] ?: char.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Factors
            OutlinedTextField(
                value = aggravatingFactors,
                onValueChange = { if (it.length <= 200) aggravatingFactors = it },
                label = { Text(loc4(it = "Fattori aggravanti", en = "Aggravating factors", fr = "Facteurs aggravants", es = "Factores agravantes")) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = relievingFactors,
                onValueChange = { if (it.length <= 200) relievingFactors = it },
                label = { Text(loc4(it = "Fattori allevianti", en = "Relieving factors", fr = "Facteurs soulageants", es = "Factores que alivian")) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Guardia anti doppio-tap: evita due rilevazioni identiche (insert asincrono)
            var saving by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!saving) {
                        saving = true
                        onSave(painScore, painLocation, painType, painCharacter, aggravatingFactors, relievingFactors)
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(S.saveAssessment)
            }
        }
    }
}
