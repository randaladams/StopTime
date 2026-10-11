package com.adamselite.stoptime.ads

import android.app.Activity
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import android.view.View
import com.adamselite.stoptime.BuildConfig
import com.adamselite.stoptime.Consent
import com.adamselite.stoptime.Premium

/**
 * Ads (turned off completely once the player buys "Remove Ads" - see Premium.kt):
 *  - banner at the bottom of the game screen
 *  - full-screen ad when the player comes BACK from the Achievements screen,
 *    but never more than once every [MIN_MINUTES_BETWEEN_FULLSCREEN] minutes
 *    (and not in the first few minutes after opening the app).
 * Ad IDs come from app/build.gradle.kts (test ids in debug builds, real ids in release).
 * Ads are only requested once the consent check (Consent.kt) allows it.
 */
class AdsManager(private val activity: Activity) {

    private var bannerView: AdView? = null
    private var interstitial: InterstitialAd? = null

    private var container: FrameLayout? = null

    fun setup(bannerContainer: FrameLayout) {
        container = bannerContainer
        applyPremium()
    }

    /** Shows or removes all ads depending on whether "Remove Ads" was bought. */
    fun applyPremium() {
        val c = container ?: return
        if (Premium.adsRemoved) {
            bannerView?.destroy()
            bannerView = null
            interstitial = null
            c.removeAllViews()
            c.visibility = View.GONE
        } else if (bannerView == null && Consent.canRequestAds(activity)) {
            c.visibility = View.VISIBLE
            startAds(c)
        }
    }

    private fun startAds(bannerContainer: FrameLayout) {
        if (lastFullScreenMs < 0) lastFullScreenMs = SystemClock.elapsedRealtime() // grace period at app start
        MobileAds.initialize(activity) { status ->
            Log.i(TAG, "MobileAds initialized: ${status.adapterStatusMap}")
        }

        val metrics = activity.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).toInt()
        bannerView = AdView(activity).apply {
            adUnitId = BANNER_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(TAG, "Banner loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner failed: $error")
                    showDebugError("Banner", error)
                }
            }
            bannerContainer.addView(this)
            loadAd(AdRequest.Builder().build())
        }
        loadInterstitial()
    }

    /** Called when the player returns to the game from the Achievements screen. */
    fun onReturnFromAchievements(onFinished: () -> Unit) {
        if (Premium.adsRemoved || !Consent.canRequestAds(activity)) { onFinished(); return }
        val waitedMs = SystemClock.elapsedRealtime() - lastFullScreenMs
        val ad = interstitial
        if (waitedMs < MIN_MINUTES_BETWEEN_FULLSCREEN * 60_000L || ad == null) {
            if (ad == null) loadInterstitial()   // not loaded (no internet etc.) - just skip it
            onFinished(); return
        }
        lastFullScreenMs = SystemClock.elapsedRealtime()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null; loadInterstitial(); onFinished()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null; loadInterstitial(); onFinished()
            }
        }
        ad.show(activity)
    }

    private fun loadInterstitial() {
        InterstitialAd.load(activity, INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.i(TAG, "Full-screen ad loaded")
                    interstitial = if (Premium.adsRemoved) null else ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Full-screen ad failed: $error")
                    interstitial = null
                    showDebugError("Full-screen", error)
                }
            })
    }

    /** DEBUG builds only: show why an ad failed, right in the banner spot. */
    private fun showDebugError(which: String, error: LoadAdError) {
        if (!BuildConfig.DEBUG) return
        val meaning = when (error.code) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> "internal error"
            AdRequest.ERROR_CODE_INVALID_REQUEST -> "invalid request"
            AdRequest.ERROR_CODE_NETWORK_ERROR -> "network error / ads blocked"
            AdRequest.ERROR_CODE_NO_FILL -> "no ad available (no fill)"
            AdRequest.ERROR_CODE_APP_ID_MISSING -> "app id missing"
            else -> "code ${error.code}"
        }
        container?.addView(TextView(activity).apply {
            text = "[debug] $which ad failed: $meaning\n${error.message}"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setBackgroundColor(0xFF333333.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        })
    }

    fun resume() { bannerView?.resume() }
    fun pause() { bannerView?.pause() }
    fun destroy() { bannerView?.destroy() }

    companion object {
        /** Minimum minutes between full-screen ads. Change this number to adjust. */
        const val MIN_MINUTES_BETWEEN_FULLSCREEN = 3
        private var lastFullScreenMs = -1L   // shared across screen redraws (e.g. theme change)
        private const val TAG = "StopTimeAds"
        private val BANNER_ID = BuildConfig.ADMOB_BANNER_ID
        private val INTERSTITIAL_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
    }
}
