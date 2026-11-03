package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.R
import com.example.woundmanagement.data.db.dao.PatientWithRisk
import com.example.woundmanagement.ui.components.BrandHeader
import com.example.woundmanagement.ui.components.BrandHeaderSubtitle
import com.example.woundmanagement.ui.components.BrandHeaderTitle
import com.example.woundmanagement.ui.components.InitialsAvatar
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    patients: List<PatientWithRisk>,
    onPatientClick: (Long) -> Unit,
    onAddPatient: () -> Unit,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        // Header a gradiente con titolo, conteggio e ricerca integrata
        BrandHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    BrandHeaderTitle(S.patients)
                    Spacer(modifier = Modifier.height(2.dp))
                    BrandHeaderSubtitle(
                        if (searchQuery.isBlank())
                            "${patients.size} ${if (patients.size == 1) loc4(it = "assistito", en = "patient", fr = "patient", es = "paciente") else loc4(it = "assistiti", en = "patients", fr = "patients", es = "pacientes")}"
                        else
                            "${patients.size} ${loc4(it = "risultati", en = "results", fr = "résultats", es = "resultados")}"
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            TextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearch(it)
                },
                placeholder = { Text(S.searchPatient) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; onSearch("") }) {
                            Icon(Icons.Default.Close, contentDescription = S.cancel)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (patients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searchQuery.isBlank()) {
                        Image(
                            painter = painterResource(id = R.drawable.empty_patients),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).padding(bottom = 12.dp)
                        )
                    }
                    Text(S.noPatients, style = MaterialTheme.typography.bodyLarge)
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            S.emptyPatientsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
            ) {
                items(patients) { patient ->
                    PatientListItem(patient = patient, onClick = { onPatientClick(patient.id) })
                }
            }
        }
    }
}

@Composable
private fun PatientListItem(patient: PatientWithRisk, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(
                initials = "${patient.surname.firstOrNull() ?: ' '}${patient.name.firstOrNull() ?: ' '}".trim()
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${patient.surname} ${patient.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (patient.ward.isNotBlank()) {
                    Text(
                        "${S.ward}: ${patient.ward} · ${S.bed}: ${patient.bed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (patient.fiscalCode.isNotBlank()) {
                    Text(
                        "${S.fiscalCodeShort}: ${patient.fiscalCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
