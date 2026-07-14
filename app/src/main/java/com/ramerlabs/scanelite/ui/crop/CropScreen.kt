package com.ramerlabs.scanelite.ui.crop

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.hypot
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
    var crop by remember(page.id) {
        mutableStateOf(
            page.crop.let {
                if (it.left == 0f && it.top == 0f && it.right == 1f && it.bottom == 1f) {
                    CropNorm(0.08f, 0.08f, 0.92f, 0.92f)
                } else it.clamped()
            }
        )
    }
    // For drawing highlight only (gesture locks handle in a local var).
    var highlightHandle by remember { mutableStateOf(DragHandle.None) }

    val cropState = rememberUpdatedState(crop)
    val pageIdState = rememberUpdatedState(page.id)

    fun persistCrop(current: CropNorm = crop) {
        sessionViewModel.updateCropForPage(pageIdState.value, current)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 28.dp)
    ) {
        Text("Crop area", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SeTextPrimary)
        Text(
            "Page ${index + 1} of ${pages.size} · drag one gold corner at a time",
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

                    // Draw image + overlay in one Canvas so touch and visuals share coordinates.
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(page.id, offsetX, offsetY, drawW, drawH) {
                                // Local lock for the whole gesture — never swap mid-drag.
                                var locked: DragHandle = DragHandle.None
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val c = cropState.value
                                        locked = pickHandle(
                                            start = start,
                                            l = offsetX + c.left * drawW,
                                            t = offsetY + c.top * drawH,
                                            r = offsetX + c.right * drawW,
                                            b = offsetY + c.bottom * drawH
                                        )
                                        highlightHandle = locked
                                    },
                                    onDragEnd = {
                                        if (locked != DragHandle.None) {
                                            persistCrop(cropState.value)
                                        }
                                        locked = DragHandle.None
                                        highlightHandle = DragHandle.None
                                    },
                                    onDragCancel = {
                                        locked = DragHandle.None
                                        highlightHandle = DragHandle.None
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        val handle = locked
                                        if (handle == DragHandle.None || drawW <= 0f || drawH <= 0f) {
                                            return@detectDragGestures
                                        }
                                        val dx = amount.x / drawW
                                        val dy = amount.y / drawH
                                        val c = cropState.value
                                        val minSize = 0.10f
                                        crop = applyHandleDrag(handle, c, dx, dy, minSize)
                                    }
                                )
                            }
                    ) {
                        drawImage(
                            image = imageBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(
                                offsetX.toInt(),
                                offsetY.toInt()
                            ),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                drawW.toInt().coerceAtLeast(1),
                                drawH.toInt().coerceAtLeast(1)
                            )
                        )
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
                        val corners = listOf(
                            DragHandle.TopLeft to Offset(l, t),
                            DragHandle.TopRight to Offset(r, t),
                            DragHandle.BottomLeft to Offset(l, b),
                            DragHandle.BottomRight to Offset(r, b)
                        )
                        corners.forEach { (h, center) ->
                            val active = highlightHandle == h
                            drawCircle(
                                color = SeGold,
                                radius = if (active) 24f else 18f,
                                center = center
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (active) 10f else 8f,
                                center = center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
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
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SeGold,
                    contentColor = SeBgPrimary
                )
            ) { Text("Done", fontWeight = FontWeight.SemiBold) }
        }
    }
}

/**
 * Pick the nearest corner first (by distance). Only fall back to edges / move
 * when the touch is clearly not on a corner — prevents “grabbed the wrong point”.
 */
private fun pickHandle(
    start: Offset,
    l: Float,
    t: Float,
    r: Float,
    b: Float
): DragHandle {
    val cornerHit = 64f
    val edgeHit = 36f
    val corners = listOf(
        DragHandle.TopLeft to Offset(l, t),
        DragHandle.TopRight to Offset(r, t),
        DragHandle.BottomLeft to Offset(l, b),
        DragHandle.BottomRight to Offset(r, b)
    )
    val nearest = corners.minBy { (_, p) -> hypot(start.x - p.x, start.y - p.y) }
    val nearestDist = hypot(start.x - nearest.second.x, start.y - nearest.second.y)
    if (nearestDist <= cornerHit) return nearest.first

    // Mid-edge grips (only if closer to that edge than the opposite one).
    if (abs(start.x - l) <= edgeHit && start.y in t..b && abs(start.x - l) <= abs(start.x - r)) {
        return DragHandle.Left
    }
    if (abs(start.x - r) <= edgeHit && start.y in t..b && abs(start.x - r) < abs(start.x - l)) {
        return DragHandle.Right
    }
    if (abs(start.y - t) <= edgeHit && start.x in l..r && abs(start.y - t) <= abs(start.y - b)) {
        return DragHandle.Top
    }
    if (abs(start.y - b) <= edgeHit && start.x in l..r && abs(start.y - b) < abs(start.y - t)) {
        return DragHandle.Bottom
    }
    if (start.x in l..r && start.y in t..b) return DragHandle.Move
    return DragHandle.None
}

private fun applyHandleDrag(
    handle: DragHandle,
    c: CropNorm,
    dx: Float,
    dy: Float,
    minSize: Float
): CropNorm {
    return when (handle) {
        DragHandle.Move -> {
            val w = c.right - c.left
            val h = c.bottom - c.top
            val nl = (c.left + dx).coerceIn(0f, 1f - w)
            val nt = (c.top + dy).coerceIn(0f, 1f - h)
            CropNorm(nl, nt, nl + w, nt + h)
        }
        DragHandle.Left -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - minSize))
        DragHandle.Right -> c.copy(right = (c.right + dx).coerceIn(c.left + minSize, 1f))
        DragHandle.Top -> c.copy(top = (c.top + dy).coerceIn(0f, c.bottom - minSize))
        DragHandle.Bottom -> c.copy(bottom = (c.bottom + dy).coerceIn(c.top + minSize, 1f))
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
