package io.github.questclassic.app.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.ai.AIConfig
import io.github.questclassic.app.ai.ChatMessage
import io.github.questclassic.app.ai.Role
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

@Composable
fun AIAssistantPanel(container: AppContainer, onClose: () -> Unit) {
    var cfg by remember { mutableStateOf<AIConfig?>(null) }
    var configuring by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<ChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var panelPrefs by remember { mutableStateOf(io.github.questclassic.app.ai.AIPanelPrefsSnapshot()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val c = container.ai.load()
        cfg = c
        if (c.apiKey.isBlank() && c.backendId != "ollama") configuring = true
        panelPrefs = container.aiPanelPrefs.load()
        // Restore conversation for the active backend.
        val saved = container.chatHistory.load(c.backendId)
        if (saved.isNotEmpty()) {
            history.clear()
            history.addAll(saved)
        }
    }

    // Save history after every mutation so a crash doesn't lose the thread.
    LaunchedEffect(Unit) {
        snapshotFlow { history.toList() }.collect { snap ->
            val id = cfg?.backendId ?: return@collect
            container.chatHistory.save(id, snap)
        }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "AI Companion",
            subtitle = cfg?.assistantName?.let {
                "Hey $it · ${container.ai.backendById(cfg!!.backendId).displayName}"
            } ?: "…",
            onClose = onClose,
        )
        Spacer(Modifier.height(12.dp))

        if (configuring || cfg == null) {
            AIConfigForm(
                initial = cfg ?: AIConfig("Classic", "claude", "", "", "You are the user's VR companion."),
                backends = container.ai.backends.map { it.id to it.displayName },
                onSave = { newCfg ->
                    scope.launch {
                        container.ai.save(newCfg)
                        cfg = newCfg
                        configuring = false
                    }
                },
            )
            return
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { configuring = true },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Configure") }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = {
                    history.clear()
                    cfg?.backendId?.let { id ->
                        scope.launch { container.chatHistory.clear(id) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Clear") }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = {
                    val next = panelPrefs.copy(voiceEnabled = !panelPrefs.voiceEnabled)
                    panelPrefs = next
                    scope.launch { container.aiPanelPrefs.save(next) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (panelPrefs.voiceEnabled) QCColors.Teal else QCColors.Edge,
                    contentColor = if (panelPrefs.voiceEnabled) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (panelPrefs.voiceEnabled) "Voice: on" else "Voice: off") }
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                onClick = {
                    val next = panelPrefs.copy(textEnabled = !panelPrefs.textEnabled)
                    panelPrefs = next
                    scope.launch { container.aiPanelPrefs.save(next) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (panelPrefs.textEnabled) QCColors.Teal else QCColors.Edge,
                    contentColor = if (panelPrefs.textEnabled) QCColors.Space else QCColors.Soft,
                ),
            ) { Text(if (panelPrefs.textEnabled) "Text: on" else "Text: off") }
        }
        Spacer(Modifier.height(10.dp))

        if (panelPrefs.textEnabled) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(history) { msg ->
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = if (msg.role == Role.user) QCColors.Amber else QCColors.Teal,
                    ) {
                        Column {
                            Text(
                                (if (msg.role == Role.user) "YOU" else cfg!!.assistantName.uppercase()),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (msg.role == Role.user) QCColors.Amber else QCColors.Teal,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(msg.content, style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft)
                        }
                    }
                }
            }
        } else {
            // Audio-only mode: show the last assistant line as a short label,
            // the rest of the vertical space stays blank.
            GlassPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column {
                    Text("AUDIO-ONLY MODE",
                        style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        history.lastOrNull { it.role == Role.assistant }?.content
                            ?: "No reply yet.",
                        style = MaterialTheme.typography.bodyLarge, color = QCColors.Soft,
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask…", color = QCColors.Dim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = QCColors.Soft,
                    unfocusedTextColor = QCColors.Soft,
                    focusedBorderColor = QCColors.Teal,
                    unfocusedBorderColor = QCColors.Edge,
                ),
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Button(
                enabled = !sending && draft.isNotBlank(),
                onClick = {
                    val text = draft.trim(); draft = ""
                    history += ChatMessage(Role.user, text)
                    sending = true
                    scope.launch {
                        val backend = container.ai.backendById(cfg!!.backendId)
                        val reply = backend.chat(
                            systemPrompt = cfg!!.systemPrompt,
                            history = history.toList(),
                            apiKey = cfg!!.apiKey,
                            model = cfg!!.model.ifBlank { null },
                        )
                        reply.onSuccess { history += ChatMessage(Role.assistant, it) }
                            .onFailure { history += ChatMessage(Role.assistant, "⚠ ${it.message}") }
                        sending = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text(if (sending) "…" else "Send") }
        }
    }
}

@Composable
private fun AIConfigForm(
    initial: AIConfig,
    backends: List<Pair<String, String>>,
    onSave: (AIConfig) -> Unit,
) {
    var backend by remember { mutableStateOf(initial.backendId) }
    var name by remember { mutableStateOf(initial.assistantName) }
    var model by remember { mutableStateOf(initial.model) }
    var key by remember { mutableStateOf(initial.apiKey) }
    var prompt by remember { mutableStateOf(initial.systemPrompt) }
    var perBackendNames by remember { mutableStateOf(initial.perBackendNames) }
    var perBackendKeys by remember { mutableStateOf(initial.perBackendKeys) }
    var backendMenu by remember { mutableStateOf(false) }

    // When the user switches backend, stash the previous name + key under
    // the OLD backend id, then restore the saved name + key for the NEW one.
    fun switchBackend(newId: String) {
        if (newId == backend) { backendMenu = false; return }
        perBackendNames = perBackendNames.toMutableMap().apply { put(backend, name) }
        perBackendKeys = perBackendKeys.toMutableMap().apply { put(backend, key) }
        backend = newId
        name = perBackendNames[newId] ?: defaultWakeNameFor(newId)
        key = perBackendKeys[newId].orEmpty()
        backendMenu = false
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("ASSISTANT SETUP", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { backendMenu = true },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Edge, contentColor = QCColors.Soft),
            ) { Text("Provider: ${backends.firstOrNull { it.first == backend }?.second ?: backend}") }
            DropdownMenu(expanded = backendMenu, onDismissRequest = { backendMenu = false }) {
                backends.forEach { (id, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { switchBackend(id) })
                }
            }
            Spacer(Modifier.height(8.dp))
            AIField(
                label = "Wake name — say \"Hey ${name.ifBlank { "<name>" }} …\"",
                value = name,
            ) { name = it }
            Spacer(Modifier.height(8.dp))
            AIField(
                label = "Model (optional)",
                value = model,
                placeholder = modelPlaceholderFor(backend),
            ) { model = it }
            Spacer(Modifier.height(8.dp))
            AIField(
                label = keyLabelFor(backend),
                value = key,
                placeholder = keyPlaceholderFor(backend),
            ) { key = it }
            Spacer(Modifier.height(8.dp))
            AIField("System prompt (your assistant's personality)", prompt, maxLines = 5) { prompt = it }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    onSave(
                        AIConfig(
                            assistantName = name.ifBlank { defaultWakeNameFor(backend) },
                            backendId = backend,
                            model = model,
                            apiKey = key,
                            systemPrompt = prompt,
                            perBackendNames = perBackendNames,
                            perBackendKeys = perBackendKeys,
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text("Save") }
        }
    }
}

/** Default wake names when the user hasn't set one yet. */
private fun defaultWakeNameFor(backendId: String): String = when (backendId) {
    "claude" -> "Classic"
    "openai" -> "Sage"
    "gemini" -> "Nova"
    "ollama" -> "Local"
    else -> "Classic"
}

private fun keyLabelFor(backendId: String): String = when (backendId) {
    "ollama" -> "Ollama base URL"
    "gemini" -> "Google API key"
    else -> "API key"
}

private fun keyPlaceholderFor(backendId: String): String = when (backendId) {
    "ollama" -> "http://10.0.0.1:11434"
    "openai" -> "sk-…"
    "claude" -> "sk-ant-…"
    "gemini" -> "AIza…"
    else -> ""
}

private fun modelPlaceholderFor(backendId: String): String = when (backendId) {
    "claude" -> "e.g. claude-sonnet-5-5"
    "openai" -> "e.g. gpt-4o-mini"
    "gemini" -> "e.g. gemini-2.0-flash"
    "ollama" -> "e.g. llama3.2"
    else -> ""
}

@Composable
private fun AIField(label: String, value: String, placeholder: String = "", maxLines: Int = 1, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = QCColors.Dim) },
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = QCColors.Soft,
                unfocusedTextColor = QCColors.Soft,
                focusedBorderColor = QCColors.Teal,
                unfocusedBorderColor = QCColors.Edge,
            ),
        )
    }
}

