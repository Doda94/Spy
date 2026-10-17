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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.addOutline

/** Card size scales everything from the 300 × 420 reveal size. */
const val CARD_ASPECT = 1.4f

private fun cutFor(width: Dp): Dp = (22f * width.value / 300f).coerceAtLeast(4f).dp

@Composable
fun cardShape(width: Dp) = CutCornerShape(cutFor(width))

/** Card shadow only exists while a card is lifted or dealt (rule 3: only cards cast shadows). */
@Composable
fun Modifier.cardShadow(width: Dp, shape: androidx.compose.ui.graphics.Shape = cardShape(width)): Modifier =
    this.shadow(elevation = (width.value / 12f).dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)

/** Face-down card: dark back with a vermilion lattice and the "Š" medallion. */
@Composable
fun CardBack(
    width: Dp,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
    medallion: Boolean = true,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    val c = Spy.colors
    val shape = cardShape(width)
    val unit = width.value / 300f
    Box(
        modifier
            .size(width, width * CARD_ASPECT)
            .then(if (lifted) Modifier.cardShadow(width, shape) else Modifier)
            .clip(shape)
            .background(c.cardBack)
            .border((1.5f * unit.coerceAtLeast(0.7f)).dp, c.cardBackStroke, shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = (12f * unit).coerceAtLeast(5f) * density
            val w = (1.5f * unit).coerceAtLeast(0.7f) * density
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(c.cardBackLattice, Offset(x, 0f), Offset(x + size.height, size.height), w)
                drawLine(c.cardBackLattice, Offset(x + size.height, 0f), Offset(x, size.height), w)
                x += step
            }
        }
        if (medallion) {
            val d = (62f * unit).dp
            Box(
                Modifier.size(d).clip(CircleShape).background(c.cardBack).border((1.5f * unit).coerceAtLeast(0.8f).dp, c.vermilion, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (width > 100.dp) {
                    Text("Š", fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = (34f * unit).sp, color = c.vermilion)
                }
            }
        }
        content?.invoke(this)
    }
}

/** Dashed outline where a knocked-out player's seat card used to be. */
@Composable
fun EmptySeat(width: Dp, modifier: Modifier = Modifier) {
    val c = Spy.colors
    Canvas(modifier.size(width, width * CARD_ASPECT).graphicsLayer { alpha = 0.7f }) {
        val shape = CutCornerShape(cutFor(width))
        val outline = shape.createOutline(size, androidx.compose.ui.unit.LayoutDirection.Ltr, this)
        val path = Path().apply { addOutline(outline) }
        drawPath(path, c.tableRing, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 5f * density))))
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
 * [flip] is read in graphicsLayer; only crossing 90° recomposes.
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
        if (!showFront) back()
        else Box(Modifier.graphicsLayer { rotationY = 180f }) { front() }
    }
}
