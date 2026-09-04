package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun LoginScreen(viewModel: AppViewModel, onLoggedIn: () -> Unit) {
    var isAdminTab by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Event Asset Tracker",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "100% Offline QR Asset Tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { isAdminTab = true; error = "" },
                modifier = Modifier.weight(1f),
                colors = if (isAdminTab) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) { Text("Admin Login") }
            Button(
                onClick = { isAdminTab = false; error = "" },
                modifier = Modifier.weight(1f),
                colors = if (!isAdminTab) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) { Text("Staff Login") }
        }
        Spacer(Modifier.height(20.dp))

        if (isAdminTab) {
            AdminLoginForm(viewModel, onLoggedIn) { error = it }
        } else {
            StaffLoginForm(viewModel, onLoggedIn) { error = it }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AdminLoginForm(viewModel: AppViewModel, onLoggedIn: () -> Unit, onError: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Login ID") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            viewModel.loginAdmin(username, password) { success, message ->
                if (success) onLoggedIn() else onError(message)
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Login as Admin") }

    Spacer(Modifier.height(8.dp))
    Text(
        "Default: admin / admin123 (pehli baar ke liye)",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StaffLoginForm(viewModel: AppViewModel, onLoggedIn: () -> Unit, onError: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Full Name") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = mobile,
        onValueChange = { if (it.length <= 10) mobile = it.filter { c -> c.isDigit() } },
        label = { Text("Mobile Number") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            viewModel.loginStaff(name, mobile) { success, message ->
                if (success) onLoggedIn() else onError(message)
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Continue as Staff") }
}
