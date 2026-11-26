package com.hidden.devices.detector.bugs.finder.wifiscan

/** Turns what we found about a device into a risk level plus reasons. */
class RiskScorer(private val portTable: List<PortSignature> = CameraSignatures.ports) {

    fun score(device: Device): ScanResult {
        if (device.isThisPhone) return ScanResult(device, Risk.LOW, listOf(Reason(ReasonCode.THIS_PHONE)))

        val reasons = mutableListOf<Pair<Int, Reason>>()

        device.onvif?.let { reasons += 10 to Reason(ReasonCode.ONVIF_CAMERA) }

        val texts = listOfNotNull(
            device.onvif?.manufacturer, device.onvif?.model, device.onvif?.friendlyName,
            device.ssdp?.manufacturer, device.ssdp?.model, device.ssdp?.friendlyName, device.ssdp?.server,
            device.hostname,
        ) + device.openPorts.mapNotNull { it.banner }

        texts.firstNotNullOfOrNull { CameraSignatures.strongKeywordIn(it) }?.let {
            reasons += 5 to Reason(ReasonCode.CAMERA_NAME, it)
        } ?: texts.firstNotNullOfOrNull { CameraSignatures.weakKeywordIn(it) }?.let {
            reasons += 2 to Reason(ReasonCode.SMART_HOME, it)
        }

        device.openPorts
            .mapNotNull { CameraSignatures.signatureFor(it.port, portTable) }
            .filter { it.weight > 0 && it.reason != null }
            .distinctBy { it.reason }
            .forEach { reasons += it.weight to Reason(it.reason!!) }

        val total = reasons.sumOf { it.first }
        val risk = when {
            total >= 3 -> Risk.HIGH
            total >= 2 -> Risk.MEDIUM
            else -> Risk.LOW
        }
        val list = reasons.sortedByDescending { it.first }.map { it.second }.ifEmpty {
            listOf(Reason(if (device.isGateway) ReasonCode.ROUTER else ReasonCode.NO_SIGNS))
        }
        return ScanResult(device, risk, list)
    }
}
