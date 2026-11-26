package com.hidden.devices.detector.bugs.finder.wifiscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

/** Finds UPnP devices (many cameras, TVs and smart plugs) and reads the maker/model they announce. */
open class SsdpDiscovery(private val listenMs: Long = 3000) {

    open suspend fun discover(): Map<String, Announcement> = withContext(Dispatchers.IO) {
        val found = mutableMapOf<String, Pair<String?, String?>>() // ip -> (location, server)
        runCatching {
            DatagramSocket().use { socket ->
                socket.soTimeout = 300
                val msg = (
                    "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n"
                    ).toByteArray()
                val group = InetAddress.getByName("239.255.255.250")
                repeat(2) { socket.send(DatagramPacket(msg, msg.size, group, 1900)) }
                val end = System.currentTimeMillis() + listenMs
                val buf = ByteArray(2048)
                while (System.currentTimeMillis() < end) {
                    val p = DatagramPacket(buf, buf.size)
                    try { socket.receive(p) } catch (_: SocketTimeoutException) { continue }
                    val ip = p.address.hostAddress ?: continue
                    val headers = parseHeaders(String(p.data, 0, p.length, Charsets.UTF_8))
                    val prev = found[ip]
                    found[ip] = (prev?.first ?: headers["location"]) to (prev?.second ?: headers["server"])
                }
            }
        }
        found.mapValues { (_, v) ->
            val xml = v.first?.let { fetch(it) }
            parseDescription(xml).copy(server = v.second)
        }
    }

    protected open fun fetch(url: String): String? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 800; c.readTimeout = 1200
        // Reads at most 32 KB. (InputStream.readNBytes needs Android 13, and minSdk is 26.)
        c.inputStream.use { input ->
            val buf = ByteArray(32_000)
            var n = 0
            while (n < buf.size) {
                val r = input.read(buf, n, buf.size - n)
                if (r < 0) break
                n += r
            }
            String(buf, 0, n, Charsets.UTF_8)
        }.also { c.disconnect() }
    }.getOrNull()

    companion object {
        fun parseHeaders(response: String): Map<String, String> =
            response.lines().drop(1).mapNotNull { line ->
                val i = line.indexOf(':')
                if (i <= 0) null else line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim()
            }.toMap()

        fun parseDescription(xml: String?): Announcement {
            fun tag(name: String) = xml?.let {
                Regex("<$name>\\s*(.*?)\\s*</$name>", RegexOption.DOT_MATCHES_ALL).find(it)?.groupValues?.get(1)
            }?.takeIf { it.isNotBlank() }
            return Announcement(
                manufacturer = tag("manufacturer"),
                model = listOfNotNull(tag("modelName"), tag("modelNumber")).distinct().joinToString(" ").ifBlank { null },
                friendlyName = tag("friendlyName"),
            )
        }
    }
}
