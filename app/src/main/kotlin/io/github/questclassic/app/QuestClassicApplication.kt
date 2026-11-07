package io.github.questclassic.app

import android.app.Application
import io.github.questclassic.app.core.AppContainer

class QuestClassicApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
