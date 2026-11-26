package com.hidden.devices.detector.bugs.finder.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Teal = Color(0xFF00C2A8)

/**
 * Short animated intro right after the system splash. The first frame matches the system splash exactly
 * (same 240dp icon box, 160dp teal circle, same glyph drawable, centered in the full window), then rings ripple
 * out, a radar wedge sweeps once and the name slides in. Total ~1.4 s. No ads here.
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val ripple = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    val title = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val tagline = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(400)
        } else {
            launch { ripple.animateTo(1f, tween(1200, easing = LinearEasing)) }
            launch { sweep.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
            launch { delay(250); title.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
            launch { delay(500); tagline.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
            delay(1400)
        }
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Sweep.Bg), contentAlignment = Alignment.Center) {
        // Ripples and radar sweep behind the icon.
        Canvas(Modifier.size(360.dp)) {
            val c = center
            val base = 80.dp.toPx()
            if (!reducedMotion) {
                // Radar wedge: one full turn, fading out at the end.
                val wedgeAlpha = (1f - sweep.value) * 0.9f
                val r = 136.dp.toPx()
                drawArc(
                    brush = Brush.radialGradient(
                        listOf(Teal.copy(alpha = 0.45f * wedgeAlpha), Teal.copy(alpha = 0f)),
                        center = c, radius = r,
                    ),
                    startAngle = -90f + sweep.value * 360f - 50f, sweepAngle = 50f, useCenter = true,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                )
                for (i in 0 until 3) {
                    val p = ((ripple.value * 1.4f) - i * 0.25f).coerceIn(0f, 1f)
                    if (p > 0f && p < 1f) {
                        drawCircle(
                            color = Teal.copy(alpha = (1f - p) * 0.7f),
                            radius = base + p * 56.dp.toPx(), // rings stay inside 136dp, above the title
                            center = c,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
        }
        // Same icon as the system splash: 160dp teal circle, glyph drawable in a 240dp box.
        Box(Modifier.size(160.dp).background(Teal, CircleShape))
        Image(painterResource(R.drawable.splash_icon), contentDescription = null, modifier = Modifier.size(240.dp))
        // Name under the icon.
        Column(
            Modifier.offset(y = 196.dp), // below the rings (max 136dp from center)
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.intro_brand),
                color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(title.value).offset(y = (16 * (1f - title.value)).dp),
            )
            Text(
                stringResource(R.string.intro_tagline),
                color = Sweep.Dim, fontSize = 16.sp,
                modifier = Modifier.alpha(tagline.value).offset(y = (12 * (1f - tagline.value)).dp),
            )
        }
    }
}
