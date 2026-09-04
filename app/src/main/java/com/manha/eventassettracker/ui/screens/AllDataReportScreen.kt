package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.util.WhatsAppShare
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun AllDataReportScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    // Recompute whenever any underlying data changes.
    val events by viewModel.events.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val scans by viewModel.scans.collectAsState()
    val staff by viewModel.staffList.collectAsState()
    val admins by viewModel.adminList.collectAsState()

    val reportText = remember(events, assets, scans, staff, admins) { viewModel.buildAllDataReport() }

    ScreenScaffold(title = "All Data Report", onBack = onBack) { padding ->
        Column(modifier = padding.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = { WhatsAppShare.shareText(context, reportText) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📲 Share via WhatsApp") }
            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(reportText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}
