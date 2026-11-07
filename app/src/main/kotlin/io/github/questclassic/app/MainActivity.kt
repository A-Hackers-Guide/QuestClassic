package io.github.questclassic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.questclassic.app.ui.nostalgic.NostalgicHomeScreen
import io.github.questclassic.app.ui.theme.QuestClassicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as QuestClassicApplication).container
        setContent {
            QuestClassicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm = remember { container }
                    NostalgicHomeScreen(container = vm)
                }
            }
        }
    }
}
