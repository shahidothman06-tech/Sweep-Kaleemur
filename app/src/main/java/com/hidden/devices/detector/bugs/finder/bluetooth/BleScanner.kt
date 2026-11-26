package com.hidden.devices.detector.bugs.finder.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.math.pow

enum class BleKind { APPLE_FIND_MY, TILE, SMARTTAG, UNNAMED, NAMED }

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val kind: BleKind,
    val firstSeen: Long,
    val lastSeen: Long,
) {
    val isTracker get() = kind == BleKind.APPLE_FIND_MY || kind == BleKind.TILE || kind == BleKind.SMARTTAG

    /** Rough distance in meters from signal strength. Good enough for "warmer / colder". */
    val distanceMeters: Double get() = 10.0.pow((-59.0 - rssi) / 20.0)

    /** 1 to 4 signal bars. */
    val bars: Int get() = when {
        rssi > -55 -> 4
        rssi > -67 -> 3
        rssi > -80 -> 2
        else -> 1
    }

    val minutesSeen: Int get() = ((lastSeen - firstSeen) / 60_000).toInt()
}

/** Scans for nearby Bluetooth Low Energy devices and spots common item trackers. */
class BleScanner(context: Context) {
    private val app = context.applicationContext
    private val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter

    val isSupported: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    fun hasPermission(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Emits the current device list about twice a second until the collector stops. */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<List<BleDevice>> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null || !hasPermission() || !isEnabled) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val seen = LinkedHashMap<String, BleDevice>()
        val lock = Any()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val now = System.currentTimeMillis()
                val address = result.device.address ?: return
                synchronized(lock) {
                    val prev = seen[address]
                    seen[address] = BleDevice(
                        address = address,
                        name = result.scanRecord?.deviceName ?: prev?.name,
                        rssi = result.rssi,
                        kind = classify(result),
                        firstSeen = prev?.firstSeen ?: now,
                        lastSeen = now,
                    )
                }
            }
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(null, settings, callback)
        val ticker = launch {
            while (true) {
                delay(500)
                val now = System.currentTimeMillis()
                val list = synchronized(lock) {
                    // Forget devices not heard from for 30 seconds.
                    seen.values.removeAll { now - it.lastSeen > 30_000 }
                    seen.values.toList()
                }
                trySend(list.sortedByDescending { it.rssi })
            }
        }
        awaitClose {
            ticker.cancel()
            runCatching { scanner.stopScan(callback) }
        }
    }

    companion object {
        private val TILE_UUIDS = setOf(
            ParcelUuid.fromString("0000feed-0000-1000-8000-00805f9b34fb"),
            ParcelUuid.fromString("0000feec-0000-1000-8000-00805f9b34fb"),
        )
        private val SMARTTAG_UUID = ParcelUuid.fromString("0000fd5a-0000-1000-8000-00805f9b34fb")
        private const val APPLE_ID = 0x004C
        private const val FIND_MY_TYPE: Byte = 0x12

        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        fun classify(result: ScanResult): BleKind {
            val record = result.scanRecord ?: return BleKind.UNNAMED
            val apple = record.getManufacturerSpecificData(APPLE_ID)
            if (apple != null && apple.isNotEmpty() && apple[0] == FIND_MY_TYPE) return BleKind.APPLE_FIND_MY
            val uuids = record.serviceUuids.orEmpty()
            if (uuids.any { it in TILE_UUIDS }) return BleKind.TILE
            if (record.serviceData?.keys?.any { it == SMARTTAG_UUID } == true || SMARTTAG_UUID in uuids) return BleKind.SMARTTAG
            return if (record.deviceName.isNullOrBlank()) BleKind.UNNAMED else BleKind.NAMED
        }
    }
}
