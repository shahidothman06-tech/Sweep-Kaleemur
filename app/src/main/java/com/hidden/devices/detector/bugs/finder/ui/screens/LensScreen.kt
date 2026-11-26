package com.hidden.devices.detector.bugs.finder.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hidden.devices.detector.bugs.finder.ui.Actions
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.components.GhostButton
import com.hidden.devices.detector.bugs.finder.ui.components.IconSquare
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.Segmented
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private suspend fun cameraProvider(context: Context): ProcessCameraProvider = suspendCoroutine { cont ->
    val f = ProcessCameraProvider.getInstance(context)
    f.addListener({ cont.resume(f.get()) }, ContextCompat.getMainExecutor(context))
}

@Composable
fun LensScreen(vm: AppViewModel, onBack: () -> Unit, onPro: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    // If the user taps Allow but Android no longer shows the prompt (denied twice), open the app settings instead.
    val camContext = LocalContext.current
    var userAsked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it && userAsked) Actions.openAppSettings(camContext)
    }
    // Re-check when coming back from the settings page.
    LifecycleResumeEffect(Unit) {
        granted = ContextCompat.checkSelfPermission(camContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    if (!granted) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
            TopBar(stringResource(R.string.tool_lens), onBack = onBack)
            SweepCard {
                Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.camera_permission_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                PrimaryButton(stringResource(R.string.allow), { userAsked = true; launcher.launch(Manifest.permission.CAMERA) })
            }
        }
        return
    }

    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    var mode by remember { mutableIntStateOf(0) } // 0 = red filter (back camera), 1 = night IR (front camera)
    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(mode) {
        val provider = cameraProvider(context)
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val selector = if (mode == 1) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        provider.unbindAll()
        camera = runCatching { provider.bindToLifecycle(lifecycleOwner, selector, preview) }.getOrNull()
        torch = false
    }
    DisposableEffect(Unit) {
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }
    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (mode == 0) Box(Modifier.fillMaxSize().background(Color(0xFFFF1A1A).copy(alpha = 0.38f)))
        // Aim box in the middle.
        Box(
            Modifier.align(Alignment.Center).size(96.dp).border(2.dp, Sweep.Amber, RoundedCornerShape(18.dp)),
        )
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconSquare(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), background = Color.Black.copy(alpha = 0.5f), onClick = onBack)
            Spacer(Modifier.weight(1f))
            Segmented(
                listOf(stringResource(R.string.lens_red), stringResource(R.string.lens_night) + if (isPro) "" else " ★"),
                mode,
                { i -> if (i == 1 && !isPro) onPro() else mode = i },
                Modifier.width(220.dp),
                background = Color.Black.copy(alpha = 0.5f),
            )
        }
        SweepCard(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Text(stringResource(R.string.lens_hint_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (mode == 0) R.string.lens_hint_red else R.string.lens_hint_night),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasFlash) {
                    PrimaryButton(
                        stringResource(if (torch) R.string.lens_flash_off else R.string.lens_flash_on),
                        {
                            torch = !torch
                            camera?.cameraControl?.enableTorch(torch)
                        },
                        Modifier.weight(1f), height = 48.dp,
                    )
                } else {
                    GhostButton(stringResource(R.string.lens_no_flash), {}, Modifier.weight(1f), height = 48.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
