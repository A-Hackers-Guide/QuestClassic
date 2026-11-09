package io.github.questclassic.app.core

import android.content.Context
import io.github.questclassic.app.ai.AIRepository
import io.github.questclassic.app.ai.ChatHistoryStore
import io.github.questclassic.app.os.DeviceIntrospection
import io.github.questclassic.app.os.SystemPropertyProbe
import io.github.questclassic.app.update.SelfUpdater
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One-stop dependency container. Nothing fancy — we hand-wire because the dependency graph
 * is small and we want zero build-time magic (DI libraries add build time we don't need here).
 */
class AppContainer(private val app: Context) {
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val appList: AppListRepository = AppListRepository(app)
    val haptics: ControllerHaptics = ControllerHaptics(app)
    val diagnostics: DiagnosticsEngine = DiagnosticsEngine(app, haptics)
    val introspection: DeviceIntrospection = DeviceIntrospection(app)
    val updater: SelfUpdater = SelfUpdater(app, http)
    val ai: AIRepository = AIRepository(app, http)
    val chatHistory: ChatHistoryStore = ChatHistoryStore(app)
    val payloadVerifier: PayloadVerifier = PayloadVerifier(app)
    val systemProbe: SystemPropertyProbe = SystemPropertyProbe()
    val rootDetector: RootDetector = RootDetector(app)
    val restore: FunctionalityRestore = FunctionalityRestore(app, haptics, diagnostics)
    val devices: DeviceRegistry = DeviceRegistry(app)
}
