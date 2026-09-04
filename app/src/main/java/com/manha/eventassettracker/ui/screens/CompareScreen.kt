package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.data.entity.EventEntity
import com.manha.eventassettracker.data.entity.ScanType
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.util.ReportParser
import com.manha.eventassettracker.util.WhatsAppShare
import com.manha.eventassettracker.viewmodel.AppViewModel

private const val MODE_CHECKLIST = "checklist"
private const val MODE_PASTE = "paste"

@Composable
fun CompareScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(MODE_CHECKLIST) }

    ScreenScaffold(title = "Missing Items Check", onBack = onBack) { padding ->
        Column(modifier = padding.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { mode = MODE_CHECKLIST },
                    modifier = Modifier.weight(1f),
                    colors = if (mode == MODE_CHECKLIST) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("✔️ Checklist") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { mode = MODE_PASTE },
                    modifier = Modifier.weight(1f),
                    colors = if (mode == MODE_PASTE) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("📋 Paste & Compare") }
            }
            Spacer(Modifier.height(12.dp))

            if (mode == MODE_CHECKLIST) ChecklistMode(viewModel) else PasteCompareMode(viewModel)
        }
    }
}

/** No copy-paste needed: pick the event, tick items off as you find them (this records a real
 *  RETURN), whatever's left unticked is missing. */
@Composable
private fun ChecklistMode(viewModel: AppViewModel) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val activeEvent by viewModel.activeEvent.collectAsState()

    var selectedEvent by remember(activeEvent) { mutableStateOf(activeEvent) }

    val outItems = remember(assets, selectedEvent) {
        val eventId = selectedEvent?.id
        if (eventId == null) emptyList()
        else assets.filter { it.currentEventId == eventId && it.status == ScanType.OUT }
            .sortedBy { it.name }
    }

    Text(
        "Select an event, then tick each item as you find it. Anything left unticked is still missing.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(10.dp))

    EventPicker(events = events, selected = selectedEvent, onSelect = { selectedEvent = it })

    Spacer(Modifier.height(12.dp))

    if (selectedEvent == null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text("Pick an event to see its items.", modifier = Modifier.padding(14.dp))
        }
    } else if (outItems.isEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text("✅ Nothing still OUT for this event.", modifier = Modifier.padding(14.dp))
        }
    } else {
        Text("${outItems.size} item(s) still OUT:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(outItems, key = { it.assetId }) { asset ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = false, onCheckedChange = { viewModel.markReturned(asset) })
                    Column {
                        Text("${asset.name}  (${asset.assetId})", style = MaterialTheme.typography.bodyLarge)
                        Text(asset.category.ifBlank { "Asset" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val text = buildString {
                    append("*Missing Items — ${selectedEvent?.name}*\n\n")
                    outItems.forEachIndexed { i, a -> append("${i + 1}. ${a.name} — ${a.assetId}\n") }
                }
                WhatsAppShare.shareText(context, text)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("📲 Share Missing List via WhatsApp") }
    }
}

/** For cross-checking against text copied from WhatsApp (e.g. an older message, or one sent by
 *  someone not using this app) rather than the app's own live data. */
@Composable
private fun PasteCompareMode(viewModel: AppViewModel) {
    val context = LocalContext.current
    var outText by remember { mutableStateOf("") }
    var returnText by remember { mutableStateOf("") }
    var missing by remember { mutableStateOf<List<ReportParser.ParsedLine>?>(null) }

    Text(
        "Paste an OUT report and a RETURN report copied from WhatsApp to find what's missing between them.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = outText,
        onValueChange = { outText = it },
        label = { Text("Paste OUT Report") },
        modifier = Modifier.fillMaxWidth().height(120.dp)
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = returnText,
        onValueChange = { returnText = it },
        label = { Text("Paste RETURN Report") },
        modifier = Modifier.fillMaxWidth().height(120.dp)
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { missing = ReportParser.findMissing(outText, returnText) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Compare") }

    missing?.let { list ->
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (list.isEmpty()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                if (list.isEmpty()) "✅ Everything came back!" else "⚠️ ${list.size} item(s) still missing:",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list) { line ->
                Text("• ${line.name.ifBlank { "(name?)" }} — ${line.assetId}", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        if (list.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val text = buildString {
                        append("*Missing Items*\n")
                        list.forEachIndexed { i, l -> append("${i + 1}. ${l.name.ifBlank { "?" }} — ${l.assetId}\n") }
                    }
                    WhatsAppShare.shareText(context, text)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📲 Share Missing List via WhatsApp") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventPicker(events: List<EventEntity>, selected: EventEntity?, onSelect: (EventEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Event") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (events.isEmpty()) {
                DropdownMenuItem(text = { Text("No events yet") }, onClick = { expanded = false })
            }
            events.forEach { event ->
                DropdownMenuItem(text = { Text(event.name) }, onClick = { onSelect(event); expanded = false })
            }
        }
    }
}
