package com.hidden.devices.detector.bugs.finder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hidden.devices.detector.bugs.finder.wifiscan.Risk

/** Colors from the approved prototype. */
object Sweep {
    val Bg = Color(0xFF0E1116)
    val NavBg = Color(0xFF0B0E12)
    val Card = Color(0xFF181C23)
    val Card2 = Color(0xFF1F242D)
    val Line = Color(0xFF2A303A)
    val Text = Color(0xFFE8EAED)
    val Dim = Color(0xFF9AA0A6)
    val Accent = Color(0xFF4F8CFF)
    val Red = Color(0xFFFF5A5F)
    val Amber = Color(0xFFFFB020)
    val Green = Color(0xFF3DDC84)

    fun risk(r: Risk): Color = when (r) {
        Risk.HIGH -> Red
        Risk.MEDIUM -> Amber
        Risk.LOW -> Green
    }
}

private val scheme = darkColorScheme(
    primary = Sweep.Accent,
    onPrimary = Color.White,
    background = Sweep.Bg,
    onBackground = Sweep.Text,
    surface = Sweep.Card,
    onSurface = Sweep.Text,
    surfaceVariant = Sweep.Card2,
    onSurfaceVariant = Sweep.Dim,
    outline = Sweep.Line,
    error = Sweep.Red,
)

private val type = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Sweep.Text),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Sweep.Text),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Sweep.Text),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = Sweep.Dim),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, color = Sweep.Dim),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun SweepTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
