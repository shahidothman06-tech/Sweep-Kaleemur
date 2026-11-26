package com.hidden.devices.detector.bugs.finder.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.components.GhostButton
import com.hidden.devices.detector.bugs.finder.ui.components.IconSquare
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val GAUGE_MAX = 200f

@Composable
fun MagnetScreen(vm: AppViewModel, onBack: () -> Unit) {
    var current by remember { mutableFloatStateOf(0f) }
    var baseline by remember { mutableFloatStateOf(-1f) }
    var peak by remember { mutableFloatStateOf(0f) }
    var sound by remember { mutableStateOf(vm.prefs.soundOn) }

    LaunchedEffect(Unit) {
        vm.magnetSensor.readings().collect { v ->
            current = v
            if (baseline < 0f) baseline = v
            peak = max(peak, v)
        }
    }
    val delta = if (baseline < 0f) 0f else current - baseline
    val deltaNow by rememberUpdatedState(delta)

    // Beeps faster as the field rises above the baseline.
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(sound) {
        while (sound) {
            val d = deltaNow
            if (d > 15f) {
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                delay((900 - (d.coerceAtMost(150f) * 5.5f)).toLong().coerceAtLeast(90))
            } else delay(200)
        }
    }

    val (statusText, statusColor) = when {
        delta >= 50 -> stringResource(R.string.magnet_status_strong) to Sweep.Red
        delta >= 15 -> stringResource(R.string.magnet_status_raised) to Sweep.Amber
        else -> stringResource(R.string.magnet_status_normal) to Sweep.Green
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.magnet_title), onBack = onBack) {
            IconSquare(
                if (sound) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                stringResource(if (sound) R.string.sound_on else R.string.sound_off),
                onClick = { sound = !sound; vm.prefs.soundOn = sound },
            )
        }
        if (!vm.magnetSensor.isAvailable) {
            SweepCard { Text(stringResource(R.string.magnet_no_sensor), style = MaterialTheme.typography.bodyMedium) }
            return@Column
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Gauge(current) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(current.toInt().toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Sweep.Text)
                Text(" " + stringResource(R.string.magnet_unit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Sweep.Dim, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(6.dp))
            Pill(statusText, statusColor)
        }
        Spacer(Modifier.height(22.dp))
        SweepCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.magnet_normal_room), style = MaterialTheme.typography.bodyMedium)
                    Text("~${baseline.coerceAtLeast(0f).toInt()} µT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Sweep.Text)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.magnet_highest), style = MaterialTheme.typography.bodyMedium)
                    Text("${peak.toInt()} µT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Sweep.Text)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SweepCard { Text(stringResource(R.string.magnet_tip), style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(20.dp))
        GhostButton(stringResource(R.string.magnet_reset), { baseline = current; peak = current })
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Gauge(value: Float) {
    Canvas(Modifier.size(width = 280.dp, height = 170.dp)) {
        val stroke = 22.dp.toPx()
        val r = size.width / 2 - stroke
        val center = Offset(size.width / 2, size.height - 10.dp.toPx())
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2, r * 2)
        drawArc(Sweep.Card2, 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Sweep.Green, 180f, 60f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Sweep.Amber, 240f, 60f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(Sweep.Red, 300f, 60f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        val a = Math.toRadians((180.0 + 180.0 * (value / GAUGE_MAX).coerceIn(0f, 1f)))
        val tip = Offset(center.x + (r * 0.8f * cos(a)).toFloat(), center.y + (r * 0.8f * sin(a)).toFloat())
        drawLine(Sweep.Text, center, tip, 5.dp.toPx(), StrokeCap.Round)
        drawCircle(Sweep.Text, 10.dp.toPx(), center)
    }
}
