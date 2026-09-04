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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.data.DEFAULT_ADMIN_USERNAME
import com.manha.eventassettracker.data.entity.AdminEntity
import com.manha.eventassettracker.data.entity.StaffEntity
import com.manha.eventassettracker.ui.components.ConfirmDialog
import com.manha.eventassettracker.ui.components.RowItemCard
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun StaffScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val admins by viewModel.adminList.collectAsState()
    val staff by viewModel.staffList.collectAsState()

    var showAddAdmin by remember { mutableStateOf(false) }
    var editingAdmin by remember { mutableStateOf<AdminEntity?>(null) }
    var deletingAdmin by remember { mutableStateOf<AdminEntity?>(null) }

    var showAddStaff by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffEntity?>(null) }
    var deletingStaff by remember { mutableStateOf<StaffEntity?>(null) }

    ScreenScaffold(title = "Staff & Admin", onBack = onBack) { padding ->
        Column(modifier = padding.fillMaxSize().padding(12.dp)) {
            Row {
                Button(onClick = { showAddAdmin = true }, modifier = Modifier.weight(1f)) { Text("＋ Add Admin") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { showAddStaff = true }, modifier = Modifier.weight(1f)) { Text("＋ Add Staff") }
            }
            Spacer(Modifier.height(10.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Text("Admins (${admins.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp)) }
                items(admins, key = { it.id }) { admin ->
                    RowItemCard(
                        title = "👑 ${admin.name}" + if (admin.isDefault) "  (Default)" else "",
                        subtitle = "Login ID: ${admin.username} • ${admin.note}",
                        onEdit = { editingAdmin = admin },
                        onDelete = if (!admin.isDefault) { { deletingAdmin = admin } } else null
                    )
                }
                item { Text("Staff (${staff.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp)) }
                items(staff, key = { it.id }) { s ->
                    RowItemCard(
                        title = s.name,
                        subtitle = "Mobile: ${s.mobile} • ${s.note}",
                        onEdit = { editingStaff = s },
                        onDelete = { deletingStaff = s }
                    )
                }
                if (staff.isEmpty()) {
                    item { Text("Koi staff nahi hai — Staff khud login karke bhi ban sakta hai.", modifier = Modifier.padding(8.dp)) }
                }
            }
        }
    }

    if (showAddAdmin) {
        AdminFormDialog(
            initial = null,
            onDismiss = { showAddAdmin = false },
            onSave = { name, username, password, note ->
                viewModel.addAdmin(name, username, password, note)
                showAddAdmin = false
            }
        )
    }
    editingAdmin?.let { admin ->
        AdminFormDialog(
            initial = admin,
            onDismiss = { editingAdmin = null },
            onSave = { name, username, password, note ->
                val updated = admin.copy(
                    name = name,
                    username = if (admin.isDefault) admin.username else username,
                    password = if (password.isBlank()) admin.password else password,
                    note = note
                )
                viewModel.updateAdmin(updated)
                editingAdmin = null
            }
        )
    }
    deletingAdmin?.let { admin ->
        ConfirmDialog(
            title = "Delete Admin?",
            message = "\"${admin.name}\" ko Admin list se delete karein?",
            onConfirm = { viewModel.deleteAdmin(admin) },
            onDismiss = { deletingAdmin = null }
        )
    }

    if (showAddStaff) {
        StaffFormDialog(
            initial = null,
            onDismiss = { showAddStaff = false },
            onSave = { name, mobile, note ->
                viewModel.addStaff(name, mobile, note)
                showAddStaff = false
            }
        )
    }
    editingStaff?.let { s ->
        StaffFormDialog(
            initial = s,
            onDismiss = { editingStaff = null },
            onSave = { name, mobile, note ->
                viewModel.updateStaff(s.copy(name = name, mobile = mobile, note = note))
                editingStaff = null
            }
        )
    }
    deletingStaff?.let { s ->
        ConfirmDialog(
            title = "Delete Staff?",
            message = "\"${s.name}\" ko Staff list se delete karein? Purane OUT/RETURN records mein naam safe rahega.",
            onConfirm = { viewModel.deleteStaff(s) },
            onDismiss = { deletingStaff = null }
        )
    }
}

@Composable
private fun AdminFormDialog(
    initial: AdminEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    val isDefault = initial?.isDefault == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Admin" else "Edit Admin") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = username, onValueChange = { username = it }, label = { Text("Login ID") },
                    enabled = !isDefault, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(if (initial == null) "Password" else "New Password (blank = no change)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && username.isNotBlank() && (initial != null || password.isNotBlank())) {
                    onSave(name.trim(), username.trim(), password, note.trim())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StaffFormDialog(
    initial: StaffEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var mobile by remember { mutableStateOf(initial?.mobile ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Staff" else "Edit Staff") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10) mobile = it.filter { c -> c.isDigit() } },
                    label = { Text("Mobile Number") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && mobile.length == 10) onSave(name.trim(), mobile, note.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
