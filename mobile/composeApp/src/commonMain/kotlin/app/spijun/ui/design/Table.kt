package app.spijun.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * turns the current seat towards whoever holds the phone. [centre] is never rotated.
 *
 * @param unit layout scale: screen width / 390.
 */
@Composable
fun SpyTable(
    names: List<String>,
    outSeats: Set<Int>,
    modifier: Modifier = Modifier,
    unit: Float = 1f,
    cameraRotation: Float = 0f,
    cameraScale: Float = 1f,
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
    val ambient = if (idle && !reduce && tension != Tension.Frozen) {
        val t = rememberInfiniteTransition(label = "table-turn")
        val deg by t.animateFloat(0f, 360f, infiniteRepeatable(tween(SpyMotion.AMBIENT_TURN_MS, easing = LinearEasing)), label = "deg")
        deg
    } else 0f
    val ringColor = if (tension == Tension.Uneasy || tension == Tension.Nervous || tension == Tension.Frozen) c.warning else c.tableRing

    Box(
        modifier
            .size((TABLE_RADIUS * 2 * unit).dp)
            .graphicsLayer { scaleX = cameraScale; scaleY = cameraScale },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = cameraRotation + ambient }, contentAlignment = Alignment.Center) {
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
                    onTap = onSeatTap?.takeIf { i !in outSeats }?.let { f -> { f(i) } },
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
    onTap: (() -> Unit)?,
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
    val lift by animateFloatAsState(if (selected) 1.2f else 1f, SpyMotion.card(), label = "seat-lift")

    // Idle tension: drift, quicker when uneasy, tremble when nervous (motion.md §3).
    var driftRot = 0f
    var driftY = 0f
    if (idle && !reduce && !out && !lifted && tension != Tension.Frozen) {
        val (ms, amp) = when (tension) {
            Tension.Calm -> (3200 + (index * 370) % 1400) to 1.4f
            Tension.Uneasy -> (1300 + (index * 150) % 600) to 1.4f
            else -> (290) to 1f
        }
        val t = rememberInfiniteTransition(label = "seat-$index")
        val v by t.animateFloat(
            -1f, 1f,
            infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse, StartOffset(index * 600 % ms, StartOffsetType.FastForward)),
            label = "drift",
        )
        driftRot = v * amp
        driftY = if (tension == Tension.Nervous) v * 1f else -1.5f - v * 2.5f
    }

    val w = (SEAT_W * unit).dp
    val h = (SEAT_H * unit).dp
    Box(
        Modifier
            .offset { IntOffset((dx * density * progress.value).toInt(), (dy * density * progress.value).toInt()) }
            .size(w, h)
            .graphicsLayer {
                rotationZ = angleDeg - 90f + driftRot
                translationY = driftY * density
                this.alpha = alpha * progress.value
                val s = (0.6f + 0.4f * progress.value) * lift
                scaleX = s; scaleY = s
            }
            .then(
                if (onTap != null) Modifier.clickable(role = Role.Button, onClick = onTap).semantics { contentDescription = description }
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            out -> EmptySeat(w)
            lifted -> EmptySeat(w)
            else -> CardBack(
                w,
                medallion = true,
                modifier = Modifier
                    .then(if (selected || ring) Modifier.border(2.dp, c.text, CutCornerShape(5.dp)) else Modifier),
            )
        }
        // Name on the outer edge; wrapContentSize(unbounded) lets it extend past the card.
        Text(
            name,
            fontFamily = Spy.type.body.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (11f * unit).coerceAtLeast(9f).sp,
            color = if (out) c.eliminated else c.text,
            textDecoration = if (out) TextDecoration.LineThrough else null,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentSize(unbounded = true)
                .offset(y = (13 * unit).dp),
        )
    }
}
