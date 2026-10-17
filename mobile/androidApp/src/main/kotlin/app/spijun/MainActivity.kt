package app.spijun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import app.spijun.ui.AppController
import app.spijun.ui.SpijunApp

/** Keeps the game across Activity recreation (resize, fold, display size); not across process death. */
class GameHolder : ViewModel() {
    val controller = AppController()
}

class MainActivity : ComponentActivity() {
    private val holder: GameHolder by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SpijunApp(holder.controller) }
    }
}
