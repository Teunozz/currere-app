package nl.teunk.currere.ui.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.serialization.json.Json
import nl.teunk.currere.data.api.QrPayload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onQrScanned: (baseUrl: String, token: String) -> Unit,
    onBack: () -> Unit,
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (hasCameraPermission && !scanned) {
                CameraPreview(
                    onBarcodeDetected = { rawValue ->
                        if (scanned) return@CameraPreview
                        try {
                            val payload = Json.decodeFromString<QrPayload>(rawValue)
                            scanned = true
                            onQrScanned(payload.baseUrl, payload.token)
                        } catch (_: Exception) {
                            // Not a valid QR payload, keep scanning
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Point camera at QR code",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else if (!hasCameraPermission) {
                Text(
                    text = "Camera permission is required to scan QR codes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val barcodeScanner = remember { BarcodeScanning.getClient(options) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(barcodeScanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context),
                ) { result ->
                    val barcodes = result?.getValue(barcodeScanner)
                    barcodes?.firstOrNull()?.rawValue?.let(onBarcodeDetected)
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            cameraController.unbind()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                controller = cameraController
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        },
    )
}
