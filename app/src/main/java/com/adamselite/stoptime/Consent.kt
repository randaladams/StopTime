package com.adamselite.stoptime

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google's consent pop-up (User Messaging Platform) for players in Europe/the UK
 * and other regions with privacy laws. Outside those regions nothing is shown.
 * The message itself is set up in your AdMob account: Privacy & messaging.
 */
object Consent {
    private const val TAG = "StopTimeConsent"
    private var asked = false

    private fun info(activity: Activity): ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /** True when ads may be requested (consent given, or not needed in this region). */
    fun canRequestAds(activity: Activity) = info(activity).canRequestAds()

    /** Whether the "Privacy settings" link must be shown (required in some regions). */
    fun privacyOptionsRequired(activity: Activity) =
        info(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Checks consent once per app launch and shows the pop-up if needed. Then calls [onDone]. */
    fun gather(activity: Activity, onDone: () -> Unit) {
        if (asked) { onDone(); return }
        asked = true
        val params = ConsentRequestParameters.Builder().build()
        info(activity).requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    onDone()
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.message}")
                onDone()
            }
        )
    }

    /** Opens Google's privacy options screen (from the "Privacy settings" link). */
    fun showPrivacyOptions(activity: Activity, onClosed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onClosed() }
    }
}
