package com.example.woundmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.data.db.entity.AuditLogEntity
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    auditLogs: List<AuditLogEntity>,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY) }
    var filterType by remember { mutableStateOf("") }
    var filterExpanded by remember { mutableStateOf(false) }

    val entityTypes = remember(auditLogs) {
        auditLogs.map { it.entityType }.distinct().sorted()
    }

    val filtered = remember(auditLogs, filterType) {
        if (filterType.isBlank()) auditLogs
        else auditLogs.filter { it.entityType == filterType }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(S.auditLog) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.back)
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { filterExpanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = S.filter)
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        DropdownMenuItem(text = { Text(S.all) }, onClick = { filterType = ""; filterExpanded = false })
                        entityTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { filterType = type; filterExpanded = false })
                        }
                    }
                }
            }
        )

        if (filterType.isNotBlank()) {
            AssistChip(
                onClick = { filterType = "" },
                label = { Text("${S.filter}: $filterType") },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Text(
            loc4(
                it = "${filtered.size} eventi registrati",
                en = "${filtered.size} registered events",
                fr = "${filtered.size} événements enregistrés",
                es = "${filtered.size} eventos registrados"
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(S.noEvents, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn {
                items(filtered) { log ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${log.entityType} #${log.entityId}", style = MaterialTheme.typography.bodySmall)
                            if (log.details.isNotBlank()) {
                                Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
