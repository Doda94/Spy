package app.spijun.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import app.spijun.engine.GameState
import app.spijun.engine.Stage
import app.spijun.resources.Res
import app.spijun.resources.quit_body
import app.spijun.resources.quit_confirm
import app.spijun.resources.quit_keep
import app.spijun.resources.quit_title
import app.spijun.ui.AppController
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyMotion
import app.spijun.ui.platform.PlatformBackHandler
import org.jetbrains.compose.resources.stringResource

/** Immersive in-game flow: no tab bar, every screen has a close button that asks first. */
@Composable
fun GameScreen(game: GameState, controller: AppController) {
    var confirmQuit by remember { mutableStateOf(false) }
    val askQuit = { confirmQuit = true }
    PlatformBackHandler(game.stage !is Stage.GameOver) { confirmQuit = true }

    // Reveal and discussion share one table so the camera can pull back instead of cutting.
    val key = when (game.stage) {
        is Stage.Reveal, Stage.Discussion -> "table"
        is Stage.Voting -> "vote"
        is Stage.Result -> "result-${game.round}-${game.eliminated.size}-${(game.stage as Stage.Result).isRevote}"
        is Stage.GameOver -> "over"
    }
    Crossfade(key, animationSpec = tween(SpyMotion.STANDARD_MS), label = "game-stage") {
        when (game.stage) {
            is Stage.Reveal, Stage.Discussion -> TableStage(game, controller, askQuit)
            is Stage.Voting -> VoteScreen(game, controller, askQuit)
            is Stage.Result -> ResultScreen(game, controller, askQuit)
            is Stage.GameOver -> GameOverScreen(game, controller, onNewGame = { controller.goHome() })
        }
    }

    if (confirmQuit) {
        val c = Spy.colors
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            containerColor = c.surface2,
            title = { Text(stringResource(Res.string.quit_title), style = Spy.type.titleMedium, color = c.text) },
            text = { Text(stringResource(Res.string.quit_body), style = Spy.type.body, color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false }) {
                    Text(stringResource(Res.string.quit_keep), color = c.text, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false; controller.goHome() }) {
                    Text(stringResource(Res.string.quit_confirm), color = c.danger)
                }
            },
        )
    }
}
