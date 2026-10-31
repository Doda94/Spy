package app.spijun.ui.screens.dictionaries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.dictionaries.TextProblem
import app.spijun.dictionaries.WordLanguage
import app.spijun.resources.*
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyIcon
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyMotion
import app.spijun.ui.design.dashedBorder
import app.spijun.ui.dictionaries.Toast
import app.spijun.ui.dictionaries.ToastKind
import app.spijun.ui.platform.BarAction
import app.spijun.ui.platform.BarSpec
import app.spijun.ui.platform.LocalNativeBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun languageName(language: WordLanguage): String = when (language) {
    WordLanguage.Croatian -> stringResource(Res.string.dict_name_hr)
    WordLanguage.English -> stringResource(Res.string.dict_name_en)
}

/** "3 kategorije · 50 riječi". */
@Composable
fun countsText(categories: Int, words: Int): String =
    pluralStringResource(Res.plurals.categories, categories, categories) + " · " + pluralStringResource(Res.plurals.words, words, words)

@Composable
fun nameProblemText(problem: TextProblem): String = when (problem) {
    TextProblem.Duplicate -> stringResource(Res.string.dict_name_taken)
    // Empty disables the button and the field stops at the limit, so these two are only a safety net.
    TextProblem.Empty, TextProblem.TooLong -> stringResource(Res.string.dict_name)
}

@Composable
fun categoryProblemText(problem: TextProblem): String = when (problem) {
    TextProblem.Duplicate -> stringResource(Res.string.dict_category_taken)
    TextProblem.Empty, TextProblem.TooLong -> stringResource(Res.string.dict_category_name)
}

/** The language tile on rows: "HR" / "EN", 40 dp, radius 12 (screens.md P1·09 a). */
@Composable
fun LanguageTile(language: WordLanguage, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val c = Spy.colors
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
        Text(
            language.code.uppercase(),
            fontFamily = Spy.type.body.fontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = c.text,
        )
    }
}

/**
 * Back, an optional title and [actions] at the top of a pushed screen. Where a native navigation
 * bar is on screen (iOS) they are handed to it and nothing is drawn here. [cancels] makes back a
 * ✕ that leaves a mode (selection) rather than the screen.
 */
@Composable
fun TopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: List<BarAction> = emptyList(),
    cancels: Boolean = false,
) {
    val native = LocalNativeBar.current
    if (native != null) {
        val closeLabel = stringResource(Res.string.common_close)
        val spec = BarSpec(title, actions, if (cancels && onBack != null) BarAction(SpyIcons.Close, "xmark", closeLabel, onClick = onBack) else null)
        SideEffect { native.show(spec) }
        DisposableEffect(native) { onDispose { native.show(null) } }
        return
    }
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            SpyIconButton(if (app.spijun.ui.platform.isIos) SpyIcons.Back else SpyIcons.BackAndroid, stringResource(Res.string.common_back), onBack)
        }
        if (title != null) {
            Text(
                title,
                style = Spy.type.headline.copy(fontFamily = Spy.type.titleLarge.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                color = Spy.colors.text,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { heading() },
            )
        } else {
            Box(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            actions.forEach { BarButton(it) }
        }
    }
}

@Composable
private fun BarButton(action: BarAction) {
    if (action.menu.isEmpty()) {
        SpyIconButton(action.icon, action.label, action.onClick)
        return
    }
    val c = Spy.colors
    var open by remember { mutableStateOf(false) }
    Box {
        SpyIconButton(action.icon, action.label, { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface2) {
            action.menu.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, color = if (item.danger) c.danger else c.text) },
                    onClick = { open = false; item.onClick() },
                )
            }
        }
    }
}

/** A tappable row of a grouped panel: [content] on the left, a chevron on the right. */
@Composable
fun NavRow(onClick: () -> Unit, minHeight: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.weight(1f)) { content() }
        SpyIcon(SpyIcons.ChevronRight, Spy.colors.textMuted, size = 18.dp)
    }
}

@Composable
fun toastText(kind: ToastKind): String = when (kind) {
    is ToastKind.WordAdded -> stringResource(Res.string.toast_word_added, kind.word)
    is ToastKind.WordsDeleted ->
        if (kind.count == 1) stringResource(Res.string.toast_word_deleted, kind.first)
        else stringResource(Res.string.toast_words_deleted, pluralStringResource(Res.plurals.words, kind.count, kind.count))
    ToastKind.LinkCopied -> stringResource(Res.string.toast_link_copied)
    ToastKind.CopyMade -> stringResource(Res.string.toast_copy_made)
    ToastKind.Imported -> stringResource(Res.string.toast_imported)
}

/** The bottom toast with an optional Undo (screens.md "toast"); it dismisses itself after 4 s. */
@Composable
fun ToastHost(toast: Toast?, onDismiss: (Long) -> Unit, modifier: Modifier = Modifier) {
    if (toast == null) return
    val c = Spy.colors
    LaunchedEffect(toast.id) {
        delay(SpyMotion.TOAST_MS.toLong())
        onDismiss(toast.id)
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface2)
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(start = 18.dp, end = 6.dp)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(toastText(toast.kind), style = Spy.type.body, color = c.text, modifier = Modifier.weight(1f).padding(vertical = 8.dp), maxLines = 2)
        if (toast.undo != null) {
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { toast.undo.invoke(); onDismiss(toast.id) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(Res.string.toast_undo), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.text)
            }
        }
    }
}

/** Radio-style option card used by the export and import sheets: `optionFill`, selected = 1.5 px `text` border. */
@Composable
fun OptionCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    dashed: Boolean = false,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = Spy.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (dashed) androidx.compose.ui.graphics.Color.Transparent else c.optionFill)
            .then(
                when {
                    dashed -> Modifier.dashedBorder(c.line, shape, 1.5.dp)
                    selected -> Modifier.border(1.5.dp, c.text, shape)
                    else -> Modifier
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) { content() }
    }
}

@Composable
fun IconTile(path: String, tint: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val c = Spy.colors
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(c.surface2), contentAlignment = Alignment.Center) {
        SpyIcon(path, tint, size = 24.dp)
    }
}
