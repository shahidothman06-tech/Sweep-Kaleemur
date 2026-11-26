package com.hidden.devices.detector.bugs.finder.wifiscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.util.UUID

/**
 * Asks "are there any network cameras here?" using ONVIF WS-Discovery.
 * Almost every IP camera from a real brand answers this. A reply is a very strong camera sign.
 */
open class OnvifDiscovery(private val listenMs: Long = 3000) {

    open suspend fun discover(): Map<String, Announcement> = withContext(Dispatchers.IO) {
        val found = mutableMapOf<String, Announcement>()
        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = 300
                val msg = probeMessage().toByteArray()
                val group = InetAddress.getByName("239.255.255.250")
                repeat(2) { socket.send(DatagramPacket(msg, msg.size, group, 3702)) }
                val end = System.currentTimeMillis() + listenMs
                val buf = ByteArray(8192)
                while (System.currentTimeMillis() < end) {
                    val p = DatagramPacket(buf, buf.size)
                    try { socket.receive(p) } catch (_: SocketTimeoutException) { continue }
                    val ip = p.address.hostAddress ?: continue
                    val body = String(p.data, 0, p.length, Charsets.UTF_8)
                    if (body.contains("ProbeMatch", ignoreCase = true)) found[ip] = parseScopes(body)
                }
            }
        }
        found
    }

    companion object {
        fun probeMessage(): String = """<?xml version="1.0" encoding="UTF-8"?>
<e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope" xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery" xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
<e:Header><w:MessageID>uuid:${UUID.randomUUID()}</w:MessageID><w:To e:mustUnderstand="true">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To><w:Action e:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action></e:Header>
<e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body>
</e:Envelope>"""

        /** Reads name and hardware from ONVIF scopes like onvif://www.onvif.org/name/Hikvision%20Cam */
        fun parseScopes(body: String): Announcement {
            val scopes = Regex("""<[\w:]*Scopes[^>]*>(.*?)</[\w:]*Scopes>""", RegexOption.DOT_MATCHES_ALL)
                .find(body)?.groupValues?.get(1).orEmpty().split(Regex("\\s+"))
            fun scope(key: String) = scopes.firstOrNull { it.contains("onvif.org/$key/", ignoreCase = true) }
                ?.substringAfter("onvif.org/$key/")
                ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
                ?.takeIf { it.isNotBlank() }
            return Announcement(
                manufacturer = scope("mfr") ?: scope("manufacturer"),
                model = scope("hardware"),
                friendlyName = scope("name"),
            )
        }
    }
}
