package com.hidden.devices.detector.bugs.finder.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Adaptive anchored banner, full screen width. Shows nothing for Pro users or before consent allows ads.
 */
@Composable
fun AdBanner(isPro: Boolean, modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    if (isPro || !ready) return
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val size = remember(widthDp) { AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp) }
    val adView = remember(size) {
        AdView(context).apply {
            loadAd(
                BannerAdRequest.Builder(AdsConfig.BANNER_ID, size).build(),
                object : AdLoadCallback<BannerAd> {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w("SweepAds", "Banner load failed: ${adError.message}")
                    }
                },
            )
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    AndroidView(
        factory = { adView },
        modifier = modifier.fillMaxWidth().height(size.height.dp),
    )
}
