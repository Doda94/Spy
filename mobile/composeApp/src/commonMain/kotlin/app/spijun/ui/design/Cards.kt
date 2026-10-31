package app.spijun.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.addOutline
import app.spijun.ui.platform.freeze
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Card size scales everything from the 300 × 420 reveal size. */
const val CARD_ASPECT = 1.4f

private fun cutFor(width: Dp): Dp = (22f * width.value / 300f).coerceAtLeast(4f).dp

@Composable
fun cardShape(width: Dp) = CutCornerShape(cutFor(width))

/** Card shadow only exists while a card is lifted or dealt (rule 3: only cards cast shadows). */
@Composable
fun Modifier.cardShadow(width: Dp, shape: androidx.compose.ui.graphics.Shape = cardShape(width)): Modifier =
    this.shadow(elevation = (width.value / 12f).dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)

/**
 * Face-down card: dark back with a vermilion lattice and the "Š" medallion. The back itself (fill,
 * lattice, medallion ring, border) is drawn once into a bitmap shared by every card that looks the
 * same, so a deal, a camera turn or a flip only draws an image. [alpha] fades that image (not the
 * medallion letter or [content]); read at draw time, it lets a seat fade without an offscreen layer.
 */
@Composable
fun CardBack(
    width: Dp,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
    medallion: Boolean = true,
    alpha: () -> Float = { 1f },
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    val c = Spy.colors
    val shape = cardShape(width)
    val unit = width.value / 300f
    val look = BackLook(width, medallion, c.cardBack, c.cardBackLattice, c.cardBackStroke, c.vermilion)
    Box(
        modifier
            .size(width, width * CARD_ASPECT)
            .then(if (lifted) Modifier.cardShadow(width, shape) else Modifier)
            .drawWithCache {
                val back = cardBackBitmap(look)
                val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
                onDrawBehind { drawImage(back, dstSize = dst, alpha = alpha(), filterQuality = FilterQuality.Medium) }
            }
            // Only added content needs the clip; a bare back (every seat) stays without a layer.
            .then(if (content != null) Modifier.clip(shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (medallion && width > 100.dp) {
            Box(Modifier.size((62f * unit).dp), contentAlignment = Alignment.Center) {
                CapDisplayFontScale {
                    Text("Š", fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = (34f * unit).sp, color = c.vermilion)
                }
            }
        }
        content?.invoke(this)
    }
}

/** Everything a bare back's pixels depend on, apart from its size. */
private data class BackLook(val width: Dp, val medallion: Boolean, val fill: Color, val lattice: Color, val stroke: Color, val ring: Color)

private data class BackKey(val look: BackLook, val size: Size, val density: Float)

// Only a few backs are on screen at once (all seats share one); the least-recently used is dropped first.
private val backs = LinkedHashMap<BackKey, ImageBitmap>()
private const val MAX_BACKS = 6

private fun CacheDrawScope.cardBackBitmap(look: BackLook): ImageBitmap {
    val key = BackKey(look, size, density)
    backs.remove(key)?.let {
        backs[key] = it
        return it
    }
    // Seat cards are seen through the camera push and turned, so they get twice the pixels.
    val oversample = if (look.width < 100.dp) 2 else 1
    val image = ImageBitmap(size.width.roundToInt() * oversample, size.height.roundToInt() * oversample)
    val layout = size
    CanvasDrawScope().draw(this, layoutDirection, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
        scale(oversample.toFloat(), pivot = Offset.Zero) { drawCardBack(look, layout) }
    }
    image.freeze()
    if (backs.size >= MAX_BACKS) backs.remove(backs.keys.first())
    backs[key] = image
    return image
}

/** The bare back at layout size [s], matching what clip + background + border + children drew. */
private fun DrawScope.drawCardBack(look: BackLook, s: Size) {
    val unit = look.width.value / 300f
    val card = Path().apply { addOutline(CutCornerShape(cutFor(look.width)).createOutline(s, layoutDirection, this@drawCardBack)) }
    clipPath(card) {
        drawRect(look.fill, size = s)
        val step = (12f * unit).coerceAtLeast(5f) * density
        val w = (1.5f * unit).coerceAtLeast(0.7f) * density
        var x = -s.height
        while (x < s.width + s.height) {
            drawLine(look.lattice, Offset(x, 0f), Offset(x + s.height, s.height), w)
            drawLine(look.lattice, Offset(x + s.height, 0f), Offset(x, s.height), w)
            x += step
        }
        if (look.medallion) {
            // Placed and stroked the way Box(Alignment.Center) and Modifier.border would.
            val d = (62f * unit).dp.roundToPx()
            val at = Alignment.Center.align(IntSize(d, d), IntSize(s.width.roundToInt(), s.height.roundToInt()), layoutDirection)
            val centre = Offset(at.x + d / 2f, at.y + d / 2f)
            val ring = ceil((1.5f * unit).coerceAtLeast(0.8f).dp.toPx())
            drawCircle(look.fill, radius = d / 2f, center = centre)
            drawCircle(look.ring, radius = (d - ring) / 2f, center = centre, style = Stroke(ring))
        }
        // Modifier.border on a path: a stroke twice as wide, centred on the edge, clipped inside.
        val border = ceil((1.5f * unit.coerceAtLeast(0.7f)).dp.toPx())
        drawPath(card, look.stroke, style = Stroke(border * 2))
    }
}

/**
 * Dashed outline where a knocked-out player's seat card used to be. It is a single stroke, so its
 * 0.7 and [alpha] go on the colour instead of a layer and look the same.
 */
@Composable
fun EmptySeat(width: Dp, modifier: Modifier = Modifier, alpha: () -> Float = { 1f }) {
    val c = Spy.colors
    Canvas(modifier.size(width, width * CARD_ASPECT)) {
        val shape = CutCornerShape(cutFor(width))
        val outline = shape.createOutline(size, androidx.compose.ui.unit.LayoutDirection.Ltr, this)
        val path = Path().apply { addOutline(outline) }
        val color = c.tableRing.copy(alpha = c.tableRing.alpha * 0.7f * alpha())
        drawPath(path, color, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 5f * density))))
    }
}

