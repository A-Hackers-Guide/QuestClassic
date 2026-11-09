package io.github.questclassic.app.ai

/**
 * Known model identifiers for each AI backend, with their rough context
 * sizes. Used by the Settings model-picker to show sensible defaults and
 * by [TokenBudgetEstimator.defaultBudgetFor] to size the UI budget bar.
 *
 * The catalog is intentionally conservative: we list models we've
 * actually tested or that are listed in the provider's current public
 * docs. Providers add + deprecate models on their own cadence, so a
 * model the user wants is NOT required to be in the catalog — the
 * settings form still accepts a hand-typed id. The catalog is a
 * convenience, not a gate.
 *
 * Context sizes are the provider's advertised ceiling; TokenBudget-
 * Estimator uses defaultBudgetFor(backendId) if you want a per-provider
 * default rather than per-model.
 *
 * Pure function; unit-testable.
 */
object BackendModelCatalog {

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val contextTokens: Int,
        val notes: String = "",
    )

    /** Models known at build time. Users may still enter a hand-typed id
     *  in the settings form — this list is for the picker, not a gate. */
    val MODELS: Map<String, List<ModelInfo>> = mapOf(
        "claude" to listOf(
            ModelInfo("claude-opus-5-5", "Opus 5.5", 200_000, "latest Opus"),
            ModelInfo("claude-sonnet-5-5", "Sonnet 5.5", 200_000, "latest Sonnet"),
            ModelInfo("claude-haiku-5-5", "Haiku 5.5", 200_000, "latest Haiku"),
            ModelInfo("claude-fable-5-1", "Fable 5.1", 200_000, "latest Fable (small)"),
        ),
        "openai" to listOf(
            ModelInfo("gpt-5-mini", "GPT-5 mini", 128_000, "small / fast"),
            ModelInfo("gpt-5", "GPT-5", 128_000, "full"),
            ModelInfo("gpt-4o", "GPT-4o", 128_000, "legacy"),
        ),
        "gemini" to listOf(
            ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", 1_000_000, "cheap / fast"),
            ModelInfo("gemini-2.0-pro", "Gemini 2.0 Pro", 1_000_000, "full"),
            ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", 2_000_000, "legacy, biggest context"),
        ),
        "ollama" to listOf(
            ModelInfo("llama3.2:3b", "Llama 3.2 (3B)", 8_192, "small CPU-friendly"),
            ModelInfo("qwen2.5:7b", "Qwen 2.5 (7B)", 32_768, "balanced"),
            ModelInfo("mistral:7b", "Mistral (7B)", 32_768, "GGUF defaults"),
        ),
    )

    fun models(backendId: String): List<ModelInfo> = MODELS[backendId.lowercase()].orEmpty()

    /** Find a model by id across every backend; useful when the user
     *  types an id and we want to look up the context size. */
    fun findById(id: String): ModelInfo? =
        MODELS.values.flatten().firstOrNull { it.id == id }

    /** Default model id per backend (first entry). */
    fun defaultFor(backendId: String): String? = models(backendId).firstOrNull()?.id
}
