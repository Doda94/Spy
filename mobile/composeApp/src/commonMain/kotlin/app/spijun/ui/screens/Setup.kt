package app.spijun.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.dictionaries.SetupSelection
import app.spijun.engine.Rules
import app.spijun.engine.SetupProblem
import app.spijun.engine.VotingMode
import app.spijun.resources.Res
import app.spijun.resources.common_cancel
import app.spijun.resources.common_done
import app.spijun.resources.common_back
import app.spijun.resources.player_default_name
import app.spijun.resources.words_in_selection
import app.spijun.resources.setup_categories
import app.spijun.resources.setup_add_words
import app.spijun.resources.setup_dict_builtin_inline
import app.spijun.resources.setup_dict_custom_inline
import app.spijun.resources.setup_dict_no_categories
import app.spijun.resources.setup_empty_pool_body
import app.spijun.resources.setup_dictionaries
import app.spijun.resources.setup_empty_pool
import app.spijun.resources.setup_loading
import app.spijun.resources.setup_name_field
import app.spijun.resources.setup_names_filled
import app.spijun.resources.setup_names_duplicate
import app.spijun.resources.setup_names_missing
import app.spijun.resources.setup_names_title
import app.spijun.resources.setup_none_selected
import app.spijun.resources.setup_player_names
import app.spijun.resources.setup_player_names_hint
import app.spijun.resources.setup_players
import app.spijun.resources.setup_select_all
import app.spijun.resources.setup_spies
import app.spijun.resources.setup_spies_max
import app.spijun.resources.setup_start
import app.spijun.resources.setup_timer
import app.spijun.resources.setup_timer_custom
import app.spijun.resources.setup_timer_custom_title
import app.spijun.resources.setup_timer_minutes
import app.spijun.resources.setup_timer_off
import app.spijun.resources.setup_title
import app.spijun.resources.setup_vote_app
import app.spijun.resources.setup_vote_app_hint_one_phone
import app.spijun.resources.setup_vote_loud
import app.spijun.resources.setup_voting
import app.spijun.resources.mode_one_phone
import app.spijun.ui.AppController
import app.spijun.ui.design.ButtonKind
import app.spijun.ui.design.CategoryRow
import app.spijun.ui.design.Panel
import app.spijun.ui.design.PanelDivider
import app.spijun.ui.design.ScreenTitle
import app.spijun.ui.design.SectionLabel
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyChips
import app.spijun.ui.design.SpyIcon
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpySegmented
import app.spijun.ui.design.SpySpace
import app.spijun.ui.design.SpyStepperRow
import app.spijun.ui.design.StickyFooter
import app.spijun.ui.platform.LocalNativeBar
import app.spijun.ui.platform.PlatformBackHandler
import app.spijun.ui.platform.isIos
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val TIMER_PRESETS = listOf<Int?>(null, 180, 300, 480)

