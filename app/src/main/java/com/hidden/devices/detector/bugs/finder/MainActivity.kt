package com.hidden.devices.detector.bugs.finder

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hidden.devices.detector.bugs.finder.ui.SweepApp
import com.hidden.devices.detector.bugs.finder.ui.theme.SweepTheme

/**
 * AppCompatActivity (not ComponentActivity) so the per-app language picker works on Android 12 and lower.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { SweepTheme { SweepApp() } }
    }
}
