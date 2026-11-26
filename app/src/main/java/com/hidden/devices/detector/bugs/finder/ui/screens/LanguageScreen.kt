package com.hidden.devices.detector.bugs.finder.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.hidden.devices.detector.bugs.finder.R
import com.hidden.devices.detector.bugs.finder.scan.AppViewModel
import com.hidden.devices.detector.bugs.finder.ui.components.PrimaryButton
import com.hidden.devices.detector.bugs.finder.ui.components.SweepCard
import com.hidden.devices.detector.bugs.finder.ui.components.TopBar
import com.hidden.devices.detector.bugs.finder.ui.theme.Sweep

/** Language tag to its name written in that language. "" means follow the phone. */
val LANGUAGES = listOf(
    "en" to "English",
    "ar" to "العربية",
    "ur" to "اردو",
    "hi" to "हिन्दी",
    "es" to "Español",
    "pt" to "Português",
    "fr" to "Français",
    "id" to "Bahasa Indonesia",
)

private fun currentTag(): String {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',')
    val lang = tag.substringBefore('-')
    return if (lang == "in") "id" else lang
}

@Composable
fun currentLanguageName(): String {
    val tag = currentTag()
    return LANGUAGES.firstOrNull { it.first == tag }?.second ?: stringResource(R.string.language_system)
}

/**
 * Language picker. Shown on first launch ([firstLaunch] = true, no back button) and from Settings.
 * The app restarts its screen in the new language right away.
 */
@Composable
fun LanguageScreen(vm: AppViewModel, firstLaunch: Boolean, onDone: () -> Unit) {
    var selected by remember { mutableStateOf(currentTag()) }
    val options = listOf("" to stringResource(R.string.language_system)) + LANGUAGES

    fun applyLanguage(tag: String) {
        vm.prefs.languageChosen = true
        val list = if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        // Recreates the activity in the new language if it changed. Navigation state is restored.
        AppCompatDelegate.setApplicationLocales(list)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        TopBar(stringResource(R.string.language_title), onBack = if (firstLaunch) null else onDone)
        if (firstLaunch) {
            Text(stringResource(R.string.welcome_language), style = MaterialTheme.typography.bodyMedium, color = Sweep.Dim)
            Spacer(Modifier.height(14.dp))
        }
        SweepCard(padding = 4.dp, modifier = Modifier.weight(1f, fill = false)) {
            LazyColumn {
                items(options, key = { it.first }) { (tag, name) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            selected = tag
                            if (!firstLaunch) {
                                applyLanguage(tag)
                                onDone()
                            }
                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == tag,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Sweep.Accent, unselectedColor = Sweep.Dim),
                            modifier = Modifier.padding(12.dp),
                        )
                        Text(name, style = MaterialTheme.typography.titleMedium)
                    }
                    if (tag != options.last().first) HorizontalDivider(color = Sweep.Line, modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
        if (firstLaunch) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.cont), onClick = {
                applyLanguage(selected)
                onDone()
            }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
    }
}