@Composable
fun SetupScreen(
    controller: AppController,
    onBack: () -> Unit,
    onAddWords: (dictId: String) -> Unit,
    onStart: (defaultName: (Int) -> String) -> Unit,
) {
    val c = Spy.colors
    val s = controller.setup
    val sources = controller.sources
    var showNames by remember { mutableStateOf(false) }
    var showCustomTimer by remember { mutableStateOf(false) }
    PlatformBackHandler(true, onBack)

    val defaultNames = List(Rules.MAX_PLAYERS) { stringResource(Res.string.player_default_name, it + 1) }
    val problem = controller.problem()
    val namesFilled = s.names.take(s.players).count { it.isNotBlank() }
    val namesRequired = s.voting == VotingMode.InApp

    Column(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SpySpace.gutter),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Under a native navigation bar (iOS) back is the bar's own.
            if (LocalNativeBar.current == null) {
                Box(Modifier.padding(top = 8.dp)) {
                    SpyIconButton(if (isIos) SpyIcons.Back else SpyIcons.BackAndroid, stringResource(Res.string.common_back), onBack)
                }
            }
            ScreenTitle(stringResource(Res.string.setup_title))
            ModeChip()

            Panel {
                SpyStepperRow(stringResource(Res.string.setup_players), s.players, Rules.MIN_PLAYERS..Rules.MAX_PLAYERS, { v -> controller.update { it.copy(players = v) } })
                PanelDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { showNames = true }.padding(start = 18.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.setup_player_names), style = Spy.type.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = c.text)
                        Text(stringResource(Res.string.setup_player_names_hint), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.textMuted)
                    }
                    val ok = namesFilled == s.players
                    Text(
                        stringResource(Res.string.setup_names_filled, namesFilled, s.players),
                        style = Spy.type.body,
                        color = if (ok) c.success else if (namesRequired) c.warningText else c.textMuted,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    SpyIcon(SpyIcons.ChevronRight, c.textMuted, size = 20.dp)
                }
                PanelDivider()
                SpyStepperRow(
                    stringResource(Res.string.setup_spies), s.spies, 1..Rules.maxSpies(s.players), { v -> controller.update { it.copy(spies = v) } },
                    hint = stringResource(Res.string.setup_spies_max, Rules.maxSpies(s.players)),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(Res.string.setup_timer))
                val custom = s.timerSeconds != null && s.timerSeconds !in TIMER_PRESETS
                val options = TIMER_PRESETS + listOf(-1)
                SpyChips(
                    options = options,
                    isSelected = { if (it == -1) custom else it == s.timerSeconds },
                    label = { opt ->
                        when {
                            opt == null -> stringResource(Res.string.setup_timer_off)
                            opt == -1 -> if (custom) stringResource(Res.string.setup_timer_minutes, s.timerSeconds!! / 60) else stringResource(Res.string.setup_timer_custom)
                            else -> stringResource(Res.string.setup_timer_minutes, opt / 60)
                        }
                    },
                    onSelect = { opt -> if (opt == -1) showCustomTimer = true else controller.update { it.copy(timerSeconds = opt) } },
                    isCustom = { it == -1 },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(Res.string.setup_voting))
                SpySegmented(
                    VotingMode.entries, s.voting,
                    label = { if (it == VotingMode.InApp) stringResource(Res.string.setup_vote_app) else stringResource(Res.string.setup_vote_loud) },
                    onSelect = { v -> controller.update { it.copy(voting = v) } },
                )
                if (s.voting == VotingMode.InApp) {
                    Text(stringResource(Res.string.setup_vote_app_hint_one_phone), style = Spy.type.caption.copy(fontSize = 13.sp), color = c.textMuted, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }

            if (sources != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(Res.string.setup_dictionaries))
                    SpyChips(
                        options = sources,
                        isSelected = { it.id in s.dictionaries },
                        label = { it.name },
                        onSelect = { controller.toggleDictionary(it.id) },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val selectedDicts = sources.filter { it.id in s.dictionaries }
                    val allSelected = controller.allCategoriesSelected()
                    SectionLabel(stringResource(Res.string.setup_categories), trailing = {
                        if (selectedDicts.isNotEmpty()) {
                            TextButton(onClick = { controller.setAllCategories(!allSelected) }) {
                                Text(stringResource(Res.string.setup_select_all), color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    })
                    val noneSelected = controller.selectedCategoryCount() == 0
                    val toFill = controller.dictionariesToFill()
                    Column(
                        Modifier.fillMaxWidth().then(if (noneSelected) Modifier.border(1.5.dp, c.warning, RoundedCornerShape(22.dp)) else Modifier),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        selectedDicts.forEach { dict ->
                            val label = if (dict.builtIn) Res.string.setup_dict_builtin_inline else Res.string.setup_dict_custom_inline
                            Text(
                                stringResource(label, dict.name),
                                style = Spy.type.caption.copy(fontSize = 13.sp),
                                color = c.textMuted,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                            if (dict.categories.isNotEmpty()) Panel {
                                dict.categories.forEachIndexed { index, category ->
                                    if (index > 0) PanelDivider(52.dp)
                                    val key = SetupSelection.key(dict.id, category.name)
                                    CategoryRow(category.name, category.words.size, key in s.categories, { controller.toggleCategory(key) }, stateText = if (key in s.categories) "✓" else "")
                                }
                            }
                            val noCategories = dict.categories.isEmpty()
                            if (!dict.builtIn && (noCategories || dict.id in toFill)) {
                                EmptyDictionaryCard(
                                    stringResource(if (noCategories) Res.string.setup_dict_no_categories else Res.string.setup_empty_pool_body),
                                    onAddWords = { onAddWords(dict.id) },
                                )
                            }
                        }
                    }
                }
            }
            Box(Modifier.height(8.dp))
        }

        StickyFooter {
            val reason = when (problem) {
                SetupProblem.NoCategories -> stringResource(Res.string.setup_none_selected) to c.warningText
                SetupProblem.EmptyPool -> stringResource(Res.string.setup_empty_pool) to c.danger
                SetupProblem.MissingNames -> stringResource(Res.string.setup_names_missing) to c.warningText
                SetupProblem.DuplicateNames -> stringResource(Res.string.setup_names_duplicate) to c.warningText
                else -> null
            }
            val poolSize = controller.pool().size
            val text = when {
                sources == null -> stringResource(Res.string.setup_loading) to c.textMuted
                reason != null -> reason
                else -> pluralStringResource(Res.plurals.words_in_selection, poolSize, poolSize) to c.textMuted
            }
            Text(text.first, style = Spy.type.body, color = text.second, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            SpyButton(stringResource(Res.string.setup_start), onClick = { onStart { defaultNames[it - 1] } }, enabled = sources != null && problem == null)
        }
    }

    if (showNames) NamesSheet(controller, defaultNames) { showNames = false }
    if (showCustomTimer) CustomTimerDialog(s.timerSeconds, { showCustomTimer = false }) { minutes ->
        controller.update { it.copy(timerSeconds = minutes * 60) }
        showCustomTimer = false
    }
}

/** P1·02 f: why a custom dictionary adds no words, and the way to fix it. */
@Composable
private fun EmptyDictionaryCard(text: String, onAddWords: () -> Unit) {
    val c = Spy.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpyIcon(SpyIcons.Error, c.danger, size = 22.dp)
        Text(text, style = Spy.type.body, color = c.text, modifier = Modifier.weight(1f))
        TextButton(onClick = onAddWords) {
            Text(stringResource(Res.string.setup_add_words), color = c.text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModeChip() {
    val c = Spy.colors
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(50)).background(c.surface2).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SpyIcon(SpyIcons.Phone, c.text, size = 16.dp)
        Text(stringResource(Res.string.mode_one_phone), style = Spy.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = c.text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NamesSheet(controller: AppController, defaults: List<String>, onClose: () -> Unit) {
    val c = Spy.colors
    val s = controller.setup
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface2,
        contentColor = c.text,
        scrimColor = c.scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.setup_names_title), style = Spy.type.titleLarge.copy(fontSize = 28.sp, lineHeight = 34.sp), color = c.text)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until s.players) {
                    NameField(
                        value = s.names[i],
                        placeholder = stringResource(Res.string.setup_name_field, i + 1),
                        duplicate = s.names[i].isNotBlank() && s.names.take(s.players).count { it.trim().equals(s.names[i].trim(), ignoreCase = true) } > 1,
                        onValue = { v -> controller.update { st -> st.copy(names = st.names.toMutableList().also { it[i] = v.take(Rules.MAX_NAME_LENGTH) }) } },
                    )
                }
            }
            SpyButton(stringResource(Res.string.common_done), onClick = onClose)
        }
    }
}

