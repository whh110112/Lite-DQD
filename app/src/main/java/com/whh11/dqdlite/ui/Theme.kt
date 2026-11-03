package com.whh11.dqdlite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 懂球帝配色。
 * 主色 #00D11F 是从官网 logo 像素里直接采样出来的（800x206 PNG，
 * 该值占 7400 个采样点，绝对主导），不是猜的。
 * 深色底往墨绿方向压，保证绿色在上面够跳但不刺眼。
 */
object DqdColors {
    val Green = Color(0xFF00D11F)
    val GreenSoft = Color(0xFF45E668)
    val GreenDeep = Color(0xFF00A818)
    val Accent = Color(0xFFFFC53D)

    val Bg = Color(0xFF071009)
    val Surface = Color(0xFF0E1A12)
    val SurfaceHigh = Color(0xFF17261B)
    val Line = Color(0xFF1F3325)

    val TextPrimary = Color(0xFFE9F2EB)
    val TextSecondary = Color(0xFF86968B)
    val TextTertiary = Color(0xFF5C6B60)
}

private val DqdScheme = darkColorScheme(
    primary = DqdColors.Green,
    onPrimary = Color(0xFF00210A),
    primaryContainer = DqdColors.GreenDeep,
    onPrimaryContainer = Color(0xFFBFF5C9),
    secondary = DqdColors.Accent,
    onSecondary = Color(0xFF241A00),
    background = DqdColors.Bg,
    onBackground = DqdColors.TextPrimary,
    surface = DqdColors.Surface,
    onSurface = DqdColors.TextPrimary,
    surfaceVariant = DqdColors.SurfaceHigh,
    onSurfaceVariant = DqdColors.TextSecondary,
    outline = DqdColors.Line,
    error = Color(0xFFE5484D)
)

private val DqdType = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
)

@Composable
fun DqdTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DqdScheme, typography = DqdType, content = content)
}
