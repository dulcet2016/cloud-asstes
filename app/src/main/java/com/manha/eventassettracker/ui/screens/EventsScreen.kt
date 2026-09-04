package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.data.entity.EventEntity
import com.manha.eventassettracker.ui.components.ConfirmDialog
import com.manha.eventassettracker.ui.components.RowItemCard
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun EventsScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenEvent: (String) -> Unit) {
    val events by viewModel.events.collectAsState()
    val activeEventId by viewModel.activeEventId.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EventEntity?>(null) }
    var deleting by remember { mutableStateOf<EventEntity?>(null) }

    ScreenScaffold(
        title = "Events",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        LazyColumn(modifier = padding.fillMaxSize().padding(12.dp)) {
            items(events) { event ->
                RowItemCard(
                    title = event.name + if (event.id == activeEventId) "  ★ Active" else "",
                    subtitle = "${event.eventDate}  •  ${event.venue.ifBlank { "No venue noted" }}",
                    onEdit = { editing = event },
                    onDelete = { deleting = event },
                    onClick = { onOpenEvent(event.id) }
                )
            }
            if (events.isEmpty()) {
                item { Text("Koi event nahi hai. + button se naya event banayein.", modifier = Modifier.padding(16.dp)) }
            }
        }
    }

    if (showAddDialog) {
        EventFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, date, venue, note ->
                viewModel.addEvent(name, date, venue, note) { viewModel.setActiveEvent(it) }
                showAddDialog = false
            }
        )
    }

    editing?.let { event ->
        EventFormDialog(
            initial = event,
            onDismiss = { editing = null },
            onSave = { name, date, venue, note ->
                viewModel.updateEvent(event.copy(name = name, eventDate = date, venue = venue, note = note))
                editing = null
            }
        )
    }

    deleting?.let { event ->
        ConfirmDialog(
            title = "Delete Event?",
            message = "\"${event.name}\" delete karein? Iske scan records history mein safe rahenge.",
            onConfirm = { viewModel.deleteEvent(event) },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun EventFormDialog(
    initial: EventEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var date by remember { mutableStateOf(initial?.eventDate ?: "") }
    var venue by remember { mutableStateOf(initial?.venue ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Event" else "Edit Event") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 12 Sep 2026)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), date.trim(), venue.trim(), note.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
