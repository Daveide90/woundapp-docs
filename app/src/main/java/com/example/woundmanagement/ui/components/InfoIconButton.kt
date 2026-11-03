package com.example.woundmanagement.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4

/**
 * Piccola icona "i" (Info) riutilizzabile: apre un AlertDialog con un testo informativo
 * fattuale. Usata negli header delle scale di valutazione e nelle sezioni di backup.
 * Nessuna interpretazione clinica: solo descrizione dello strumento (vincolo MDR).
 */
@Composable
fun InfoIconButton(
    text: String,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val dialogTitle = title ?: loc4(it = "Informazioni", en = "Information", fr = "Informations", es = "Información")

    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = loc4(it = "Informazioni", en = "Information", fr = "Informations", es = "Información")
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(dialogTitle) },
            text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(S.close)
                }
            }
        )
    }
}
