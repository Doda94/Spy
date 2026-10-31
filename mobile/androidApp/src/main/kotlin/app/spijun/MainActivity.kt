package app.spijun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.content.Intent
import androidx.lifecycle.ViewModel
import app.spijun.ui.AppShell
import app.spijun.ui.IncomingLinks
import app.spijun.ui.SpijunApp

/** Keeps the game and tabs across Activity recreation (resize, fold, display size); not across process death. */
class GameHolder : ViewModel() {
    val shell = AppShell()
}

class MainActivity : ComponentActivity() {
    private val holder: GameHolder by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The link that launched us; a recreation after that must not deliver it a second time.
        if (savedInstanceState == null) deliver(intent)
        setContent { SpijunApp(holder.shell) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deliver(intent)
    }

    private fun deliver(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString?.let(IncomingLinks::deliver)
    }
}
