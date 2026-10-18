package app.spijun.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.ui.platform.isIos

enum class ButtonKind { Primary, Secondary, Tertiary, Destructive }

/** Press feedback: scale 0.97 in 100 ms (skipped under Reduce Motion). */
@Composable
private fun pressScale(pressed: Boolean): Float {
    val reduce = LocalReduceMotion.current
    val s by animateFloatAsState(if (pressed && !reduce) SpyMotion.PRESS_SCALE else 1f, tween100())
    return s
}

private fun tween100() = androidx.compose.animation.core.tween<Float>(SpyMotion.PRESS_MS)

@Composable
fun SpyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    small: Boolean = false,
    icon: String? = null,
) {
    val c = Spy.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val height = when {
        small -> 52.dp
        isIos -> SpySpace.buttonIos
        else -> SpySpace.buttonAndroid
    }
    // Android Large buttons morph radius 16 -> full on press (M3 Expressive).
    val shape: Shape = when {
        isIos || small -> RoundedCornerShape(50)
        pressed -> RoundedCornerShape(50)
        else -> SpyShape.md
    }
    val (bg, fg) = when {
        !enabled && kind != ButtonKind.Tertiary -> c.disabledFill to c.textMuted
        kind == ButtonKind.Primary -> c.primary to c.onPrimary
        kind == ButtonKind.Secondary -> c.surface2 to c.text
        kind == ButtonKind.Destructive -> c.dangerFill to c.onDangerFill
        else -> Color.Transparent to if (enabled) c.text else c.textMuted
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .scale(pressScale(pressed))
            .clip(shape)
            .background(bg)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            SpyIcon(icon, fg, size = 22.dp)
            Box(Modifier.width(10.dp))
        }
        Text(
            text,
            style = Spy.type.button,
            color = fg,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Round 48 dp button with an icon, used for close / back. Chrome layer. */
@Composable
fun SpyIconButton(path: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Spy.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .size(SpySpace.minTarget)
            .scale(pressScale(pressed))
            .clip(CircleShape)
            .background(if (isIos) c.glass else c.surface2)
            .then(if (isIos) Modifier.border(1.dp, c.glassLine, CircleShape) else Modifier)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        SpyIcon(path, c.text, size = 22.dp)
    }
}

/** In-game header pill (40 tall). */
@Composable
fun SpyPill(text: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val c = Spy.colors
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isIos) c.glass else c.surface2)
            .then(if (isIos) Modifier.border(1.dp, c.glassLine, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading?.invoke()
        Text(text, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = Spy.type.body.fontFamily)
    }
}

/** Grouped panel: `surface`, radius 22, rows separated by a hairline. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(SpyShape.lg).background(Spy.colors.surface), content = content)
}

@Composable
fun PanelDivider(inset: Dp = 16.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(Spy.colors.line))
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = Spy.type.label,
            color = Spy.colors.textMuted,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke(this)
    }
}

@Composable
fun SpyStepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    hintWarning: Boolean = false,
) {
    val c = Spy.colors
    val haptics = Spy.haptics
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 18.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Spy.type.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = c.text)
            if (hint != null) {
                Text(hint, style = Spy.type.caption.copy(fontSize = 13.sp), color = if (hintWarning) c.warningText else c.textMuted)
            }
        }
        StepperButton("−", enabled = value > range.first, description = label + " −") {
            haptics.perform(SpyHaptic.StepperTick); onChange(value - 1)
        }
        Text(
            value.toString(),
            style = Spy.type.headline.copy(fontSize = 24.sp, fontFeatureSettings = "tnum"),
            color = c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 36.dp).semantics { stateDescription = "$label $value" },
        )
        StepperButton("+", enabled = value < range.last, description = label + " +") {
            haptics.perform(SpyHaptic.StepperTick); onChange(value + 1)
        }
    }
}

@Composable
private fun StepperButton(glyph: String, enabled: Boolean, description: String, onClick: () -> Unit) {
    val c = Spy.colors
    Box(
        Modifier
            .size(SpySpace.minTarget)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description; if (!enabled) disabled() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 24.sp, color = if (enabled) c.text else c.disabledFill, fontFamily = Spy.type.body.fontFamily)
    }
}

/** Wrapping chip row. [custom] chips draw a dashed outline (opens a picker). */
@Composable
fun <T> SpyChips(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isCustom: (T) -> Boolean = { false },
) {
    val haptics = Spy.haptics
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val selected = isSelected(option)
            val c = Spy.colors
            val bg by animateColorAsState(if (selected) c.primary else c.surface)
            val text = label(option)
            Row(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .then(if (isCustom(option) && !selected) DashedBorder(c.checkboxOutline, RoundedCornerShape(50)) else Modifier)
                    .clickable(role = Role.RadioButton) { haptics.perform(SpyHaptic.StepperTick); onSelect(option) }
                    .padding(horizontal = 14.dp)
                    .semantics { this.selected = selected },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (selected) SpyIcon(SpyIcons.Check, c.onPrimary, size = 16.dp, strokeWidth = 3f)
                Text(
                    text,
                    fontFamily = Spy.type.body.fontFamily,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) c.onPrimary else c.text,
                )
            }
        }
    }
}

/** Segmented control: track `surface`, selected thumb `primary`. */
@Composable
fun <T> SpySegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spy.colors
    val haptics = Spy.haptics
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(c.surface).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) c.primary else Color.Transparent)
                    .clickable(role = Role.RadioButton) { haptics.perform(SpyHaptic.StepperTick); onSelect(option) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    fontFamily = Spy.type.body.fontFamily,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (on) c.onPrimary else c.text,
                )
            }
        }
    }
}

@Composable
fun CategoryRow(
    name: String,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    countText: String = count.toString(),
    stateText: String,
) {
    val c = Spy.colors
    val haptics = Spy.haptics
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 55.dp)
            .clickable(role = Role.Checkbox, onClick = { haptics.perform(SpyHaptic.StepperTick); onToggle() })
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { stateDescription = stateText },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) c.primary else Color.Transparent)
                .then(if (!checked) Modifier.border(1.5.dp, c.checkboxOutline, RoundedCornerShape(6.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) SpyIcon(SpyIcons.Check, c.onPrimary, size = 15.dp, strokeWidth = 3.2f)
        }
        Text(name, style = Spy.type.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
        Text(
            countText,
            style = Spy.type.body.copy(fontFeatureSettings = "tnum"),
            color = if (count == 0) c.danger else c.textMuted,
        )
    }
}

/** A sticky footer: hairline, status line, primary action. */
@Composable
fun StickyFooter(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Spy.colors.bg),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Spy.colors.line))
        Column(
            Modifier.padding(horizontal = SpySpace.gutter).padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** Border helper for dashed outlines (empty seat, custom chip, dropped player). */
@Composable
fun DashedBorder(color: Color, shape: Shape, width: Dp = 1.5.dp): Modifier = Modifier.dashedBorder(color, shape, width)

fun Modifier.dashedBorder(color: Color, shape: Shape, width: Dp = 1.5.dp, dash: Dp = 6.dp): Modifier =
    drawBehind {
        val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
        val path = Path().apply { addOutline(outline) }
        val d = dash.toPx()
        drawPath(path, color, style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(d, d))))
    }

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Spy.type.titleLarge,
        color = Spy.colors.text,
        modifier = modifier.semantics { heading() },
    )
}

/** Detects a tap without consuming scroll; used by the whole-card flip target. */
fun Modifier.tapTarget(onTap: () -> Unit): Modifier = pointerInput(onTap) { detectTapGestures { onTap() } }
