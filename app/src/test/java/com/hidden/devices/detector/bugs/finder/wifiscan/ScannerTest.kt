package com.hidden.devices.detector.bugs.finder.wifiscan

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.concurrent.thread
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class SubnetTest {
    @Test fun `slash 24 gives 253 hosts without self`() {
        val hosts = Subnet.hostsToScan("192.168.1.37", 24)
        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertTrue("192.168.1.37" !in hosts)
    }

    @Test fun `big networks are cut to own slash 24`() {
        val hosts = Subnet.hostsToScan("10.0.5.9", 16)
        assertEquals(253, hosts.size)
        assertTrue(hosts.all { it.startsWith("10.0.5.") })
    }

    @Test fun `round trip`() = assertEquals("172.20.255.1", Subnet.fromInt(Subnet.toInt("172.20.255.1")))
}

class RiskScorerTest {
    private val scorer = RiskScorer()

    @Test fun `onvif reply is high`() =
        assertEquals(Risk.HIGH, scorer.score(Device("1.1.1.2", onvif = Announcement())).risk)

    @Test fun `rtsp port is high`() =
        assertEquals(Risk.HIGH, scorer.score(Device("1.1.1.2", openPorts = listOf(OpenPort(554)))).risk)

    @Test fun `camera brand in upnp is high`() {
        val r = scorer.score(Device("1.1.1.2", ssdp = Announcement(manufacturer = "Hikvision Digital")))
        assertEquals(Risk.HIGH, r.risk)
        assertEquals(Reason(ReasonCode.CAMERA_NAME, "hikvision"), r.reasons.first())
    }

    @Test fun `tuya plug is medium`() =
        assertEquals(Risk.MEDIUM, scorer.score(Device("1.1.1.2", openPorts = listOf(OpenPort(6668)))).risk)

    @Test fun `plain web server is low`() =
        assertEquals(Risk.LOW, scorer.score(Device("1.1.1.2", openPorts = listOf(OpenPort(80, "nginx")))).risk)

    @Test fun `ring does not match inside other words`() =
        assertEquals(Risk.LOW, scorer.score(Device("1.1.1.2", hostname = "printer-string.lan")).risk)

    @Test fun `samsung tv is low`() = assertEquals(
        Risk.LOW,
        scorer.score(Device("1.1.1.2", ssdp = Announcement(manufacturer = "Samsung Electronics", model = "UE55"))).risk,
    )
}

class ParserTest {
    @Test fun `ssdp description`() {
        val a = SsdpDiscovery.parseDescription(
            "<root><device><friendlyName>Living Room Cam</friendlyName><manufacturer>Reolink</manufacturer>" +
                "<modelName>E1 Pro</modelName></device></root>",
        )
        assertEquals("Reolink", a.manufacturer); assertEquals("E1 Pro", a.model); assertEquals("Living Room Cam", a.friendlyName)
    }

    @Test fun `ssdp headers`() {
        val h = SsdpDiscovery.parseHeaders("HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.9:49152/d.xml\r\nSERVER: Linux UPnP/1.0 IPCamera\r\n\r\n")
        assertEquals("http://192.168.1.9:49152/d.xml", h["location"])
        assertEquals("Linux UPnP/1.0 IPCamera", h["server"])
    }

    @Test fun `onvif scopes`() {
        val a = OnvifDiscovery.parseScopes(
            "<d:ProbeMatch><d:Scopes>onvif://www.onvif.org/type/video_encoder onvif://www.onvif.org/name/HIKVISION%20DS-2CD " +
                "onvif://www.onvif.org/hardware/DS-2CD2143G0-I</d:Scopes></d:ProbeMatch>",
        )
        assertEquals("HIKVISION DS-2CD", a.friendlyName); assertEquals("DS-2CD2143G0-I", a.model)
    }

    @Test fun `banner`() = assertEquals(
        "Hipcam RealServer/V1.0 | Basic realm=\"IPCAM\" | Login",
        BannerParser.summarize("RTSP/1.0 401 Unauthorized\r\nServer: Hipcam RealServer/V1.0\r\nWWW-Authenticate: Basic realm=\"IPCAM\"\r\n\r\n<title>Login</title>"),
    )
}

class NetworkScannerTest {
    private class NoDiscovery : SsdpDiscovery() { override suspend fun discover() = emptyMap<String, Announcement>() }
    private class NoOnvif : OnvifDiscovery() { override suspend fun discover() = emptyMap<String, Announcement>() }

    @Test fun `finds fake rtsp camera on localhost`() = runBlocking {
        val server = ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            while (!server.isClosed) runCatching {
                server.accept().use { s ->
                    s.getInputStream().read(ByteArray(512))
                    s.getOutputStream().write("RTSP/1.0 200 OK\r\nCSeq: 1\r\nServer: Hipcam RealServer\r\n\r\n".toByteArray())
                }
            }
        }
        // Pretend this random port is RTSP, so the real banner/score logic runs.
        val table = listOf(PortSignature(server.localPort, ReasonCode.PORT_RTSP, 3))
        val prober = object : HostProber() {
            override suspend fun openPorts(ip: String, ports: List<Int>) =
                super.openPorts(ip, ports).map { it.copy(banner = it.banner ?: grab()) }
            fun grab() = "Hipcam RealServer"
            override suspend fun hostname(ip: String) = null
        }
        val events = NetworkScanner(prober, NoDiscovery(), NoOnvif(), table)
            .scanHosts(listOf("127.0.0.1", "127.0.0.2"), localIp = "127.0.0.9", gatewayIp = null).toList()
        server.close()

        val finished = events.last() as ScanEvent.Finished
        val progress = events.filterIsInstance<ScanEvent.Progress>()
        assertEquals(2, progress.maxOf { it.checked })
        assertEquals(1, finished.results.size)
        val r = finished.results.single()
        assertEquals("127.0.0.1", r.device.ip)
        assertEquals(Risk.HIGH, r.risk)
        assertTrue(r.reasons.any { it.arg == "hipcam" })
    }
}
