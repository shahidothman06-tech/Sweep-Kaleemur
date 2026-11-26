package com.hidden.devices.detector.bugs.finder.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.components.IconSquare
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartScan: () -> Unit,
    onLens: () -> Unit,
    onBluetooth: () -> Unit,
    onMagnet: () -> Unit,
    onSettings: () -> Unit,
) {
    val scan by vm.scan.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.app_full_name), style = MaterialTheme.typography.headlineSmall)
            }
            IconSquare(Icons.Rounded.Settings, stringResource(R.string.settings_title), onClick = onSettings)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1B2333), Color(0xFF141922))))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScanBadge()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.home_scan_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.home_scan_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            PrimaryButton(stringResource(R.string.home_start), onStartScan)
        }

        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_tools), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            val last = scan.lastScanTime
            Text(
                if (last == null) stringResource(R.string.home_never)
                else stringResource(R.string.home_last_scan, DateUtils.getRelativeTimeSpanString(last).toString()),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tool(Icons.Rounded.Wifi, Sweep.Accent, stringResource(R.string.tool_wifi), stringResource(R.string.tool_wifi_sub), onStartScan, Modifier.weight(1f))
            Tool(Icons.Rounded.CenterFocusStrong, Sweep.Red, stringResource(R.string.tool_lens), stringResource(R.string.tool_lens_sub), onLens, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tool(Icons.Rounded.Bluetooth, Sweep.Green, stringResource(R.string.tool_bt), stringResource(R.string.tool_bt_sub), onBluetooth, Modifier.weight(1f))
            Tool(Icons.Rounded.Sensors, Sweep.Amber, stringResource(R.string.tool_magnet), stringResource(R.string.tool_magnet_sub), onMagnet, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ScanBadge() {
    androidx.compose.foundation.layout.Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Sweep.Accent.copy(alpha = 0.12f), radius = size.minDimension / 2 - 2, style = Stroke(2.dp.toPx()))
            drawCircle(Sweep.Accent.copy(alpha = 0.22f), radius = size.minDimension * 0.35f, style = Stroke(2.dp.toPx()))
            drawCircle(Sweep.Accent, radius = size.minDimension * 0.23f)
        }
        Icon(Icons.Rounded.Videocam, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun Tool(icon: ImageVector, tint: Color, title: String, sub: String, onClick: () -> Unit, modifier: Modifier) {
    SweepCard(modifier = modifier.height(100.dp), padding = 14.dp, onClick = onClick) {
        IconSquare(icon, null, tint = tint, background = tint.copy(alpha = 0.15f), size = 34.dp)
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
