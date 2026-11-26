package com.hidden.devices.detector.bugs.finder.wifiscan

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address

data class WifiInfo(val localIp: String, val prefixLength: Int, val gatewayIp: String?)

/** Reads the phone's Wi-Fi IP, subnet size and router address. No location permission needed. */
class NetworkInfoProvider(context: Context) {
    private val app = context.applicationContext
    private val cm = app.getSystemService(ConnectivityManager::class.java)

    /** Returns null when the phone is not on Wi-Fi. */
    fun current(): WifiInfo? {
        val network = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        val props = cm.getLinkProperties(network) ?: return null
        val addr = props.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
        val gateway = props.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress
        return WifiInfo(addr.address.hostAddress!!, addr.prefixLength, gateway)
    }

    /** Lets the app hear UPnP/ONVIF multicast replies while scanning. Always release it. */
    fun acquireMulticastLock(): WifiManager.MulticastLock =
        app.getSystemService(WifiManager::class.java)
            .createMulticastLock("hidden-cam-scan")
            .apply { setReferenceCounted(false); acquire() }
}
