package com.hidden.devices.detector.bugs.finder.scan

import com.hidden.devices.detector.bugs.finder.bluetooth.BleDevice
import com.hidden.devices.detector.bugs.finder.bluetooth.BleKind
import com.hidden.devices.detector.bugs.finder.wifiscan.Risk
import com.hidden.devices.detector.bugs.finder.wifiscan.ScanResult

enum class FindingSource { WIFI, BLUETOOTH, MAGNET }

/** One row on the results screen. */
data class Finding(
    val id: String,
    val source: FindingSource,
    val risk: Risk,
    val wifi: ScanResult? = null,
    val ble: BleDevice? = null,
    /** For magnet findings: µT above the room's normal level. */
    val magnetDelta: Int? = null,
)

object Findings {
    fun fromWifi(r: ScanResult) = Finding("wifi:" + r.device.ip, FindingSource.WIFI, r.risk, wifi = r)

    fun fromBle(d: BleDevice): Finding {
        val risk = when {
            d.isTracker && d.minutesSeen >= 10 -> Risk.HIGH
            d.isTracker -> Risk.MEDIUM
            d.kind == BleKind.UNNAMED && d.rssi > -60 -> Risk.MEDIUM
            else -> Risk.LOW
        }
        return Finding("ble:" + d.address, FindingSource.BLUETOOTH, risk, ble = d)
    }

    fun fromMagnet(delta: Int): Finding? =
        if (delta >= 50) Finding("magnet", FindingSource.MAGNET, Risk.MEDIUM, magnetDelta = delta) else null

    fun sort(list: List<Finding>) = list.sortedWith(compareBy<Finding> { it.risk.ordinal }.thenBy { it.source.ordinal })
}
