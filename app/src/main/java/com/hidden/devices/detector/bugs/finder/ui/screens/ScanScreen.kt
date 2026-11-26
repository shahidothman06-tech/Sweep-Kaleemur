package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.hidden.devices.detector.bugs.finder.bluetooth.BleScanner
import com.hidden.devices.detector.bugs.finder.ui.Actions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.scan.StepStatus
import com.hidden.devices.detector.bugs.finder.ui.components.GhostButton
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

@Composable
fun ScanScreen(vm: AppViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    val s by vm.scan.collectAsStateWithLifecycle()
    LaunchedEffect(s.finished, s.running) { if (s.finished && !s.running) onFinished() }
    val cancel = { vm.cancelScan(); onBack() }
    // Bluetooth permission was denied: ask again, or open app settings if Android won't show the prompt any more.
    val context = LocalContext.current
    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!vm.ble.hasPermission()) Actions.openAppSettings(context)
    }
    val grantBt = { btPermission.launch(BleScanner.requiredPermissions()) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.scan_title), onBack = cancel)
        // Scrolls on small screens / large fonts, so Cancel below always stays visible.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Radar() }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scan_checking), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { s.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Sweep.Accent,
            trackColor = Sweep.Card,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepRow(stringResource(R.string.step_wifi), s.wifi, doneText = { stringResource(R.string.status_done_devices, it) }, skippedText = stringResource(R.string.status_no_wifi))
            StepRow(stringResource(R.string.step_bt), s.bt, doneText = { stringResource(R.string.status_done_found, it) }, skippedText = stringResource(R.string.status_no_bt), onGrant = grantBt)
            StepRow(stringResource(R.string.step_magnet), s.magnet, doneText = { stringResource(R.string.status_done_ut, it) }, skippedText = stringResource(R.string.status_no_sensor))
        }
        Spacer(Modifier.height(16.dp))
        }
        GhostButton(stringResource(R.string.cancel), cancel, Modifier.padding(top = 8.dp, bottom = 16.dp))
    }
}

@Composable
private fun StepRow(
    label: String,
    status: StepStatus,
    doneText: @Composable (Int) -> String,
    skippedText: String,
    onGrant: (() -> Unit)? = null,
) {
    SweepCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = Sweep.Text), modifier = Modifier.weight(1f))
            when (status) {
                StepStatus.Waiting -> Pill(stringResource(R.string.status_waiting), Sweep.Dim)
                StepStatus.Running -> Pill(stringResource(R.string.status_scanning), Sweep.Accent)
                is StepStatus.Done -> Pill(doneText(status.count), Sweep.Green, dot = true)
                StepStatus.Skipped -> Pill(skippedText, Sweep.Amber)
                StepStatus.NoPermission -> {
                    Pill(stringResource(R.string.status_no_permission), Sweep.Amber)
                    if (onGrant != null) {
                        TextButton(onClick = onGrant) { Text(stringResource(R.string.grant_permission), color = Sweep.Accent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Radar() {
    val t = rememberInfiniteTransition(label = "radar")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    Canvas(Modifier.size(230.dp)) {
        val r = size.minDimension / 2
        drawCircle(Sweep.Card, r)
        listOf(1f, 0.7f, 0.4f).forEach { drawCircle(Sweep.Line, r * it, style = Stroke(1.dp.toPx())) }
        drawArc(
            brush = Brush.sweepGradient(listOf(Sweep.Accent.copy(alpha = 0f), Sweep.Accent.copy(alpha = 0.5f))),
            startAngle = angle - 60f, sweepAngle = 60f, useCenter = true,
            topLeft = Offset.Zero, size = Size(size.width, size.height),
        )
        drawCircle(Sweep.Accent, 9.dp.toPx())
    }
}
