package io.github.questclassic.app.core

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

/**
 * Discovers QuestClassic Companion hosts on the LAN via Android's NsdManager.
 *
 * Service type matches `companion/crates/discovery/src/lib.rs::SERVICE_TYPE`
 * (minus the trailing dot, which NsdManager doesn't want). TXT records
 * are mapped into a plain Map<String, String> for the caller to use.
 *
 * Lifecycle:
 *   - start() registers a discovery listener; emits Resolved rows on
 *     [discovered] as they resolve.
 *   - stop() tears it down. Always call this before process death.
 *
 * This is a side-effectful Android-only path — the pure parser + the
 * cross-platform DNS-SD constants live in companion/crates/discovery
 * (and in [DISCOVERY_SERVICE_TYPE] here for the Kotlin side).
 */
class NsdDiscoverer(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    data class Resolved(
        val serviceName: String,
        val host: String,
        val port: Int,
        val txt: Map<String, String>,
    )

    private val _discovered = MutableStateFlow<List<Resolved>>(emptyList())
    val discovered: StateFlow<List<Resolved>> = _discovered.asStateFlow()

    private var listener: NsdManager.DiscoveryListener? = null

    fun start() {
        if (listener != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) { /* no-op */ }
            override fun onDiscoveryStopped(serviceType: String) { /* no-op */ }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { /* no-op */ }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { /* no-op */ }
            override fun onServiceLost(info: NsdServiceInfo) {
                _discovered.value = _discovered.value.filter { it.serviceName != info.serviceName }
            }
            override fun onServiceFound(info: NsdServiceInfo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val cb = object : NsdManager.ServiceInfoCallback {
                        override fun onServiceUpdated(updated: NsdServiceInfo) = record(updated)
                        override fun onServiceLost() { /* duplicate of onServiceLost above */ }
                        override fun onServiceInfoCallbackUnregistered() { /* no-op */ }
                        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) { /* no-op */ }
                    }
                    runCatching {
                        nsd.registerServiceInfoCallback(info, java.util.concurrent.Executors.newSingleThreadExecutor(), cb)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) { /* no-op */ }
                        override fun onServiceResolved(resolved: NsdServiceInfo) = record(resolved)
                    })
                }
            }
        }
        listener = l
        runCatching { nsd.discoverServices(DISCOVERY_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stop() {
        val l = listener ?: return
        runCatching { nsd.stopServiceDiscovery(l) }
        listener = null
        _discovered.value = emptyList()
    }

    private fun record(info: NsdServiceInfo) {
        val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            info.hostAddresses.firstOrNull()?.hostAddress.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.host?.hostAddress.orEmpty()
        }
        val txt: Map<String, String> = info.attributes.orEmpty().mapValues { (_, v) ->
            v?.let { String(it, StandardCharsets.UTF_8) } ?: ""
        }
        val row = Resolved(info.serviceName, host, info.port, txt)
        _discovered.value = (_discovered.value.filterNot { it.serviceName == row.serviceName } + row)
    }

    companion object {
        /** DNS-SD service type (no trailing dot — NsdManager adds that). */
        const val DISCOVERY_SERVICE_TYPE: String = "_questclassic-companion._tcp."
    }
}
