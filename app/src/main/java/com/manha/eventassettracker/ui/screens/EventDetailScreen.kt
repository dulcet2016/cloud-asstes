package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.data.entity.ScanEntity
import com.manha.eventassettracker.data.entity.ScanType
import com.manha.eventassettracker.ui.components.ConfirmDialog
import com.manha.eventassettracker.ui.components.RowItemCard
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.util.WhatsAppShare
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun EventDetailScreen(viewModel: AppViewModel, eventId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    val scans by viewModel.scans.collectAsState()
    val activeEventId by viewModel.activeEventId.collectAsState()
    val event = events.find { it.id == eventId }
    var tab by remember { mutableStateOf(ScanType.OUT) }
    var editing by remember { mutableStateOf<ScanEntity?>(null) }
    var deleting by remember { mutableStateOf<ScanEntity?>(null) }

    ScreenScaffold(title = event?.name ?: "Event", onBack = onBack) { padding ->
        if (event == null) {
            Text("Event not found.", modifier = Modifier.padding(16.dp))
            return@ScreenScaffold
        }

        val tabScans = remember(scans, tab, eventId) {
            scans.filter { it.eventId == eventId && it.type == tab }.sortedBy { it.timestamp }
        }

        Column(modifier = padding.fillMaxSize().padding(12.dp)) {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(event.name, style = MaterialTheme.typography.titleLarge)
                    Text("${event.eventDate}  •  ${event.venue.ifBlank { "No venue" }}")
                    if (event.id != activeEventId) {
                        Button(onClick = { viewModel.setActiveEvent(event) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Set as Active Event")
                        }
                    } else {
                        Text("★ Currently Active Event", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { tab = ScanType.OUT },
                    modifier = Modifier.weight(1f),
                    colors = if (tab == ScanType.OUT) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("OUT (${scans.count { it.eventId == eventId && it.type == ScanType.OUT }})") }
                Button(
                    onClick = { tab = ScanType.RETURN },
                    modifier = Modifier.weight(1f),
                    colors = if (tab == ScanType.RETURN) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("RETURN (${scans.count { it.eventId == eventId && it.type == ScanType.RETURN }})") }
            }

            Button(
                onClick = { WhatsAppShare.shareText(context, viewModel.buildEventReportText(eventId, tab)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📲 Share Report via WhatsApp") }

            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tabScans, key = { it.id }) { scan ->
                    RowItemCard(
                        title = "${scan.assetName}  (${scan.assetId})",
                        subtitle = "By ${scan.staffName} • ${java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(scan.timestamp))}",
                        onEdit = { editing = scan },
                        onDelete = { deleting = scan }
                    )
                }
                if (tabScans.isEmpty()) {
                    item { Text("No entries yet.", modifier = Modifier.padding(16.dp)) }
                }
            }
        }
    }

    editing?.let { scan ->
        ScanEditDialog(
            scan = scan,
            onDismiss = { editing = null },
            onSave = { assetName ->
                viewModel.updateScan(scan.copy(assetName = assetName))
                editing = null
            }
        )
    }

    deleting?.let { scan ->
        ConfirmDialog(
            title = "Delete Entry?",
            message = "Remove this ${scan.type} entry for ${scan.assetName} (${scan.assetId})?",
            onConfirm = { viewModel.deleteScan(scan) },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ScanEditDialog(scan: ScanEntity, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(scan.assetName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry — ${scan.assetId}") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Asset Name") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
