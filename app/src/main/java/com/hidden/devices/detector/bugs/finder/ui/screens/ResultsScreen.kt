package com.hidden.devices.detector.bugs.finder.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.scan.Finding
import com.hidden.devices.detector.bugs.finder.scan.FindingSource
import com.hidden.devices.detector.bugs.finder.ui.components.Dot
import com.hidden.devices.detector.bugs.finder.ui.components.IconSquare
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.Segmented
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.findingReasons
import com.hidden.devices.detector.bugs.finder.ui.findingSubtitle
import com.hidden.devices.detector.bugs.finder.ui.findingTitle
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import com.hidden.devices.detector.bugs.finder.wifiscan.Risk

@Composable
fun ResultsScreen(vm: AppViewModel, onBack: () -> Unit, onOpen: (Finding) -> Unit, onStartScan: () -> Unit, onPro: () -> Unit) {
    val s by vm.scan.collectAsStateWithLifecycle()
    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by remember { mutableIntStateOf(0) }
    val high = s.count(Risk.HIGH)
    val medium = s.count(Risk.MEDIUM)
    val low = s.count(Risk.LOW)
    val shareText = stringResource(R.string.results_share_text, high, medium, low)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.results_title), onBack = onBack) {
            if (s.finished) {
                IconSquare(Icons.Rounded.IosShare, stringResource(R.string.results_share), onClick = {
                    if (!isPro) onPro() else {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        context.startActivity(Intent.createChooser(send, null))
                    }
                })
            }
        }
        if (!s.finished) {
            SweepCard {
                Text(stringResource(R.string.results_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.results_empty_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                PrimaryButton(stringResource(R.string.home_start), onStartScan)
            }
            return@Column
        }

        val (title, color) = when {
            high > 0 -> stringResource(R.string.results_cameras, high) to Sweep.Red
            medium > 0 -> stringResource(R.string.results_to_check, medium) to Sweep.Amber
            else -> stringResource(R.string.results_clear) to Sweep.Green
        }
        SweepCard(color = Color(0xFF1A1719).takeIf { high > 0 } ?: Sweep.Card, border = if (high > 0) Sweep.Red.copy(alpha = 0.3f) else null) {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(color = color))
            Text(
                stringResource(if (high + medium > 0) R.string.results_note_found else R.string.results_note_clear),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(stringResource(R.string.pill_high, high), Sweep.Red)
                Pill(stringResource(R.string.pill_check, medium), Sweep.Amber)
                Pill(stringResource(R.string.pill_normal, low), Sweep.Green)
            }
        }
        val wifi = s.findings.filter { it.source == FindingSource.WIFI }
        val bt = s.findings.filter { it.source == FindingSource.BLUETOOTH }
        Segmented(
            listOf(
                stringResource(R.string.filter_all, s.findings.size),
                stringResource(R.string.filter_wifi, wifi.size),
                stringResource(R.string.filter_bt, bt.size),
            ),
            filter, { filter = it }, Modifier.padding(vertical = 14.dp),
        )
        val shown = when (filter) { 1 -> wifi; 2 -> bt; else -> s.findings }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.id }) { f -> FindingRow(f, onClick = if (f.source == FindingSource.MAGNET) null else ({ onOpen(f) })) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun FindingRow(f: Finding, onClick: (() -> Unit)?) {
    SweepCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Dot(Sweep.risk(f.risk), 8.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(findingTitle(f), style = MaterialTheme.typography.titleMedium)
                Text(findingSubtitle(f), style = MaterialTheme.typography.bodySmall)
                val reasons = findingReasons(f)
                if (reasons.isNotEmpty() && f.risk != Risk.LOW) {
                    Spacer(Modifier.height(6.dp))
                    reasons.take(3).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Sweep.Dim, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}
