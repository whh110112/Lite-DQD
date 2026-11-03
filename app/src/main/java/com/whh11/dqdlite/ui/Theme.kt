package com.whh11.dqdlite.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("白天模式"),
    DARK("夜间模式")
}

object ThemeManager {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun init(context: Context) {
        val sp = context.getSharedPreferences("dqd_pref", Context.MODE_PRIVATE)
        val saved = sp.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        mode = runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        val sp = context.getSharedPreferences("dqd_pref", Context.MODE_PRIVATE)
        sp.edit().putString("theme_mode", newMode.name).apply()
    }
}

val LocalIsDark = compositionLocalOf { true }

object DqdColors {
    val Green = Color(0xFF00D11F)
    val GreenSoft = Color(0xFF45E668)
    val GreenDeep = Color(0xFF00A818)
    val Accent = Color(0xFFFFC53D)
    val Red = Color(0xFFE04E4C)

    val Primary: Color get() = Green

    val Bg: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF071009) else Color(0xFFF4F6F4)

    val Surface: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF0E1A12) else Color(0xFFFFFFFF)

    val Card: Color
        @Composable get() = Surface

    val SurfaceHigh: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF17261B) else Color(0xFFEBF0EB)

    val Line: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF1F3325) else Color(0xFFE0E6E1)

    val Border: Color
        @Composable get() = Line

    val TextPrimary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFFE9F2EB) else Color(0xFF1A241C)

    val TextSecondary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF86968B) else Color(0xFF617366)

    val TextTertiary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF5C6B60) else Color(0xFF8C9B90)
}

private val DarkScheme = darkColorScheme(
    primary = DqdColors.Green,
    onPrimary = Color(0xFF00210A),
    primaryContainer = DqdColors.GreenDeep,
    onPrimaryContainer = Color(0xFFBFF5C9),
    secondary = DqdColors.Accent,
    onSecondary = Color(0xFF241A00),
    background = Color(0xFF071009),
    onBackground = Color(0xFFE9F2EB),
    surface = Color(0xFF0E1A12),
    onSurface = Color(0xFFE9F2EB),
    surfaceVariant = Color(0xFF17261B),
    onSurfaceVariant = Color(0xFF86968B),
    outline = Color(0xFF1F3325),
    error = Color(0xFFE5484D)
)

private val LightScheme = lightColorScheme(
    primary = DqdColors.GreenDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCF7E1),
    onPrimaryContainer = Color(0xFF003912),
    secondary = DqdColors.Accent,
    onSecondary = Color(0xFF241A00),
    background = Color(0xFFF4F6F4),
    onBackground = Color(0xFF1A241C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A241C),
    surfaceVariant = Color(0xFFEBF0EB),
    onSurfaceVariant = Color(0xFF617366),
    outline = Color(0xFFE0E6E1),
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
    val systemDark = isSystemInDarkTheme()
    val isDark = when (ThemeManager.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val scheme = if (isDark) DarkScheme else LightScheme

    CompositionLocalProvider(LocalIsDark provides isDark) {
        MaterialTheme(colorScheme = scheme, typography = DqdType, content = content)
    }
}
