package com.ramerlabs.scanelite.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ramerlabs.scanelite.domain.EdgeStyle
import com.ramerlabs.scanelite.ui.theme.SeEdgeGold
import com.ramerlabs.scanelite.ui.theme.SeEdgeNeon
import com.ramerlabs.scanelite.ui.theme.SeEmerald

@Composable
fun EdgeOverlay(
    locked: Boolean,
    style: EdgeStyle,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "edge")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val color = when {
        locked -> SeEmerald
        style == EdgeStyle.Gold -> SeEdgeGold
        else -> SeEdgeNeon
    }
    val stroke = if (locked) 5f else 3.5f

    Canvas(modifier = modifier) {
        val insetX = size.width * 0.12f * (if (locked) 1f else pulse)
        val insetY = size.height * 0.16f * (if (locked) 1f else pulse)
        val path = Path().apply {
            moveTo(insetX, insetY + 24f)
            lineTo(insetX + 28f, insetY)
            lineTo(size.width - insetX - 20f, insetY + 8f)
            lineTo(size.width - insetX, size.height - insetY - 18f)
            lineTo(insetX + 16f, size.height - insetY)
            close()
        }
        drawPath(
            path = path,
            color = color.copy(alpha = if (locked) 0.95f else 0.75f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Corner accents
        listOf(
            Offset(insetX, insetY + 24f),
            Offset(size.width - insetX, size.height - insetY - 18f)
        ).forEach { c ->
            drawCircle(color = color, radius = 6f, center = c)
        }
    }
}
