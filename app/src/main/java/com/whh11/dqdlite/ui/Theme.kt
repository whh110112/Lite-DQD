package com.whh11.dqdlite.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.data.ThemeMode

val LocalIsDark = compositionLocalOf { false }
val LocalIsAmoled = compositionLocalOf { false }

/**
 * 懂球帝正统视觉语义 Token（参考 hupuX 设计哲学）：
 * - 白底承载内容，草皮绿（#16B13A）克制点睛，绝不大面积铺荧光色块
 * - 深色模式采用无色偏高级深灰，AMOLED 模式纯黑省电
 */
object DqdColors {
    val BrandGreen = Color(0xFF16B13A)
    val BrandGreenDark = Color(0xFF22C55E)
    val Red = Color(0xFFE53935)
    val Amber = Color(0xFFF59E0B)
    val Blue = Color(0xFF2563EB)

    val Primary: Color
        @Composable get() = if (LocalIsDark.current) BrandGreenDark else BrandGreen

    val PrimaryContainer: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF132A19) else Color(0xFFE8F7EC)

    val Bg: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF000000)
            LocalIsDark.current -> Color(0xFF111214)
            else -> Color(0xFFF5F6F8)
        }

    val Card: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF000000)
            LocalIsDark.current -> Color(0xFF1A1C20)
            else -> Color(0xFFFFFFFF)
        }

    val SurfaceHigh: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF141414)
            LocalIsDark.current -> Color(0xFF23262C)
            else -> Color(0xFFF0F2F5)
        }

    val Divider: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF1C1C1C)
            LocalIsDark.current -> Color(0xFF282C34)
            else -> Color(0xFFE8EAEE)
        }

    val TextPrimary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFFECEFF4) else Color(0xFF18181B)

    val TextSecondary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF9AA3B2) else Color(0xFF6B7280)

    val TextTertiary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF687182) else Color(0xFF9CA3AF)
}

private val LightScheme = lightColorScheme(
    primary = DqdColors.BrandGreen,
    onPrimary = Color.White,
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF18181B),
    surface = Color.White,
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE8EAEE),
    error = DqdColors.Red
)

private val DarkScheme = darkColorScheme(
    primary = DqdColors.BrandGreenDark,
    onPrimary = Color(0xFF052E16),
    background = Color(0xFF111214),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF1A1C20),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF23262C),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF282C34),
    error = Color(0xFFF87171)
)

private val AmoledScheme = DarkScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF141414),
    outline = Color(0xFF1C1C1C)
)

private val DqdType = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.5.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun DqdTheme(content: @Composable () -> Unit) {
    val mode by LocalStore.themeMode.collectAsState()
    val amoled by LocalStore.amoled.collectAsState()
    val fontScale by LocalStore.fontScale.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val isDark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val scheme = when {
        !isDark -> LightScheme
        amoled -> AmoledScheme
        else -> DarkScheme
    }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalIsAmoled provides (isDark && amoled),
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale)
    ) {
        MaterialTheme(colorScheme = scheme, typography = DqdType, content = content)
    }
}
