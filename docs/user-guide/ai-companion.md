# AI Companion panel

A programmable AI assistant that lives beside you in VR. You name it, pick which LLM backend answers you, enter your own API key (or point it at an on-LAN Ollama), write a system prompt that defines its personality, and chat.

Zero telemetry. Zero shared keys. Your API key never leaves the device except in the HTTPS request to the backend you picked.

## Multi-provider wake names

Each backend remembers its own **wake name** + API key, so you can set up several and route by the "Hey {name}" phrase (push-to-talk today; on-device wake-word detection is tracked in [issue #38](https://github.com/A-Hackers-Guide/QuestClassic/issues/38)).

| Backend | Default wake name | Default model |
|---|---|---|
| Claude (Anthropic) | **Classic** | `claude-sonnet-5-5` |
| OpenAI (ChatGPT) | **Sage** | `gpt-4o-mini` |
| Gemini (Google) | **Nova** | `gemini-2.0-flash` |
| Ollama (LAN) | **Local** | `llama3.2` |

Switching the active provider in the setup form restores that provider's saved name + key — switching back doesn't force you to re-type either one.

## First-launch setup

| Field | What to put |
|---|---|
| **Provider** | Claude · OpenAI · Gemini · Ollama |
| **Wake name** | What you say to address this provider. Shown in the header as "Hey …". |
| **Model** | Model ID string. Leave blank for the backend default above. |
| **API key / base URL** | Your key for Claude / OpenAI / Gemini, or the Ollama base URL. |
| **System prompt** | The assistant's persona + instructions. |

Hit **Save** → chat UI.

## Backends + endpoints

| Backend | Auth | Endpoint |
|---|---|---|
| Claude | `x-api-key` header | `https://api.anthropic.com/v1/messages` |
| OpenAI | `Authorization: Bearer …` | `https://api.openai.com/v1/chat/completions` |
| Gemini | `?key=…` query param | `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` |
| Ollama | none (LAN) | `${base}/api/chat` |

## Where state lives

`DataStore (Preferences)` at:

```
/data/data/io.github.questclassic.app/datastore/ai_config.preferences_pb
```

Keys: `name`, `backend`, `model`, `api_key`, `prompt`, `per_backend_names` (JSON map), `per_backend_keys` (JSON map). The file is excluded from Android's cloud backup by `data_extraction_rules.xml`. On factory reset or app uninstall, everything's gone.

## Config buttons

- **Configure** — return to the setup form to change anything.
- **Clear** — wipe current chat (not the config).

## Adding a new backend

One file. See [contributing/getting-started.md](../contributing/getting-started.md) and [issue #6](https://github.com/A-Hackers-Guide/QuestClassic/issues/6) for the `good first issue` version.

```kotlin
class MyBackend(private val http: OkHttpClient) : AIBackend {
    override val id = "mybackend"
    override val displayName = "My Backend"
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

- API keys stored on-device only, under the app-private DataStore.
- No request logging (we never read your chats; nothing writes them anywhere).
- The system prompt you set is sent to the backend in every request — because that's how chat APIs work. If you don't want a provider to see something, don't put it in the system prompt or the chat.
- **Ollama is the privacy-preserving option:** your chats go to a machine on your LAN. Set the "URL" field to the Ollama base URL; no actual key is needed.

## Why not Meta's on-device AI endpoints?

Meta's `genai-graph.facebook.com` and `genai-graph.instagram.com` endpoints we surfaced during the reverse-engineering (see [reverse-engineering/meta-auth-flow.md](../reverse-engineering/meta-auth-flow.md)) need a Meta device access token, which requires the `com.oculus.horizon.auth` signature permission. We can't hold that. You could still *call* them with a hand-crafted token stolen from another app, but that's a terms-of-service violation and we wouldn't ship it.

---

_Last reviewed against v0.2.0._
