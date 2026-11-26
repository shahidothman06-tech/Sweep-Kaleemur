package com.hidden.devices.detector.bugs.finder.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hidden.devices.detector.bugs.finder.billing.BillingManager
import com.hidden.devices.detector.bugs.finder.bluetooth.BleDevice
import com.hidden.devices.detector.bugs.finder.bluetooth.BleScanner
import com.hidden.devices.detector.bugs.finder.magnet.MagnetSensor
import com.hidden.devices.detector.bugs.finder.wifiscan.NetworkInfoProvider
import com.hidden.devices.detector.bugs.finder.wifiscan.NetworkScanner
import com.hidden.devices.detector.bugs.finder.wifiscan.Risk
import com.hidden.devices.detector.bugs.finder.wifiscan.ScanEvent
import com.hidden.devices.detector.bugs.finder.wifiscan.ScanResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Status of one part of the full scan, shown as a pill on the scanning screen. */
sealed interface StepStatus {
    data object Waiting : StepStatus
    data object Running : StepStatus
    data class Done(val count: Int) : StepStatus
    data object Skipped : StepStatus
}

data class FullScanState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val wifiProgress: Float = 0f,
    val btProgress: Float = 0f,
    val magnetProgress: Float = 0f,
    val wifi: StepStatus = StepStatus.Waiting,
    val bt: StepStatus = StepStatus.Waiting,
    val magnet: StepStatus = StepStatus.Waiting,
    val magnetPeak: Int = 0,
    val findings: List<Finding> = emptyList(),
    val lastScanTime: Long? = null,
) {
    val progress: Float get() = (wifiProgress + btProgress + magnetProgress) / 3f
    fun count(r: Risk) = findings.count { it.risk == r }
}

