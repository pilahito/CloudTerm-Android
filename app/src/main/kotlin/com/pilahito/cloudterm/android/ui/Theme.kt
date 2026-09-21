package com.pilahito.cloudterm.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CtBg = Color(0xFF071018)
val CtSurface = Color(0xFF0E1A22)
val CtCard = Color(0xFF12202A)
val CtAccent = Color(0xFF22D3EE)
val CtAccentDim = Color(0xFF167A8A)
val CtPink = Color(0xFFFF4D9A)
val CtText = Color(0xFFF4FBFF)
val CtMuted = Color(0xFF8AA3B2)
val CtOnline = Color(0xFF34E0A1)

private val Scheme = darkColorScheme(
    primary = CtAccent,
    onPrimary = Color(0xFF042026),
    secondary = CtPink,
    background = CtBg,
    surface = CtSurface,
    surfaceVariant = CtCard,
    onBackground = CtText,
    onSurface = CtText,
    onSurfaceVariant = CtMuted,
    outline = CtAccentDim,
)

@Composable
fun CloudTermTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

@Composable
fun HexLogo(size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = s * 0.46f
        val hex = Path()
        for (i in 0..5) {
            val a = Math.toRadians((-90.0 + i * 60.0))
            val x = cx + r * Math.cos(a).toFloat()
            val y = cy + r * Math.sin(a).toFloat()
            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
        }
        hex.close()
        drawPath(hex, CtAccent.copy(alpha = 0.18f))
        drawPath(hex, CtAccent, style = Stroke(width = s * 0.08f, cap = StrokeCap.Round))
        val sw = s * 0.10f
        drawLine(CtAccent, Offset(cx - r * 0.22f, cy - r * 0.28f), Offset(cx + r * 0.18f, cy - r * 0.28f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(CtAccent, Offset(cx + r * 0.18f, cy - r * 0.28f), Offset(cx - r * 0.10f, cy + r * 0.08f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(CtAccent, Offset(cx - r * 0.22f, cy + r * 0.08f), Offset(cx + r * 0.22f, cy + r * 0.08f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(CtAccent, Offset(cx - r * 0.10f, cy + r * 0.08f), Offset(cx + r * 0.10f, cy + r * 0.32f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}
