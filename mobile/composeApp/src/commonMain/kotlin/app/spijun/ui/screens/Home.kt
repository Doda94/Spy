package app.spijun.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.resources.Res
import app.spijun.resources.home_hero_category
import app.spijun.resources.home_hero_word
import app.spijun.resources.common_close
import app.spijun.resources.home_join
import app.spijun.resources.home_new_game
import app.spijun.resources.home_tagline
import app.spijun.resources.mode_multi
import app.spijun.resources.mode_multi_desc
import app.spijun.resources.mode_multi_soon
import app.spijun.resources.mode_note
import app.spijun.resources.mode_one_phone
import app.spijun.resources.mode_one_phone_desc
import app.spijun.resources.mode_title
import app.spijun.resources.reveal_category
import app.spijun.resources.reveal_secret_word
import app.spijun.resources.reveal_spy
import app.spijun.resources.reveal_you_are
import app.spijun.ui.design.ButtonKind
import app.spijun.ui.design.CardBack
import app.spijun.ui.design.CitizenFace
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyFace
import app.spijun.ui.design.SpyGlyph
import app.spijun.ui.design.SpyIcon
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpySpace
import app.spijun.ui.design.cardShadow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOnePhone: () -> Unit, onSheetChange: (Boolean) -> Unit = {}) {
    val c = Spy.colors
    var showModes by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(showModes) { onSheetChange(showModes) }

    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg)) {
        val unit = (minOf(maxWidth.value / 390f, maxHeight.value / 844f)).coerceIn(0.7f, 1.3f)
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp),
        ) {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpyGlyph(c.spy, 26.dp)
                    Text(
                        "Špijun",
                        style = Spy.type.titleLarge.copy(fontSize = 46.sp, lineHeight = 46.sp),
                        color = c.text,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Text(stringResource(Res.string.home_tagline), style = Spy.type.bodyLarge.copy(fontSize = 18.sp, lineHeight = 25.sp), color = c.textMuted, modifier = Modifier.widthIn(max = 300.dp))
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                HeroCards(unit)
            }

            Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SpyButton(stringResource(Res.string.home_new_game), onClick = { showModes = true })
                SpyButton(stringResource(Res.string.home_join), onClick = {}, kind = ButtonKind.Secondary, enabled = false, icon = SpyIcons.JoinCode)
            }
        }
    }

    if (showModes) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        fun dismiss(then: () -> Unit = {}) = scope.launch { sheet.hide() }.invokeOnCompletion { showModes = false; then() }
        ModalBottomSheet(
            onDismissRequest = { showModes = false },
            sheetState = sheet,
            containerColor = c.surface2,
            contentColor = c.text,
            scrimColor = c.scrim,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            ModeSheetContent(onOnePhone = { dismiss(onOnePhone) }, onClose = { dismiss() })
        }
    }
}

@Composable
private fun HeroCards(unit: Float) {
    val c = Spy.colors
    val cat = stringResource(Res.string.home_hero_category)
    Box(Modifier.size((340 * unit).dp, (300 * unit).dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        CardBack(
            (150 * unit).dp,
            lifted = true,
            modifier = Modifier.graphicsLayer { rotationZ = -13f; translationX = -70f * density * unit; translationY = 14f * density * unit },
        )
        CitizenFace(
            stringResource(Res.string.home_hero_word), cat, stringResource(Res.string.reveal_category, cat), stringResource(Res.string.reveal_secret_word), (150 * unit).dp,
            Modifier.graphicsLayer { rotationZ = 11f; translationX = 74f * density * unit; translationY = 6f * density * unit }
                .cardShadow((150 * unit).dp),
        )
        SpyFace(
            stringResource(Res.string.reveal_you_are), stringResource(Res.string.reveal_spy), stringResource(Res.string.reveal_category, cat),
            null, "", emptyList(), (164 * unit).dp,
            Modifier.graphicsLayer { rotationZ = -1f; translationY = -4f * density * unit }.cardShadow((164 * unit).dp),
        )
    }
}

@Composable
private fun ModeSheetContent(onOnePhone: () -> Unit, onClose: () -> Unit) {
    val c = Spy.colors
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.mode_title),
                style = Spy.type.titleLarge.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = c.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            SpyIconButton(SpyIcons.Close, stringResource(Res.string.common_close), onClose)
        }
        ModeOption(SpyIcons.OnePhone, stringResource(Res.string.mode_one_phone), stringResource(Res.string.mode_one_phone_desc), enabled = true, onClick = onOnePhone)
        ModeOption(SpyIcons.OwnPhones, stringResource(Res.string.mode_multi), stringResource(Res.string.mode_multi_desc), enabled = false, tag = stringResource(Res.string.mode_multi_soon), onClick = {})
        Text(stringResource(Res.string.mode_note), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.textMuted, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable
private fun ModeOption(icon: String, title: String, desc: String, enabled: Boolean, onClick: () -> Unit, tag: String? = null) {
    val c = Spy.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.optionFill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            SpyIcon(icon, if (enabled) c.text else c.textMuted, size = 28.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = if (enabled) c.text else c.textMuted)
            Text(desc, style = Spy.type.body, color = c.textMuted)
            if (tag != null) Text(tag.uppercase(), style = Spy.type.label, color = c.warningText, modifier = Modifier.padding(top = 4.dp))
        }
        if (enabled) SpyIcon(SpyIcons.ChevronRight, c.textMuted, size = 20.dp)
    }
}
