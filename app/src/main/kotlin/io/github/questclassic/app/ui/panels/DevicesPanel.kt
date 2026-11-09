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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.Device
import io.github.questclassic.app.core.DeviceKind
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * Devices panel — CRUD over the DeviceRegistry.
 *
 * Shows every registered device grouped by kind. Add a new one via the
 * form at the top; tap a row to rename, toggle trust, or remove. The
 * registry persists to DataStore on every write.
 */
@Composable
fun DevicesPanel(container: AppContainer, onClose: () -> Unit) {
    val devices = remember { mutableStateListOf<Device>() }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        val loaded = container.devices.load()
        devices.clear()
        devices.addAll(loaded)
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Devices",
            subtitle = "${devices.size} registered",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { adding = !adding },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adding) QCColors.Teal else QCColors.Edge,
                    contentColor = if (adding) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (adding) "Cancel" else "Add device") }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = { scope.launch { refresh() } },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge,
                    contentColor = QCColors.Soft,
                ),
            ) { Text("Refresh") }
        }
        Spacer(Modifier.height(12.dp))

        if (adding) {
            AddDeviceForm(onSave = { d ->
                scope.launch {
                    runCatching { container.devices.add(d) }
                        .onSuccess { refresh(); adding = false }
                }
            })
            Spacer(Modifier.height(12.dp))
        }

        if (devices.isEmpty() && !adding) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "NO DEVICES REGISTERED",
                        style = MaterialTheme.typography.labelLarge,
                        color = QCColors.Dim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap 'Add device' to pair a companion desktop, " +
                            "an on-LAN Ollama server, or anything else " +
                            "QuestClassic should know about.",
                        style = MaterialTheme.typography.bodySmall,
                        color = QCColors.Soft,
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(devices) { d ->
                DeviceRow(
                    device = d,
                    onToggleTrust = {
                        scope.launch {
                            container.devices.setTrusted(d.id, !d.trusted)
                            refresh()
                        }
                    },
                    onRemove = {
                        scope.launch {
                            container.devices.remove(d.id); refresh()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AddDeviceForm(onSave: (Device) -> Unit) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(DeviceKind.COMPANION_DESKTOP) }
    var kindMenu by remember { mutableStateOf(false) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("NEW DEVICE", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
            Spacer(Modifier.height(10.dp))
            AIFieldLocal("Id (unique)", id) { id = it }
            Spacer(Modifier.height(8.dp))
            AIFieldLocal("Display name", name) { name = it }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { kindMenu = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge,
                    contentColor = QCColors.Soft,
                ),
            ) { Text("Kind: ${kind.name}") }
            DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                DeviceKind.values().forEach { k ->
                    DropdownMenuItem(text = { Text(k.name) }, onClick = { kind = k; kindMenu = false })
                }
            }
            Spacer(Modifier.height(8.dp))
            AIFieldLocal("Address (IP / mDNS / MAC)", address) { address = it }
            Spacer(Modifier.height(14.dp))
            Button(
                enabled = id.isNotBlank() && name.isNotBlank(),
                onClick = {
                    onSave(Device(
                        id = id.trim(),
                        displayName = name.trim(),
                        kind = kind,
                        address = address.trim(),
                    ))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Teal,
                    contentColor = QCColors.Space,
                ),
            ) { Text("Save") }
        }
    }
}

@Composable
private fun DeviceRow(
    device: Device,
    onToggleTrust: () -> Unit,
    onRemove: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(device.displayName, style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft)
                Text(
                    "${device.kind.name} · ${device.address.ifBlank { "<no address>" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QCColors.Dim,
                )
            }
            Button(
                onClick = onToggleTrust,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.trusted) QCColors.Teal else QCColors.Edge,
                    contentColor = if (device.trusted) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (device.trusted) "Trusted" else "Trust") }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QCColors.Edge,
                    contentColor = QCColors.Soft,
                ),
            ) { Text("Remove") }
        }
    }
}

@Composable
private fun AIFieldLocal(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = QCColors.Soft,
                unfocusedTextColor = QCColors.Soft,
                focusedBorderColor = QCColors.Teal,
                unfocusedBorderColor = QCColors.Edge,
            ),
        )
    }
}
