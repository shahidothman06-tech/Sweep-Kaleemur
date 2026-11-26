package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.bluetooth.BleDevice
import com.hidden.devices.detector.bugs.finder.bluetooth.BleScanner
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.scan.Findings
import com.hidden.devices.detector.bugs.finder.ui.components.Dot
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.findingReasons
import com.hidden.devices.detector.bugs.finder.ui.findingTitle
import com.hidden.devices.detector.bugs.finder.ui.formatMeters
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

@Composable
fun BluetoothScreen(vm: AppViewModel, onBack: () -> Unit) {
    var granted by remember { mutableStateOf(vm.ble.hasPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted = vm.ble.hasPermission() }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(BleScanner.requiredPermissions()) }
    var devices by remember { mutableStateOf<List<BleDevice>>(emptyList()) }
    LaunchedEffect(granted) { if (granted) vm.ble.scan().collect { devices = it } }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.bt_title), onBack = onBack)
        when {
            !granted -> SweepCard {
                Text(stringResource(R.string.bt_permission_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.bt_permission_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                PrimaryButton(stringResource(R.string.allow), { launcher.launch(BleScanner.requiredPermissions()) })
            }
            !vm.ble.isEnabled -> SweepCard { Text(stringResource(R.string.bt_off), style = MaterialTheme.typography.bodyMedium) }
            else -> {
                Text(stringResource(R.string.bt_sub), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 14.dp))
                if (devices.isEmpty()) Text(stringResource(R.string.bt_searching), style = MaterialTheme.typography.bodyMedium)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(devices.map(Findings::fromBle).sortedBy { it.risk.ordinal }, key = { it.id }) { f ->
                        val d = f.ble!!
                        val color = Sweep.risk(f.risk)
                        SweepCard(padding = 14.dp) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 6.dp)) { Dot(color, 8.dp) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(findingTitle(f), style = MaterialTheme.typography.titleMedium)
                                    findingReasons(f).firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        repeat(4) { i ->
                                            Box(Modifier.size(width = 26.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < d.bars) color else Sweep.Card2))
                                        }
                                    }
                                }
                                Text(stringResource(R.string.bt_distance, formatMeters(d.distanceMeters)), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
