package com.stoptime.game.ads

import android.app.Activity
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * FREE VERSION ads: banner at the bottom + full-screen ad every 4th try.
 * The IDs below are Google's official TEST ids - safe to use while developing.
 * Replace them with your real AdMob ids before publishing.
 */
class AdsManager(private val activity: Activity) {

    private var bannerView: AdView? = null
    private var interstitial: InterstitialAd? = null

    fun setup(bannerContainer: FrameLayout) {
        MobileAds.initialize(activity) {}

        val metrics = activity.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).toInt()
        bannerView = AdView(activity).apply {
            adUnitId = BANNER_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            bannerContainer.addView(this)
            loadAd(AdRequest.Builder().build())
        }
        loadInterstitial()
    }

    fun shouldShowFullScreenAd(totalTries: Int) = totalTries > 0 && totalTries % EVERY_N_TRIES == 0

    fun showFullScreenAd(onFinished: () -> Unit) {
        val ad = interstitial
        if (ad == null) {           // not loaded yet (no internet etc.) - just skip it
            onFinished(); loadInterstitial(); return
        }
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
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            })
    }

    fun resume() { bannerView?.resume() }
    fun pause() { bannerView?.pause() }
    fun destroy() { bannerView?.destroy() }

    companion object {
        const val EVERY_N_TRIES = 4
        private const val BANNER_ID = "ca-app-pub-3940256099942544/9214589741"       // TEST
        private const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712" // TEST
    }
}
