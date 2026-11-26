package com.hidden.devices.detector.bugs.finder.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.billing.BillingManager

/** Small helpers for actions that leave the app: store page, email, share, subscriptions. */
object Actions {

    fun Context.findActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    fun storeUrl(context: Context) = "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) { }
    }

    fun openStorePage(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            openUrl(context, storeUrl(context))
        }
    }

    /** Shows Google's in-app review card. Falls back to the store page if it can't. */
    suspend fun rate(context: Context) {
        val activity = context.findActivity()
        if (activity == null) {
            openStorePage(context); return
        }
        try {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReview()
            manager.launchReview(activity, info)
        } catch (_: Exception) {
            openStorePage(context)
        }
    }

    fun sendFeedback(context: Context) {
        val email = context.getString(R.string.support_email)
        val subject = context.getString(R.string.app_full_name) + " feedback"
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openUrl(context, "mailto:$email")
        }
    }

    fun shareApp(context: Context) {
        val text = context.getString(R.string.share_app_text, storeUrl(context))
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, null))
    }

    fun manageSubscription(context: Context) {
        openUrl(
            context,
            "https://play.google.com/store/account/subscriptions?sku=${BillingManager.PRO_PRODUCT_ID}&package=${context.packageName}",
        )
    }

    /** Opens this app's page in Android settings (for permissions the user denied for good). */
    fun openAppSettings(context: Context) {
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
        }
    }

    fun versionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }
}
