package app.spijun.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.engine.GameState
import app.spijun.engine.Outcome
import app.spijun.engine.Stage
import app.spijun.resources.Res
import app.spijun.resources.a11y_knocked_out
import app.spijun.resources.quit_a11y
import app.spijun.resources.result_no_votes
import app.spijun.resources.result_out
import app.spijun.resources.result_out_stamp
import app.spijun.resources.result_pill
import app.spijun.resources.result_revote
import app.spijun.resources.result_roles_end
import app.spijun.resources.result_round_n
import app.spijun.resources.result_next_round
import app.spijun.resources.result_tie
import app.spijun.resources.result_tie_again
import app.spijun.resources.result_tie_again_cards
import app.spijun.resources.result_tie_body
import app.spijun.resources.result_votes
import app.spijun.resources.votes
import app.spijun.ui.AppController
import app.spijun.ui.design.CardBack
import app.spijun.ui.design.LocalReduceMotion
import app.spijun.ui.design.Panel
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyHaptic
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyMotion
import app.spijun.ui.design.SpyPill
import app.spijun.ui.design.StickyFooter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** "A, B or C": the last name goes in the second slot so Croatian "ili" reads right. */
@Composable
private fun joinedNames(names: List<String>): Pair<String, String> =
    names.dropLast(1).joinToString(", ") to names.last()

@Composable
fun ResultScreen(game: GameState, controller: AppController, onQuit: () -> Unit) {
    val result = game.stage as Stage.Result
    val c = Spy.colors
    val haptics = Spy.haptics
    val reduce = LocalReduceMotion.current
    val outcome = result.outcome
    // Suspense → knock-out → text (motion.md §4). Ties skip the suspense.
    var revealed by remember(game.round, game.eliminated.size, result.isRevote) { mutableStateOf(false) }
    val cardTilt = remember(game.round, game.eliminated.size, result.isRevote) { Animatable(0f) }
    LaunchedEffect(game.round, game.eliminated.size, result.isRevote, outcome) {
        if (outcome is Outcome.KnockedOut) {
            // Haptics carry meaning, so they stay under reduce motion; only the tilt is skipped (motion.md §7, §9).
            repeat(3) { haptics.perform(SpyHaptic.SuspenseTick); delay(230) }
            haptics.perform(SpyHaptic.KnockOut)
            if (reduce) cardTilt.snapTo(8f) else cardTilt.animateTo(8f, SpyMotion.card())
        }
        revealed = true
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val unit = minOf(maxWidth.value / 390f, maxHeight.value / 844f).coerceIn(0.7f, 1.3f)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SpyIconButton(SpyIcons.Close, stringResource(Res.string.quit_a11y), onQuit)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SpyPill(stringResource(Res.string.result_pill, game.round)) }
                Box(Modifier.width(48.dp))
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (outcome) {
                    is Outcome.KnockedOut -> {
                        val name = game.player(outcome.playerId).name
                        OutCard(name, unit, tilt = cardTilt.value)
                        if (revealed) {
                            val desc = stringResource(Res.string.a11y_knocked_out, name)
                            Text(
                                stringResource(Res.string.result_out, name), style = Spy.type.titleLarge, color = c.text, textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { contentDescription = desc; liveRegion = LiveRegionMode.Polite },
                            )
                            Text(stringResource(Res.string.result_roles_end), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
                        }
                    }
                    is Outcome.Tie -> {
                        TiedCards(outcome.playerIds.map { game.player(it).name }, unit)
                        val (a, b) = joinedNames(outcome.playerIds.map { game.player(it).name })
                        Text(stringResource(Res.string.result_tie), style = Spy.type.titleLarge, color = c.text, textAlign = TextAlign.Center)
                        Text(stringResource(Res.string.result_tie_body, a, b), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
                    }
                    is Outcome.SecondTie -> {
                        TiedCards(outcome.playerIds.map { game.player(it).name }, unit)
                        Text(stringResource(Res.string.result_tie_again), style = Spy.type.titleLarge, color = c.text, textAlign = TextAlign.Center)
                        Text(stringResource(Res.string.result_tie_again_cards), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
                    }
                }
                if (revealed && result.tally.any { it.votes > 0 }) Tally(game, result)
                Box(Modifier.height(8.dp))
            }
            StickyFooter {
                val label = when (outcome) {
                    is Outcome.KnockedOut -> stringResource(Res.string.result_next_round)
                    is Outcome.Tie -> {
                        val (a, b) = joinedNames(outcome.playerIds.map { game.player(it).name })
                        stringResource(Res.string.result_revote, a, b)
                    }
                    is Outcome.SecondTie -> stringResource(Res.string.result_round_n, game.round + 1)
                }
                SpyButton(label, onClick = { controller.continueAfterResult() }, enabled = revealed)
            }
        }
    }
}

