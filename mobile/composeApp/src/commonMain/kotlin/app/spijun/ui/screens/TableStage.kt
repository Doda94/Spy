package app.spijun.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.spijun.engine.Role
import app.spijun.engine.Rules
import app.spijun.engine.Stage
import app.spijun.engine.Tension
import app.spijun.engine.VotingMode
import app.spijun.engine.tensionFor
import app.spijun.resources.Res
import app.spijun.resources.a11y_card_hidden
import app.spijun.resources.a11y_citizen
import app.spijun.resources.a11y_other_spies
import app.spijun.resources.a11y_spy
import app.spijun.resources.in_game
import app.spijun.resources.out_count
import app.spijun.resources.quit_a11y
import app.spijun.resources.reveal_autohide
import app.spijun.resources.reveal_category
import app.spijun.resources.reveal_hide_next
import app.spijun.resources.reveal_hide_start
import app.spijun.resources.reveal_nobody_look
import app.spijun.resources.reveal_other_spies
import app.spijun.resources.reveal_pass_to
import app.spijun.resources.reveal_player_of
import app.spijun.resources.reveal_secret_word
import app.spijun.resources.reveal_show
import app.spijun.resources.reveal_spy
import app.spijun.resources.reveal_spy_alone
import app.spijun.resources.reveal_tap_card
import app.spijun.resources.reveal_you_are
import app.spijun.resources.round_add_minute
import app.spijun.resources.round_almost
import app.spijun.resources.round_category_pill
import app.spijun.resources.round_dealing
import app.spijun.resources.round_end_discussion
import app.spijun.resources.round_in_game_out
import app.spijun.resources.round_knock_out
import app.spijun.resources.round_label
import app.spijun.resources.round_loud_tap
import app.spijun.resources.round_loud_title
import app.spijun.resources.round_loud_vote
import app.spijun.resources.round_no_timer
import app.spijun.resources.round_nobody
import app.spijun.resources.round_talk
import app.spijun.resources.round_time_up
import app.spijun.resources.round_to_vote
import app.spijun.resources.round_vote_opens
import app.spijun.resources.round_who
import app.spijun.ui.AppController
import app.spijun.ui.design.ButtonKind
import app.spijun.ui.design.CardBack
import app.spijun.ui.design.CitizenFace
import app.spijun.ui.design.FlipCard
import app.spijun.ui.design.LocalReduceMotion
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyFace
import app.spijun.ui.design.SpyHaptic
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyMotion
import app.spijun.ui.design.SpyPill
import app.spijun.ui.design.SpyTable
import app.spijun.ui.design.cameraRotationFor
import app.spijun.ui.design.cardShadow
import app.spijun.ui.platform.OnAppBackground
import app.spijun.ui.platform.SecureWhile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.ceil
import kotlin.time.TimeSource

private enum class RevealPhase { Dealing, Turning, Handoff, Revealed, Hiding }

/** Auto-hide after 8 s (Settings will offer 5 / 8 / off). */
private const val AUTO_HIDE_MS = SpyMotion.AUTO_HIDE_DEFAULT_MS

/**
 * One table for the role reveal and the discussion: the camera turns to each seat while the phone
 * is passed round, then pulls back when everyone has looked (motion.md §2–3).
 */
