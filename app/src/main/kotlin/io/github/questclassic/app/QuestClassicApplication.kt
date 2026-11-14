package io.github.questclassic.app

import android.app.Application
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.BootTiming
import io.github.questclassic.app.core.LogRingBuffer

class QuestClassicApplication : Application() {
    /** Captures startup-phase timings so DiagnosticsBundle can attach
     *  them when a user reports "the app takes 20s to open". */
    val bootTiming: BootTiming = BootTiming()

    /** In-process log ring that mirrors a subset of our own Log.* calls
     *  so the diagnostics bundle can attach the last 256 lines without
     *  needing adb. SecretRedactor runs on write; see LogRingBuffer. */
    val logRing: LogRingBuffer = LogRingBuffer()

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        bootTiming.mark("application.onCreate.enter")
        container = AppContainer(this)
        bootTiming.mark("appcontainer.ready")
        logRing.info("app", "QuestClassic boot — container ready")
    }
}