@Composable
private fun NameField(value: String, placeholder: String, duplicate: Boolean, onValue: (String) -> Unit) {
    val c = Spy.colors
    var focused by remember { mutableStateOf(false) }
    val border = when {
        duplicate -> c.danger
        focused -> c.text
        else -> c.line
    }
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = Spy.type.bodyLarge.copy(color = c.text),
        cursorBrush = SolidColor(c.text),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            // The placeholder disappears once a name is typed; keep the seat as the field's label.
            .semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).background(c.surface)
                    .border(if (focused || duplicate) 2.dp else 1.dp, border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = Spy.type.bodyLarge, color = c.textMuted)
                inner()
            }
        },
    )
}

@Composable
private fun CustomTimerDialog(current: Int?, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val c = Spy.colors
    var minutes by remember { mutableStateOf(((current ?: 300) / 60).coerceIn(1, 30)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2,
        title = { Text(stringResource(Res.string.setup_timer_custom_title), style = Spy.type.titleMedium, color = c.text) },
        text = {
            Panel {
                SpyStepperRow(stringResource(Res.string.setup_timer_minutes, minutes), minutes, 1..30, { minutes = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(minutes) }) { Text(stringResource(Res.string.common_done), color = c.text, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel), color = c.textMuted) } },
    )
}
