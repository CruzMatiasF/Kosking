package com.kosking.app.scanner.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
internal fun CamaraEscaner(
    onCodigo: (String) -> Unit,
    isYaDetecto: () -> Boolean,
    buscandoProducto: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val pv = PreviewView(ctx)
                val exec = Executors.newSingleThreadExecutor()
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build()
                        .also { it.setSurfaceProvider(pv.surfaceProvider) }
                    val scanner = BarcodeScanning.getClient()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { ia ->
                            ia.setAnalyzer(exec) { proxy ->
                                @androidx.camera.core.ExperimentalGetImage
                                val media = proxy.image
                                if (media != null && !isYaDetecto()) {
                                    val img = InputImage.fromMediaImage(
                                        media, proxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(img)
                                        .addOnSuccessListener { codes ->
                                            codes.firstOrNull { it.rawValue != null }
                                                ?.rawValue?.let { onCodigo(it) }
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                } else proxy.close()
                            }
                        }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                pv
            },
            modifier = Modifier.fillMaxSize()
        )

        if (buscandoProducto) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}
