package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import com.manha.eventassettracker.data.entity.AssetEntity
import com.manha.eventassettracker.ui.components.ConfirmDialog
import com.manha.eventassettracker.ui.components.RowItemCard
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun AssetsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val assets by viewModel.assets.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(assets, query) {
        if (query.isBlank()) assets
        else assets.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.assetId.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
        }
    }
    var editing by remember { mutableStateOf<AssetEntity?>(null) }
    var deleting by remember { mutableStateOf<AssetEntity?>(null) }

    ScreenScaffold(title = "Assets (${assets.size})", onBack = onBack) { padding ->
        Column(modifier = padding.fillMaxSize().padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search by name, Asset ID or category") },
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(filtered, key = { it.assetId }) { asset ->
                    RowItemCard(
                        title = "${asset.name}  (${asset.assetId})",
                        subtitle = "${asset.category.ifBlank { "Asset" }} • ${asset.status} • ${asset.currentEventName ?: "No event"}",
                        trailingBadge = asset.status,
                        onEdit = { editing = asset },
                        onDelete = { deleting = asset }
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            if (assets.isEmpty()) "Abhi tak koi asset scan nahi hui." else "Kuch nahi mila \"$query\" ke liye.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    editing?.let { asset ->
        AssetEditDialog(
            asset = asset,
            onDismiss = { editing = null },
            onSave = { name, category ->
                viewModel.updateAsset(asset.copy(name = name, category = category))
                editing = null
            }
        )
    }

    deleting?.let { asset ->
        ConfirmDialog(
            title = "Delete Asset?",
            message = "\"${asset.name}\" (${asset.assetId}) ko inventory se delete karein? Purane scan records history mein safe rahenge.",
            onConfirm = { viewModel.deleteAsset(asset) },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun AssetEditDialog(asset: AssetEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(asset.name) }
    var category by remember { mutableStateOf(asset.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${asset.assetId}") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), category.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
