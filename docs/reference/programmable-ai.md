# Programmable AI — tool registry

The AI companion is more than a chat window. `AgentToolRegistry` is the allowlisted catalog of safe functions the model can call via its provider's function-calling / tool-use feature (`tools` for Anthropic + OpenAI, `functions` for Gemini).

## Why an explicit registry

- The model never sees anything the user hasn't opted into.
- Each tool declares its own params + safety level, so the UI can show "this tool can do X — allow?" before the first call.
- Features register their tools; the AI module stays decoupled from the rest of the app.

## Shape

```kotlin
AgentToolRegistry.register(AgentTool(
    id = "device.battery",
    displayName = "Battery level",
    description = "Reads the current battery percentage.",
    category = AgentToolCategory.DEVICE_INFO,
    safety = SafetyLevel.READ_ONLY,
    params = emptyList(),
    invoke = { _ ->
        val pct = // ... read battery ...
        ToolResult(ok = true, summary = "$pct%", payload = JsonPrimitive(pct))
    },
))
```

## Categories

| Category | Examples |
|---|---|
| `DEVICE_INFO` | battery, uptime, installed apps, free memory |
| `NAVIGATION` | open launcher, start an app, flip to a panel |
| `FEEDBACK` | buzz pattern, speak via TTS |
| `META` | list providers, set active provider, rename assistant |
| `DEVICES` | enumerate registered devices, mark trusted |
| `DISPLAYS` | add/remove virtual monitors (requires companion) |
| `EMULATORS` | list installed emulators, launch one |

## Safety levels

| Level | Default | Confirms? |
|---|---|---|
| `READ_ONLY` | allowed | no |
| `WRITE_LOCAL` | allowed | no |
| `NETWORK` | allowed (user-approved endpoints only) | no |
| `SIDE_EFFECT` | allowed | yes (one-line chip) |
| `CROSS_DEVICE` | **denied by default** | yes (full confirmation + opt-in in Settings) |

The AI panel never bypasses these — the confirmation UI lives on the headset, not in the model's reply path.

## Registering vs invoking

Features register during `AppContainer` construction (or later, if the feature is lazily loaded). The AI panel builds the tool list for the current provider at send-time, filtering by:

- user's enabled set (default: every `READ_ONLY` and `WRITE_LOCAL`),
- the provider's capability (Gemini supports tools; older Ollama models don't; the registry ships a `supportsTools` capability check on each `AIBackend`).

## Unknowns / future work

- **Streaming tool calls** — today we call each tool to completion then feed the result back. Streaming would let long tools (LAN scan) update the chat as they progress; deferred.
- **Rate limiting** — tools are naturally bounded by the model's token budget, but a rogue prompt could ask for 100 battery reads in a row. Follow-up: per-tool call-count budget.
- **Provider parity** — Gemini + Claude + OpenAI tool-use schemas differ; the registry serializes a single shape, each backend translates to the provider's JSON.