@Composable
private fun OutCard(name: String, unit: Float, tilt: Float) {
    val width = (150 * unit).dp
    Box(Modifier.padding(top = 12.dp).graphicsLayer { rotationZ = tilt }, contentAlignment = Alignment.Center) {
        CardBack(width, lifted = true) {
            Stamp(Modifier.align(Alignment.Center))
            Text(
                name, fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = (22 * unit).sp,
                color = Spy.colors.cardCitizenFace, maxLines = 1, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun Stamp(modifier: Modifier = Modifier) {
    val c = Spy.colors
    Box(
        modifier.graphicsLayer { rotationZ = -8f }.clip(RoundedCornerShape(6.dp)).border(2.dp, c.cardCitizenFace, RoundedCornerShape(6.dp)).background(c.cardBack.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(stringResource(Res.string.result_out_stamp), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.4.sp, color = c.cardCitizenFace)
    }
}

@Composable
private fun TiedCards(names: List<String>, unit: Float) {
    val width = ((if (names.size > 2) 100 else 130) * unit).dp
    Box(Modifier.padding(top = 12.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            names.take(3).forEachIndexed { i, name ->
                CardBack(width, lifted = true, modifier = Modifier.graphicsLayer { rotationZ = if (i % 2 == 0) -6f else 6f }) {
                    Text(name, fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = (18 * unit).sp, color = Spy.colors.cardCitizenFace, maxLines = 1, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
                }
            }
        }
        Box(Modifier.size(36.dp).clip(CircleShape).background(Spy.colors.primary), contentAlignment = Alignment.Center) {
            Text("=", color = Spy.colors.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, fontFamily = Spy.type.body.fontFamily)
        }
    }
}

@Composable
private fun Tally(game: GameState, result: Stage.Result) {
    val c = Spy.colors
    val rows = result.tally.filter { it.votes > 0 }
    val max = rows.maxOf { it.votes }
    val total = rows.sumOf { it.votes }
    val leaders = rows.filter { it.votes == max }.map { it.playerId }.toSet()
    Panel {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.result_votes), style = Spy.type.label, color = c.textMuted, modifier = Modifier.weight(1f))
                Text(pluralStringResource(Res.plurals.votes, total, total), style = Spy.type.body, color = c.textMuted)
            }
            rows.forEach { row ->
                val lead = row.playerId in leaders
                val frac by animateFloatAsState(row.votes / max.toFloat(), tween(SpyMotion.EMPHASIZED_MS, easing = SpyMotion.Emphasized), label = "bar")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        game.player(row.playerId).name, style = Spy.type.bodyLarge.copy(fontWeight = if (lead) FontWeight.Bold else FontWeight.Normal),
                        color = if (lead) c.text else c.textMuted, maxLines = 1, modifier = Modifier.width(96.dp),
                    )
                    Box(Modifier.weight(1f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(c.surface2)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(6.dp)).background(if (lead) c.primary else c.disabledFill))
                    }
                    Text(row.votes.toString(), style = Spy.type.bodyLarge.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold), color = c.text, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}
