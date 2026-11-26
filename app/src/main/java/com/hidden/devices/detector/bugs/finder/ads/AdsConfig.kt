package com.hidden.devices.detector.bugs.finder.ads

/**
 * All AdMob IDs in one place.
 *
 * These are Google's official TEST IDs. They always show test ads and are safe to use while developing.
 * TODO(before release): replace all three with your real IDs from AdMob (Apps > Sweep > App settings / Ad units),
 *   and also replace the APPLICATION_ID meta-data value in AndroidManifest.xml with the same real app ID.
 *   Never click your own real ads.
 */
object AdsConfig {
    /** TODO: real AdMob app ID (must match AndroidManifest.xml). */
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /** TODO: real adaptive banner ad unit ID. Used on Home, Results and Reports. */
    const val BANNER_ID = "ca-app-pub-3940256099942544/9214589741"

    /** TODO: real interstitial ad unit ID. Shown once after a finished full scan. */
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    /** At most one interstitial in this time. */
    const val INTERSTITIAL_INTERVAL_MS = 3 * 60 * 1000L
}
