package com.example.woundmanagement.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.woundmanagement.WoundManagementApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Cognome Nome" del paziente da mostrare come sottotitolo nelle schermate figlie
 * (scale, nuova lesione, medicazione): l'operatore vede sempre su chi sta lavorando.
 * Stringa vuota finché non caricato o se il paziente non esiste.
 */
@Composable
fun rememberPatientLabel(patientId: Long): String {
    var label by remember(patientId) { mutableStateOf("") }
    LaunchedEffect(patientId) {
        label = withContext(Dispatchers.IO) {
            runCatching {
                WoundManagementApp.instance.database.patientDao().getById(patientId)
                    ?.let { "${it.surname} ${it.name}".trim() } ?: ""
            }.getOrDefault("")
        }
    }
    return label
}

/** Come [rememberPatientLabel], ma partendo dalla lesione: "Cognome Nome — Sede". */
@Composable
fun rememberPatientLabelForAssessment(assessmentId: Long): String {
    var label by remember(assessmentId) { mutableStateOf("") }
    val lang = com.example.woundmanagement.ui.strings.LocalLanguage.current
    LaunchedEffect(assessmentId, lang) {
        label = withContext(Dispatchers.IO) {
            runCatching {
                val db = WoundManagementApp.instance.database
                val wound = db.woundAssessmentDao().getById(assessmentId) ?: return@runCatching ""
                val patient = db.patientDao().getById(wound.patientId) ?: return@runCatching ""
                val loc = com.example.woundmanagement.domain.model.locationDisplay(wound.location, lang)
                "${patient.surname} ${patient.name}".trim() + " — " + loc
            }.getOrDefault("")
        }
    }
    return label
}

/** Sottotitolo standard sotto il titolo della TopAppBar. */
@Composable
fun PatientSubtitle(label: String) {
    if (label.isNotBlank()) {
        androidx.compose.material3.Text(
            label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
