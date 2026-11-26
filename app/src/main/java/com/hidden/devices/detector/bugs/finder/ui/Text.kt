package com.hidden.devices.detector.bugs.finder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.bluetooth.BleKind
import com.hidden.devices.detector.bugs.finder.scan.Finding
import com.hidden.devices.detector.bugs.finder.scan.FindingSource
import com.hidden.devices.detector.bugs.finder.wifiscan.Reason
import com.hidden.devices.detector.bugs.finder.wifiscan.ReasonCode
import com.hidden.devices.detector.bugs.finder.wifiscan.Risk
import java.util.Locale

/** Translated text helpers for scan results. */

@Composable
fun reasonText(r: Reason): String = when (r.code) {
    ReasonCode.ONVIF_CAMERA -> stringResource(R.string.reason_onvif)
    ReasonCode.CAMERA_NAME -> stringResource(R.string.reason_camera_name, r.arg.orEmpty())
    ReasonCode.SMART_HOME -> stringResource(R.string.reason_smart_home, r.arg.orEmpty())
    ReasonCode.PORT_RTSP -> stringResource(R.string.reason_rtsp)
    ReasonCode.PORT_DAHUA -> stringResource(R.string.reason_dahua)
    ReasonCode.PORT_XMEYE -> stringResource(R.string.reason_xmeye)
    ReasonCode.PORT_HIKVISION -> stringResource(R.string.reason_hikvision)
    ReasonCode.PORT_RTMP -> stringResource(R.string.reason_rtmp)
    ReasonCode.PORT_TUYA -> stringResource(R.string.reason_tuya)
    ReasonCode.PORT_ONVIF -> stringResource(R.string.reason_onvif_port)
    ReasonCode.THIS_PHONE -> stringResource(R.string.reason_this_phone)
    ReasonCode.ROUTER -> stringResource(R.string.reason_router)
    ReasonCode.NO_SIGNS -> stringResource(R.string.reason_no_signs)
}

@Composable
fun riskLabel(r: Risk): String = when (r) {
    Risk.HIGH -> stringResource(R.string.risk_high)
    Risk.MEDIUM -> stringResource(R.string.risk_medium)
    Risk.LOW -> stringResource(R.string.risk_low)
}

@Composable
fun findingTitle(f: Finding): String = when (f.source) {
    FindingSource.WIFI -> {
        val d = f.wifi!!.device
        val name = d.displayName
        when (name) {
            "Wi-Fi router" -> stringResource(R.string.wifi_router)
            "Unknown device", "This phone" -> stringResource(R.string.unknown_device)
            else -> name
        }
    }
    FindingSource.BLUETOOTH -> {
        val d = f.ble!!
        when (d.kind) {
            BleKind.APPLE_FIND_MY -> stringResource(R.string.bt_apple)
            BleKind.TILE -> stringResource(R.string.bt_tile)
            BleKind.SMARTTAG -> stringResource(R.string.bt_smarttag)
            BleKind.UNNAMED -> stringResource(R.string.bt_unnamed)
            BleKind.NAMED -> d.name ?: stringResource(R.string.bt_unnamed)
        }
    }
    FindingSource.MAGNET -> stringResource(R.string.strong_field)
}

@Composable
fun findingSubtitle(f: Finding): String = when (f.source) {
    FindingSource.WIFI -> f.wifi!!.device.ip + " · " + stringResource(R.string.source_wifi)
    FindingSource.BLUETOOTH -> stringResource(R.string.bt_distance, formatMeters(f.ble!!.distanceMeters)) + " · " + stringResource(R.string.source_bt)
    FindingSource.MAGNET -> stringResource(R.string.source_magnet)
}

@Composable
fun findingReasons(f: Finding): List<String> = when (f.source) {
    FindingSource.WIFI -> f.wifi!!.reasons.map { reasonText(it) }
    FindingSource.BLUETOOTH -> {
        val d = f.ble!!
        buildList {
            if (d.isTracker) add(stringResource(R.string.reason_tracker))
            if (d.isTracker && d.minutesSeen >= 1) add(stringResource(R.string.reason_seen_long, d.minutesSeen))
            if (d.kind == BleKind.UNNAMED && d.rssi > -60) add(stringResource(R.string.reason_hidden_name))
        }
    }
    FindingSource.MAGNET -> listOf(stringResource(R.string.reason_strong_field, f.magnetDelta ?: 0))
}

fun formatMeters(m: Double): String =
    if (m < 10) String.format(Locale.getDefault(), "%.1f", m) else m.toInt().toString()
