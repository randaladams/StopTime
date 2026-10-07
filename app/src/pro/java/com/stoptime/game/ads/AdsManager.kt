package com.stoptime.game.ads

import android.app.Activity
import android.view.View
import android.widget.FrameLayout

/** PRO VERSION: no ads at all. Same functions as the free version, but they do nothing. */
@Suppress("UNUSED_PARAMETER")
class AdsManager(activity: Activity) {
    fun setup(bannerContainer: FrameLayout) { bannerContainer.visibility = View.GONE }
    fun shouldShowFullScreenAd(totalTries: Int) = false
    fun showFullScreenAd(onFinished: () -> Unit) = onFinished()
    fun resume() {}
    fun pause() {}
    fun destroy() {}
}
