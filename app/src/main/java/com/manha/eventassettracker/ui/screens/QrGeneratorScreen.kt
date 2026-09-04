package com.manha.eventassettracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.manha.eventassettracker.data.entity.QrLabelEntity
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.util.PdfLabelBuilder
import com.manha.eventassettracker.util.PdfSaveUtil
import com.manha.eventassettracker.util.PrintUtil
import com.manha.eventassettracker.util.WhatsAppShare
import com.manha.eventassettracker.viewmodel.AppViewModel
import android.widget.Toast

@Composable
fun QrGeneratorScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("3") }
    var count by remember { mutableStateOf("10") }
    var lastBatch by remember { mutableStateOf<List<QrLabelEntity>>(emptyList()) }

    ScreenScaffold(title = "Generate QR Labels", onBack = onBack) { padding ->
        Column(
            modifier = padding
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item / Asset Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row {
                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it.filter { c -> c.isDigit() } },
                    label = { Text("Size (cm)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter { c -> c.isDigit() } },
                    label = { Text("Copies") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val safeName = name.trim()
                    if (safeName.isEmpty()) {
                        Toast.makeText(context, "Item ka naam likhein.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val safeCount = (count.toIntOrNull() ?: 1).coerceIn(1, 1000)
                    val safeSize = (size.toIntOrNull() ?: 3).coerceIn(1, 20)
                    viewModel.generateLabels(safeName, category.trim(), safeSize, safeCount) { labels ->
                        lastBatch = labels
                        Toast.makeText(context, "${labels.size} labels ban gaye — hamesha ke liye QR Register mein saved hain.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate Asset QR Labels") }

            if (lastBatch.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${lastBatch.size} labels ready: ${lastBatch.first().assetId} – ${lastBatch.last().assetId}")
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Button(
                                onClick = {
                                    val items = viewModel.labelItemsFor(lastBatch)
                                    val doc = PdfLabelBuilder.build("Asset QR Labels — ${lastBatch.first().name}", items)
                                    PrintUtil.print(context, doc, "Event-Asset-Tracker-QR-Labels")
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("🖨️ Print") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val items = viewModel.labelItemsFor(lastBatch)
                                    val doc = PdfLabelBuilder.build("Asset QR Labels — ${lastBatch.first().name}", items)
                                    val uri = PdfSaveUtil.saveToDownloads(context, doc, "Event-Asset-Tracker-QR-Labels-${System.currentTimeMillis()}")
                                    doc.close()
                                    if (uri != null) {
                                        Toast.makeText(context, "PDF Downloads folder mein save ho gayi.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "PDF save nahi ho payi.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("⬇️ Download") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val items = viewModel.labelItemsFor(lastBatch)
                                val doc = PdfLabelBuilder.build("Asset QR Labels — ${lastBatch.first().name}", items)
                                val uri = PdfSaveUtil.saveToDownloads(context, doc, "Event-Asset-Tracker-QR-Labels-${System.currentTimeMillis()}")
                                doc.close()
                                if (uri != null) WhatsAppShare.sharePdf(context, uri, "Asset QR Labels — ${lastBatch.first().name}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📲 Share PDF via WhatsApp") }
                    }
                }
            }
        }
    }
}
