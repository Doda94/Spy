package app.spijun.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.engine.Tension
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val TABLE_RADIUS = 172f
const val SEAT_RADIUS = 130f
const val SEAT_W = 56f
const val SEAT_H = 78f

/** Angle (degrees, clockwise from +x, y down) of [seat] before the camera turns the table. 90° = bottom. */
fun seatAngle(seat: Int, n: Int): Float = 90f + 360f / n * seat

/** Camera rotation (degrees) that brings [seat] to the bottom. */
fun cameraRotationFor(seat: Int, n: Int): Float = -(360f / n) * seat

/**
 * The round table. Every player has a face-down card on a seat; the camera (rotation + scale)
 * turns the current seat towards whoever holds the phone. [centre] is never rotated. The camera is
 * passed as lambdas and read only in graphicsLayer, so turning it doesn't recompose the table.
 *
 * @param unit layout scale: screen width / 390.
 */
@Composable
fun SpyTable(
    names: List<String>,
    outSeats: Set<Int>,
    modifier: Modifier = Modifier,
    unit: Float = 1f,
    cameraRotation: () -> Float = { 0f },
    cameraScale: () -> Float = { 1f },
    hiddenSeat: Int? = null,
    ringSeat: Int? = null,
    dimOthers: Boolean = false,
    tension: Tension = Tension.Calm,
    idle: Boolean = false,
    deal: Boolean = false,
    selectedSeat: Int? = null,
    onSeatTap: ((Int) -> Unit)? = null,
    seatDescription: (Int) -> String = { names[it] },
    centre: @Composable BoxScope.() -> Unit = {},
) {
    val c = Spy.colors
    val reduce = LocalReduceMotion.current
    val n = names.size
    // Ambient turn: read only in graphicsLayer, so the table doesn't recompose every frame.
    // Leaving idle (Frozen, pick mode) pauses it where it is (motion.md §3); reduce motion resets it.
    val ambient = remember { Animatable(0f) }
    val turning = idle && !reduce && tension != Tension.Frozen
    LaunchedEffect(turning, reduce) {
        if (reduce) ambient.snapTo(0f)
        while (turning) {
            val from = ambient.value % 360f
            ambient.snapTo(from)
            ambient.animateTo(from + 360f, tween(SpyMotion.AMBIENT_TURN_MS, easing = LinearEasing))
        }
    }
    val ringColor = if (tension == Tension.Uneasy || tension == Tension.Nervous || tension == Tension.Frozen) c.warning else c.tableRing

    Box(
        modifier
            .size((TABLE_RADIUS * 2 * unit).dp)
            .graphicsLayer { scaleX = cameraScale(); scaleY = scaleX },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = cameraRotation() + ambient.value }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(c.table)
                drawCircle(ringColor, style = Stroke(1.5f * density * unit.coerceAtLeast(0.8f)))
            }
            for (i in 0 until n) {
                val a = seatAngle(i, n) * PI.toFloat() / 180f
                val dx = SEAT_RADIUS * unit * cos(a)
                val dy = SEAT_RADIUS * unit * sin(a)
                Seat(
                    index = i,
                    name = names[i],
                    out = i in outSeats,
                    lifted = hiddenSeat == i,
                    ring = ringSeat == i,
                    dim = dimOthers && hiddenSeat != i,
                    unit = unit,
                    angleDeg = seatAngle(i, n),
                    dx = dx,
                    dy = dy,
                    tension = tension,
                    idle = idle,
                    deal = deal,
                    selected = selectedSeat == i,
                    onSeatTap = onSeatTap,
                    description = seatDescription(i),
                )
            }
        }
        Box(Modifier.size((200 * unit).dp), contentAlignment = Alignment.Center, content = centre)
    }
}

