package com.selftrain.app.ui.routines

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.selftrain.app.util.RoutineShareCodec
import com.selftrain.app.util.SharedRoutine

// --- Compartir: genera el QR de una rutina/programa ---

@Composable
fun ShareRoutineDialog(
    routineId: Long,
    routineName: String,
    viewModel: RoutinesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(routineId) {
        viewModel.buildSharePayload(routineId) { shared ->
            payload = shared?.let { RoutineShareCodec.encode(it) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Compartir \"$routineName\"") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val p = payload
                when {
                    p == null -> Text("Generando QR…")
                    else -> {
                        val bmp = remember(p) { toQrBitmap(p) }
                        if (bmp == null) {
                            Text("La rutina es demasiado grande para un QR")
                        } else {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "QR de la rutina",
                                modifier = Modifier.size(240.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Escanea con otro dispositivo para importar.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        // fallback si la cámara no lee el QR
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { copyPayload(context, p) }) {
                                Text("Copiar código")
                            }
                            Button(onClick = { sharePayload(context, p) }) {
                                Text("Compartir…")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- Escáner: cámara a pantalla completa dentro de un Dialog ---

@Composable
fun ScanQrDialog(
    onScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (hasPermission) {
                CameraScanner(onScanned = onScanned)
                // guía de encuadre: recuadro para centrar el QR
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.75f)
                            .aspectRatio(1f)
                            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Necesitamos acceso a la cámara para escanear el QR.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Permitir cámara")
                    }
                }
            }
            // fallback sin cámara: pegar el código copiado desde el otro dispositivo
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    val text = clip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.text?.toString()?.trim()
                    if (text.isNullOrEmpty()) {
                        Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                    } else {
                        onScanned(text)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Pegar código", color = Color.White)
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
private fun CameraScanner(onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // ponytail: guardia para no disparar onScanned varias veces antes de cerrar el diálogo
    var handled by remember { mutableStateOf(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val preview = remember { Preview.Builder().build() }

    val analysis = remember {
        ImageAnalysis.Builder()
            // ponytail: resolución alta (los lectores de sistema usan resolución completa) —
            // el default de 640x480 no resuelve QRs densos con cámaras malas
            .setTargetResolution(Size(1920, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            if (!handled) {
                                barcodes.firstOrNull { it.rawValue != null }?.let {
                                    handled = true
                                    onScanned(it.rawValue!!)
                                }
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
            }
    }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    val provider by produceState<ProcessCameraProvider?>(null) { value = providerFuture.get() }

    LaunchedEffect(provider) {
        val p = provider ?: return@LaunchedEffect
        try {
            p.unbindAll()
            // bindToLifecycle devuelve la Camera (no hay getCamera(CameraSelector) en 1.4.x)
            val camera = p.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            cameraControl = camera.cameraControl
            // ponytail: zoom inicial para acercar QRs pequeños; respeta el rango de la cámara
            camera.cameraInfo.zoomState.value?.let { zs ->
                if (zs.maxZoomRatio >= 1.5f) cameraControl?.setZoomRatio(1.5f)
            }
        } catch (_: Exception) {
            // cámara en uso por otra app: el preview simplemente no arranca
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                preview.setSurfaceProvider(surfaceProvider)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // tap-to-focus: mapea el toque del preview al sensor
                    val factory = SurfaceOrientedMeteringPointFactory(
                        size.width.toFloat(),
                        size.height.toFloat()
                    )
                    cameraControl?.startFocusAndMetering(
                        FocusMeteringAction.Builder(factory.createPoint(offset.x, offset.y)).build()
                    )
                }
            }
    ) {}
}

// --- Import: confirmación clonando PredefinedRoutinesDialog, con ejercicios listados ---

@Composable
fun ImportRoutineDialog(
    shared: SharedRoutine,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(shared.name) },
        text = {
            Column {
                Text(
                    if (shared.days.size > 1) "Se crearán ${shared.days.size} rutinas:"
                    else "Se creará una rutina:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    items(shared.days.size) { i ->
                        val day = shared.days[i]
                        Text(
                            "• ${day.name} (${day.exercises.size} ejercicios)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        day.exercises.forEach { ex ->
                            Text(
                                "   - ${ex.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Los ejercicios que no tengas se añadirán a tu biblioteca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Importar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- helpers ---

private fun copyPayload(context: Context, payload: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("rutina selftrain", payload))
    Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
}

private fun sharePayload(context: Context, payload: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, payload)
    }
    context.startActivity(Intent.createChooser(send, "Compartir rutina"))
}

private fun toQrBitmap(payload: String, sizePx: Int = 1024): Bitmap? {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 2
    )
    val matrix = runCatching {
        QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    }.getOrNull() ?: return null
    val pixels = IntArray(matrix.width * matrix.height)
    val black = AndroidColor.BLACK
    val white = AndroidColor.WHITE
    for (i in pixels.indices) {
        pixels[i] = if (matrix.get(i % matrix.width, i / matrix.width)) black else white
    }
    return Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}
