package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.data.db.entity.PatientEntity
import com.example.woundmanagement.ui.components.DateSlashTransformation
import com.example.woundmanagement.ui.components.dateDigitsToSlashed
import com.example.woundmanagement.ui.strings.S

internal val CF_REGEX = Regex("^[A-Z]{6}[0-9]{2}[A-EHLMPRST][0-9]{2}[A-Z][0-9]{3}[A-Z]$")
internal val DATE_REGEX = Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/(19|20)[0-9]{2}$")

// Un valore preesistente non valido (import da backup, dati legacy) lasciato invariato
// non deve tenere in ostaggio il salvataggio degli altri campi: blocca solo ciò che
// l'utente ha reso non valido in questa modifica.
internal fun invalidEditBlocksSave(value: String, original: String, regex: Regex): Boolean =
    value.isNotBlank() && !regex.matches(value) && value != original

@Composable
fun NewPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (PatientEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var fiscalCode by remember { mutableStateOf("") }
    // Solo cifre (max 8): gli slash di GG/MM/AAAA sono visivi (DateSlashTransformation)
    // e vengono ricomposti al confine (validazione/salvataggio) con dateDigitsToSlashed.
    var birthDate by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var bed by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val cfError = fiscalCode.isNotBlank() && !CF_REGEX.matches(fiscalCode)
    val birthDateSlashed = dateDigitsToSlashed(birthDate)
    val dateError = birthDate.isNotBlank() && !DATE_REGEX.matches(birthDateSlashed)
    val canSave = name.isNotBlank() && surname.isNotBlank() && !cfError && !dateError
    // C'è almeno un dato inserito: in tal caso chiediamo conferma prima di scartare.
    val hasData = name.isNotBlank() || surname.isNotBlank() || fiscalCode.isNotBlank() ||
        birthDate.isNotBlank() || ward.isNotBlank() || bed.isNotBlank() || notes.isNotBlank()

    // Chiusura richiesta dall'utente (back o tap fuori): se ci sono dati, chiedi conferma.
    val requestDismiss = { if (hasData) showDiscardConfirm = true else onDismiss() }

    AlertDialog(
        onDismissRequest = requestDismiss,
        title = { Text(S.newPatient) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = surname,
                    onValueChange = { if (it.length <= 50) surname = it.replaceFirstChar { c -> c.uppercaseChar() } },
                    label = { Text(S.surname) },
                    isError = showErrors && surname.isBlank(),
                    supportingText = if (showErrors && surname.isBlank()) {{ Text(S.requiredField) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it.replaceFirstChar { c -> c.uppercaseChar() } },
                    label = { Text(S.firstName) },
                    isError = showErrors && name.isBlank(),
                    supportingText = if (showErrors && name.isBlank()) {{ Text(S.requiredField) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fiscalCode,
                    onValueChange = { if (it.length <= 16) fiscalCode = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                    label = { Text(S.fiscalCode) },
                    isError = cfError,
                    supportingText = if (cfError) {{ Text(S.fiscalCodeInvalid) }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { v -> birthDate = v.filter { it.isDigit() }.take(8) },
                    label = { Text(S.birthDate) },
                    isError = dateError,
                    supportingText = if (dateError) {{ Text(S.invalidDate) }} else null,
                    placeholder = { Text("01/01/1970") },
                    visualTransformation = remember { DateSlashTransformation() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ward,
                    onValueChange = { if (it.length <= 50) ward = it },
                    label = { Text(S.ward) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bed,
                    onValueChange = { if (it.length <= 20) bed = it },
                    label = { Text(S.bed) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 500) notes = it },
                    label = { Text(S.notes) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            // Button filled come nel dialog gemello di modifica (PatientInfoDialog):
            // stessa enfasi per lo stesso Salva
            Button(
                onClick = {
                    if (!canSave) { showErrors = true; return@Button }
                    onConfirm(
                        PatientEntity(
                            name = name.trim(),
                            surname = surname.trim(),
                            fiscalCode = fiscalCode.trim(),
                            // Al salvataggio la data torna nel formato con slash (GG/MM/AAAA)
                            birthDate = birthDateSlashed,
                            ward = ward.trim(),
                            bed = bed.trim(),
                            notes = notes.trim()
                        )
                    )
                }
            ) { Text(S.save) }
        },
        dismissButton = {
            TextButton(onClick = requestDismiss) { Text(S.cancel) }
        }
    )

    // Conferma uscita con dati non salvati.
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(S.unsavedExitTitle) },
            text = { Text(S.unsavedExitMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDismiss()
                }) { Text(S.unsavedExitConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text(S.cancel) }
            }
        )
    }
}