/** Shared state for the whole app: full scan, results, reports, checklist and Pro status. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val billing = BillingManager(app, prefs).also { it.connect() }

    private val net = NetworkInfoProvider(app)
    private val wifiScanner = NetworkScanner()
    val ble = BleScanner(app)
    val magnetSensor = MagnetSensor(app)

    private val _scan = MutableStateFlow(FullScanState(lastScanTime = prefs.reports().firstOrNull { it.type == ReportType.SCAN }?.time))
    val scan: StateFlow<FullScanState> = _scan.asStateFlow()

    private val _reports = MutableStateFlow(prefs.reports())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _checked = MutableStateFlow(prefs.checkedSpots)
    val checked: StateFlow<Set<String>> = _checked.asStateFlow()

    /** True when the rate-us prompt should show (after the 2nd finished scan, once). */
    private val _askRating = MutableStateFlow(false)
    val askRating: StateFlow<Boolean> = _askRating.asStateFlow()

    private var job: Job? = null

    fun startFullScan() {
        if (job?.isActive == true) return
        _scan.value = FullScanState(running = true, lastScanTime = _scan.value.lastScanTime)
        job = viewModelScope.launch {
            var wifiResults: List<ScanResult> = emptyList()
            var bleDevices: List<BleDevice> = emptyList()
            var baseline = 0f
            var peak = 0f
            coroutineScope {
                launch { wifiResults = runWifi() }
                launch { bleDevices = runBluetooth() }
                launch {
                    val r = runMagnet()
                    baseline = r.first
                    peak = r.second
                }
            }
            val findings = buildList {
                addAll(wifiResults.map(Findings::fromWifi))
                addAll(bleDevices.map(Findings::fromBle))
                Findings.fromMagnet((peak - baseline).toInt())?.let { add(it) }
            }.let(Findings::sort)
            val now = System.currentTimeMillis()
            _scan.update { it.copy(running = false, finished = true, findings = findings, lastScanTime = now) }
            saveReport(
                Report(
                    time = now, type = ReportType.SCAN,
                    high = findings.count { it.risk == Risk.HIGH },
                    medium = findings.count { it.risk == Risk.MEDIUM },
                    low = findings.count { it.risk == Risk.LOW },
                ),
            )
            prefs.completedScans += 1
            if (prefs.completedScans >= 2 && !prefs.rateAnswered) _askRating.value = true
        }
    }

    fun cancelScan() {
        job?.cancel()
        _scan.update { it.copy(running = false) }
    }

    private suspend fun runWifi(): List<ScanResult> {
        val info = net.current()
        if (info == null) {
            _scan.update { it.copy(wifi = StepStatus.Skipped, wifiProgress = 1f) }
            return emptyList()
        }
        _scan.update { it.copy(wifi = StepStatus.Running) }
        val lock = net.acquireMulticastLock()
        var results: List<ScanResult> = emptyList()
        try {
            wifiScanner.scan(info.localIp, info.prefixLength, info.gatewayIp).collect { e ->
                when (e) {
                    is ScanEvent.Progress -> _scan.update {
                        it.copy(wifiProgress = maxOf(it.wifiProgress, if (e.total == 0) 1f else e.checked.toFloat() / e.total))
                    }
                    is ScanEvent.Finished -> results = e.results
                }
            }
        } finally {
            lock.release()
        }
        _scan.update { it.copy(wifi = StepStatus.Done(results.size), wifiProgress = 1f) }
        return results
    }

    private suspend fun runBluetooth(): List<BleDevice> {
        if (!ble.isSupported || !ble.isEnabled || !ble.hasPermission()) {
            _scan.update { it.copy(bt = StepStatus.Skipped, btProgress = 1f) }
            return emptyList()
        }
        _scan.update { it.copy(bt = StepStatus.Running) }
        val seconds = 10
        var last: List<BleDevice> = emptyList()
        val start = System.currentTimeMillis()
        withTimeoutOrNull(seconds * 1000L) {
            ble.scan().collect { list ->
                last = list
                val p = (System.currentTimeMillis() - start) / (seconds * 1000f)
                _scan.update { it.copy(btProgress = p.coerceAtMost(0.99f)) }
            }
        }
        val flagged = last.count { Findings.fromBle(it).risk != Risk.LOW }
        _scan.update { it.copy(bt = StepStatus.Done(flagged), btProgress = 1f) }
        return last
    }

    /** Samples the magnetometer for a few seconds. Returns (baseline, peak) in µT. */
    private suspend fun runMagnet(): Pair<Float, Float> {
        if (!magnetSensor.isAvailable) {
            _scan.update { it.copy(magnet = StepStatus.Skipped, magnetProgress = 1f) }
            return 0f to 0f
        }
        _scan.update { it.copy(magnet = StepStatus.Running) }
        val samples = mutableListOf<Float>()
        val start = System.currentTimeMillis()
        val durationMs = 6000L
        magnetSensor.readings().takeWhile { System.currentTimeMillis() - start < durationMs }.collect { v ->
            samples += v
            _scan.update { it.copy(magnetProgress = ((System.currentTimeMillis() - start) / durationMs.toFloat()).coerceAtMost(0.99f)) }
        }
        val sorted = samples.sorted()
        val baseline = if (sorted.isEmpty()) 0f else sorted[sorted.size / 4] // lower quartile = quiet room level
        val peak = sorted.lastOrNull() ?: 0f
        _scan.update { it.copy(magnet = StepStatus.Done(peak.toInt()), magnetPeak = peak.toInt(), magnetProgress = 1f) }
        return baseline to peak
    }

    fun finding(id: String): Finding? = _scan.value.findings.firstOrNull { it.id == id }

    fun saveReport(r: Report) {
        val keep = if (billing.isPro.value) 500 else 3
        prefs.addReport(r, keep)
        _reports.value = prefs.reports()
    }

    fun clearReports() {
        prefs.clearReports()
        _reports.value = emptyList()
    }

    fun toggleSpot(key: String) {
        val now = _checked.value.toMutableSet().apply { if (!add(key)) remove(key) }
        _checked.value = now
        prefs.checkedSpots = now
    }

    fun ratingAnswered() {
        prefs.rateAnswered = true
        _askRating.value = false
    }

    fun ratingLater() {
        _askRating.value = false
    }
}
