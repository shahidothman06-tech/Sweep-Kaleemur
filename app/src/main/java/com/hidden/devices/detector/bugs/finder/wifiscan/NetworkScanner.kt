package com.hidden.devices.detector.bugs.finder.wifiscan

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Scans the Wi-Fi network for devices that look like cameras.
 *
 * Runs three checks at the same time:
 *  1. Port sweep of every address on the subnet (camera ports like RTSP 554).
 *  2. UPnP/SSDP discovery (devices that announce maker and model).
 *  3. ONVIF discovery (IP cameras announce themselves).
 *
 * Note: Android 10+ hides other devices' MAC addresses from apps, so we can't look up
 * the maker from the MAC. These three checks are what's left, and they catch most cameras.
 */
class NetworkScanner(
    private val prober: HostProber = HostProber(),
    private val ssdp: SsdpDiscovery = SsdpDiscovery(),
    private val onvif: OnvifDiscovery = OnvifDiscovery(),
    private val portTable: List<PortSignature> = CameraSignatures.ports,
    private val parallelHosts: Int = 48,
) {
    private val scorer = RiskScorer(portTable)

    /** Scan the subnet the phone is on. */
    fun scan(localIp: String, prefixLength: Int, gatewayIp: String?): Flow<ScanEvent> =
        scanHosts(Subnet.hostsToScan(localIp, prefixLength), localIp, gatewayIp)

    /** Scan an explicit list of addresses (also used by tests). */
    fun scanHosts(hosts: List<String>, localIp: String?, gatewayIp: String?): Flow<ScanEvent> = channelFlow {
        val ports = portTable.map { it.port }
        val total = hosts.size
        val done = AtomicInteger(0)
        val gate = Semaphore(parallelHosts)
        send(ScanEvent.Progress(0, total))

        val (alive, ssdpFound, onvifFound) = coroutineScope {
            val ssdpJob = async { ssdp.discover() }
            val onvifJob = async { onvif.discover() }
            val probes = hosts.map { ip ->
                async {
                    gate.withPermit {
                        val open = prober.openPorts(ip, ports)
                        val n = done.incrementAndGet()
                        send(ScanEvent.Progress(n, total))
                        if (open.isEmpty()) null else ip to open
                    }
                }
            }
            Triple(probes.mapNotNull { it.await() }.toMap(), ssdpJob.await(), onvifJob.await())
        }

        val ips = (alive.keys + ssdpFound.keys + onvifFound.keys).filter { it != localIp }.toSortedSet(
            compareBy { runCatching { Subnet.toInt(it).toUInt() }.getOrDefault(UInt.MAX_VALUE) },
        )
        val devices = coroutineScope {
            ips.map { ip ->
                async {
                    gate.withPermit {
                        Device(
                            ip = ip,
                            hostname = prober.hostname(ip),
                            openPorts = alive[ip].orEmpty(),
                            ssdp = ssdpFound[ip],
                            onvif = onvifFound[ip],
                            isGateway = ip == gatewayIp,
                        )
                    }
                }
            }.map { it.await() }
        }

        val results = devices.map { scorer.score(it) }
            .sortedWith(compareBy<ScanResult> { it.risk.ordinal }.thenBy { Subnet.toInt(it.device.ip).toUInt() })
        send(ScanEvent.Progress(total, total))
        send(ScanEvent.Finished(results))
    }
}
