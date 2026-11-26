package com.hidden.devices.detector.bugs.finder.wifiscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** Checks which ports are open on one host, and grabs a short banner from web/RTSP services. */
open class HostProber(
    private val connectTimeoutMs: Int = 400,
    private val readTimeoutMs: Int = 800,
) {
    open suspend fun openPorts(ip: String, ports: List<Int>): List<OpenPort> = withContext(Dispatchers.IO) {
        ports.mapNotNull { port ->
            runCatching {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, port), connectTimeoutMs)
                    s.soTimeout = readTimeoutMs
                    OpenPort(port, grabBanner(s, ip, port))
                }
            }.getOrNull()
        }
    }

    open suspend fun hostname(ip: String): String? = withContext(Dispatchers.IO) {
        runCatching { InetAddress.getByName(ip).canonicalHostName }.getOrNull()?.takeIf { it != ip }
    }

    private fun grabBanner(s: Socket, ip: String, port: Int): String? = runCatching {
        val request = when (port) {
            554, 8554 -> "OPTIONS rtsp://$ip:$port/ RTSP/1.0\r\nCSeq: 1\r\n\r\n"
            80, 8080, 8000, 8899 -> "GET / HTTP/1.0\r\nHost: $ip\r\nConnection: close\r\n\r\n"
            else -> return null
        }
        s.getOutputStream().apply { write(request.toByteArray()); flush() }
        val buf = ByteArray(4096)
        var len = 0
        val input = s.getInputStream()
        while (len < buf.size) {
            val n = runCatching { input.read(buf, len, buf.size - len) }.getOrDefault(-1)
            if (n <= 0) break
            len += n
        }
        BannerParser.summarize(String(buf, 0, len, Charsets.ISO_8859_1))
    }.getOrNull()
}

object BannerParser {
    private val headerRegex = Regex("""(?im)^(server|www-authenticate):\s*(.+?)\s*$""")
    private val titleRegex = Regex("""(?is)<title[^>]*>\s*(.*?)\s*</title>""")

    /** Pulls the Server header, auth realm and HTML title out of a response, joined with " | ". */
    fun summarize(response: String): String? {
        if (response.isBlank()) return null
        val parts = headerRegex.findAll(response).map { it.groupValues[2] }.toMutableList()
        titleRegex.find(response)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { parts += it }
        return parts.distinct().joinToString(" | ").take(200).ifBlank { null }
    }
}
