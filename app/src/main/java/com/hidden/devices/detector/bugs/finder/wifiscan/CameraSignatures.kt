package com.hidden.devices.detector.bugs.finder.wifiscan

/**
 * A TCP port worth checking, and what it means if it's open.
 * [weight]: 3 = strong camera sign, 2 = medium, 1 = weak, 0 = just "device is alive".
 */
data class PortSignature(val port: Int, val reason: ReasonCode?, val weight: Int)

object CameraSignatures {

    val ports: List<PortSignature> = listOf(
        PortSignature(554, ReasonCode.PORT_RTSP, 3),
        PortSignature(8554, ReasonCode.PORT_RTSP, 3),
        PortSignature(37777, ReasonCode.PORT_DAHUA, 3),
        PortSignature(34567, ReasonCode.PORT_XMEYE, 3),
        PortSignature(8000, ReasonCode.PORT_HIKVISION, 2),
        PortSignature(1935, ReasonCode.PORT_RTMP, 2),
        PortSignature(6668, ReasonCode.PORT_TUYA, 2),
        PortSignature(8899, ReasonCode.PORT_ONVIF, 1),
        PortSignature(80, null, 0),
        PortSignature(8080, null, 0),
        PortSignature(443, null, 0),
    )

    /** Words that point to a camera brand or camera software, matched case-insensitively. */
    val strongKeywords: List<String> = listOf(
        "hikvision", "dahua", "ezviz", "imou", "wyze", "reolink", "tapo c", "eufycam", "eufy security",
        "arlo", "blink", "foscam", "amcrest", "lorex", "swann", "annke", "uniview", "axis",
        "vivotek", "hanwha", "wisenet", "v380", "icsee", "xmeye", "yoosee", "vstarcam", "wansview",
        "sricam", "zmodo", "jooan", "chuangmi", "mijia camera", "yi camera", "kami", "ip camera",
        "ipcam", "ip cam", "netcam", "network camera", "webcam", "dvr", "nvr", "camera", "spycam",
        "boa/", "hipcam", "gm8136", "goahead",
    )

    /** Words that only hint at a smart device that *might* have a camera. */
    val weakKeywords: List<String> = listOf("tuya", "smart life", "espressif", "esp32", "realtek", "ring", "nest")

    fun signatureFor(port: Int, table: List<PortSignature> = ports): PortSignature? =
        table.firstOrNull { it.port == port }

    fun strongKeywordIn(text: String?): String? = text?.lowercase()?.let { t ->
        strongKeywords.firstOrNull { k -> containsWord(t, k) }
    }

    fun weakKeywordIn(text: String?): String? = text?.lowercase()?.let { t ->
        weakKeywords.firstOrNull { k -> containsWord(t, k) }
    }

    /** Match keyword only at word boundaries, so "ring" doesn't match "string". */
    private fun containsWord(text: String, keyword: String): Boolean {
        var from = 0
        while (true) {
            val i = text.indexOf(keyword, from)
            if (i < 0) return false
            val beforeOk = i == 0 || !text[i - 1].isLetterOrDigit()
            val end = i + keyword.length
            val afterOk = end >= text.length || !text[end].isLetterOrDigit() || !keyword.last().isLetterOrDigit()
            if (beforeOk && afterOk) return true
            from = i + 1
        }
    }
}
