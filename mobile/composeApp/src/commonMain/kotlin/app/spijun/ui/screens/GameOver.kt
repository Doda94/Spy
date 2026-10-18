package app.spijun.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.engine.GameState
import app.spijun.engine.Role
import app.spijun.engine.Stage
import app.spijun.engine.Winner
import app.spijun.resources.Res
import app.spijun.resources.gameover_citizens
import app.spijun.resources.gameover_label
import app.spijun.resources.gameover_new
import app.spijun.resources.gameover_play_again
import app.spijun.resources.gameover_play_again_hint
import app.spijun.resources.gameover_rounds_meta
import app.spijun.resources.gameover_recap
import app.spijun.resources.gameover_spies
import app.spijun.resources.gameover_unmasked_spies
import app.spijun.resources.reveal_category
import app.spijun.resources.reveal_secret_word
import app.spijun.resources.role_citizen
import app.spijun.resources.role_spy
import app.spijun.resources.round_nobody
import app.spijun.resources.rounds
import app.spijun.resources.spies_caught
import app.spijun.ui.AppController
import app.spijun.ui.design.CitizenFace
import app.spijun.ui.design.CitizenGlyph
import app.spijun.ui.design.LocalReduceMotion
import app.spijun.ui.design.Panel
import app.spijun.ui.design.PanelDivider
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyGlyph
import app.spijun.ui.design.SpyHaptic
import app.spijun.ui.design.SpyMotion
import app.spijun.ui.design.StickyFooter
import app.spijun.ui.design.cardShadow
import app.spijun.ui.design.cardShape
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GameOverScreen(game: GameState, controller: AppController, onNewGame: () -> Unit) {
    val over = game.stage as Stage.GameOver
    val c = Spy.colors
    val haptics = Spy.haptics
    val reduce = LocalReduceMotion.current
    val citizensWon = over.winner == Winner.Citizens
    val rounds = game.round
    val spies = game.players.filter { it.id in game.spyIds }
    val unmasked = spies.filter { it.id !in game.eliminated }

    // Spy cards and the word card are dealt to the front (motion.md §5).
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        haptics.perform(SpyHaptic.GameOverReveal)
        delay(if (reduce) 0 else 250)
        shown = true
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val unit = minOf(maxWidth.value / 390f, maxHeight.value / 844f).coerceIn(0.7f, 1.3f)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(Res.string.gameover_rounds_meta, pluralStringResource(Res.plurals.rounds, rounds, rounds)),
                        style = Spy.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.textMuted,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (citizensWon) CitizenGlyph(c.citizen, 26.dp) else SpyGlyph(c.spy, 26.dp)
                        Text(
                            stringResource(if (citizensWon) Res.string.gameover_citizens else Res.string.gameover_spies),
                            style = Spy.type.titleLarge.copy(fontSize = 36.sp, lineHeight = 38.sp), color = c.text,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    Text(
                        if (unmasked.isEmpty()) pluralStringResource(Res.plurals.spies_caught, spies.size, spies.size)
                        else stringResource(Res.string.gameover_unmasked_spies, unmasked.joinToString(", ") { it.name }),
                        style = Spy.type.body, color = c.textMuted,
                    )
                }

                FannedCards(game, spies.map { it.name }, shown, unit, reduce)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.gameover_recap), style = Spy.type.label, color = c.textMuted, modifier = Modifier.padding(horizontal = 6.dp))
                    AnimatedVisibility(shown, enter = if (reduce) fadeIn() else fadeIn(tween(SpyMotion.STANDARD_MS)) + slideInVertically(tween(SpyMotion.STANDARD_MS)) { it / 6 }) {
                        Panel {
                            game.history.forEachIndexed { i, record ->
                                if (i > 0) PanelDivider(50.dp)
                                RecapRow(record.round, record.knockedOut?.let { game.player(it).name }, record.knockedOut?.let { game.roleOf(it) })
                            }
                        }
                    }
                }
                Box(Modifier.height(8.dp))
            }
            StickyFooter {
                SpyButton(stringResource(Res.string.gameover_play_again), onClick = { controller.playAgain() })
                Text(stringResource(Res.string.gameover_play_again_hint), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.textMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                SpyButton(stringResource(Res.string.gameover_new), onClick = onNewGame, kind = app.spijun.ui.design.ButtonKind.Tertiary, small = true)
            }
        }
    }
}

