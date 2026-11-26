package com.hidden.devices.detector.bugs.finder.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent (Google UMP) + Google Mobile Ads (Next-Gen SDK).
 *
 * Order at app start: ask for consent first, and only start the ads SDK and load ads once
 * [ConsentInformation.canRequestAds] is true. Pro users never see ads (checked by the callers).
 * The Next-Gen SDK calls ad callbacks on a background thread, so UI work is posted to the main thread.
 */
object AdsManager {
    private const val TAG = "SweepAds"

    private val main = Handler(Looper.getMainLooper())
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initStarted = AtomicBoolean(false)

    private lateinit var consent: ConsentInformation

    private val _ready = MutableStateFlow(false)
    /** True once consent allows ads and the SDK is initialized. Banners wait for this. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True when the user must be able to change consent (shows "Privacy options" in Settings). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var loadingInterstitial = false
    @Volatile private var lastInterstitialAt = 0L

    /** Call from the activity's onCreate, on every launch. */
    fun gatherConsent(activity: Activity) {
        consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.errorCode} ${error.message}")
                    updatePrivacyState()
                    if (consent.canRequestAds()) start(activity.applicationContext)
                }
            },
            { error ->
                Log.w(TAG, "Consent info update: ${error.errorCode} ${error.message}")
                updatePrivacyState()
                if (consent.canRequestAds()) start(activity.applicationContext)
            },
        )
        // Consent from an earlier session can be used right away, while the update runs.
        if (consent.canRequestAds()) start(activity.applicationContext)
    }

    /** Opens Google's privacy options form so the user can change their ad consent choice. */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.errorCode} ${error.message}")
            updatePrivacyState()
            if (::consent.isInitialized && consent.canRequestAds()) start(activity.applicationContext)
        }
    }

    private fun updatePrivacyState() {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun start(context: Context) {
        if (!initStarted.compareAndSet(false, true)) return
        // Google requires initialize() off the main thread.
        io.launch {
            MobileAds.initialize(context, InitializationConfig.Builder(AdsConfig.APP_ID).build()) {
                _ready.value = true
                preloadInterstitial()
            }
        }
    }

    private fun preloadInterstitial() {
        if (!_ready.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            AdRequest.Builder(AdsConfig.INTERSTITIAL_ID).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w(TAG, "Interstitial load failed: ${adError.message}")
                    interstitial = null
                    loadingInterstitial = false
                }
            },
        )
    }

    /**
     * Shows the interstitial if one is loaded, the user is not Pro and the 3-minute cap allows it.
     * [onDone] always runs once on the main thread: right away when no ad is shown, else when the ad closes.
     */
    fun showInterstitialThen(activity: Activity?, isPro: Boolean, onDone: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val capOk = lastInterstitialAt == 0L || now - lastInterstitialAt >= AdsConfig.INTERSTITIAL_INTERVAL_MS
        if (isPro || activity == null || ad == null || !capOk) {
            if (!isPro) preloadInterstitial()
            onDone()
            return
        }
        val finished = AtomicBoolean(false)
        val finish = {
            if (finished.compareAndSet(false, true)) {
                main.post {
                    onDone()
                    preloadInterstitial()
                }
            }
        }
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Interstitial show failed: ${fullScreenContentError.message}")
                finish()
            }
        }
        interstitial = null
        lastInterstitialAt = now
        ad.show(activity)
    }
}
