package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.ai.AssistantToneProfile
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.UserPrefs
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * Settings panel over [UserPrefs] (#125): one control per typed pref.
 * Switch for Bool, Slider for Int/Float, menu for StringPref.
 */
@Composable
fun UserPrefsPanel(container: AppContainer, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val boolState = remember { mutableStateMapOf<UserPrefs.BoolPref, Boolean>() }
    val intState = remember { mutableStateMapOf<UserPrefs.IntPref, Int>() }
    val floatState = remember { mutableStateMapOf<UserPrefs.FloatPref, Float>() }
    val stringState = remember { mutableStateMapOf<UserPrefs.StringPref, String>() }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (p in UserPrefs.BoolPref.entries) boolState[p] = container.userPrefs.bool(p)
        for (p in UserPrefs.IntPref.entries) intState[p] = container.userPrefs.int(p)
        for (p in UserPrefs.FloatPref.entries) floatState[p] = container.userPrefs.float(p)
        for (p in UserPrefs.StringPref.entries) stringState[p] = container.userPrefs.string(p)
        loaded = true
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Preferences",
            subtitle = if (loaded) "small knobs that don't fit the typed stores" else "loading…",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        if (!loaded) return

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(UserPrefs.BoolPref.entries.toList()) { pref ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pref.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = QCColors.Soft,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                        Switch(
                            checked = boolState[pref] ?: pref.default,
                            onCheckedChange = { want ->
                                boolState[pref] = want
                                scope.launch { container.userPrefs.set(pref, want) }
                            },
                        )
                    }
                }
            }
            items(UserPrefs.IntPref.entries.toList()) { pref ->
                val v = intState[pref] ?: pref.default
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "${pref.name}: $v",
                            style = MaterialTheme.typography.titleSmall,
                            color = QCColors.Soft,
                        )
                        Slider(
                            value = v.toFloat(),
                            onValueChange = {
                                intState[pref] = it.toInt()
                            },
                            onValueChangeFinished = {
                                scope.launch { container.userPrefs.set(pref, intState[pref] ?: pref.default) }
                            },
                            valueRange = 0f..100f,
                        )
                    }
                }
            }
            items(UserPrefs.FloatPref.entries.toList()) { pref ->
                val v = floatState[pref] ?: pref.default
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "${pref.name}: %.2f".format(v),
                            style = MaterialTheme.typography.titleSmall,
                            color = QCColors.Soft,
                        )
                        Slider(
                            value = v,
                            onValueChange = { floatState[pref] = it },
                            onValueChangeFinished = {
                                scope.launch { container.userPrefs.set(pref, floatState[pref] ?: pref.default) }
                            },
                            valueRange = 0.5f..2.0f,
                        )
                    }
                }
            }
            items(UserPrefs.StringPref.entries.toList()) { pref ->
                val v = stringState[pref] ?: pref.default
                if (pref == UserPrefs.StringPref.PREFERRED_TONE) {
                    // Dropdown over AssistantToneProfile enum.
                    var open by remember(pref) { mutableStateOf(false) }
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${pref.name}:",
                                style = MaterialTheme.typography.titleSmall,
                                color = QCColors.Soft,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { open = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QCColors.Edge, contentColor = QCColors.Soft,
                                ),
                            ) { Text(v) }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                AssistantToneProfile.entries.forEach { tone ->
                                    DropdownMenuItem(
                                        text = { Text("${tone.displayName} (${tone.name})") },
                                        onClick = {
                                            stringState[pref] = tone.name
                                            open = false
                                            scope.launch { container.userPrefs.set(pref, tone.name) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Simple read-only display for the other StringPrefs for now.
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${pref.name}: ${if (v.isBlank()) "(system)" else v}",
                            style = MaterialTheme.typography.titleSmall,
                            color = QCColors.Soft,
                        )
                    }
                }
            }
        }
    }
}
