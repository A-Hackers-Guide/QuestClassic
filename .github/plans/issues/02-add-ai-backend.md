---
title: "good first issue: add a new AI backend (Gemini / Mistral / Groq)"
labels: [enhancement, good first issue, ai]
---
Each AI provider is a one-function implementation of the `AIBackend` interface. Adding one is self-contained, no cross-cutting changes, and makes a great first PR.

### How

1. New file `app/src/main/kotlin/io/github/questclassic/app/ai/GeminiBackend.kt` (or Mistral/Groq).
2. Implement:
   ```kotlin
   class GeminiBackend(private val http: OkHttpClient) : AIBackend {
       override val id = "gemini"
       override val displayName = "Google Gemini"
       override suspend fun chat(systemPrompt: String, history: List<ChatMessage>,
                                 apiKey: String, model: String?): Result<String> {
           // POST to https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
       }
   }
   ```
3. Register it in `AIRepository.backends`.
4. That's it. The backend picker and "Model" field work automatically.

### Notes

- Keep deps limited to what the other backends already use (`OkHttp` + `kotlinx.serialization`).
- API-key placeholder: match the provider's actual key format (`AIza…` for Gemini, `mistral-…` for Mistral, `gsk_…` for Groq).
- Document the default model in `docs/ai_providers.md`.
- Users supply their own key; no shared keys in this repo.

### Done when

- [ ] Backend class added.
- [ ] Registered in `AIRepository`.
- [ ] Row in the `docs/ai_providers.md` table.
- [ ] Manually tested with a real API key on-device.
