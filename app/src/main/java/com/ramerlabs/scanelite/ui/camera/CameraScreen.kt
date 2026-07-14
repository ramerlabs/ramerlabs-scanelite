package com.ramerlabs.scanelite.ui.camera

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ramerlabs.scanelite.domain.ScanMode
import com.ramerlabs.scanelite.ui.components.EdgeOverlay
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeEmerald
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    sessionViewModel: SessionViewModel,
    onClose: () -> Unit,
    onCapturedContinue: () -> Unit,
    onBatchDone: () -> Unit
) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var holdSteadyMs by remember { mutableIntStateOf(0) }

    LaunchedEffect(cameraPermission.status.isGranted) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // Simulated auto-capture lock: after preview is up, pulse then lock then fire
    LaunchedEffect(state.autoCapture, state.pages.size, state.processing) {
        if (!state.autoCapture || state.processing) return@LaunchedEffect
        sessionViewModel.setEdgeLocked(false)
        holdSteadyMs = 0
        while (holdSteadyMs < 500) {
            delay(100)
            holdSteadyMs += 100
        }
        sessionViewModel.setEdgeLocked(true)
        delay(450)
        // Auto shutter is handled by parent calling takePicture via a shared callback —
        // we set a flag and let UI shutter logic fire once.
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
    ) {
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val future = ProcessCameraProvider.getInstance(ctx)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Camera permission required",
                color = SeTextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        EdgeOverlay(
            locked = state.edgeLocked,
            style = state.edgeStyle,
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = if (state.edgeLocked) "Ready" else if (state.autoCapture) "Hold steady…" else "Manual",
                color = if (state.edgeLocked) SeEmerald else SeTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (state.autoCapture) "Auto" else "Manual",
                color = SeGold,
                modifier = Modifier
                    .clickable { sessionViewModel.setAutoCapture(!state.autoCapture) }
                    .padding(8.dp)
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SeBgElevated.copy(alpha = 0.92f))
                .padding(16.dp)
        ) {
            ModeToggle(
                mode = state.mode,
                onMode = sessionViewModel::setMode
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "${state.pages.size}",
                    color = SeTextSecondary,
                    modifier = Modifier.width(40.dp)
                )
                ShutterButton(
                    enabled = !state.processing,
                    onClick = {
                        takePicture(imageCapture, executor, context) { bmp ->
                            sessionViewModel.onCaptureBitmap(bmp)
                        }
                    }
                )
                if (state.mode == ScanMode.Batch && state.pages.isNotEmpty()) {
                    IconButton(onClick = onBatchDone) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Done", tint = SeEmerald)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
            if (state.mode == ScanMode.Batch && state.pages.isNotEmpty()) {
                Text(
                    "${state.pages.size} page(s) · tap ✓ when finished",
                    color = SeTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            }
        }

        if (state.processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SeGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Enhancing…", color = SeTextPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // After single capture completes, navigate to editor
    LaunchedEffect(state.pages.size, state.processing, state.mode) {
        if (!state.processing && state.pages.isNotEmpty() && state.mode == ScanMode.Single) {
            onCapturedContinue()
        }
    }

    // Auto-capture trigger when locked
    LaunchedEffect(state.edgeLocked, state.autoCapture, state.processing) {
        if (state.edgeLocked && state.autoCapture && !state.processing && cameraPermission.status.isGranted) {
            delay(200)
            takePicture(imageCapture, executor, context) { bmp ->
                sessionViewModel.onCaptureBitmap(bmp)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }
}

@Composable
private fun ModeToggle(mode: ScanMode, onMode: (ScanMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SeBgPrimary, RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(ScanMode.Single, ScanMode.Batch).forEach { m ->
            val selected = mode == m
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) SeGold else Color.Transparent,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable { onMode(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = m.name,
                    color = if (selected) SeBgPrimary else SeTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .border(3.dp, SeGold, CircleShape)
            .padding(6.dp)
            .background(if (enabled) Color.White else Color.Gray, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    )
}

private fun takePicture(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    context: android.content.Context,
    onBitmap: (Bitmap) -> Unit
) {
    val photo = java.io.File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(photo).build()
    imageCapture.takePicture(
        output,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bmp = android.graphics.BitmapFactory.decodeFile(photo.absolutePath)
                if (bmp != null) onBitmap(bmp)
            }

            override fun onError(exception: ImageCaptureException) {
                // User can retry via shutter
            }
        }
    )
}
