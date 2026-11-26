package com.hidden.devices.detector.bugs.finder.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.scan.ReportType
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

@Composable
fun ReportsScreen(vm: AppViewModel, onPro: () -> Unit) {
    val reports by vm.reports.collectAsStateWithLifecycle()
    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (reports.isNotEmpty()) TextButton(onClick = vm::clearReports) { Text(stringResource(R.string.reports_clear), color = Sweep.Dim) }
        }
        if (!isPro) {
            SweepCard(onClick = onPro, border = Sweep.Accent.copy(alpha = 0.4f)) {
                Text(stringResource(R.string.reports_free_limit), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
        }
        if (reports.isEmpty()) {
            Text(stringResource(R.string.reports_empty), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(reports, key = { it.time }) { r ->
                SweepCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(if (r.type == ReportType.SCAN) R.string.report_scan else R.string.report_checklist),
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        )
                        Text(DateUtils.getRelativeTimeSpanString(r.time).toString(), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (r.type == ReportType.SCAN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(stringResource(R.string.pill_high, r.high), Sweep.Red)
                            Pill(stringResource(R.string.pill_check, r.medium), Sweep.Amber)
                            Pill(stringResource(R.string.pill_normal, r.low), Sweep.Green)
                        }
                    } else {
                        val roomName = when (r.room) {
                            "bathroom" -> stringResource(R.string.room_bathroom)
                            "living" -> stringResource(R.string.room_living)
                            else -> stringResource(R.string.room_bedroom)
                        }
                        Text(roomName + " · " + stringResource(R.string.report_checklist_summary, r.checked, r.total), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
