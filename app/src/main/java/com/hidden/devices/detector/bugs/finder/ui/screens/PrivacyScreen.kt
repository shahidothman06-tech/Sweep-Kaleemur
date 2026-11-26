package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.ui.Actions
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

/** In-app privacy policy. The same text should be hosted on a public URL for the Play listing. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.privacy_title), onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.privacy_note), style = MaterialTheme.typography.bodySmall, color = Sweep.Dim)
            Spacer(Modifier.height(10.dp))
            SweepCard {
                Text(stringResource(R.string.privacy_updated), style = MaterialTheme.typography.labelMedium, color = Sweep.Dim)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium, color = Sweep.Text)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { Actions.sendFeedback(context) }) {
                    Text(stringResource(R.string.support_email), color = Sweep.Accent)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
