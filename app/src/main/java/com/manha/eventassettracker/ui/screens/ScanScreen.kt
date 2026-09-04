package com.manha.eventassettracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.manha.eventassettracker.data.entity.EventEntity
import com.manha.eventassettracker.data.entity.ScanType
import com.manha.eventassettracker.ui.components.ScreenScaffold
import com.manha.eventassettracker.viewmodel.AppViewModel
import com.manha.eventassettracker.viewmodel.ScanUiEvent
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val activeEvent by viewModel.activeEvent.collectAsState()
    val events by viewModel.events.collectAsState()
    val scanMode by viewModel.scanMode.collectAsState()
    val scanEvent by viewModel.scanEvents.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            try {

                val image = InputImage.fromFilePath(
                    context,
                    uri
                )

                val scanner = BarcodeScanning.getClient()

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->

                        val value =
                            barcodes
                                .firstOrNull {
                                    !it.rawValue.isNullOrBlank()
                                }
                                ?.rawValue

                        if (value != null) {

                            viewModel.onQrScanned(value)

                        } else {

                            android.widget.Toast.makeText(
                                context,
                                "No QR code found in this image.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener {

                        android.widget.Toast.makeText(
                            context,
                            "Could not read this image.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

            } catch (e: Exception) {

                android.widget.Toast.makeText(
                    context,
                    "Could not read this image.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

    LaunchedEffect(Unit) {

        if (!hasCameraPermission) {
            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    LaunchedEffect(scanEvent) {

        if (scanEvent != null) {

            kotlinx.coroutines.delay(1600)

            viewModel.consumeScanEvent()
        }
    }

    ScreenScaffold(
        title = "Scan OUT / RETURN",
        onBack = onBack
    ) { padding ->

        Column(
            modifier = padding
                .fillMaxSize()
                .padding(16.dp)
        ) {

            EditableEventField(
                events = events,
                activeEvent = activeEvent,
                onSelectExisting = {
                    viewModel.setActiveEvent(it)
                },
                onCreateNew = { name ->

                    viewModel.addEvent(
                        name,
                        date = "",
                        venue = "",
                        note = ""
                    ) { created ->

                        viewModel.setActiveEvent(
                            created
                        )
                    }
                }
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        viewModel.setScanMode(
                            ScanType.OUT
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        if (scanMode == ScanType.OUT) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                ) {
                    Text("OUT")
                }

                Button(
                    onClick = {
                        viewModel.setScanMode(
                            ScanType.RETURN
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        if (scanMode == ScanType.RETURN) {
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                ) {
                    Text("RETURN")
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            when {

                activeEvent == null -> {

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {

                        Text(
                            "Scan shuru karne se pehle upar se ek Event select karein.",
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                else -> {

                    if (!hasCameraPermission) {

                        Card {

                            Column(
                                Modifier.padding(14.dp)
                            ) {

                                Text(
                                    "Camera permission chahiye scan karne ke liye."
                                )

                                Spacer(
                                    Modifier.height(8.dp)
                                )

                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            Manifest.permission.CAMERA
                                        )
                                    }
                                ) {
                                    Text(
                                        "Camera Allow Karein"
                                    )
                                }
                            }
                        }

                    } else {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(
                                    Color.Black,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {

                            CameraPreviewWithAnalyzer(
                                onBarcodeDetected = { value ->
                                    viewModel.onQrScanned(
                                        value
                                    )
                                }
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Button(
                        onClick = {
                            galleryLauncher.launch(
                                "image/*"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "🖼️  Scan QR from Gallery"
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            when (val event = scanEvent) {

                is ScanUiEvent.Scanned -> {

                    val label =
                        if (event.type == ScanType.OUT) {
                            "OUT ✅"
                        } else {
                            "RETURN ✅"
                        }

                    ResultBanner(
                        text =
                            "$label — ${event.name} (${event.assetId})",
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                is ScanUiEvent.AlreadyDone -> {

                    ResultBanner(
                        text =
                            "${event.name} (${event.assetId}) already ${event.currentStatus} hai.",
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                }

                is ScanUiEvent.Error -> {

                    ResultBanner(
                        text = event.message,
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                }

                null -> {
                }
            }
        }
    }
}

@Composable
private fun ResultBanner(
    text: String,
    containerColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun EditableEventField(
    events: List<EventEntity>,
    activeEvent: EventEntity?,
    onSelectExisting: (EventEntity) -> Unit,
    onCreateNew: (String) -> Unit
) {
    var text by remember(activeEvent?.id) {
        mutableStateOf(
            activeEvent?.name ?: ""
        )
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val suggestions =
        remember(text, events) {

            if (text.isBlank()) {
                events
            } else {
                events.filter {
                    it.name.contains(
                        text,
                        ignoreCase = true
                    )
                }
            }
        }

    val matchesExisting =
        events.any {
            it.name.equals(
                text.trim(),
                ignoreCase = true
            )
        }

    Column {

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            }
        ) {

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    expanded = true
                },
                label = {
                    Text(
                        "Event Name (pick existing or type a new one)"
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            DropdownMenu(
                expanded =
                    expanded &&
                            suggestions.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                }
            ) {

                suggestions.forEach { event ->

                    DropdownMenuItem(
                        text = {
                            Text(event.name)
                        },
                        onClick = {

                            text = event.name

                            expanded = false

                            onSelectExisting(
                                event
                            )
                        }
                    )
                }
            }
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Button(
            onClick = {

                val trimmed =
                    text.trim()

                if (trimmed.isEmpty()) {
                    return@Button
                }

                val existing =
                    events.find {
                        it.name.equals(
                            trimmed,
                            ignoreCase = true
                        )
                    }

                if (existing != null) {

                    onSelectExisting(
                        existing
                    )

                } else {

                    onCreateNew(
                        trimmed
                    )
                }

                expanded = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (matchesExisting) {
                    "Use This Event"
                } else {
                    "Create & Use New Event"
                }
            )
        }
    }
}

@Composable
private fun CameraPreviewWithAnalyzer(
    onBarcodeDetected: (String) -> Unit
) {
    val lifecycleOwner =
        LocalLifecycleOwner.current

    val scanner =
        remember {
            BarcodeScanning.getClient()
        }

    val analysisExecutor =
        remember {
            Executors.newSingleThreadExecutor()
        }

    DisposableEffect(Unit) {

        onDispose {

            analysisExecutor.shutdown()

            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),

        factory = { ctx ->

            val previewView =
                PreviewView(ctx)

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(
                    ctx
                )

            cameraProviderFuture.addListener({

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(
                                previewView.surfaceProvider
                            )
                        }

                val analysis =
                    ImageAnalysis.Builder()
                        .setTargetResolution(
                            Size(
                                1280,
                                1280
                            )
                        )
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()

                analysis.setAnalyzer(
                    analysisExecutor
                ) { imageProxy ->

                    val mediaImage =
                        imageProxy.image

                    if (mediaImage != null) {

                        val image =
                            InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy
                                    .imageInfo
                                    .rotationDegrees
                            )

                        scanner.process(image)
                            .addOnSuccessListener {
                                    barcodes: List<Barcode> ->

                                val value =
                                    barcodes
                                        .firstOrNull {
                                            !it.rawValue
                                                .isNullOrBlank()
                                        }
                                        ?.rawValue

                                if (value != null) {

                                    onBarcodeDetected(
                                        value
                                    )
                                }
                            }
                            .addOnCompleteListener {

                                imageProxy.close()
                            }

                    } else {

                        imageProxy.close()
                    }
                }

                try {

                    cameraProvider.unbindAll()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )

                } catch (e: Exception) {

                    // Camera bind failed.
                    // Manual entry can still be used.
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}
