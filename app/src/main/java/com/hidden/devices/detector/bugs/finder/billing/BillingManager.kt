package com.hidden.devices.detector.bugs.finder.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.hidden.devices.detector.bugs.finder.scan.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProPlan(val basePlanId: String) { WEEKLY("weekly"), MONTHLY("monthly") }

/** One plan's price from Play. [trialDays] is set when the offer starts with a free trial. */
data class PlanInfo(
    val price: String,
    val priceMicros: Long,
    val currencyCode: String,
    val trialDays: Int? = null,
)

/** Days in an ISO 8601 period like "P3D", "P1W" or "P1M" (a month counts as 30 days). */
fun isoPeriodDays(period: String): Int {
    val m = Regex("""P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?""").matchEntire(period) ?: return 0
    val (y, mo, w, d) = m.destructured
    return (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
}

/**
 * Google Play Billing for the "Sweep Pro" subscription.
 *
 * Play Console setup (Monetize > Subscriptions):
 *   subscription product ID: [PRO_PRODUCT_ID]
 *   base plan "weekly"  (auto-renewing, 1 week)
 *   base plan "monthly" (auto-renewing, 1 month)
 */
class BillingManager(context: Context, private val prefs: Prefs) {

    companion object {
        const val PRO_PRODUCT_ID = "sweep_pro"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPro = MutableStateFlow(prefs.isProCached)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _plans = MutableStateFlow<Map<ProPlan, PlanInfo>>(emptyMap())
    /** Prices and trial info from Play. Empty until loaded or if the store is unavailable (e.g. a sideloaded build). */
    val plans: StateFlow<Map<ProPlan, PlanInfo>> = _plans.asStateFlow()

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                scope.launch { purchases.forEach { handle(it) } }
            }
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadProduct()
                        restore()
                    }
                }
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private suspend fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            ),
        ).build()
        val d = client.queryProductDetails(params).productDetailsList?.firstOrNull()
        details = d
        _plans.value = ProPlan.entries.mapNotNull { plan ->
            val phases = offerFor(d, plan)?.pricingPhases?.pricingPhaseList ?: return@mapNotNull null
            // The last phase is the regular recurring price; a free first phase is a free trial.
            val regular = phases.lastOrNull() ?: return@mapNotNull null
            val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
            plan to PlanInfo(
                price = regular.formattedPrice,
                priceMicros = regular.priceAmountMicros,
                currencyCode = regular.priceCurrencyCode,
                trialDays = trial?.let { isoPeriodDays(it.billingPeriod) * maxOf(1, it.billingCycleCount) },
            )
        }.toMap()
    }

    private fun offerFor(d: ProductDetails?, plan: ProPlan): ProductDetails.SubscriptionOfferDetails? =
        d?.subscriptionOfferDetails
            ?.filter { it.basePlanId == plan.basePlanId }
            // Prefer an intro offer (free trial etc.) if one exists, else the plain base plan.
            ?.minByOrNull { if (it.offerId != null) 0 else 1 }

    /** Re-checks active subscriptions. Also used by the "Restore purchase" button. */
    suspend fun restore() {
        if (!client.isReady) return
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        val active = result.purchasesList.any {
            PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        setPro(active)
        result.purchasesList.forEach { handle(it) }
    }

    /** Returns false when the store isn't ready, so the screen can show a message. */
    fun subscribe(activity: Activity, plan: ProPlan): Boolean {
        val d = details ?: return false
        val offer = offerFor(d, plan) ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(d)
                    .setOfferToken(offer.offerToken)
                    .build(),
            ),
        ).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    private suspend fun handle(p: Purchase) {
        if (PRO_PRODUCT_ID !in p.products || p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        setPro(true)
        if (!p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
    }

    private fun setPro(v: Boolean) {
        _isPro.value = v
        prefs.isProCached = v
    }
}
