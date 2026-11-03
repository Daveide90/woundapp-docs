package com.example.woundmanagement.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import androidx.compose.ui.text.input.KeyboardType
import com.example.woundmanagement.domain.model.WoundOrigin
import com.example.woundmanagement.domain.model.WoundType
import com.example.woundmanagement.domain.model.locationDisplay
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4
import com.example.woundmanagement.ui.viewmodel.NewAssessmentViewModel

// Parsing dimensioni tollerante alla virgola decimale: gli utenti IT digitano "0,5"
// e toFloatOrNull() da solo la rifiuterebbe. Unico punto di parsing per le 3 dimensioni.
private fun parseDim(s: String): Float? = s.trim().replace(',', '.').toFloatOrNull()

// Formatta una misura per il campo: interi senza decimali, altrimenti fino a 2 cifre,
// senza zeri finali. Usa il punto (parseDim accetta sia punto sia virgola).
private fun formatDim(v: Float): String {
    val r = (Math.round(v * 100f) / 100f)
    return if (r == r.toLong().toFloat()) r.toLong().toString()
    else r.toString().trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAssessmentScreen(
    patientId: Long,
    location: String,
    onSave: (WoundAssessmentEntity, (Long) -> Unit) -> Unit,
    onWoundCreated: (Long) -> Unit,
    onBack: () -> Unit
) {
    val vm: NewAssessmentViewModel = viewModel(factory = NewAssessmentViewModel.Factory)
    val suggestedName by vm.suggestedName.collectAsState()

    // Carica il nome suggerito quando la schermata si apre (nella lingua dell'app)
    val suggestedNameLang = LocalLanguage.current
    LaunchedEffect(patientId, location, suggestedNameLang) {
        vm.loadSuggestedName(patientId, location, suggestedNameLang)
    }

    var woundName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WoundType.ULCERA_PRESSIONE) }
    // Descrizione libera del tipo, mostrata/salvata solo quando selectedType == ALTRO
    var typeOther by remember { mutableStateOf("") }
    var selectedOrigin by remember { mutableStateOf(WoundOrigin.OSPEDALE) }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var depth by remember { mutableStateOf("") }
    // Unità della profondità: spesso si misura in mm. Il DB conserva sempre depthCm in cm.
    var depthUnit by remember { mutableStateOf("mm") }
    var notes by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var originExpanded by remember { mutableStateOf(false) }

    val lang = LocalLanguage.current
    // Codice sede → etichetta localizzata (mai codici grezzi tipo "CAVIGLIA_DX_ANT")
    val locationLabel = locationDisplay(location, lang)

    // Dati non salvati: l'utente ha digitato un nome o una misura. Tipo/origine partono
    // da valori predefiniti, quindi da soli non contano come input dell'utente.
    val hasUnsavedData = woundName.isNotBlank() ||
        length.isNotBlank() || width.isNotBlank() || depth.isNotBlank() || notes.isNotBlank() ||
        (selectedType == WoundType.ALTRO && typeOther.isNotBlank())
    var showExitDialog by remember { mutableStateOf(false) }
    // Indietro guardato: se ci sono dati non salvati chiede conferma, altrimenti esce subito.
    val requestBack = { if (hasUnsavedData) showExitDialog = true else onBack() }

    // Intercetta il back di sistema solo quando ci sono dati non salvati.
    BackHandler(enabled = hasUnsavedData) { showExitDialog = true }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(S.newWoundTitle)
                    val patientLabel = com.example.woundmanagement.ui.components.rememberPatientLabel(patientId)
                    com.example.woundmanagement.ui.components.PatientSubtitle(
                        if (patientLabel.isBlank()) "" else "$patientLabel — $locationLabel"
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = requestBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = loc4(it = "Indietro", en = "Back", fr = "Retour", es = "Atrás"))
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nome ferita (opzionale — auto-generato se vuoto)
            OutlinedTextField(
                value = woundName,
                onValueChange = { if (it.length <= 60) woundName = it },
                label = { Text(S.woundNameOptional) },
                placeholder = { Text(suggestedName.ifBlank { "…" }) },
                supportingText = {
                    Text(
                        if (woundName.isBlank()) "${S.leaveBlankForDefault} \"$suggestedName\""
                        else S.customName
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Location (read-only)
            OutlinedTextField(
                value = locationLabel,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(S.location) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Type dropdown
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.label(lang),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(S.woundTypeLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    WoundType.entries.forEach { t ->
                        DropdownMenuItem(text = { Text(t.label(lang)) }, onClick = { selectedType = t; typeExpanded = false })
                    }
                }
            }

            // Descrizione libera quando il tipo è "Altro" (facoltativa: vuota è ammessa)
            if (selectedType == WoundType.ALTRO) {
                OutlinedTextField(
                    value = typeOther,
                    onValueChange = { if (it.length <= 100) typeOther = it },
                    label = { Text(S.typeOtherLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Origin dropdown
            ExposedDropdownMenuBox(expanded = originExpanded, onExpandedChange = { originExpanded = it }) {
                OutlinedTextField(
                    value = selectedOrigin.label(lang),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(S.origin) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = originExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = originExpanded, onDismissRequest = { originExpanded = false }) {
                    // COMUNITA resta nell'enum solo per i record storici: le nuove lesioni
                    // usano DOMICILIO ("Domicilio/Comunità")
                    WoundOrigin.entries.filter { it != WoundOrigin.COMUNITA }.forEach { o ->
                        DropdownMenuItem(text = { Text(o.label(lang)) }, onClick = { selectedOrigin = o; originExpanded = false })
                    }
                }
            }

            // Dimensions
            Text(S.initialDimensions, style = MaterialTheme.typography.titleSmall)
            Text(
                S.measureHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val l = parseDim(length)
            val w = parseDim(width)
            // La profondità è convertita in cm per la validazione e il salvataggio.
            val depthCm = parseDim(depth)?.let { if (depthUnit == "mm") it / 10f else it }
            val lErr = length.isNotBlank() && (l == null || l <= 0f || l > 100f)
            val wErr = width.isNotBlank() && (w == null || w <= 0f || w > 100f)
            val dErr = depth.isNotBlank() && (depthCm == null || depthCm < 0f || depthCm > 30f)

            val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("${S.lengthLabel} (cm)") }, isError = lErr, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = decimalKeyboard)
                OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("${S.widthLabel} (cm)") }, isError = wErr, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = decimalKeyboard)
            }
            // Profondità con unità selezionabile (mm/cm): la si misura spesso in millimetri.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = depth,
                    onValueChange = { depth = it },
                    label = { Text(S.depthLabel) },
                    placeholder = { Text(if (depthUnit == "mm") "5" else "0,5") },
                    isError = dErr,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = decimalKeyboard
                )
                // Cambiando unità si converte anche il valore digitato, così la misura
                // fisica resta la stessa (8 mm non diventa 8 cm per sbaglio).
                FilterChip(selected = depthUnit == "mm", onClick = {
                    if (depthUnit != "mm") { parseDim(depth)?.let { depth = formatDim(it * 10f) }; depthUnit = "mm" }
                }, label = { Text("mm") })
                FilterChip(selected = depthUnit == "cm", onClick = {
                    if (depthUnit != "cm") { parseDim(depth)?.let { depth = formatDim(it / 10f) }; depthUnit = "cm" }
                }, label = { Text("cm") })
            }
            if (lErr || wErr || dErr) {
                Text(S.dimensionError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if ((l ?: 0f) > 0 && (w ?: 0f) > 0 && !lErr && !wErr) {
                // Il metodo di stima è dichiarato: L×W, non area planimetrica reale
                Text("${S.areaLxW}: ${"%.1f".format(l!! * w!!)} cm²", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            // Note / anamnesi della lesione (presa in carico, contesto clinico)
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 500) notes = it },
                label = { Text(S.woundNotesLabel) },
                placeholder = { Text(S.woundNotesHint) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val lVal = parseDim(length)
        val wVal = parseDim(width)
        // Profondità sempre convertita in cm (il campo può essere in mm o cm).
        val dVal = parseDim(depth)?.let { if (depthUnit == "mm") it / 10f else it }
        val hasInvalidDimensions =
            (length.isNotBlank() && (lVal == null || lVal <= 0f || lVal > 100f)) ||
            (width.isNotBlank() && (wVal == null || wVal <= 0f || wVal > 100f)) ||
            (depth.isNotBlank() && (dVal == null || dVal < 0f || dVal > 30f))

        // Guardia anti-doppio-tap: un secondo tap rapido creerebbe una ferita duplicata.
        var saving by remember { mutableStateOf(false) }
        Button(
            onClick = {
                if (saving) return@Button
                saving = true
                onSave(
                    WoundAssessmentEntity(
                        patientId = patientId,
                        location = location,
                        name = woundName.trim(),   // blank = il ViewModel usa il suggerito
                        type = selectedType.name,
                        origin = selectedOrigin.name,
                        lengthCm = lVal ?: 0f,
                        widthCm = wVal ?: 0f,
                        depthCm = dVal ?: 0f,
                        areaCm2 = (lVal ?: 0f) * (wVal ?: 0f),
                        notes = notes.trim(),
                        typeOther = if (selectedType == WoundType.ALTRO) typeOther.trim() else ""
                    ),
                    onWoundCreated
                )
            },
            enabled = !hasInvalidDimensions && !saving,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(S.saveWound)
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(S.unsavedExitTitle) },
            text = { Text(S.unsavedExitMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
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
}
