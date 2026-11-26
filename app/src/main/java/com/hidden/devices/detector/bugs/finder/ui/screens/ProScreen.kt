package com.hidden.devices.detector.bugs.finder.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.billing.PlanInfo
import com.hidden.devices.detector.bugs.finder.billing.ProPlan
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.Actions
import com.hidden.devices.detector.bugs.finder.ui.Actions.findActivity
import com.hidden.devices.detector.bugs.finder.ui.components.Pill
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency

private val AccentLight = Color(0xFF7AA9FF)

/** Paywall for the Sweep Pro subscription (weekly or monthly). */
@Composable
fun ProScreen(vm: AppViewModel, snackbar: SnackbarHostState, onBack: () -> Unit, onPrivacy: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPro by vm.billing.isPro.collectAsStateWithLifecycle()
    val plans by vm.billing.plans.collectAsStateWithLifecycle()
    var plan by rememberSaveable { mutableStateOf(ProPlan.MONTHLY) }
    val unavailable = stringResource(R.string.pro_unavailable)
    val restored = stringResource(R.string.pro_restored)
    val fromPlay = remember { installedFromPlay(context) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        // Close (X) at the top right.
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.Close, stringResource(R.string.cancel), tint = Sweep.Dim)
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Hero(isPro)
            Spacer(Modifier.height(20.dp))
            SweepCard(padding = 14.dp) {
                listOf(R.string.pro_feature_no_ads, R.string.pro_feature_reports, R.string.pro_feature_share, R.string.pro_feature_night).forEach {
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Sweep.Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(it), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (!isPro) {
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlanCard(ProPlan.MONTHLY, plans[ProPlan.MONTHLY], plan == ProPlan.MONTHLY) { plan = ProPlan.MONTHLY }
                    PlanCard(ProPlan.WEEKLY, plans[ProPlan.WEEKLY], plan == ProPlan.WEEKLY) { plan = ProPlan.WEEKLY }
                }
                if (plans.isEmpty() && !fromPlay) {
                    Text(
                        stringResource(R.string.pro_sideload_note),
                        style = MaterialTheme.typography.bodySmall, color = Sweep.Amber, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Sticky bottom area (the Scaffold keeps it above the system navigation bar).
        if (isPro) {
            GradientButton(stringResource(R.string.pro_manage)) { Actions.manageSubscription(context) }
        } else {
            val info = plans[plan]
            val priceText = info?.let { stringResource(if (plan == ProPlan.MONTHLY) R.string.pro_per_month else R.string.pro_per_week, it.price) }
            val label = when {
                info?.trialDays != null -> stringResource(R.string.pro_start_trial)
                priceText != null -> stringResource(R.string.pro_continue, priceText)
                else -> stringResource(R.string.pro_subscribe)
            }
            GradientButton(label) {
                val activity = context.findActivity()
                val started = activity != null && vm.billing.subscribe(activity, plan)
                if (!started) {
                    vm.billing.connect()
                    scope.launch { snackbar.showSnackbar(unavailable) }
                }
            }
            val terms = when {
                info?.trialDays != null && priceText != null -> stringResource(R.string.pro_terms_trial, info.trialDays, priceText)
                priceText != null -> stringResource(R.string.pro_terms, priceText)
                else -> stringResource(R.string.pro_cancel_note)
            }
            Text(
                terms,
                style = MaterialTheme.typography.bodySmall, color = Sweep.Dim, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinkText(stringResource(R.string.pro_link_restore)) {
                scope.launch {
                    vm.billing.restore()
                    snackbar.showSnackbar(restored)
                }
            }
            Dot()
            LinkText(stringResource(R.string.pro_link_manage)) { Actions.manageSubscription(context) }
            Dot()
            LinkText(stringResource(R.string.pro_link_privacy), onPrivacy)
        }
    }
}

@Composable
private fun Hero(isPro: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(112.dp).background(
                Brush.radialGradient(listOf(Sweep.Accent.copy(alpha = 0.35f), Color.Transparent)),
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Sweep.Accent, modifier = Modifier.size(60.dp))
        }
        Text(
            stringResource(if (isPro) R.string.pro_active_title else R.string.pro_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (isPro) R.string.pro_owned else R.string.pro_body),
            style = MaterialTheme.typography.bodyLarge, color = Sweep.Dim, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlanCard(plan: ProPlan, info: PlanInfo?, selected: Boolean, onClick: () -> Unit) {
    val monthly = plan == ProPlan.MONTHLY
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Sweep.Accent.copy(alpha = 0.10f) else Sweep.Card)
            .border(if (selected) 2.dp else 1.dp, if (selected) Sweep.Accent else Sweep.Line, RoundedCornerShape(18.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Sweep.Accent, unselectedColor = Sweep.Dim),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (monthly) R.string.pro_monthly else R.string.pro_weekly),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false),
                )
                if (monthly) {
                    Spacer(Modifier.width(8.dp))
                    Pill(stringResource(R.string.pro_best_value), Sweep.Accent)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (info == null) {
                Shimmer()
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(info.price, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Sweep.Text)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(if (monthly) R.string.pro_period_month else R.string.pro_period_week),
                        style = MaterialTheme.typography.bodyMedium, color = Sweep.Dim, modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                if (monthly) weeklyEquivalent(info)?.let {
                    Text(stringResource(R.string.pro_week_equiv, it), style = MaterialTheme.typography.bodySmall, color = Sweep.Dim)
                }
                info.trialDays?.let {
                    Text(stringResource(R.string.pro_trial_days, it), style = MaterialTheme.typography.bodySmall, color = Sweep.Green)
                }
            }
        }
    }
}

@Composable
private fun Shimmer() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    Box(Modifier.width(96.dp).height(26.dp).alpha(a).clip(RoundedCornerShape(8.dp)).background(Sweep.Card2))
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Sweep.Accent, AccentLight)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall, color = Sweep.Dim,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
    )
}

@Composable
private fun Dot() {
    Text("·", color = Sweep.Dim)
}

/** Monthly price as a weekly amount (price × 12 ÷ 52), in the same currency. Null if it can't be formatted. */
private fun weeklyEquivalent(info: PlanInfo): String? = runCatching {
    val fmt = NumberFormat.getCurrencyInstance()
    fmt.currency = Currency.getInstance(info.currencyCode)
    fmt.format(info.priceMicros / 1_000_000.0 * 12.0 / 52.0)
}.getOrNull()

/** False for sideloaded/debug installs, where Play can't return prices. */
@Suppress("DEPRECATION")
private fun installedFromPlay(context: Context): Boolean = runCatching {
    val pm = context.packageManager
    val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        pm.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        pm.getInstallerPackageName(context.packageName)
    }
    installer == "com.android.vending"
}.getOrDefault(false)
