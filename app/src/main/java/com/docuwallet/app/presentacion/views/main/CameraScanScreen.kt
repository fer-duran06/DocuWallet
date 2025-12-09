package com.docuwallet.app.presentacion.views.main

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.docuwallet.app.utils.CameraUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    onNavigateBack: () -> Unit,
    onImagesCapture: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isCapturing by remember { mutableStateOf(false) }

    val imageCapture = remember { CameraUtils.createImageCaptureUseCase() }
    val cameraSelector = remember { CameraUtils.getDefaultCameraSelector() }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        try {
            val cameraProvider = CameraUtils.getCameraProvider(context)

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Create preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Escanear Documento")
                        Text(
                            "Páginas: ${capturedImages.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (capturedImages.isNotEmpty()) {
                        IconButton(
                            onClick = { onImagesCapture(capturedImages) }
                        ) {
                            Icon(Icons.Default.Check, "Finalizar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera Preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Capture Button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!isCapturing) {
                            scope.launch {
                                try {
                                    isCapturing = true
                                    val uri = CameraUtils.captureImage(
                                        context = context,
                                        imageCapture = imageCapture,
                                        executor = executor
                                    )
                                    capturedImages = capturedImages + uri
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isCapturing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Capturar",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }

            // Loading indicator
            if (isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}