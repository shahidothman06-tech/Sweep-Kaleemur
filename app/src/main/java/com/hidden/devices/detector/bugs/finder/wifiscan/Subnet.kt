package com.hidden.devices.detector.bugs.finder.wifiscan

object Subnet {

    /**
     * All host addresses on the phone's IPv4 subnet, except the phone itself.
     * Networks bigger than /24 are cut down to the phone's own /24 (254 hosts) so a scan stays fast.
     */
    fun hostsToScan(localIp: String, prefixLength: Int): List<String> {
        val ip = toInt(localIp)
        val prefix = prefixLength.coerceIn(24, 30)
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        val network = ip and mask
        val broadcast = network or mask.inv()
        return ((network + 1) until broadcast)
            .filter { it != ip }
            .map { fromInt(it) }
    }

    fun toInt(ip: String): Int {
        val parts = ip.trim().split(".")
        require(parts.size == 4) { "Not an IPv4 address: $ip" }
        return parts.fold(0) { acc, p ->
            val n = p.toInt()
            require(n in 0..255) { "Not an IPv4 address: $ip" }
            (acc shl 8) or n
        }
    }

    fun fromInt(v: Int): String =
        "${(v ushr 24) and 255}.${(v ushr 16) and 255}.${(v ushr 8) and 255}.${v and 255}"
}
