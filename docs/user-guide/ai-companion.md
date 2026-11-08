# AI Companion panel

A programmable AI assistant that lives beside you in VR. You name it, pick which LLM backend answers you, enter your own API key (or point it at an on-LAN Ollama), write a system prompt that defines its personality, and chat.

Zero telemetry. Zero shared keys. Your API key never leaves the device except in the HTTPS request to the backend you picked.

## First-launch setup

Six fields. All optional except backend + key.

| Field | What to put |
|---|---|
| **Name** | What you call your assistant. Defaults to "Classic". |
| **Backend** | Claude · OpenAI · Ollama (local LAN) |
| **Model** | Model ID string. Leave blank to use the backend's default (listed below). |
| **API key / base URL** | Your key for Claude/OpenAI, or the Ollama base URL (`http://10.0.0.1:11434` by default). |
| **System prompt** | The assistant's persona + instructions. Up to five lines on the setup form, no hard limit. |

Hit **Save** → chat UI.

## Backends + defaults

| Backend | Default model | Auth | Endpoint |
|---|---|---|---|
| Claude | `claude-sonnet-5-5` | `x-api-key` header | `https://api.anthropic.com/v1/messages` |
| OpenAI | `gpt-4o-mini` | `Authorization: Bearer …` | `https://api.openai.com/v1/chat/completions` |
| Ollama | `llama3.2` | none (LAN) | `${base}/api/chat` |

## Where state lives

`DataStore (Preferences)` at:

```
/data/data/io.github.questclassic.app/datastore/ai_config.preferences_pb
```

Five keys: `name`, `backend`, `model`, `api_key`, `prompt`. The file is excluded from Android's cloud backup by `data_extraction_rules.xml`. On factory reset or app uninstall, everything's gone.

## Config buttons

- **Configure** — return to the setup form to change anything.
- **Clear** — wipe current chat (not the config).

## Adding a new backend

One file. See [contributing/getting-started.md](../contributing/getting-started.md) and [issue #6](https://github.com/A-Hackers-Guide/QuestClassic/issues/6) for the `good first issue` version.

```kotlin
class GeminiBackend(private val http: OkHttpClient) : AIBackend {
    override val id = "gemini"
    override val displayName = "Google Gemini"
    override suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String> { /* POST → return assistant text */ }
}
```

Register it in `AIRepository.backends`. Done.

## Privacy posture

- API keys stored on-device only.
- No request logging (we never read your chats; nothing writes them anywhere).
- The system prompt you set is sent to the backend in every request — because that's how chat APIs work. If you don't want a provider to see something, don't put it in the system prompt or the chat.
- Ollama backend is the privacy-preserving option: your chats go to a machine on your LAN. Set `apiKey` to the Ollama URL; no actual key is needed.

## Why not Meta's on-device AI endpoints?

Meta's `genai-graph.facebook.com` and `genai-graph.instagram.com` endpoints we surfaced during the reverse-engineering (see [reverse-engineering/meta-auth-flow.md](../reverse-engineering/meta-auth-flow.md)) need a Meta device access token, which requires the `com.oculus.horizon.auth` signature permission. We can't hold that. You could still *call* them with a hand-crafted token stolen from another app, but that's a terms-of-service violation and we wouldn't ship it.

---

_Last reviewed against v0.1.0._
