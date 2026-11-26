package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.ui.components.IconSquare
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val items = listOf(
        Triple(Icons.Rounded.Wifi, R.string.help_wifi_title, R.string.help_wifi_body),
        Triple(Icons.Rounded.CenterFocusStrong, R.string.help_lens_title, R.string.help_lens_body),
        Triple(Icons.Rounded.Bluetooth, R.string.help_bt_title, R.string.help_bt_body),
        Triple(Icons.Rounded.Sensors, R.string.help_magnet_title, R.string.help_magnet_body),
        Triple(Icons.Rounded.Info, R.string.help_limits_title, R.string.help_limits_body),
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.help_title), onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (icon, title, body) -> HelpCard(icon, stringResource(title), stringResource(body)) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpCard(icon: ImageVector, title: String, body: String) {
    SweepCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSquare(icon, null, tint = Sweep.Accent, background = Sweep.Card2)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Sweep.Dim)
    }
}
