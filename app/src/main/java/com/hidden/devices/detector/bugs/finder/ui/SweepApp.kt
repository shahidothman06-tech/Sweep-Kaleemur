package com.hidden.devices.detector.bugs.finder.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hidden.devices.detector.bugs.finder.ads.AdBanner
import com.hidden.devices.detector.bugs.finder.ads.AdsManager
import com.hidden.devices.detector.bugs.finder.ui.Actions.findActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.bluetooth.BleScanner
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.screens.BluetoothScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.ChecklistScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.DeviceScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.HelpScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.HomeScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.LanguageScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.LensScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.MagnetScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.PrivacyScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.ProScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.RateDialog
import com.hidden.devices.detector.bugs.finder.ui.screens.ReportsScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.ResultsScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.ScanScreen
import com.hidden.devices.detector.bugs.finder.ui.screens.SettingsScreen
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULTS = "results"
    const val DEVICE = "device/{id}"
    const val LENS = "lens"
    const val MAGNET = "magnet"
    const val BLUETOOTH = "bluetooth"
    const val CHECKLIST = "checklist"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val LANGUAGE = "language"
    const val WELCOME = "welcome"
    const val PRIVACY = "privacy"
    const val HELP = "help"
    const val PRO = "pro"
    fun device(id: String) = "device/" + Uri.encode(id)
}

/** Screens that show the bottom banner ad. */
private val AD_ROUTES = setOf(Routes.HOME, Routes.RESULTS, Routes.REPORTS)

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    Tab(Routes.CHECKLIST, R.string.nav_checklist, Icons.Rounded.Checklist),
    Tab(Routes.REPORTS, R.string.nav_reports, Icons.Rounded.Description),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

@Composable
fun SweepApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val askRating by vm.askRating.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    // Decided once, so a language change (which recreates the screen) doesn't send the user back.
    val start = rememberSaveable { if (vm.prefs.languageChosen) Routes.HOME else Routes.WELCOME }

    // Bluetooth permission is asked right before the first full scan. The scan runs either way;
    // without permission the Bluetooth step is just skipped.
    // While a permission prompt is open, no ads show.
    var askingPermission by rememberSaveable { mutableStateOf(false) }
    val blePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askingPermission = false
        startScan(vm, nav)
    }
    val onStartScan = {
        if (vm.ble.isSupported && !vm.ble.hasPermission()) {
            askingPermission = true
            blePermission.launch(BleScanner.requiredPermissions())
        } else startScan(vm, nav)
    }
    val context = LocalContext.current
    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    val back: () -> Unit = { nav.popBackStack() }
    val toPro: () -> Unit = { nav.navigate(Routes.PRO) }

    Scaffold(
        containerColor = Sweep.Bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val hasTabs = TABS.any { it.route == route }
            // The tab bar handles the system navigation bar itself. On other screens this bar is always at least
            // as tall as the system navigation bar, so content (buttons) never goes under the phone's nav buttons.
            Column(if (hasTabs) Modifier else Modifier.navigationBarsPadding()) {
                // Banner only on Home, Results and Reports. Never on splash, scan, lens, paywall or during permission prompts.
                if (route in AD_ROUTES && !askingPermission) AdBanner(isPro)
                if (hasTabs) BottomBar(nav, route)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = start) {
                composable(Routes.WELCOME) {
                    LanguageScreen(vm, firstLaunch = true, onDone = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    })
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        vm,
                        onStartScan = onStartScan,
                        onLens = { nav.navigate(Routes.LENS) },
                        onBluetooth = { nav.navigate(Routes.BLUETOOTH) },
                        onMagnet = { nav.navigate(Routes.MAGNET) },
                        onSettings = { switchTab(nav, Routes.SETTINGS) },
                    )
                }
                composable(Routes.SCAN) {
                    ScanScreen(
                        vm,
                        onBack = { vm.cancelScan(); nav.popBackStack() },
                        onFinished = {
                            // One interstitial after a finished full scan, before the results (capped, never for Pro).
                            AdsManager.showInterstitialThen(context.findActivity(), isPro) {
                                if (nav.currentDestination?.route == Routes.SCAN) {
                                    nav.navigate(Routes.RESULTS) { popUpTo(Routes.SCAN) { inclusive = true } }
                                }
                            }
                        },
                    )
                }
                composable(Routes.RESULTS) {
                    ResultsScreen(vm, onBack = back, onOpen = { nav.navigate(Routes.device(it.id)) }, onStartScan = onStartScan, onPro = toPro)
                }
                composable(Routes.DEVICE, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    val id = it.arguments?.getString("id").orEmpty()
                    DeviceScreen(vm, id, onBack = back, onLens = { nav.navigate(Routes.LENS) })
                }
                composable(Routes.LENS) { LensScreen(vm, onBack = back, onPro = toPro) }
                composable(Routes.MAGNET) { MagnetScreen(vm, onBack = back) }
                composable(Routes.BLUETOOTH) { BluetoothScreen(vm, onBack = back) }
                composable(Routes.CHECKLIST) { ChecklistScreen(vm, snackbar) }
                composable(Routes.REPORTS) { ReportsScreen(vm, onPro = toPro) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        vm,
                        onLanguage = { nav.navigate(Routes.LANGUAGE) },
                        onPro = toPro,
                        onPrivacy = { nav.navigate(Routes.PRIVACY) },
                        onHelp = { nav.navigate(Routes.HELP) },
                    )
                }
                composable(Routes.LANGUAGE) { LanguageScreen(vm, firstLaunch = false, onDone = back) }
                composable(Routes.PRIVACY) { PrivacyScreen(onBack = back) }
                composable(Routes.HELP) { HelpScreen(onBack = back) }
                composable(Routes.PRO) { ProScreen(vm, snackbar, onBack = back, onPrivacy = { nav.navigate(Routes.PRIVACY) }) }
            }
        }
    }

    if (askRating && route != Routes.SCAN) RateDialog(vm)
}

private fun startScan(vm: AppViewModel, nav: NavHostController) {
    vm.startFullScan()
    nav.navigate(Routes.SCAN) { launchSingleTop = true }
}

private fun switchTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = Sweep.NavBg) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route,
                onClick = { switchTab(nav, tab.route) },
                icon = { Icon(tab.icon, null) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Sweep.Accent,
                    selectedTextColor = Sweep.Accent,
                    indicatorColor = Sweep.Card2,
                    unselectedIconColor = Sweep.Dim,
                    unselectedTextColor = Sweep.Dim,
                ),
            )
        }
    }
}
