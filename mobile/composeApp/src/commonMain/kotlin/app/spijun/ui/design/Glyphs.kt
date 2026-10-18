package app.spijun.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Whole diamond = citizen. 56×76 viewBox (docs/design/assets/glyphs). */
@Composable
fun CitizenGlyph(color: Color, width: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width, width * 76f / 56f)) {
        val k = size.width / 56f
        val p = Path().apply {
            moveTo(28f * k, 0f); lineTo(56f * k, 38f * k); lineTo(28f * k, 76f * k); lineTo(0f, 38f * k); close()
        }
        drawPath(p, color)
    }
}

/** Split diamond = spy, and the brand mark. */
@Composable
fun SpyGlyph(color: Color, width: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width, width * 76f / 56f)) {
        val k = size.width / 56f
        val left = Path().apply { moveTo(25f * k, 0f); lineTo(0f, 36f * k); lineTo(25f * k, 72f * k); close() }
        val right = Path().apply { moveTo(31f * k, 4f * k); lineTo(56f * k, 40f * k); lineTo(31f * k, 76f * k); close() }
        drawPath(left, color)
        drawPath(right, color)
    }
}

/** Hollow, struck diamond = knocked out. */
@Composable
fun EliminatedGlyph(color: Color, width: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width, width * 76f / 56f)) {
        val k = size.width / 56f
        val sw = 3f * k
        val p = Path().apply {
            moveTo(28f * k, 3f * k); lineTo(53f * k, 38f * k); lineTo(28f * k, 73f * k); lineTo(3f * k, 38f * k); close()
        }
        drawPath(p, color, style = Stroke(sw, join = StrokeJoin.Round))
        drawLine(color, Offset(10f * k, 64f * k), Offset(46f * k, 12f * k), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

/** 24-grid, 2 pt round-cap stroke icons ported from docs/design/assets/icons. */
object SpyIcons {
    private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float): String =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}" +
            "a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    const val Close = "M6 6l12 12M18 6L6 18"
    const val Back = "M15 5l-7 7 7 7"
    const val BackAndroid = "M19 12H5M11 6l-6 6 6 6"
    const val Check = "M5 12l5 5 9-10"
    const val ChevronRight = "M9 5l7 7-7 7"
    const val ChevronDown = "M7 10l5 5 5-5"
    val JoinCode = rr(3f, 3f, 7f, 7f, 1.5f) + rr(14f, 3f, 7f, 7f, 1.5f) + rr(3f, 14f, 7f, 7f, 1.5f) +
        "M14 14h3v3M21 14v.01M14 21h7v-4"
    val Phone = rr(7f, 3f, 10f, 18f, 2f)
    val OnePhone = rr(8f, 3f, 8f, 14f, 1.5f) + "M3 19c2 2 5 2.5 9 2.5s7-.5 9-2.5M19 17l2 2-2 2"
    val OwnPhones = rr(2f, 7f, 6f, 11f, 1.2f) + rr(9f, 4f, 6f, 11f, 1.2f) + rr(16f, 7f, 6f, 11f, 1.2f)
    const val Vote = "M4 20h16M6 20V9h12v11M9 9V5h6v4M10 13l2 2 3-3"
    const val Add = "M12 5v14M5 12h14"
    const val Warning = "M12 3l10 18H2zM12 10v5M12 18v.01"
    const val Timer = "M12 7v5l3 2"
    val TimerRing = "M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0z"
    val Voted = "M21 12a9 9 0 1 1-18 0a9 9 0 0 1 18 0zM8 12l3 3 5-6"
    const val Import = "M12 4v11M7 10l5 5 5-5M5 20h14"
    const val Share = "M12 15V3M7 8l5-5 5 5M5 13v7h14v-7"
    val Search = "M18 11a7 7 0 1 1-14 0a7 7 0 0 1 14 0zM20 20l-4-4"
    val Lock = rr(5f, 11f, 14f, 10f, 2f) + "M8 11V7a4 4 0 0 1 8 0v4"
    const val More = "M5 12h.01M12 12h.01M19 12h.01"
    const val Delete = "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13"
    const val FileInvalid = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" + "M14 3v5h5M10 12l4 4M14 12l-4 4"
    const val File = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8zM14 3v5h5"
    const val Link = "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"
    const val TabPlay = "M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"
    const val TabPlaySelected = TabPlay + "M12 9l3 3-3 3-3-3z"
    const val TabDictionaries = "M4 5h7v15H4zM13 5h7v15h-7z"
}

@Composable
fun SpyIcon(path: String, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp, strokeWidth: Float = 2f) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size)) {
        val k = this.size.width / 24f
        scale(k, k, pivot = Offset.Zero) {
            drawPath(parsed, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
