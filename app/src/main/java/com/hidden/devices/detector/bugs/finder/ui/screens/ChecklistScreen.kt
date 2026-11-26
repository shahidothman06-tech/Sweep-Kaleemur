package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.scan.Report
import com.hidden.devices.detector.bugs.finder.scan.ReportType
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.Segmented
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.launch

data class Spot(val key: String, @StringRes val title: Int, @StringRes val hint: Int)

private val S = mapOf(
    "smoke" to Spot("smoke", R.string.chk_smoke, R.string.chk_smoke_hint),
    "clock" to Spot("clock", R.string.chk_clock, R.string.chk_clock_hint),
    "chargers" to Spot("chargers", R.string.chk_chargers, R.string.chk_chargers_hint),
    "tv" to Spot("tv", R.string.chk_tv, R.string.chk_tv_hint),
    "mirror" to Spot("mirror", R.string.chk_mirror, R.string.chk_mirror_hint),
    "vents" to Spot("vents", R.string.chk_vents, R.string.chk_vents_hint),
    "frames" to Spot("frames", R.string.chk_frames, R.string.chk_frames_hint),
    "shower" to Spot("shower", R.string.chk_shower, R.string.chk_shower_hint),
    "hooks" to Spot("hooks", R.string.chk_hooks, R.string.chk_hooks_hint),
    "speaker" to Spot("speaker", R.string.chk_speaker, R.string.chk_speaker_hint),
    "plants" to Spot("plants", R.string.chk_plants, R.string.chk_plants_hint),
    "router" to Spot("router", R.string.chk_router, R.string.chk_router_hint),
)

/** Rooms and the spots to check in each. Keys are stored as "room/spot". */
val ROOMS: List<Pair<String, List<Spot>>> = listOf(
    "bedroom" to listOf("smoke", "clock", "chargers", "tv", "mirror", "vents", "frames").map { S.getValue(it) },
    "bathroom" to listOf("smoke", "vents", "mirror", "shower", "hooks").map { S.getValue(it) },
    "living" to listOf("smoke", "tv", "speaker", "plants", "frames", "router").map { S.getValue(it) },
)

@Composable
fun ChecklistScreen(vm: AppViewModel, snackbar: SnackbarHostState) {
    val checked by vm.checked.collectAsStateWithLifecycle()
    var room by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.checklist_saved)
    val (roomKey, spots) = ROOMS[room]
    val done = spots.count { "$roomKey/${it.key}" in checked }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.checklist_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Pill("$done / ${spots.size}", Sweep.Green)
        }
        LinearProgressIndicator(
            progress = { done / spots.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Sweep.Green, trackColor = Sweep.Card, drawStopIndicator = {},
        )
        Segmented(
            listOf(stringResource(R.string.room_bedroom), stringResource(R.string.room_bathroom), stringResource(R.string.room_living)),
            room, { room = it }, Modifier.padding(vertical = 14.dp),
        )
        SweepCard(padding = 4.dp) {
            spots.forEachIndexed { i, spot ->
                val key = "$roomKey/${spot.key}"
                val on = key in checked
                Row(
                    Modifier.fillMaxWidth().clickable { vm.toggleSpot(key) }.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (on) Sweep.Green else Sweep.Card)
                            .border(2.dp, if (on) Sweep.Green else Sweep.Line, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Rounded.Check, null, tint = Sweep.Bg, modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(spot.title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(spot.hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (i < spots.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(Sweep.Line))
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.checklist_save), {
            vm.saveReport(Report(System.currentTimeMillis(), ReportType.CHECKLIST, checked = done, total = spots.size, room = roomKey))
            scope.launch { snackbar.showSnackbar(savedMsg) }
        })
        Spacer(Modifier.height(16.dp))
    }
}
