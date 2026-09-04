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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import com.manha.eventassettracker.data.entity.QrLabelEntity
import com.manha.eventassettracker.ui.components.ConfirmDialog
import com.manha.eventassettracker.ui.components.RowItemCard
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.util.LabelItem
import com.manha.eventassettracker.util.PdfLabelBuilder
import com.manha.eventassettracker.util.PdfSaveUtil
import com.manha.eventassettracker.util.PrintUtil
import com.manha.eventassettracker.viewmodel.AppViewModel

private data class LabelGroup(val name: String, val category: String, val labels: List<QrLabelEntity>)
private data class CategoryGroup(val category: String, val labels: List<QrLabelEntity>)

private const val VIEW_BY_ITEM = "item"
private const val VIEW_BY_CATEGORY = "category"

@Composable
fun QrRegisterScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val allLabels by viewModel.qrLabels.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val assignedIds = remember(assets) { assets.map { it.assetId }.toSet() }

    var viewMode by remember { mutableStateOf(VIEW_BY_ITEM) }
    var openGroup by remember { mutableStateOf<LabelGroup?>(null) }
    var openCategory by remember { mutableStateOf<CategoryGroup?>(null) }

    val itemGroups = remember(allLabels) {
        allLabels.groupBy { it.name to it.category }
            .map { (key, labels) -> LabelGroup(key.first, key.second, labels.sortedByDescending { it.createdAt }) }
            .sortedByDescending { it.labels.maxOf { l -> l.createdAt } }
    }

    val categoryGroups = remember(allLabels) {
        allLabels.groupBy { it.category.ifBlank { "Uncategorized" } }
            .map { (cat, labels) -> CategoryGroup(cat, labels.sortedByDescending { it.createdAt }) }
            .sortedByDescending { it.labels.maxOf { l -> l.createdAt } }
    }

    ScreenScaffold(title = "Asset QR Register", onBack = onBack) { padding ->
        Column(modifier = padding.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewMode = VIEW_BY_ITEM },
                    modifier = Modifier.weight(1f),
                    colors = if (viewMode == VIEW_BY_ITEM) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("By Item") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewMode = VIEW_BY_CATEGORY },
                    modifier = Modifier.weight(1f),
                    colors = if (viewMode == VIEW_BY_CATEGORY) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) { Text("By Category") }
            }
            Spacer(Modifier.height(8.dp))

            if (viewMode == VIEW_BY_ITEM) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(itemGroups) { group ->
                        val unassigned = group.labels.count { it.assetId !in assignedIds }
                        RowItemCard(
                            title = group.name,
                            subtitle = "${group.category.ifBlank { "Asset" }} • ${group.labels.size} labels • $unassigned unassigned",
                            onClick = { openGroup = group }
                        )
                    }
                    if (itemGroups.isEmpty()) {
                        item {
                            Text(
                                "Abhi tak koi Asset QR label generate nahi hui. Generate QR Labels screen se banayein.",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categoryGroups) { cat ->
                        val unassigned = cat.labels.count { it.assetId !in assignedIds }
                        val itemCount = cat.labels.map { it.name }.distinct().size
                        RowItemCard(
                            title = cat.category,
                            subtitle = "$itemCount item type(s) • ${cat.labels.size} labels • $unassigned unassigned",
                            onClick = { openCategory = cat }
                        )
                    }
                    if (categoryGroups.isEmpty()) {
                        item {
                            Text(
                                "Abhi tak koi Asset QR label generate nahi hui. Generate QR Labels screen se banayein.",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    openGroup?.let { group ->
        GroupDetailDialog(
            group = group,
            assignedIds = assignedIds,
            onDismiss = { openGroup = null },
            onDeleteUnassigned = {
                viewModel.deleteUnassignedInGroup(group.name, group.category)
                openGroup = null
            }
        )
    }

    openCategory?.let { cat ->
        CategoryDetailDialog(
            category = cat,
            assignedIds = assignedIds,
            onDismiss = { openCategory = null },
            onOpenItem = { name, category ->
                openCategory = null
                openGroup = itemGroups.find { it.name == name && it.category == category }
            }
        )
    }
}

@Composable
private fun CategoryDetailDialog(
    category: CategoryGroup,
    assignedIds: Set<String>,
    onDismiss: () -> Unit,
    onOpenItem: (String, String) -> Unit
) {
    val context = LocalContext.current
    val byItem = remember(category) {
        category.labels.groupBy { it.name }.toList().sortedBy { it.first }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category: ${category.category}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("${category.labels.size} labels across ${byItem.size} item type(s)")
                Spacer(Modifier.height(10.dp))
                Row {
                    Button(
                        onClick = {
                            val items = category.labels.map { LabelItem(it.assetId, it.name, it.category, it.sizeCm) }
                            val doc = PdfLabelBuilder.build("Asset QR Labels — ${category.category}", items)
                            PrintUtil.print(context, doc, "Event-Asset-Tracker-QR-Category-${category.category}")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("🖨️ Print All") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val items = category.labels.map { LabelItem(it.assetId, it.name, it.category, it.sizeCm) }
                            val doc = PdfLabelBuilder.build("Asset QR Labels — ${category.category}", items)
                            val uri = PdfSaveUtil.saveToDownloads(
                                context, doc, "Event-Asset-Tracker-QR-Category-${category.category}-${System.currentTimeMillis()}"
                            )
                            doc.close()
                            Toast.makeText(
                                context,
                                if (uri != null) "PDF Downloads folder mein save ho gayi." else "PDF save nahi ho payi.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("⬇️ Download All") }
                }
                Spacer(Modifier.height(12.dp))
                Text("Item types in this category:", style = MaterialTheme.typography.labelLarge)
                byItem.forEach { (name, labels) ->
                    val unassigned = labels.count { it.assetId !in assignedIds }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("$name  (${labels.size} labels, $unassigned unassigned)", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onOpenItem(name, category.category.let { if (it == "Uncategorized") "" else it }) }) {
                            Text("Open")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun GroupDetailDialog(
    group: LabelGroup,
    assignedIds: Set<String>,
    onDismiss: () -> Unit,
    onDeleteUnassigned: () -> Unit
) {
    val context = LocalContext.current
    var confirmingDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("${group.labels.size} labels • ${group.category.ifBlank { "Asset" }}")
                Spacer(Modifier.height(10.dp))
                Row {
                    Button(
                        onClick = {
                            val items = group.labels.map { LabelItem(it.assetId, it.name, it.category, it.sizeCm) }
                            val doc = PdfLabelBuilder.build("Asset QR Labels — ${group.name}", items)
                            PrintUtil.print(context, doc, "Event-Asset-Tracker-QR-${group.name}")
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("🖨️ Print") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val items = group.labels.map { LabelItem(it.assetId, it.name, it.category, it.sizeCm) }
                            val doc = PdfLabelBuilder.build("Asset QR Labels — ${group.name}", items)
                            val uri = PdfSaveUtil.saveToDownloads(context, doc, "Event-Asset-Tracker-QR-${group.name}-${System.currentTimeMillis()}")
                            doc.close()
                            Toast.makeText(
                                context,
                                if (uri != null) "PDF Downloads folder mein save ho gayi." else "PDF save nahi ho payi.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("⬇️ Download") }
                }
                Spacer(Modifier.height(10.dp))
                Text("Individual labels (tap Reprint any time):", style = MaterialTheme.typography.labelLarge)
                group.labels.forEach { label ->
                    val assigned = label.assetId in assignedIds
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "${label.assetId}  ${if (assigned) "(IN INVENTORY)" else "(unassigned)"}",
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val doc = PdfLabelBuilder.build(
                                "Reprint — ${label.assetId}",
                                listOf(LabelItem(label.assetId, label.name, label.category, label.sizeCm))
                            )
                            PrintUtil.print(context, doc, "Event-Asset-Tracker-Reprint-${label.assetId}")
                        }) { Text("Reprint") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { confirmingDelete = true }) { Text("Delete Unassigned", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (confirmingDelete) {
        ConfirmDialog(
            title = "Delete Unassigned Labels?",
            message = "\"${group.name}\" ke sirf un labels ko delete karein jo abhi tak kisi asset se scan nahi hue. Already scanned assets safe rahenge.",
            onConfirm = onDeleteUnassigned,
            onDismiss = { confirmingDelete = false }
        )
    }
}
