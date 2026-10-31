package com.ashna.streakkeeper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color(0xFF1A0E05),
    secondary = Color(0xFFB39DFF),
    background = Color(0xFF0E0E12),
    surface = Color(0xFF16161D),
    surfaceVariant = Color(0xFF1F1F29),
    onBackground = Color(0xFFEDEDF2),
    onSurface = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFFB9B9C6),
    error = Color(0xFFFF5449)
)

@Composable
fun StreakKeeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
