package com.pilahito.cloudterm.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF1E2327)
val AppSurface = Color(0xFF252B31)
val AppKey = Color(0xFF3A444C)

@Composable
fun CloudTermTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6CB6FF),
            onPrimary = Color(0xFF0B1620),
            background = AppBackground,
            onBackground = Color(0xFFE6EDF3),
            surface = AppSurface,
            onSurface = Color(0xFFE6EDF3),
            surfaceVariant = Color(0xFF2D353C),
            onSurfaceVariant = Color(0xFFA9B4BE),
            error = Color(0xFFFF7B72),
        ),
        content = content,
    )
}
