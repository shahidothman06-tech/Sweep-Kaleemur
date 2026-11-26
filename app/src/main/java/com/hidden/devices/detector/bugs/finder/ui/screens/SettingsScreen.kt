package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import com.hidden.devices.detector.bugs.finder.ads.AdsManager
import com.hidden.devices.detector.bugs.finder.ui.Actions.findActivity
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.Actions
import com.hidden.devices.detector.bugs.finder.ui.components.IconSquare
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onLanguage: () -> Unit,
    onPro: () -> Unit,
    onPrivacy: () -> Unit,
    onHelp: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    val privacyOptions by AdsManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        )

        SweepCard(padding = 4.dp, border = if (isPro) null else Sweep.Accent.copy(alpha = 0.4f)) {
            SettingsRow(
                Icons.Rounded.WorkspacePremium, stringResource(R.string.settings_pro),
                value = if (isPro) stringResource(R.string.settings_pro_active) else null,
                onClick = onPro, divider = false, tint = Sweep.Accent,
            )
        }

        Section(stringResource(R.string.settings_section_app))
        SweepCard(padding = 4.dp) {
            SettingsRow(Icons.Rounded.Language, stringResource(R.string.settings_language), value = currentLanguageName(), onClick = onLanguage)
            SettingsRow(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.settings_help), onClick = onHelp, divider = false)
        }

        Section(stringResource(R.string.settings_section_about))
        SweepCard(padding = 4.dp) {
            SettingsRow(Icons.Rounded.Star, stringResource(R.string.settings_rate), onClick = {
                vm.ratingAnswered()
                scope.launch { Actions.rate(context) }
            })
            SettingsRow(Icons.Rounded.Share, stringResource(R.string.settings_share), onClick = { Actions.shareApp(context) })
            SettingsRow(Icons.Rounded.Email, stringResource(R.string.settings_feedback), onClick = { Actions.sendFeedback(context) })
            SettingsRow(Icons.Rounded.PrivacyTip, stringResource(R.string.settings_privacy), onClick = onPrivacy, divider = privacyOptions)
            // Required by Google's consent rules (e.g. EEA/UK users): lets the user change their ad consent choice.
            if (privacyOptions) {
                SettingsRow(Icons.Rounded.Tune, stringResource(R.string.settings_privacy_options), onClick = {
                    context.findActivity()?.let { AdsManager.showPrivacyOptions(it) }
                }, divider = false)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.app_full_name) + "\n" + stringResource(R.string.settings_version, Actions.versionName(context)),
            style = MaterialTheme.typography.bodySmall,
            color = Sweep.Dim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Sweep.Dim,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
    divider: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = Sweep.Text,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconSquare(icon, null, tint = tint, background = Sweep.Card2)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = Sweep.Dim)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Sweep.Dim)
        }
        if (divider) HorizontalDivider(color = Sweep.Line, modifier = Modifier.padding(start = 60.dp))
    }
}
