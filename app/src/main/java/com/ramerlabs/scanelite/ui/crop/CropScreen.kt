package com.ramerlabs.scanelite.ui.crop

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.domain.CropNorm
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class DragHandle {
    None, Move, Left, Top, Right, Bottom, TopLeft, TopRight, BottomLeft, BottomRight
}

@Composable
fun CropScreen(
    sessionViewModel: SessionViewModel,
    onFinished: () -> Unit
) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    val pages = state.pages
    if (pages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SeBgPrimary)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No pages to crop", color = SeTextSecondary)
        }
        return
    }

    val index = state.cropPageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[index]
    val imageBitmap = remember(page.uriPath) {
        BitmapFactory.decodeFile(page.uriPath)?.asImageBitmap()
    }
    // Start slightly inset so handles are easy to grab (full-bleed looks “stuck”).
    var crop by remember(page.id) {
        mutableStateOf(
            page.crop.let {
                if (it.left == 0f && it.top == 0f && it.right == 1f && it.bottom == 1f) {
                    CropNorm(0.08f, 0.08f, 0.92f, 0.92f)
                } else it.clamped()
            }
        )
    }
    var activeHandle by remember { mutableStateOf(DragHandle.None) }

    fun persistCrop() {
        sessionViewModel.updateCropForPage(page.id, crop)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
            .padding(16.dp)
    ) {
        Text("Crop area", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SeTextPrimary)
        Text(
            "Page ${index + 1} of ${pages.size} · drag the gold corners to resize",
            color = SeTextSecondary,
            fontSize = 13.sp
        )
        Text(
            state.documentTitle,
            color = SeGold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SeBgElevated, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (imageBitmap != null) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val iw = imageBitmap.width.toFloat()
                    val ih = imageBitmap.height.toFloat()
                    val scale = min(boxW / iw, boxH / ih)
                    val drawW = iw * scale
                    val drawH = ih * scale
                    val offsetX = (boxW - drawW) / 2f
                    val offsetY = (boxH - drawH) / 2f

                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Page to crop",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // IMPORTANT: do not put `crop` in pointerInput keys — that restarts gestures mid-drag.
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(page.id, offsetX, offsetY, drawW, drawH) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val c = crop
                                        val l = offsetX + c.left * drawW
                                        val t = offsetY + c.top * drawH
                                        val r = offsetX + c.right * drawW
                                        val b = offsetY + c.bottom * drawH
                                        val hit = 72f
                                        activeHandle = when {
                                            near(start, l, t, hit) -> DragHandle.TopLeft
                                            near(start, r, t, hit) -> DragHandle.TopRight
                                            near(start, l, b, hit) -> DragHandle.BottomLeft
                                            near(start, r, b, hit) -> DragHandle.BottomRight
                                            abs(start.x - l) < hit && start.y in (t - hit)..(b + hit) ->
                                                DragHandle.Left
                                            abs(start.x - r) < hit && start.y in (t - hit)..(b + hit) ->
                                                DragHandle.Right
                                            abs(start.y - t) < hit && start.x in (l - hit)..(r + hit) ->
                                                DragHandle.Top
                                            abs(start.y - b) < hit && start.x in (l - hit)..(r + hit) ->
                                                DragHandle.Bottom
                                            start.x in l..r && start.y in t..b -> DragHandle.Move
                                            else -> DragHandle.None
                                        }
                                    },
                                    onDragEnd = {
                                        activeHandle = DragHandle.None
                                        persistCrop()
                                    },
                                    onDragCancel = { activeHandle = DragHandle.None },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        if (activeHandle == DragHandle.None || drawW <= 0f || drawH <= 0f) return@detectDragGestures
                                        val dx = amount.x / drawW
                                        val dy = amount.y / drawH
                                        val c = crop
                                        val minSize = 0.08f
                                        crop = when (activeHandle) {
                                            DragHandle.Move -> {
                                                val w = c.right - c.left
                                                val h = c.bottom - c.top
                                                val nl = (c.left + dx).coerceIn(0f, 1f - w)
                                                val nt = (c.top + dy).coerceIn(0f, 1f - h)
                                                CropNorm(nl, nt, nl + w, nt + h)
                                            }
                                            DragHandle.Left -> c.copy(
                                                left = (c.left + dx).coerceIn(0f, c.right - minSize)
                                            )
                                            DragHandle.Right -> c.copy(
                                                right = (c.right + dx).coerceIn(c.left + minSize, 1f)
                                            )
                                            DragHandle.Top -> c.copy(
                                                top = (c.top + dy).coerceIn(0f, c.bottom - minSize)
                                            )
                                            DragHandle.Bottom -> c.copy(
                                                bottom = (c.bottom + dy).coerceIn(c.top + minSize, 1f)
                                            )
                                            DragHandle.TopLeft -> c.copy(
                                                left = (c.left + dx).coerceIn(0f, c.right - minSize),
                                                top = (c.top + dy).coerceIn(0f, c.bottom - minSize)
                                            )
                                            DragHandle.TopRight -> c.copy(
                                                right = (c.right + dx).coerceIn(c.left + minSize, 1f),
                                                top = (c.top + dy).coerceIn(0f, c.bottom - minSize)
                                            )
                                            DragHandle.BottomLeft -> c.copy(
                                                left = (c.left + dx).coerceIn(0f, c.right - minSize),
                                                bottom = (c.bottom + dy).coerceIn(c.top + minSize, 1f)
                                            )
                                            DragHandle.BottomRight -> c.copy(
                                                right = (c.right + dx).coerceIn(c.left + minSize, 1f),
                                                bottom = (c.bottom + dy).coerceIn(c.top + minSize, 1f)
                                            )
                                            DragHandle.None -> c
                                        }.clamped()
                                    }
                                )
                            }
                    ) {
                        val l = offsetX + crop.left * drawW
                        val t = offsetY + crop.top * drawH
                        val r = offsetX + crop.right * drawW
                        val b = offsetY + crop.bottom * drawH
                        val dim = Color.Black.copy(alpha = 0.55f)
                        drawRect(dim, Offset(offsetX, offsetY), Size(drawW, max(0f, t - offsetY)))
                        drawRect(dim, Offset(offsetX, b), Size(drawW, max(0f, offsetY + drawH - b)))
                        drawRect(dim, Offset(offsetX, t), Size(max(0f, l - offsetX), max(0f, b - t)))
                        drawRect(dim, Offset(r, t), Size(max(0f, offsetX + drawW - r), max(0f, b - t)))
                        drawRect(
                            color = SeGold,
                            topLeft = Offset(l, t),
                            size = Size(r - l, b - t),
                            style = Stroke(width = 4f)
                        )
                        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { center ->
                            drawCircle(SeGold, radius = 18f, center = center)
                            drawCircle(Color.White, radius = 8f, center = center)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (index > 0) {
                Button(
                    onClick = {
                        persistCrop()
                        sessionViewModel.setCropPageIndex(index - 1)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SeBgElevated,
                        contentColor = SeTextPrimary
                    )
                ) { Text("Previous") }
            }
            if (index < pages.lastIndex) {
                Button(
                    onClick = {
                        persistCrop()
                        sessionViewModel.setCropPageIndex(index + 1)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SeBgElevated,
                        contentColor = SeTextPrimary
                    )
                ) { Text("Next page") }
            }
            Button(
                onClick = {
                    persistCrop()
                    sessionViewModel.ensureAutoName()
                    onFinished()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SeGold,
                    contentColor = SeBgPrimary
                )
            ) { Text("Done", fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun near(p: Offset, x: Float, y: Float, hit: Float): Boolean {
    return abs(p.x - x) <= hit && abs(p.y - y) <= hit
}
