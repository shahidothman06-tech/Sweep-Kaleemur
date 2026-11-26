package com.hidden.devices.detector.bugs.finder.update

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play In-App Updates.
 *
 * - On every app start/resume, asks Play if a newer version is available.
 * - Important updates (priority 4–5 in Play Console, or older than [IMMEDIATE_AFTER_DAYS] days)
 *   use a full-screen IMMEDIATE update the user must finish.
 * - Other updates download in the background (FLEXIBLE); when ready, the app shows a "Restart" snackbar.
 * - An immediate update that was interrupted is resumed when the user comes back.
 *
 * Only works for apps installed from Google Play (not for sideloaded debug APKs).
 * To set priority, upload with the Play Developer API `inAppUpdatePriority` (0–5); Play Console defaults to 0.
 */
class UpdateManager(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "SweepUpdate"
        const val IMMEDIATE_PRIORITY = 4
        const val IMMEDIATE_AFTER_DAYS = 7
    }

    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private val _readyToInstall = MutableStateFlow(false)
    /** True when a flexible update has downloaded and needs a restart to install. */
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            Log.i(TAG, "Update flow result: ${result.resultCode}")
        }

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) _readyToInstall.value = true
    }

    private var asked = false

    /** Call from onCreate. Checks once per app start. */
    fun check() {
        manager.registerListener(listener)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (asked || info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
            val stale = (info.clientVersionStalenessDays() ?: 0) >= IMMEDIATE_AFTER_DAYS
            val important = info.updatePriority() >= IMMEDIATE_PRIORITY || stale
            val type = when {
                important && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                else -> return@addOnSuccessListener
            }
            asked = true
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.addOnFailureListener { Log.i(TAG, "Update check failed (normal for non-Play installs): ${it.message}") }
    }

    /** Call from onResume: shows a finished download, and resumes an interrupted immediate update. */
    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) _readyToInstall.value = true
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            }
        }
    }

    /** Installs a downloaded flexible update (the app restarts). */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    fun onDestroy() {
        manager.unregisterListener(listener)
    }
}
