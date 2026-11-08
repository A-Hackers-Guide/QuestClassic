# API — `ai` package

Pluggable LLM backends behind a one-method interface.

## `AIBackend`

```kotlin
sealed interface AIBackend {
    val id: String
    val displayName: String
    suspend fun chat(
        systemPrompt: String,
        history: List<ChatMessage>,
        apiKey: String,
        model: String?,
    ): Result<String>
}

enum class Role { user, assistant, system }
data class ChatMessage(val role: Role, val content: String)
```

- `chat(...)` must return a `Result<String>` — success = the assistant's reply text; failure = anything that went wrong.
- `model` is nullable so backends can fall back to their own default.
- `apiKey` is overloaded for Ollama (we repurpose it as the base URL since Ollama doesn't use auth).

## Shipped backends

### `ClaudeBackend`

- id: `claude`
- displayName: `Claude (Anthropic)`
- endpoint: `https://api.anthropic.com/v1/messages`
- default model: `claude-sonnet-5-5`
- auth: `x-api-key: <key>` + `anthropic-version: 2023-06-01` header
- request: `{model, max_tokens=1024, system, messages: [{role, content}]}`
- extracts first `text` content from the response

### `OpenAIBackend`

- id: `openai`
- displayName: `OpenAI`
- endpoint: `https://api.openai.com/v1/chat/completions`
- default model: `gpt-4o-mini`
- auth: `Authorization: Bearer <key>`
- request: `{model, messages: [{role, content}], max_tokens=1024}`
- extracts first `choices[0].message.content`

### `OllamaBackend`

- id: `ollama`
- displayName: `Ollama (local LAN)`
- endpoint: `${base}/api/chat` (base supplied by user)
- default model: `llama3.2`
- auth: none
- request: `{model, messages: [{role, content}], stream=false}`
- extracts `message.content`

## `AIRepository`

```kotlin
class AIRepository(context: Context, http: OkHttpClient) {
    val backends: List<AIBackend>
    fun backendById(id: String): AIBackend
    suspend fun load(): AIConfig
    suspend fun save(cfg: AIConfig)
}

data class AIConfig(
    val assistantName: String,
    val backendId: String,
    val model: String,
    val apiKey: String,
    val systemPrompt: String,
)
```

`AIRepository` wraps:

- the backend registry (`backends`),
- DataStore-backed config persistence,
- a `backendById` lookup that never returns null (falls back to the first registered backend).

## Adding a new backend

1. Create `app/src/main/kotlin/io/github/questclassic/app/ai/GeminiBackend.kt`.
2. Implement `AIBackend`.
3. Register in `AIRepository.backends`.
4. It appears in the backend picker automatically.

See [issue #6](https://github.com/A-Hackers-Guide/QuestClassic/issues/6) for a `good first issue` version.

## What we don't do here

- No conversation memory beyond what the user types in one session. (Chat history cleared on panel close.)
- No streaming. We wait for the full response before adding it to the history. Streaming is tracked informally for a future PR.
- No tool use. Models can't call our code; this is pure chat.
- No caching. Every `chat(...)` is a fresh HTTP request.

---

_Last reviewed against v0.1.0._
