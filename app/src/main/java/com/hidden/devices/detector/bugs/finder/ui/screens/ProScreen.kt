package com.hidden.devices.detector.bugs.finder.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.billing.ProPlan
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.Actions
import com.hidden.devices.detector.bugs.finder.ui.Actions.findActivity
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.launch

/** Paywall for the Sweep Pro subscription (weekly or monthly). */
@Composable
fun ProScreen(vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    val prices by vm.billing.prices.collectAsStateWithLifecycle()
    var plan by rememberSaveable { mutableStateOf(ProPlan.MONTHLY) }
    val unavailable = stringResource(R.string.pro_unavailable)
    val restored = stringResource(R.string.pro_restored)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.pro_title), onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Sweep.Accent, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(if (isPro) R.string.pro_owned else R.string.pro_body),
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            SweepCard {
                listOf(R.string.pro_feature_no_ads, R.string.pro_feature_reports, R.string.pro_feature_share, R.string.pro_feature_night).forEach {
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Sweep.Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(it), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (!isPro) {
                Text(
                    stringResource(R.string.pro_choose_plan).uppercase(),
                    style = MaterialTheme.typography.labelMedium, color = Sweep.Dim,
                    modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanCard(
                        title = stringResource(R.string.pro_monthly),
                        price = prices[ProPlan.MONTHLY]?.let { stringResource(R.string.pro_per_month, it) },
                        badge = stringResource(R.string.pro_best_value),
                        selected = plan == ProPlan.MONTHLY,
                        onClick = { plan = ProPlan.MONTHLY },
                    )
                    PlanCard(
                        title = stringResource(R.string.pro_weekly),
                        price = prices[ProPlan.WEEKLY]?.let { stringResource(R.string.pro_per_week, it) },
                        badge = null,
                        selected = plan == ProPlan.WEEKLY,
                        onClick = { plan = ProPlan.WEEKLY },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (!isPro) {
            PrimaryButton(stringResource(R.string.pro_subscribe), onClick = {
                val activity = context.findActivity()
                val started = activity != null && vm.billing.subscribe(activity, plan)
                if (!started) {
                    vm.billing.connect()
                    scope.launch { snackbar.showSnackbar(unavailable) }
                }
            }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.pro_cancel_note),
                style = MaterialTheme.typography.bodySmall, color = Sweep.Dim, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = {
                scope.launch {
                    vm.billing.restore()
                    snackbar.showSnackbar(restored)
                }
            }) { Text(stringResource(R.string.pro_restore), color = Sweep.Dim) }
            TextButton(onClick = { Actions.manageSubscription(context) }) {
                Text(stringResource(R.string.pro_manage), color = Sweep.Dim)
            }
        }
    }
}

@Composable
private fun PlanCard(title: String, price: String?, badge: String?, selected: Boolean, onClick: () -> Unit) {
    SweepCard(onClick = onClick, border = if (selected) Sweep.Accent else Sweep.Line, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected, onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = Sweep.Accent, unselectedColor = Sweep.Dim),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    price ?: stringResource(R.string.pro_price_loading),
                    style = MaterialTheme.typography.bodyMedium, color = Sweep.Dim,
                )
            }
            if (badge != null) Pill(badge, Sweep.Accent)
        }
    }
}