@Composable
private fun Seat(
    index: Int,
    name: String,
    out: Boolean,
    lifted: Boolean,
    ring: Boolean,
    dim: Boolean,
    unit: Float,
    angleDeg: Float,
    dx: Float,
    dy: Float,
    tension: Tension,
    idle: Boolean,
    deal: Boolean,
    selected: Boolean,
    onSeatTap: ((Int) -> Unit)?,
    description: String,
) {
    val c = Spy.colors
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (deal) 0f else 1f) }
    LaunchedEffect(deal) {
        if (deal) {
            if (reduce) progress.animateTo(1f, tween(SpyMotion.REDUCED_FADE_MS))
            else {
                delay(index * SpyMotion.DEAL_STAGGER_MS.toLong())
                progress.animateTo(1f, SpyMotion.deal())
            }
        }
    }
    val alpha by animateFloatAsState(if (dim) 0.4f else 1f, SpyMotion.standard(), label = "seat-alpha")
    // Applied to each thing the seat draws, not to its layer: a layer with alpha below 1 is drawn
    // offscreen every frame, and the dimmed seats stay dimmed through the whole reveal. The card,
    // the outline and the name don't overlap, so it looks the same.
    val fade = { (alpha * progress.value).coerceIn(0f, 1f) }
    val lift by animateFloatAsState(if (selected) 1.2f else 1f, SpyMotion.card(), label = "seat-lift")

    // Idle tension: drift, quicker when uneasy, tremble when nervous (motion.md §3). [drift] runs
    // -1 ↔ 1 and is read only in graphicsLayer; Frozen stops it in place, keeping the last pose.
    val drift = remember { Animatable(0f) }
    var pose by remember { mutableStateOf<Tension?>(null) }
    val drifting = idle && !reduce && !out && !lifted
    LaunchedEffect(drifting, tension) {
        if (!drifting) {
            pose = null
            drift.snapTo(0f)
            return@LaunchedEffect
        }
        if (tension == Tension.Frozen) return@LaunchedEffect
        val ms = when (tension) {
            Tension.Calm -> 3200 + (index * 370) % 1400
            Tension.Uneasy -> 1300 + (index * 150) % 600
            else -> 290
        }
        // Phase offset per seat so the cards never move in unison.
        if (pose == null) drift.snapTo(-1f + 2f * (index * 600 % ms) / ms)
        pose = tension
        var target = 1f
        while (true) {
            val left = (kotlin.math.abs(target - drift.value) / 2f * ms).toInt()
            drift.animateTo(target, tween(left, easing = LinearEasing))
            target = -target
        }
    }

    val w = (SEAT_W * unit).dp
    val h = (SEAT_H * unit).dp
    val tappable = onSeatTap != null && !out
    Box(
        Modifier
            .offset {
                // Reduce motion deals in place: full offset, only alpha fades (motion.md §9).
                val p = if (reduce) 1f else progress.value
                IntOffset((dx * density * p).toInt(), (dy * density * p).toInt())
            }
            .size(w, h)
            .graphicsLayer {
                val v = drift.value
                val amp = if (pose == Tension.Calm || pose == Tension.Uneasy) 1.4f else 1f
                rotationZ = angleDeg - 90f + if (pose != null) v * amp else 0f
                translationY = density * when (pose) {
                    null -> 0f
                    Tension.Nervous, Tension.Frozen -> v
                    else -> -1.5f - v * 2.5f
                }
                val s = (if (reduce) 1f else 0.6f + 0.4f * progress.value) * lift
                scaleX = s; scaleY = s
            }
            .then(
                if (tappable) Modifier.clickable(role = Role.Button) { onSeatTap(index) }.semantics { contentDescription = description }
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            out -> EmptySeat(w, alpha = fade)
            lifted -> EmptySeat(w, alpha = fade)
            else -> CardBack(
                w,
                medallion = true,
                alpha = fade,
                modifier = Modifier
                    .then(if (selected || ring) Modifier.border(2.dp, c.text, CutCornerShape(5.dp)) else Modifier),
            )
        }
        // Name on the outer edge; wrapContentSize(unbounded) lets it extend past the card.
        // BasicText (styled as Text would be) because its colour can follow [fade] at draw time.
        val nameColor = if (out) c.eliminated else c.text
        BasicText(
            name,
            style = LocalTextStyle.current.merge(
                fontFamily = Spy.type.body.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (11f * unit).coerceAtLeast(9f).sp,
                textDecoration = if (out) TextDecoration.LineThrough else null,
                textAlign = TextAlign.Center,
            ),
            color = { nameColor.copy(alpha = nameColor.alpha * fade()) },
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentSize(unbounded = true)
                .offset(y = (13 * unit).dp),
        )
    }
}
