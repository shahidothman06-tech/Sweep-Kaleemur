package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.scan.FindingSource
import com.hidden.devices.detector.bugs.finder.ui.components.InfoRow
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.findingReasons
import com.hidden.devices.detector.bugs.finder.ui.findingTitle
import com.hidden.devices.detector.bugs.finder.ui.formatMeters
import com.hidden.devices.detector.bugs.finder.ui.riskLabel
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

@Composable
fun DeviceScreen(vm: AppViewModel, id: String, onBack: () -> Unit, onLens: () -> Unit) {
    val f = vm.finding(id)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.detail_title), onBack = onBack)
        if (f == null) return@Column
        val color = Sweep.risk(f.risk)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (f.source == FindingSource.BLUETOOTH) Icons.Rounded.Bluetooth else Icons.Rounded.Videocam, null, tint = color, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(findingTitle(f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Pill(riskLabel(f.risk), color, dot = true)
        }
        Spacer(Modifier.height(20.dp))
        SweepCard(padding = 16.dp) {
            val none = stringResource(R.string.detail_none)
            when (f.source) {
                FindingSource.WIFI -> {
                    val d = f.wifi!!.device
                    val maker = d.onvif?.manufacturer ?: d.ssdp?.manufacturer
                    val model = d.onvif?.model ?: d.ssdp?.model
                    InfoRow(stringResource(R.string.detail_ip), d.ip)
                    InfoRow(stringResource(R.string.detail_maker), maker ?: stringResource(R.string.detail_unknown))
                    if (model != null) InfoRow(stringResource(R.string.detail_model), model)
                    InfoRow(stringResource(R.string.detail_ports), d.openPorts.joinToString(", ") { it.port.toString() }.ifEmpty { none })
                    val by = buildList {
                        if (d.openPorts.isNotEmpty()) add(stringResource(R.string.found_by_ports))
                        if (d.ssdp != null) add(stringResource(R.string.found_by_upnp))
                        if (d.onvif != null) add(stringResource(R.string.found_by_onvif))
                    }
                    InfoRow(stringResource(R.string.detail_found_by), by.joinToString(", ").ifEmpty { none }, divider = false)
                }
                FindingSource.BLUETOOTH -> {
                    val d = f.ble!!
                    InfoRow(stringResource(R.string.detail_signal), "${d.rssi} dBm · " + stringResource(R.string.bt_distance, formatMeters(d.distanceMeters)))
                    InfoRow("MAC", d.address, divider = false)
                }
                FindingSource.MAGNET -> Unit
            }
        }
        val reasons = findingReasons(f)
        if (reasons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SweepCard { reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) } }
        }
        Spacer(Modifier.height(12.dp))
        SweepCard {
            Text(stringResource(R.string.detail_what_to_do), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.detail_steps), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.detail_open_lens), onLens)
        Spacer(Modifier.height(16.dp))
    }
}
