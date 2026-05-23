package com.zoya.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZoyaDarkColors = darkColorScheme(
    primary = Color(0xFF00F5FF),
    onPrimary = Color(0xFF020412),
    secondary = Color(0xFF0080FF),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF020412),
    surface = Color(0xFF07182E),
    onBackground = Color(0xFFC8EEFF),
    onSurface = Color(0xFFC8EEFF)
)

@Composable
fun ZoyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZoyaDarkColors,
        content = content
    )
}
