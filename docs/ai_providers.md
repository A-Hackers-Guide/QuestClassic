# AI providers

QuestClassic ships three AI backends out of the box. The user picks one from the "AI Companion" panel, enters their own API key (or the Ollama URL), names their assistant, and writes a system prompt — all of it lives in device-local DataStore, never leaves the device except in HTTPS requests to the chosen provider.

## Built-in backends

| ID | Provider | Default model | Auth | Endpoint |
|---|---|---|---|---|
| `claude` | Anthropic | `claude-sonnet-5-5` | `x-api-key` header | `https://api.anthropic.com/v1/messages` |
| `openai` | OpenAI | `gpt-4o-mini` | `Authorization: Bearer <key>` | `https://api.openai.com/v1/chat/completions` |
| `ollama` | Local Ollama server | `llama3.2` | (none — point at your LAN URL) | `${base}/api/chat` |

The user can override the model string per backend.

## Adding a new backend

1. Create `ai/MyBackend.kt` that implements `AIBackend`:
   ```kotlin
   class MyBackend(private val http: OkHttpClient) : AIBackend {
       override val id = "mybackend"
       override val displayName = "My backend"
       override suspend fun chat(...) : Result<String> { ... }
   }
   ```
2. Register it in `AIRepository.backends`.
3. That's it — it shows up in the backend picker automatically.

## Why not auto-pick a model

We considered a "recommended default" but the cost/quality trade-off is yours to make. We print the suggested defaults in the placeholder and let you override.

## Privacy

- API keys are stored in `DataStore (Preferences)` under the app's private data directory. They never appear in logs.
- No telemetry. The only network calls the AI panel makes are to the backend you configured.
- The `DataStore` file is excluded from Android's cloud backup (`data_extraction_rules.xml`), so your key never leaves the device by that path either.
