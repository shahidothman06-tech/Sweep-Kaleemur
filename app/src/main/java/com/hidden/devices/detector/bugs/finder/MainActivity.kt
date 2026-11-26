package com.hidden.devices.detector.bugs.finder

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hidden.devices.detector.bugs.finder.ads.AdsManager
import com.hidden.devices.detector.bugs.finder.ui.SweepApp
import com.hidden.devices.detector.bugs.finder.ui.theme.SweepTheme
import com.hidden.devices.detector.bugs.finder.update.UpdateManager

/**
 * AppCompatActivity (not ComponentActivity) so the per-app language picker works on Android 12 and lower.
 */
class MainActivity : AppCompatActivity() {
    // Created here so its result launcher is registered before the activity starts.
    private val updates by lazy { UpdateManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        updates // register the launcher now
        setContent { SweepTheme { SweepApp(updates = updates) } }
        // Consent first (Google UMP); the ads SDK starts and ads load only after consent allows it.
        AdsManager.gatherConsent(this)
        // Google Play in-app update check (does nothing for non-Play installs).
        if (savedInstanceState == null) updates.check()
    }

    override fun onResume() {
        super.onResume()
        updates.onResume()
    }

    override fun onDestroy() {
        updates.onDestroy()
        super.onDestroy()
    }
}