/** Citizen face: ivory, whole diamond, the secret word and its category. */
@Composable
fun CitizenFace(word: String, category: String, categoryLine: String, wordLabel: String, width: Dp, modifier: Modifier = Modifier) =
    CapDisplayFontScale { CitizenFaceContent(word, categoryLine, wordLabel, width, modifier) }

@Composable
private fun CitizenFaceContent(word: String, categoryLine: String, wordLabel: String, width: Dp, modifier: Modifier) {
    val c = Spy.colors
    val shape = cardShape(width)
    val unit = width.value / 300f
    Box(
        modifier.size(width, width * CARD_ASPECT).clip(shape).background(c.cardCitizenFace),
    ) {
        CardFrame(c.cardCitizenInk.copy(alpha = 0.45f), width)
        Box(Modifier.align(Alignment.TopStart).padding((22 * unit).dp)) { CitizenGlyph(c.cardCitizenInk, (16f * unit).dp) }
        Box(Modifier.align(Alignment.BottomEnd).padding((22 * unit).dp)) { CitizenGlyph(c.cardCitizenInk, (16f * unit).dp) }
        Column(
            Modifier.fillMaxSize().padding(horizontal = (28 * unit).dp),
            verticalArrangement = Arrangement.spacedBy((14 * unit).dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CitizenGlyph(c.cardCitizenInk, (40f * unit).dp)
            Text(
                wordLabel,
                fontFamily = Spy.type.body.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (12f * unit).coerceAtLeast(7f).sp,
                letterSpacing = 0.12.em,
                color = Color(0xFF6B645C),
            )
            AutoFitWord(word, Spy.type.wordDisplay.copy(color = c.cardCitizenText), scale = unit)
            DashedDivider(Color(0xFFC9C2B9), (160 * unit).dp)
            Text(
                categoryLine,
                fontFamily = Spy.type.body.fontFamily,
                fontSize = (15f * unit).coerceAtLeast(8f).sp,
                color = Color(0xFF5E574F),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Spy face: dark in both themes, split diamond, "Špijun", the category, and fellow spies. */
@Composable
fun SpyFace(
    youAre: String,
    roleName: String,
    categoryLine: String,
    aloneLine: String?,
    othersLabel: String,
    others: List<String>,
    width: Dp,
    modifier: Modifier = Modifier,
) = CapDisplayFontScale { SpyFaceContent(youAre, roleName, categoryLine, aloneLine, othersLabel, others, width, modifier) }

@Composable
private fun SpyFaceContent(
    youAre: String,
    roleName: String,
    categoryLine: String,
    aloneLine: String?,
    othersLabel: String,
    others: List<String>,
    width: Dp,
    modifier: Modifier,
) {
    val c = Spy.colors
    val shape = cardShape(width)
    val unit = width.value / 300f
    Box(
        modifier.size(width, width * CARD_ASPECT).clip(shape).background(c.cardSpyFace).border(1.dp, c.cardSpyFaceStroke, shape),
    ) {
        CardFrame(c.vermilion.copy(alpha = 0.55f), width)
        Column(
            Modifier.fillMaxSize().padding(horizontal = (24 * unit).dp),
            verticalArrangement = Arrangement.spacedBy((10 * unit).dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpyGlyph(c.vermilion, (40f * unit).dp)
            Text(
                youAre,
                fontFamily = Spy.type.body.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (12f * unit).coerceAtLeast(7f).sp,
                letterSpacing = 0.12.em,
                color = Color(0xFFA39C94),
            )
            Text(
                roleName,
                style = Spy.type.roleDisplay.copy(fontSize = (64f * unit).sp, lineHeight = (64f * unit).sp),
                color = c.cardCitizenFace,
                maxLines = 1,
            )
            Text(
                categoryLine,
                fontFamily = Spy.type.body.fontFamily,
                fontSize = (15f * unit).coerceAtLeast(8f).sp,
                color = Color(0xFFC9C2B9),
                textAlign = TextAlign.Center,
            )
            if (others.isEmpty()) {
                if (aloneLine != null) {
                    Text(
                        aloneLine,
                        fontFamily = Spy.type.body.fontFamily,
                        fontSize = (14f * unit).coerceAtLeast(8f).sp,
                        color = Color(0xFFA39C94),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                DashedDivider(Color(0xFF4A3D33), (160 * unit).dp)
                Text(
                    othersLabel,
                    fontFamily = Spy.type.body.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (11f * unit).coerceAtLeast(7f).sp,
                    letterSpacing = 0.12.em,
                    color = Color(0xFFA39C94),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy((6 * unit).dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy((6 * unit).dp),
                ) {
                    others.forEach { name ->
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF26221E)).padding(horizontal = (12 * unit).dp, vertical = (6 * unit).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((6 * unit).dp),
                        ) {
                            SpyGlyph(c.vermilion, (10f * unit).dp)
                            Text(name, fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = (17f * unit).coerceAtLeast(9f).sp, color = c.cardCitizenFace, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Inner frame, cut corner 14 inset 12 (scaled). */
@Composable
private fun CardFrame(color: Color, width: Dp) {
    val unit = width.value / 300f
    val inset = (11f * unit).dp
    Box(
        Modifier
            .fillMaxSize()
            .padding(inset)
            .border((1.5f * unit).coerceAtLeast(0.8f).dp, color, CutCornerShape((14f * unit).coerceAtLeast(3f).dp)),
    )
}

@Composable
private fun DashedDivider(color: Color, width: Dp) {
    Canvas(Modifier.size(width, 2.dp)) {
        drawLine(color, Offset(0f, 1f), Offset(size.width, 1f), 1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * density, 4f * density)))
    }
}

/**
 * The secret word: Bricolage 56 → 40 sp, at most two lines, breaking only between words
 * (docs/design/screens.md, Role reveal › Faces). Display text caps font scaling at 1.3×.
 */
@Composable
fun AutoFitWord(text: String, style: TextStyle, scale: Float = 1f, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val maxWidth = constraints.maxWidth
        // One measuring pass picks the size before the first frame, so the word never jumps.
        val fitted = remember(text, style, scale, maxWidth, measurer) {
            val sizes = AUTO_FIT_SIZES.map { it * scale }
            val styles = sizes.map { style.copy(fontSize = it.sp, lineHeight = it.sp) }
            styles.firstOrNull { s ->
                val r = measurer.measure(text, s.copy(textAlign = TextAlign.Center), maxLines = 2, constraints = Constraints(maxWidth = maxWidth))
                !r.hasVisualOverflow && !r.breaksInsideAWord(text)
            } ?: styles.last()
        }
        Text(text, Modifier.fillMaxWidth(), style = fitted, textAlign = TextAlign.Center, maxLines = 2, softWrap = true)
    }
}

private val AUTO_FIT_SIZES = listOf(56f, 52f, 48f, 44f, 40f, 36f, 32f)

/** True when a line ends in the middle of a word (the text didn't fit, so it broke by character). */
private fun TextLayoutResult.breaksInsideAWord(text: String): Boolean =
    (0 until lineCount - 1).any { line ->
        val end = getLineEnd(line)
        end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
    }

/**
 * Flip container: rotateY 0→180 swapping to [front] at 90°. Content is mirrored so it reads correctly.
 * Both faces are composed and laid out up front and 90° only switches which one is drawn, so the
 * turn never waits on a face being built (the word alone measures up to seven layouts).
 * [flip] is read in graphicsLayer and draw; nothing recomposes during the turn. [front] is never
 * drawn before 90°, and callers keep its text out of semantics.
 */
@Composable
fun FlipCard(
    flip: () -> Float,
    width: Dp,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit,
    front: @Composable () -> Unit,
) {
    val currentFlip by rememberUpdatedState(flip)
    val showFront by remember { derivedStateOf { currentFlip() > 90f } }
    Box(
        modifier
            .size(width, width * CARD_ASPECT)
            .graphicsLayer {
                rotationY = currentFlip()
                cameraDistance = 14f * density
            },
    ) {
        Box(Modifier.drawWithContent { if (!showFront) drawContent() }) { back() }
        Box(Modifier.graphicsLayer { rotationY = 180f }.drawWithContent { if (showFront) drawContent() }) { front() }
    }
}