@Composable
private fun RecapRow(round: Int, name: String?, role: Role?) {
    val c = Spy.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("R$round", style = Spy.type.caption.copy(fontSize = 13.sp, fontFeatureSettings = "tnum"), color = c.textMuted, modifier = Modifier.width(22.dp))
        if (name == null) {
            Text(stringResource(Res.string.round_nobody), style = Spy.type.body, color = c.textMuted, modifier = Modifier.weight(1f))
        } else {
            Text(name, style = Spy.type.bodyLarge, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (role == Role.Spy) SpyGlyph(c.spy, 9.dp) else CitizenGlyph(c.citizen, 9.dp)
                Text(
                    stringResource(if (role == Role.Spy) Res.string.role_spy else Res.string.role_citizen),
                    fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = if (role == Role.Spy) c.spy else c.citizen,
                )
            }
        }
    }
}

/** Every spy card (dark) plus the ivory word card, fanned (decisions.md §3: every card is shown). */
@Composable
private fun FannedCards(game: GameState, spyNames: List<String>, shown: Boolean, unit: Float, reduce: Boolean) {
    val c = Spy.colors
    val cardW = (118 * unit).dp
    val cat = game.category
    Box(Modifier.fillMaxWidth().height((240 * unit).dp), contentAlignment = Alignment.Center) {
        val count = spyNames.size
        // Up to 5 spies (12 players); more cards share the same fan width, so they overlap more.
        val step = if (count <= 3) 62f else 124f / (count - 1)
        val tiltStep = if (count <= 3) 6f else 12f / (count - 1)
        spyNames.forEachIndexed { i, name ->
            val progress = remember { Animatable(0f) }
            LaunchedEffect(shown) {
                if (shown) {
                    if (reduce) progress.snapTo(1f) else { delay(i * SpyMotion.GAME_OVER_STAGGER_MS.toLong()); progress.animateTo(1f, SpyMotion.deal()) }
                }
            }
            val spread = (i - (count - 1) / 2f)
            Box(
                Modifier
                    .offset(x = ((-70 + spread * step) * unit).dp)
                    .graphicsLayer {
                        rotationZ = (-8f + i * tiltStep) * progress.value
                        translationY = (1f - progress.value) * 80f * density
                        alpha = progress.value
                    }
                    .cardShadow(cardW),
            ) {
                SpyNameCard(name, cardW)
            }
        }
        val wordProgress = remember { Animatable(0f) }
        LaunchedEffect(shown) {
            if (shown) {
                if (reduce) wordProgress.snapTo(1f) else { delay((count + 1) * SpyMotion.GAME_OVER_STAGGER_MS.toLong()); wordProgress.animateTo(1f, SpyMotion.deal()) }
            }
        }
        Box(
            Modifier.offset(x = (86 * unit).dp).graphicsLayer {
                rotationZ = 6f * wordProgress.value
                translationY = (1f - wordProgress.value) * 80f * density
                alpha = wordProgress.value
            }.cardShadow((150 * unit).dp),
        ) {
            CitizenFace(game.word, cat, stringResource(Res.string.reveal_category, cat), stringResource(Res.string.reveal_secret_word), (150 * unit).dp)
        }
    }
}

@Composable
private fun SpyNameCard(name: String, width: androidx.compose.ui.unit.Dp) {
    val c = Spy.colors
    val shape = cardShape(width)
    Box(Modifier.size(width, width * 1.4f).clip(shape).background(c.cardSpyFace).border(1.5.dp, c.cardSpyFaceStroke, shape), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
            SpyGlyph(c.vermilion, 22.dp)
            Text(name, fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.cardCitizenFace, maxLines = 1)
            Text(stringResource(Res.string.role_spy).uppercase(), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.3.sp, color = c.vermilion)
        }
    }
}
