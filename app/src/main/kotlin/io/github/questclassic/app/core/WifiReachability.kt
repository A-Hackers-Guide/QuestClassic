package io.github.questclassic.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Quick 'is this URL reachable from the headset?' check. Used by the
 * AI settings form to verify the user's Ollama endpoint before they
 * save a bad one and get a cryptic 'connection refused' on first send.
 *
 * - HEAD / GET with a short timeout (default 3s).
 * - Any 2xx or 3xx counts as reachable.
 * - 4xx is 'the server answered, you just hit the wrong path' — still
 *   reachable; the user's URL is at least alive.
 * - Timeout / network error → Unreachable with the exception message.
 *
 * Runs on Dispatchers.IO; suspending.
 */
class WifiReachability(http: OkHttpClient = OkHttpClient()) {

    private val client: OkHttpClient = http.newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    sealed interface Verdict {
        data class Reachable(val statusCode: Int, val latencyMs: Long) : Verdict
        data class Unreachable(val reason: String) : Verdict
    }

    suspend fun probe(url: String): Verdict = withContext(Dispatchers.IO) {
        val normalised = url.trim().ifEmpty { return@withContext Verdict.Unreachable("empty URL") }
        val sanitised = if (normalised.startsWith("http://") || normalised.startsWith("https://")) normalised
            else "http://$normalised"
        val req = runCatching {
            Request.Builder().url(sanitised).get().build()
        }.getOrElse { return@withContext Verdict.Unreachable("bad URL: ${it.message}") }
        val start = System.nanoTime()
        try {
            client.newCall(req).execute().use { resp ->
                val ms = (System.nanoTime() - start) / 1_000_000
                Verdict.Reachable(resp.code, ms)
            }
        } catch (t: Throwable) {
            Verdict.Unreachable(t.message ?: t::class.simpleName.orEmpty())
        }
    }
}
