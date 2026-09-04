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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
                ) { Text("Find Missing") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { mode = MODE_PASTE },
                    modifier = Modifier.weight(1f),
                    colors = if (mode == MODE_PASTE) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("Paste & Compare") }
            }

            Spacer(Modifier.height(16.dp))

            if (mode == MODE_CHECKLIST) {
                ChecklistMode(viewModel)
            } else {
                PasteMode()
            }
        }
    }
}

@Composable
private fun ChecklistMode(viewModel: AppViewModel) {
    val events by viewModel.events.collectAsState()
    val assets by viewModel.assets.collectAsState()
    var selectedEvent by remember { mutableStateOf<EventEntity?>(null) }
    var checked by remember { mutableStateOf(setOf<String>()) }

    val outAssets = remember(selectedEvent, assets) {
        if (selectedEvent == null) emptyList()
        else assets.filter { it.currentEventId == selectedEvent!!.id && it.status == ScanType.OUT }
    }

    EventPicker(events = events, selected = selectedEvent) { selectedEvent = it }

    Spacer(Modifier.height(12.dp))

    if (selectedEvent == null) {
        Text("Pehle Event select karein")
        return
    }

    Text("${outAssets.size} items still OUT", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    LazyColumn(modifier = Modifier.weight(1f)) {
        items(outAssets) { asset ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = asset.assetId in checked,
                    onCheckedChange = { isChecked ->
                        checked = if (isChecked) checked + asset.assetId else checked - asset.assetId
                        if (isChecked) {
                            viewModel.onQrScanned(asset.assetId) // mark as RETURN
                        }
                    }
                )
                Text("${asset.name} (${asset.assetId})")
            }
        }
    }

    val stillMissing = outAssets.filter { it.assetId !in checked }
    if (stillMissing.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val text = buildString {
                    append("*Still Missing*\n")
                    stillMissing.forEachIndexed { i, a ->
                        append("${i + 1}. ${a.name} — ${a.assetId}\n")
                    }
                }
                WhatsAppShare.shareText(LocalContext.current, text)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("📲 Share Still Missing via WhatsApp") }
    }
}

@Composable
private fun PasteMode() {
    val context = LocalContext.current
    var outText by remember { mutableStateOf("") }
    var returnText by remember { mutableStateOf("") }
    var missing by remember { mutableStateOf<List<ReportParser.ParsedLine>?>(null) }

    OutlinedTextField(
        value = outText,
        onValueChange = { outText = it },
        label = { Text("Paste OUT list") },
        modifier = Modifier.fillMaxWidth().height(120.dp)
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = returnText,
        onValueChange = { returnText = it },
        label = { Text("Paste RETURN list") },
        modifier = Modifier.fillMaxWidth().height(120.dp)
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            val outLines = ReportParser.parse(outText)
            val returnIds = ReportParser.parse(returnText).map { it.assetId }.toSet()
            missing = outLines.filter { it.assetId !in returnIds }
        },
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
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
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
