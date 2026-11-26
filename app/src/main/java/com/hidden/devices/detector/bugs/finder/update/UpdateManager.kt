package com.hidden.devices.detector.bugs.finder.update

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Result of a manual "Check for updates". */
enum class UpdateCheckResult { UP_TO_DATE, UPDATE_STARTED, READY_TO_INSTALL, UNAVAILABLE }

/**
 * Google Play In-App Updates.
 *
 * - Checks on every resume (not only cold start). Which flow to use is decided by [UpdatePolicy]:
 *   important updates (priority 4–5, or 7+ days old) are IMMEDIATE (full screen), others FLEXIBLE (background).
 * - An IMMEDIATE update the user left is resumed on the next resume.
 * - A downloaded FLEXIBLE update shows the "Update ready – Restart" snackbar on every resume, and is installed
 *   automatically when the app goes to the background.
 * - The FLEXIBLE prompt is shown at most once per app session, so it doesn't nag on every resume.
 *
 * Only works for apps installed from Google Play (not for sideloaded debug APKs). Users still on an old version
 * without this code get the new version through Play's own auto-update.
 */
class UpdateManager(private val activity: ComponentActivity) {

    companion object {
        private const val TAG = "SweepUpdate"
    }

    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    private val _readyToInstall = MutableStateFlow(false)
    /** True when a flexible update has downloaded and needs a restart to install. */
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    private val _resumes = MutableStateFlow(0)
    /** Increases on every resume, so the UI can show the "Update ready" snackbar again. */
    val resumes: StateFlow<Int> = _resumes.asStateFlow()

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            Log.i(TAG, "Update flow result: ${result.resultCode}")
        }

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) _readyToInstall.value = true
    }

    private var flexibleAsked = false

    fun onCreate() {
        manager.registerListener(listener)
    }

    /** Call from onResume. */
    fun onResume() {
        _resumes.value += 1
        manager.appUpdateInfo
            .addOnSuccessListener { info -> handle(info, manual = false) }
            .addOnFailureListener { Log.i(TAG, "Update check failed (normal for non-Play installs): ${it.message}") }
    }

    /** Call from onStop: installs a downloaded flexible update silently while the app is in the background. */
    fun onStop() {
        if (_readyToInstall.value) manager.completeUpdate()
    }

    /** Settings > Check for updates. */
    suspend fun checkNow(): UpdateCheckResult = try {
        handle(manager.requestAppUpdateInfo(), manual = true)
    } catch (e: Exception) {
        Log.i(TAG, "Manual update check failed: ${e.message}")
        UpdateCheckResult.UNAVAILABLE
    }

    private fun handle(info: AppUpdateInfo, manual: Boolean): UpdateCheckResult {
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _readyToInstall.value = true
            return UpdateCheckResult.READY_TO_INSTALL
        }
        // An immediate update the user left: bring it back.
        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            start(info, AppUpdateType.IMMEDIATE)
            return UpdateCheckResult.UPDATE_STARTED
        }
        val flow = UpdatePolicy.decide(
            updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            priority = info.updatePriority(),
            stalenessDays = info.clientVersionStalenessDays(),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
        )
        return when (flow) {
            UpdateFlow.NONE -> UpdateCheckResult.UP_TO_DATE
            UpdateFlow.IMMEDIATE -> { start(info, AppUpdateType.IMMEDIATE); UpdateCheckResult.UPDATE_STARTED }
            UpdateFlow.FLEXIBLE -> {
                if (manual || !flexibleAsked) {
                    flexibleAsked = true
                    start(info, AppUpdateType.FLEXIBLE)
                }
                UpdateCheckResult.UPDATE_STARTED
            }
        }
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
    }

    /** Installs a downloaded flexible update (the app restarts). */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    fun onDestroy() {
        manager.unregisterListener(listener)
    }
}
