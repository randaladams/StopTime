package com.adamselite.stoptime

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * The one-time "Remove Ads" purchase (Google Play Billing).
 *
 * In the Google Play Console create an IN-APP product (one-time, NOT a subscription)
 * with the product ID below. The price is set there, not in this code.
 *
 * The result is saved on the phone so the app starts ad-free even offline, and it is
 * re-checked with Google Play every time the app opens (this also restores the purchase
 * after a reinstall or on a new phone with the same Google account).
 */
object Premium {

    const val PRODUCT_ID = "remove_ads"

    private const val TAG = "StopTimeBilling"
    private const val PREFS = "stoptime_settings"
    private const val KEY_ADS_REMOVED = "ads_removed"
    private const val KEY_DEBUG_FAKE = "debug_fake_premium"

    private lateinit var app: Context
    private lateinit var client: BillingClient
    private val main = Handler(Looper.getMainLooper())
    private var productDetails: ProductDetails? = null
    private val listeners = mutableSetOf<() -> Unit>()

    /** True once the player has bought "Remove Ads". */
    var adsRemoved = false
        private set

    /** Localized price from Google Play, e.g. "$1.99" (null until loaded). */
    val price: String?
        get() = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice

    fun init(context: Context) {
        app = context.applicationContext
        adsRemoved = prefs().getBoolean(KEY_ADS_REMOVED, false) || debugFake()

        client = BillingClient.newBuilder(app)
            .setListener { result, purchases ->
                when (result.responseCode) {
                    BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it, fromBuy = true) }
                    BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
                    BillingResponseCode.USER_CANCELED -> Unit
                    else -> Log.w(TAG, "Purchase failed: ${result.debugMessage}")
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                Log.i(TAG, "Billing setup: ${result.responseCode} ${result.debugMessage}")
                if (result.responseCode == BillingResponseCode.OK) {
                    loadProduct()
                    refresh()
                }
            }
            override fun onBillingServiceDisconnected() { /* auto-reconnect is on */ }
        })
    }

    /** Get notified (on the main thread) when [adsRemoved] or [price] changes. */
    fun addListener(l: () -> Unit) { listeners += l }
    fun removeListener(l: () -> Unit) { listeners -= l }

    /** Opens Google's payment screen. */
    fun buy(activity: Activity) {
        val details = productDetails
        if (details == null) {
            Toast.makeText(activity,
                if (BuildConfig.DEBUG) "Store not available. Purchases only work when the app is installed from Google Play (internal testing)."
                else "The store isn't available right now. Please try again later.",
                Toast.LENGTH_LONG).show()
            loadProduct()
            return
        }
        val offer = details.oneTimePurchaseOfferDetails
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { offer?.offerToken?.let { setOfferToken(it) } }
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = client.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow: ${result.responseCode} ${result.debugMessage}")
        }
    }

    /** Asks Google Play what the player owns (restores purchases). */
    fun refresh() {
        if (!::client.isInitialized || !client.isReady) return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any {
                it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            purchases.forEach { handlePurchase(it, fromBuy = false) }
            // If Google says it's not owned (e.g. refunded), turn ads back on.
            if (!owned && !debugFake()) setAdsRemoved(false)
        }
    }

    private fun loadProduct() {
        if (!::client.isInitialized || !client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )).build()
        client.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.firstOrNull()
                Log.i(TAG, "Product loaded: ${productDetails != null}, price=$price")
                notifyListeners()
            } else {
                Log.w(TAG, "Product query failed: ${result.debugMessage}")
            }
        }
    }

    private fun handlePurchase(p: Purchase, fromBuy: Boolean) {
        if (!p.products.contains(PRODUCT_ID)) return
        when (p.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!p.isAcknowledged) {
                    // Must acknowledge within 3 days or Google refunds it automatically.
                    val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
                    client.acknowledgePurchase(ack) { r -> Log.i(TAG, "Acknowledge: ${r.responseCode}") }
                }
                val wasRemoved = adsRemoved
                setAdsRemoved(true)
                if (fromBuy && !wasRemoved) main.post {
                    Toast.makeText(app, "Thank you! Ads removed. 🎉", Toast.LENGTH_LONG).show()
                }
            }
            Purchase.PurchaseState.PENDING -> if (fromBuy) main.post {
                Toast.makeText(app, "Payment pending. Ads will be removed once it completes.", Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    private fun setAdsRemoved(value: Boolean) {
        val v = value || debugFake()
        if (v == adsRemoved) return
        adsRemoved = v
        prefs().edit().putBoolean(KEY_ADS_REMOVED, value).apply()
        notifyListeners()
    }

    private fun notifyListeners() = main.post { listeners.toList().forEach { it() } }

    // ---------- TEST BUILDS ONLY: pretend the purchase was made ----------
    // (Real purchases can't be tested on a sideloaded APK.)
    private fun debugFake() = BuildConfig.DEBUG && prefs().getBoolean(KEY_DEBUG_FAKE, false)

    fun debugToggleFakePurchase(): Boolean {
        if (!BuildConfig.DEBUG) return adsRemoved
        val fake = !debugFake()
        prefs().edit().putBoolean(KEY_DEBUG_FAKE, fake).apply()
        adsRemoved = fake || prefs().getBoolean(KEY_ADS_REMOVED, false)
        notifyListeners()
        return adsRemoved
    }

    private fun prefs() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
