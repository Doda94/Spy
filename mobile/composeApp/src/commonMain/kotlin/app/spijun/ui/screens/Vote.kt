package app.spijun.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.engine.GameState
import app.spijun.engine.Player
import app.spijun.engine.Stage
import app.spijun.resources.Res
import app.spijun.resources.a11y_vote_self
import app.spijun.resources.a11y_vote_selected
import app.spijun.resources.quit_a11y
import app.spijun.resources.reveal_pass_to
import app.spijun.resources.vote_confirm
import app.spijun.resources.vote_i_am
import app.spijun.resources.vote_locked
import app.spijun.resources.vote_pass_label
import app.spijun.resources.vote_pass_secret
import app.spijun.resources.vote_pick_pill
import app.spijun.resources.vote_progress
import app.spijun.resources.vote_secret
import app.spijun.resources.vote_title
import app.spijun.resources.vote_out_tag
import app.spijun.resources.vote_you_tag
import app.spijun.ui.AppController
import app.spijun.ui.design.CardBack
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyHaptic
import app.spijun.ui.design.SpyIcon
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyMotion
import app.spijun.ui.design.SpyPill
import app.spijun.ui.design.StickyFooter
import app.spijun.ui.design.cardShadow
import app.spijun.ui.design.dashedBorder
import org.jetbrains.compose.resources.stringResource

/** One-phone, in-app voting: pass the phone for a private vote, then pick (screens.md P1·06 a + c). */
@Composable
fun VoteScreen(game: GameState, controller: AppController, onQuit: () -> Unit) {
    val voting = game.stage as Stage.Voting
    val voterId = voting.currentVoter ?: return
    val voter = game.player(voterId)
    // A new voter (or a re-vote) always starts at the "pass the phone" screen.
    var ready by remember(voting.ballots.size, voting.isRevote, game.round) { mutableStateOf(false) }
    var picked by remember(voting.ballots.size, voting.isRevote, game.round) { mutableStateOf<Int?>(null) }
    val haptics = Spy.haptics
    val c = Spy.colors

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val unit = minOf(maxWidth.value / 390f, maxHeight.value / 844f).coerceIn(0.7f, 1.3f)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SpyIconButton(SpyIcons.Close, stringResource(Res.string.quit_a11y), onQuit)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SpyPill(
                        if (ready) stringResource(Res.string.vote_pick_pill, game.round)
                        else stringResource(Res.string.vote_progress, voting.ballots.size, voting.voters.size),
                    )
                }
                Box(Modifier.width(48.dp))
            }
            Crossfade(ready, Modifier.weight(1f), animationSpec = tween(SpyMotion.STANDARD_MS), label = "vote-step") { isPick ->
                if (!isPick) {
                    PassPhone(voter, unit) { ready = true }
                } else {
                    PickPlayer(game, voting, voter, picked, onPick = { picked = it; haptics.perform(SpyHaptic.StepperTick) }) { target ->
                        haptics.perform(SpyHaptic.VoteCast)
                        controller.castVote(target)
                    }
                }
            }
        }
    }
}

@Composable
private fun PassPhone(voter: Player, unit: Float, onReady: () -> Unit) {
    val c = Spy.colors
    val cardWidth = (230 * unit).dp
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.reveal_pass_to, voter.name), style = Spy.type.headline, color = c.text, textAlign = TextAlign.Center)
            Box(Modifier.height(20.dp))
            CardBack(cardWidth, lifted = true, medallion = false) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size((62 * unit).dp).clip(CircleShape).background(c.cardBack).border(1.5.dp, c.vermilion, CircleShape), contentAlignment = Alignment.Center) {
                        SpyIcon(SpyIcons.Vote, c.vermilion, size = (30 * unit).dp)
                    }
                    Text(stringResource(Res.string.vote_pass_label), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp, color = Color(0xFFA39C94))
                    Text(
                        voter.name, fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = (40 * unit).sp,
                        color = c.cardCitizenFace, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            Box(Modifier.height(20.dp))
            Text(stringResource(Res.string.vote_pass_secret), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
        }
        Box(Modifier.padding(bottom = 16.dp)) { SpyButton(stringResource(Res.string.vote_i_am, voter.name), onClick = onReady) }
    }
}

@Composable
private fun PickPlayer(game: GameState, voting: Stage.Voting, voter: Player, picked: Int?, onPick: (Int) -> Unit, onConfirm: (Int) -> Unit) {
    val c = Spy.colors
    // A re-vote shows only the tied players; the first vote shows everyone.
    val shown = if (voting.isRevote) game.players.filter { it.id in voting.candidates } else game.players
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.height(8.dp))
            Text(stringResource(Res.string.vote_title), style = Spy.type.titleLarge, color = c.text)
            Text(stringResource(Res.string.vote_secret), style = Spy.type.body, color = c.textMuted)
            Box(Modifier.height(4.dp))
            shown.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { p ->
                        val state = when {
                            p.id == voter.id -> OptionState.Self
                            p.id in game.eliminated -> OptionState.Out
                            else -> if (picked == p.id) OptionState.Selected else OptionState.Selectable
                        }
                        Box(Modifier.weight(1f)) { VoteOption(p, state) { onPick(p.id) } }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
            Box(Modifier.height(8.dp))
        }
        StickyFooter {
            Text(stringResource(Res.string.vote_locked), style = Spy.type.body, color = c.textMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            val target = picked
            SpyButton(
                if (target != null) stringResource(Res.string.vote_confirm, game.player(target).name) else stringResource(Res.string.vote_title),
                onClick = { if (target != null) onConfirm(target) },
                enabled = target != null,
            )
        }
    }
}

private enum class OptionState { Selectable, Selected, Self, Out }

@Composable
private fun VoteOption(p: Player, state: OptionState, onClick: () -> Unit) {
    val c = Spy.colors
    val shape = RoundedCornerShape(18.dp)
    val enabled = state == OptionState.Selectable || state == OptionState.Selected
    val selectedDesc = stringResource(Res.string.a11y_vote_selected, p.name)
    val selfDesc = stringResource(Res.string.a11y_vote_self, p.name)
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(if (enabled) c.surface else Color.Transparent)
            .then(
                when (state) {
                    OptionState.Selected -> Modifier.border(1.5.dp, c.text, shape)
                    OptionState.Self, OptionState.Out -> Modifier.dashedBorder(c.line, shape)
                    else -> Modifier
                },
            )
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics {
                if (state == OptionState.Selected) { selected = true; contentDescription = selectedDesc }
                if (state == OptionState.Self) { disabled(); contentDescription = selfDesc }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val inverted = state == OptionState.Selected
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (inverted) c.primary else c.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                p.name.take(1).uppercase(),
                fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = if (inverted) c.onPrimary else if (enabled) c.text else c.eliminated,
            )
        }
        Column {
            Text(
                p.name,
                fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1,
                color = if (enabled) c.text else c.eliminated,
                textDecoration = if (state == OptionState.Out) TextDecoration.LineThrough else null,
            )
            when (state) {
                OptionState.Self -> Text(stringResource(Res.string.vote_you_tag), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.textMuted)
                OptionState.Out -> Text(stringResource(Res.string.vote_out_tag), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.eliminated)
                else -> Unit
            }
        }
    }
}
