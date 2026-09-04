package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manha.eventassettracker.data.entity.ScanType
import com.manha.eventassettracker.ui.Routes
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.viewmodel.AppViewModel

/**
 * The home screen is deliberately bare — just two large OUT / RETURN buttons — so that even a
 * non-technical staff member can use it without any training. Everything else (Events, Assets,
 * QR tools, Staff, Compare, Logout) lives behind the ⋮ menu in the top bar instead of cluttering
 * this screen.
 */
@Composable
fun HomeScreen(viewModel: AppViewModel, navigate: (String) -> Unit, onLogout: () -> Unit) {
    val session by viewModel.session.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Event Asset Tracker",
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Events") }, onClick = { menuOpen = false; navigate(Routes.EVENTS) })
                DropdownMenuItem(text = { Text("Assets") }, onClick = { menuOpen = false; navigate(Routes.ASSETS) })
                if (session.isAdmin) {
                    DropdownMenuItem(text = { Text("Generate QR Labels") }, onClick = { menuOpen = false; navigate(Routes.QR_GENERATE) })
                }
                DropdownMenuItem(text = { Text("Asset QR Register") }, onClick = { menuOpen = false; navigate(Routes.QR_REGISTER) })
                DropdownMenuItem(text = { Text("Staff & Admin") }, onClick = { menuOpen = false; navigate(Routes.STAFF) })
                DropdownMenuItem(text = { Text("Missing Items Check") }, onClick = { menuOpen = false; navigate(Routes.COMPARE) })
                DropdownMenuItem(text = { Text("All Data Report") }, onClick = { menuOpen = false; navigate(Routes.ALL_DATA_REPORT) })
                DropdownMenuItem(text = { Text("Settings (Device Name)") }, onClick = { menuOpen = false; navigate(Routes.SETTINGS) })
                DropdownMenuItem(text = { Text("Logout (${session.name})") }, onClick = { menuOpen = false; onLogout() })
            }
        }
    ) { padding ->
        Column(
            modifier = padding
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { viewModel.setScanMode(ScanType.OUT); navigate(Routes.SCAN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.4f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("📤  OUT", fontSize = 32.sp)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 20.dp))

            Button(
                onClick = { viewModel.setScanMode(ScanType.RETURN); navigate(Routes.SCAN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.4f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("📥  RETURN", fontSize = 32.sp)
            }
        }
    }
}
