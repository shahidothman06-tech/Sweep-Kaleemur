package com.hidden.devices.detector.bugs.finder.wifiscan

/** How likely a device on the network is a camera. */
enum class Risk { HIGH, MEDIUM, LOW }

/** How we learned about a device. */
enum class Source { PORT_SCAN, SSDP, ONVIF }

data class OpenPort(
    val port: Int,
    /** Short text the service sent back, e.g. an HTTP "Server" header or page title. */
    val banner: String? = null,
)

/** Info a device announced about itself over UPnP (SSDP) or ONVIF. */
data class Announcement(
    val manufacturer: String? = null,
    val model: String? = null,
    val friendlyName: String? = null,
    val server: String? = null,
)

data class Device(
    val ip: String,
    val hostname: String? = null,
    val openPorts: List<OpenPort> = emptyList(),
    val ssdp: Announcement? = null,
    val onvif: Announcement? = null,
    val isGateway: Boolean = false,
    val isThisPhone: Boolean = false,
) {
    val sources: Set<Source>
        get() = buildSet {
            if (openPorts.isNotEmpty()) add(Source.PORT_SCAN)
            if (ssdp != null) add(Source.SSDP)
            if (onvif != null) add(Source.ONVIF)
        }

    /** Best human name we have for the device. */
    val displayName: String
        get() = listOfNotNull(
            onvif?.friendlyName, ssdp?.friendlyName,
            listOfNotNull(onvif?.manufacturer ?: ssdp?.manufacturer, onvif?.model ?: ssdp?.model)
                .joinToString(" ").ifBlank { null },
            hostname?.takeIf { it != ip },
        ).firstOrNull() ?: when {
            isGateway -> "Wi-Fi router"
            isThisPhone -> "This phone"
            else -> "Unknown device"
        }
}

/** Why a device got its risk level. The app turns these into translated text. */
enum class ReasonCode {
    ONVIF_CAMERA, CAMERA_NAME, SMART_HOME,
    PORT_RTSP, PORT_DAHUA, PORT_XMEYE, PORT_HIKVISION, PORT_RTMP, PORT_TUYA, PORT_ONVIF,
    THIS_PHONE, ROUTER, NO_SIGNS,
}

data class Reason(val code: ReasonCode, val arg: String? = null)

data class ScanResult(
    val device: Device,
    val risk: Risk,
    /** Reasons, strongest first. */
    val reasons: List<Reason>,
)

sealed interface ScanEvent {
    data class Progress(val checked: Int, val total: Int) : ScanEvent
    data class Finished(val results: List<ScanResult>) : ScanEvent
}
