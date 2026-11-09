package io.github.questclassic.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Enumerates the Meta-owned hosts this build is wired to talk to and resolves/pings each one from
 * the headset. Nothing privileged — standard `InetAddress.getByName` + `isReachable`. Educational.
 *
 * Every host in this list came out of the reverse-engineering (section 18 of the parent project's
 * report): decompiled DEX strings + `libshell.so` strings. If a host is in the list but refuses
 * to resolve on your headset, it's probably an internal-only or sandbox host we're not supposed
 * to be reaching anyway.
 */
object MetaHostsAudit {
    enum class Category { AUTH, GRAPH, GENAI, PAYMENTS, CDN, INTERNAL, TELEMETRY, OTA }

    data class Host(val name: String, val category: Category, val note: String = "")

    val all: List<Host> = listOf(
        // Identity / accounts
        Host("accountscenter.meta.com", Category.AUTH, "Meta Accounts Center PWA"),
        Host("secure.oculus.com", Category.AUTH, "Oculus login"),
        Host("graph.facebook-hardware.com", Category.AUTH, "Device-auth challenge/response endpoint"),

        // Primary GraphQL fabric
        Host("graph.oculus.com", Category.GRAPH, "Oculus GraphQL"),
        Host("graph.facebook.com", Category.GRAPH, "Facebook main GraphQL"),
        Host("graph.instagram.com", Category.GRAPH, "Instagram GraphQL (cross-linked accounts)"),
        Host("b-graph.facebook.com", Category.GRAPH, "Facebook bulk GraphQL"),
        Host("meta.graph.meta.com", Category.GRAPH, "Meta-branded GraphQL"),
        Host("graph-fallback.oculus.com", Category.GRAPH, "Oculus GraphQL fallback"),
        Host("realitylabs-graph.oculus.com", Category.GRAPH, "Reality Labs GraphQL"),

        // AI endpoints
        Host("genai-graph.facebook.com", Category.GENAI, "Meta AI GraphQL"),
        Host("genai-graph.instagram.com", Category.GENAI, "Meta AI GraphQL (Instagram-linked)"),

        // Payments
        Host("payments-graph.oculus.com", Category.PAYMENTS, ""),
        Host("payments-graph.facebook.com", Category.PAYMENTS, ""),
        Host("payments-graph.instagram.com", Category.PAYMENTS, ""),
        Host("payments-graph.whatsapp.com", Category.PAYMENTS, ""),
        Host("payments-graph.graph.meta.com", Category.PAYMENTS, ""),

        // CDN
        Host("scontent.xx.fbcdn.net", Category.CDN, "Facebook CDN"),
        Host("securecdn.oculus.com", Category.CDN, "Oculus CDN"),
        Host("rupload.facebook.com", Category.CDN, "Resumable upload"),

        // Internal — should NOT resolve from a consumer device
        Host("graph.intern.facebook.com", Category.INTERNAL, "Internal FB graph — expected: unreachable"),
        Host("m.intern.facebook.com", Category.INTERNAL, "Internal FB mobile — expected: unreachable"),
        Host("our.intern.facebook.com", Category.INTERNAL, "Internal FB portal — expected: unreachable"),

        // Telemetry / logging / crash reporting (opt-out candidates)
        Host("falco.facebook.com", Category.TELEMETRY, "FB logger backend (Scuba/Falco pipeline)"),
        Host("edge-chat.facebook.com", Category.TELEMETRY, "MQTT event channel"),
        Host("mqtt-mini.facebook.com", Category.TELEMETRY, "FB MQTT (mini-protocol)"),
        Host("crashlytics-oculus.facebook.com", Category.TELEMETRY, "Oculus crash reports"),
        Host("stats.g.doubleclick.net", Category.TELEMETRY, "DoubleClick analytics tag"),

        // OTA / config / feature flags / certificates
        Host("ota.oculus.com", Category.OTA, "Oculus OTA endpoint"),
        Host("oculuscdn.com", Category.OTA, "Oculus package mirror"),
        Host("config.facebook.com", Category.OTA, "Server-pushed feature flag payload"),
        Host("mobile.config.facebook.com", Category.OTA, "Mobile feature-flag mutation"),
        Host("fbpkg.oculus.com", Category.OTA, "Internal package format mirror"),
        Host("device-provisioning.oculus.com", Category.OTA, "First-boot provisioning"),

        // GenAI – the newer wave
        Host("llama-graph.facebook.com", Category.GENAI, "LLaMA-backed GraphQL"),
        Host("ai-chat.facebook.com", Category.GENAI, "AI chat REST endpoint"),

        // Hardware auth (device attestation)
        Host("hera.oculus.com", Category.AUTH, "Hardware attestation service"),
        Host("certs.oculus.com", Category.AUTH, "Oculus device cert chain host"),
    )

    data class Probe(val host: Host, val ip: String?, val reachable: Boolean, val latencyMs: Long?, val error: String?)

    suspend fun probe(host: Host, timeoutMs: Int = 1500): Probe = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        try {
            val addr = InetAddress.getByName(host.name)
            val reachable = runCatching { addr.isReachable(timeoutMs) }.getOrDefault(false)
            val ms = (System.nanoTime() - start) / 1_000_000
            Probe(host, addr.hostAddress, reachable, ms, null)
        } catch (t: Throwable) {
            Probe(host, null, false, null, t.message ?: t::class.simpleName)
        }
    }
}