@Composable
fun TableStage(game: GameState, controller: AppController, onQuit: () -> Unit) {
    val c = Spy.colors
    val haptics = Spy.haptics
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val n = game.players.size
    val names = remember(game.players) { game.players.map { it.name } }
    val outSeats = remember(game.eliminated) { game.eliminated.toSet() }

    val reveal = game.stage as? Stage.Reveal
    val inDiscussion = game.stage == Stage.Discussion
    val seat = reveal?.seat ?: (n - 1)

    // ── Reveal state ───────────────────────────────────────────────────────────────
    var phase by remember { mutableStateOf(RevealPhase.Dealing) }
    var dealt by remember { mutableStateOf(false) }
    val flip = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val barProgress = remember { Animatable(1f) }
    val faceAlpha = remember { Animatable(1f) }

    LaunchedEffect(reveal?.seat) {
        if (reveal == null) return@LaunchedEffect
        if (!dealt) {
            phase = RevealPhase.Dealing
            repeat(n) { i -> haptics.perform(SpyHaptic.StepperTick); delay(SpyMotion.DEAL_STAGGER_MS.toLong()) }
            delay(450)
            dealt = true
        }
        phase = RevealPhase.Turning
        delay(if (reduce) 120 else SpyMotion.TABLE_TURN_MS.toLong())
        phase = RevealPhase.Handoff
        haptics.perform(SpyHaptic.HideCard)
        if (reduce) { faceAlpha.snapTo(0f); lift.snapTo(1f); faceAlpha.animateTo(1f, tween(SpyMotion.REDUCED_FADE_MS)) }
        else lift.animateTo(1f, tween(SpyMotion.LIFT_MS, easing = SpyMotion.Emphasized))
    }

    fun hideAndPass() {
        if (phase != RevealPhase.Revealed) return
        phase = RevealPhase.Hiding
        scope.launch {
            haptics.perform(SpyHaptic.HideCard)
            if (reduce) { flip.snapTo(0f) } else flip.animateTo(0f, tween(SpyMotion.FLIP_BACK_MS, easing = SpyMotion.Emphasized))
            if (reduce) lift.snapTo(0f) else lift.animateTo(0f, tween(SpyMotion.LIFT_MS, easing = SpyMotion.Emphasized))
            controller.finishReveal()
        }
    }

    fun flipUp() {
        if (phase != RevealPhase.Handoff) return
        phase = RevealPhase.Revealed
        val role = game.roleOf(seat)
        scope.launch {
            if (reduce) {
                flip.snapTo(180f)
                haptics.perform(if (role == Role.Spy) SpyHaptic.RevealSpy else SpyHaptic.RevealCitizen)
            } else {
                flip.animateTo(90f, tween(SpyMotion.FLIP_IN_MS, easing = LinearEasing))
                haptics.perform(if (role == Role.Spy) SpyHaptic.RevealSpy else SpyHaptic.RevealCitizen)
                flip.animateTo(180f, SpyMotion.card())
            }
        }
        scope.launch {
            barProgress.snapTo(1f)
            barProgress.animateTo(0f, tween(AUTO_HIDE_MS, easing = LinearEasing))
            hideAndPass()
        }
    }

    // Privacy: the instant the app leaves the screen, a face-up card flips back with no animation.
    OnAppBackground {
        if (phase == RevealPhase.Revealed) {
            scope.launch { flip.snapTo(0f); barProgress.snapTo(1f); phase = RevealPhase.Handoff }
        }
    }
    SecureWhile(reveal != null)

    // ── Discussion state ───────────────────────────────────────────────────────────
    val total = game.config.timerSeconds
    val mark = remember(game.round, inDiscussion) { TimeSource.Monotonic.markNow() }
    var extraSeconds by remember(game.round) { mutableIntStateOf(0) }
    var remaining by remember(game.round) { mutableStateOf(total) }
    var pickMode by remember(game.round) { mutableStateOf(false) }
    var picked by remember(game.round) { mutableStateOf<Int?>(null) }
    var timeUp by remember(game.round) { mutableStateOf(false) }

    LaunchedEffect(game.round, inDiscussion, total, extraSeconds) {
        if (!inDiscussion || total == null) return@LaunchedEffect
        while (true) {
            val left = ceil(total + extraSeconds - mark.elapsedNow().inWholeMilliseconds / 1000.0).toInt()
            remaining = left.coerceAtLeast(0)
            if (left <= 0) break
            delay(200)
        }
    }
    LaunchedEffect(remaining, inDiscussion) {
        val r = remaining ?: return@LaunchedEffect
        if (!inDiscussion) return@LaunchedEffect
        if (r == Rules.WARNING_SECONDS) haptics.perform(SpyHaptic.ThirtySeconds)
        if (r == 0 && !timeUp) {
            timeUp = true
            haptics.perform(SpyHaptic.TimeUp)
            delay(1200)
            if (game.config.voting == VotingMode.InApp) controller.openVote() else pickMode = true
        }
    }
    val tension = if (inDiscussion) tensionFor(remaining) else Tension.Calm

    // ── Camera ────────────────────────────────────────────────────────────────────
    val focusing = reveal != null && phase != RevealPhase.Dealing
    val camRotation by animateFloatAsState(
        cameraRotationFor(seat, n),
        if (reduce) snap() else tween(SpyMotion.TABLE_TURN_MS, easing = SpyMotion.Emphasized),
        label = "camera-rotation",
    )
    val camScale by animateFloatAsState(
        if (focusing && !reduce) SpyMotion.CAMERA_PUSH else 1f,
        if (reduce) snap() else tween(SpyMotion.TABLE_TURN_MS, easing = SpyMotion.Emphasized),
        label = "camera-scale",
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val unit = minOf(maxWidth.value / 390f, maxHeight.value / 844f).coerceIn(0.7f, 1.3f)
        val cardWidth = (300 * unit).dp

        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Top row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpyIconButton(SpyIcons.Close, stringResource(Res.string.quit_a11y), onQuit)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        pickMode -> Unit
                        reveal != null && phase == RevealPhase.Dealing -> SpyPill(stringResource(Res.string.round_dealing))
                        reveal != null -> SpyPill(stringResource(Res.string.reveal_player_of, seat + 1, n))
                        else -> SpyPill(stringResource(Res.string.round_category_pill, game.category))
                    }
                }
                Box(Modifier.width(48.dp))
            }

            // Sub-line under the pill during discussion
            if (inDiscussion && !pickMode) {
                val out = game.eliminated.size
                val alive = pluralStringResource(Res.plurals.in_game, game.alive.size, game.alive.size)
                val line = if (out > 0) stringResource(Res.string.round_in_game_out, alive, pluralStringResource(Res.plurals.out_count, out, out)) else alive
                Text(line, style = Spy.type.body, color = c.textMuted, modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp))
            }
            if (pickMode) {
                Column(Modifier.align(Alignment.TopCenter).padding(top = 64.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.round_loud_title), style = Spy.type.titleMedium.copy(fontSize = 24.sp), color = c.text, textAlign = TextAlign.Center)
                    Text(stringResource(Res.string.round_loud_tap), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
                }
            }

            // The table
            SpyTable(
                names = names,
                outSeats = outSeats,
                modifier = Modifier.align(Alignment.Center).offset(y = (8 * unit).dp),
                unit = unit,
                cameraRotation = camRotation,
                cameraScale = camScale,
                hiddenSeat = if (reveal != null && lift.value > 0.01f) seat else null,
                ringSeat = if (reduce && reveal != null && phase != RevealPhase.Dealing) seat else null,
                dimOthers = reveal != null && focusing && !reduce,
                tension = tension,
                idle = inDiscussion && !pickMode,
                deal = reveal != null && !dealt,
                selectedSeat = picked,
                onSeatTap = if (pickMode) { s -> picked = s; haptics.perform(SpyHaptic.StepperTick) } else null,
            ) {
                if (inDiscussion) DiscussionCentre(total, remaining, tension, game.round, pickMode, unit)
            }

            // The lifted role card (reveal only)
            if (reveal != null && lift.value > 0.01f) {
                val player = game.player(seat)
                val role = game.roleOf(player.id)
                val others = game.otherSpiesOf(player.id).map { it.name }
                val hiddenDesc = stringResource(Res.string.a11y_card_hidden, player.name)
                val shownDesc = if (role == Role.Citizen) stringResource(Res.string.a11y_citizen, game.word, game.category)
                else stringResource(Res.string.a11y_spy, game.category) +
                    if (others.isNotEmpty()) " " + stringResource(Res.string.a11y_other_spies, others.joinToString(", ")) else ""
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset(y = (-6 * unit).dp)
                        .graphicsLayer {
                            val l = lift.value
                            scaleX = 0.25f + 0.75f * l * (if (phase == RevealPhase.Revealed && !reduce) 1.04f else 1f)
                            scaleY = scaleX
                            translationY = (1f - l) * 170f * unit * density
                            alpha = if (reduce) faceAlpha.value * l else l.coerceIn(0f, 1f)
                            rotationZ = if (phase == RevealPhase.Revealed) -2f else 0f
                        }
                        .semantics {
                            contentDescription = if (phase == RevealPhase.Revealed) shownDesc else hiddenDesc
                            liveRegion = LiveRegionMode.Polite
                        },
                ) {
                    val modifier = Modifier.cardShadow(cardWidth)
                    FlipCard(
                        flip = flip.value,
                        width = cardWidth,
                        modifier = modifier.then(
                            if (phase == RevealPhase.Handoff) Modifier.clickableNoRipple { flipUp() } else Modifier,
                        ),
                        back = {
                            CardBack(cardWidth) {
                                Text(
                                    player.name,
                                    fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold,
                                    fontSize = (24 * unit).sp, color = c.cardCitizenFace, maxLines = 1,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (28 * unit).dp),
                                )
                            }
                        },
                        front = {
                            if (role == Role.Citizen) {
                                CitizenFace(game.word, game.category, stringResource(Res.string.reveal_category, game.category), stringResource(Res.string.reveal_secret_word), cardWidth)
                            } else {
                                SpyFace(
                                    stringResource(Res.string.reveal_you_are), stringResource(Res.string.reveal_spy),
                                    stringResource(Res.string.reveal_category, game.category),
                                    stringResource(Res.string.reveal_spy_alone), stringResource(Res.string.reveal_other_spies), others, cardWidth,
                                )
                            }
                        },
                    )
                }
            }

            // Bottom: hints + primary action
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (reveal != null) {
                    RevealFooter(
                        phase = phase,
                        name = game.player(seat).name,
                        last = seat == n - 1,
                        secondsLeft = ceil(barProgress.value * AUTO_HIDE_MS / 1000f).toInt(),
                        bar = barProgress.value,
                        onFlip = ::flipUp,
                        onHide = ::hideAndPass,
                    )
                } else if (pickMode) {
                    val sel = picked
                    if (sel == null) {
                        SpyButton(stringResource(Res.string.round_loud_vote), onClick = {}, enabled = false)
                    } else {
                        SpyButton(stringResource(Res.string.round_knock_out, game.player(sel).name), onClick = { controller.knockOutLoud(sel) })
                    }
                    TextButton(onClick = { controller.knockOutLoud(null) }) {
                        Text(stringResource(Res.string.round_nobody), color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                } else if (inDiscussion) {
                    val inApp = game.config.voting == VotingMode.InApp
                    if (total != null && remaining != null && remaining!! > 0) {
                        Text(
                            if (inApp) stringResource(Res.string.round_vote_opens) else "",
                            style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (total != null && (remaining ?: 0) > 0) {
                            Box(Modifier.width(112.dp)) {
                                SpyButton(stringResource(Res.string.round_add_minute), onClick = { extraSeconds += Rules.TIMER_EXTRA_SECONDS }, kind = ButtonKind.Secondary, small = true)
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            val label = if (inApp && total == null) stringResource(Res.string.round_to_vote) else stringResource(Res.string.round_end_discussion)
                            SpyButton(label, onClick = { if (inApp) controller.openVote() else pickMode = true })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevealFooter(
    phase: RevealPhase,
    name: String,
    last: Boolean,
    secondsLeft: Int,
    bar: Float,
    onFlip: () -> Unit,
    onHide: () -> Unit,
) {
    val c = Spy.colors
    val hint = when (phase) {
        RevealPhase.Handoff -> stringResource(Res.string.reveal_tap_card, name)
        RevealPhase.Revealed -> stringResource(Res.string.reveal_autohide, secondsLeft)
        else -> ""
    }
    Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hint, style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
            if (phase == RevealPhase.Handoff) Text(stringResource(Res.string.reveal_nobody_look), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.textMuted)
        }
    }
    when (phase) {
        RevealPhase.Dealing, RevealPhase.Turning ->
            SpyButton(if (phase == RevealPhase.Turning) stringResource(Res.string.reveal_pass_to, name) else stringResource(Res.string.round_dealing), onClick = {}, enabled = false)
        RevealPhase.Handoff -> SpyButton(stringResource(Res.string.reveal_show), onClick = onFlip)
        RevealPhase.Revealed, RevealPhase.Hiding -> Box {
            SpyButton(
                if (last) stringResource(Res.string.reveal_hide_start) else stringResource(Res.string.reveal_hide_next),
                onClick = onHide,
                enabled = phase == RevealPhase.Revealed,
            )
            Box(
                Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp).fillMaxWidth(bar.coerceIn(0f, 1f)).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Spy.colors.onPrimary.copy(alpha = 0.4f)),
            )
        }
    }
}

/** Inside the table: round label + countdown (or "Razgovarajte") + sub-line. */
@Composable
private fun DiscussionCentre(total: Int?, remaining: Int?, tension: Tension, round: Int, pickMode: Boolean, unit: Float) {
    val c = Spy.colors
    val reduce = LocalReduceMotion.current
    val warn = tension == Tension.Uneasy || tension == Tension.Nervous || tension == Tension.Frozen
    val tone = if (warn) c.warning else c.text
    val pulse by animateFloatAsState(
        if (tension == Tension.Nervous && !reduce && (remaining ?: 0) % 2 == 0) SpyMotion.PULSE_SCALE else 1f,
        tween(SpyMotion.DIGIT_MS * 3), label = "pulse",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(Res.string.round_label, round), style = Spy.type.label, color = c.textMuted)
        if (total == null || remaining == null) {
            Text(stringResource(Res.string.round_talk), style = Spy.type.titleLarge, color = c.text, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.round_no_timer), style = Spy.type.body, color = c.textMuted)
        } else if (remaining <= 0) {
            Text(stringResource(Res.string.round_time_up), style = Spy.type.titleLarge, color = c.warning)
            if (pickMode) Text(stringResource(Res.string.round_loud_vote), style = Spy.type.body, color = c.textMuted)
        } else {
            val text = "${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    if (reduce) fadeIn(snap()) togetherWith fadeOut(snap())
                    else (slideInVertically(tween(SpyMotion.DIGIT_MS)) { it / 2 } + fadeIn(tween(SpyMotion.DIGIT_MS))) togetherWith
                        (slideOutVertically(tween(SpyMotion.DIGIT_MS)) { -it / 2 } + fadeOut(tween(SpyMotion.DIGIT_MS)))
                },
                label = "countdown",
            ) { t ->
                Text(
                    t,
                    style = Spy.type.countdown.copy(fontSize = (64 * unit).sp, lineHeight = (64 * unit).sp),
                    color = tone,
                    modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse },
                )
            }
            Text(
                if (warn) stringResource(Res.string.round_almost) else stringResource(Res.string.round_who),
                style = Spy.type.body.copy(fontWeight = if (warn) FontWeight.Bold else FontWeight.Normal),
                color = if (warn) c.warning else c.textMuted,
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick),
)
