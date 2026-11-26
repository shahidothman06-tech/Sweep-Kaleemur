package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.Actions
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep
import kotlinx.coroutines.launch

/**
 * Two-step rate prompt. Happy users go to Google's review card; unhappy users are
 * offered a feedback email instead, so problems reach you before they become 1-star reviews.
 */
@Composable
fun RateDialog(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var unhappy by remember { mutableStateOf(false) }

    if (!unhappy) {
        AlertDialog(
            onDismissRequest = vm::ratingLater,
            containerColor = Sweep.Card,
            icon = { Row { repeat(5) { Icon(Icons.Rounded.Star, null, tint = Sweep.Amber) } } },
            title = { Text(stringResource(R.string.rate_title)) },
            text = { Text(stringResource(R.string.rate_body), color = Sweep.Dim) },
            confirmButton = {
                TextButton(onClick = {
                    vm.ratingAnswered()
                    scope.launch { Actions.rate(context) }
                }) { Text(stringResource(R.string.rate_yes), color = Sweep.Accent) }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { unhappy = true }) { Text(stringResource(R.string.rate_no), color = Sweep.Dim) }
                    TextButton(onClick = vm::ratingLater) { Text(stringResource(R.string.later), color = Sweep.Dim) }
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = vm::ratingAnswered,
            containerColor = Sweep.Card,
            title = { Text(stringResource(R.string.feedback_title)) },
            text = { Text(stringResource(R.string.feedback_body), color = Sweep.Dim) },
            confirmButton = {
                TextButton(onClick = {
                    vm.ratingAnswered()
                    Actions.sendFeedback(context)
                }) { Text(stringResource(R.string.feedback_send), color = Sweep.Accent) }
            },
            dismissButton = {
                TextButton(onClick = vm::ratingAnswered) { Text(stringResource(R.string.cancel), color = Sweep.Dim) }
            },
        )
    }
}
