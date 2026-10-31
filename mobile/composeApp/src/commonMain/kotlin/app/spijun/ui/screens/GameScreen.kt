package app.spijun.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import app.spijun.ui.platform.AlertButton
import app.spijun.ui.platform.AlertButtonKind
import app.spijun.ui.platform.NativeAlert
import app.spijun.ui.platform.PlatformBackHandler
import app.spijun.ui.platform.isIos
import org.jetbrains.compose.resources.stringResource

/** Immersive in-game flow: no tab bar, every screen has a close button that asks first. */
@Composable
fun GameScreen(game: GameState, controller: AppController) {
    var confirmQuit by remember { mutableStateOf(false) }
    val askQuit = { confirmQuit = true }
    // Back asks before quitting; once the game is over there is nothing to lose, so it goes Home.
    PlatformBackHandler(true) { if (game.stage is Stage.GameOver) controller.goHome() else confirmQuit = true }

    // Crossfade where each slot renders its own state: the outgoing screen keeps the old stage while
    // it fades, and effects (haptics, the deal) run once, in the incoming screen only.
    AnimatedContent(
        targetState = game,
        transitionSpec = { fadeIn(tween(SpyMotion.STANDARD_MS)) togetherWith fadeOut(tween(SpyMotion.STANDARD_MS)) },
        contentKey = ::stageKey,
        label = "game-stage",
    ) { g ->
        when (g.stage) {
            is Stage.Reveal, Stage.Discussion -> TableStage(g, controller, askQuit)
            is Stage.Voting -> VoteScreen(g, controller, askQuit)
            is Stage.Result -> ResultScreen(g, controller, askQuit)
            is Stage.GameOver -> GameOverScreen(g, controller, onNewGame = { controller.goHome() })
        }
    }

    if (confirmQuit && isIos) {
        NativeAlert(
            title = stringResource(Res.string.quit_title),
            message = stringResource(Res.string.quit_body),
            buttons = listOf(
                AlertButton(stringResource(Res.string.quit_confirm), AlertButtonKind.Destructive) { confirmQuit = false; controller.goHome() },
                AlertButton(stringResource(Res.string.quit_keep), AlertButtonKind.Cancel) { confirmQuit = false },
            ),
            sheet = true,
        )
    } else if (confirmQuit) {
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

/** Reveal and discussion share one table so the camera can pull back instead of cutting. */
private fun stageKey(game: GameState): String = when (val stage = game.stage) {
    is Stage.Reveal, Stage.Discussion -> "table"
    is Stage.Voting -> "vote"
    is Stage.Result -> "result-${game.round}-${game.eliminated.size}-${stage.isRevote}"
    is Stage.GameOver -> "over"
}
