package com.pilahito.cloudterm.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF0B1214)
val AppSurface = Color(0xFF151C1E)
val AppKey = Color(0xFF1E2A2E)
val Mint = Color(0xFF40E0D0)

@Composable
fun CloudTermTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint,
            onPrimary = Color(0xFF06201C),
            background = AppBackground,
            onBackground = Color(0xFFE6EDF3),
            surface = AppSurface,
            onSurface = Color(0xFFE6EDF3),
            surfaceVariant = Color(0xFF1E2A2E),
            onSurfaceVariant = Color(0xFFA9B4BE),
            error = Color(0xFFFF7B72),
            secondary = Mint,
        ),
        content = content,
    )
}
