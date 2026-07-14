package com.ramerlabs.scanelite.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SeBgPrimary = Color(0xFF14181F)
val SeBgElevated = Color(0xFF1C222C)
val SeBgSurface = Color(0xFF242B36)
val SeTextPrimary = Color(0xFFF5F7FA)
val SeTextSecondary = Color(0xFF9AA3B2)
val SeGold = Color(0xFFC9A227)
val SeEmerald = Color(0xFF2ECC8A)
val SeEdgeNeon = Color(0xFF4DA3FF)
val SeEdgeGold = Color(0xFFE6C35C)
val SeDanger = Color(0xFFE45D5D)
val SeDivider = Color(0xFF2E3644)

val RadiusMd = 12.dp

private val DarkColors = darkColorScheme(
    primary = SeGold,
    onPrimary = SeBgPrimary,
    secondary = SeEmerald,
    background = SeBgPrimary,
    surface = SeBgElevated,
    onBackground = SeTextPrimary,
    onSurface = SeTextPrimary,
    error = SeDanger
)

private val SeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = SeTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    )
)

@Composable
fun ScanEliteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = SeTypography,
        content = content
    )
}

val CardShape = RoundedCornerShape(RadiusMd)
