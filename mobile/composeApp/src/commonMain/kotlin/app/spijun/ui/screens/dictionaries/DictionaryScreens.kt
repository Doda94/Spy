package app.spijun.ui.screens.dictionaries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.dictionaries.CustomDictionary
import app.spijun.dictionaries.DictionaryRules
import app.spijun.dictionaries.TextProblem
import app.spijun.dictionaries.WordLanguage
import app.spijun.resources.*
import app.spijun.ui.data.Dictionary
import app.spijun.ui.design.ButtonKind
import app.spijun.ui.design.Panel
import app.spijun.ui.design.PanelDivider
import app.spijun.ui.design.ScreenTitle
import app.spijun.ui.design.SectionLabel
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyIcon
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpyTextField
import app.spijun.ui.design.StickyFooter
import app.spijun.ui.design.dashedBorder
import app.spijun.ui.dictionaries.DictRoute
import app.spijun.ui.dictionaries.DictSheet
import app.spijun.ui.dictionaries.DictionariesController
import app.spijun.ui.platform.PlatformBackHandler
import app.spijun.ui.platform.isIos
import org.jetbrains.compose.resources.pluralStringResource
import app.spijun.ui.platform.BarAction
import org.jetbrains.compose.resources.stringResource

private fun builtInLanguage(d: Dictionary) = if (d.id == "en") WordLanguage.English else WordLanguage.Croatian

@Composable
private fun builtInName(d: Dictionary) = languageName(builtInLanguage(d))

// ───────────────────────────── a · list (and f · empty) ─────────────────────────────

@Composable
fun DictionaryListScreen(controller: DictionariesController, builtIns: List<Dictionary>, onImport: () -> Unit) {
    val c = Spy.colors
    Column(Modifier.fillMaxSize()) {
        // Nothing can be added until the saved dictionaries are in, or the file would replace it.
        TopBar(
            onBack = null,
            actions = if (!controller.loaded) emptyList() else listOf(
                BarAction(SpyIcons.Import, "square.and.arrow.down", stringResource(Res.string.dict_import), onClick = onImport),
                BarAction(SpyIcons.Add, "plus", stringResource(Res.string.dict_new), onClick = { controller.open(DictSheet.Create) }),
            ),
        )
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { ScreenTitle(stringResource(Res.string.dict_title)) }
            item { SectionLabel(stringResource(Res.string.dict_section_builtin)) }
            item {
                Panel {
                    builtIns.forEachIndexed { i, d ->
                        if (i > 0) PanelDivider(inset = 72.dp)
                        DictionaryRow(
                            language = builtInLanguage(d),
                            name = builtInName(d),
                            meta = countsText(d.categories.size, d.wordCount),
                            metaWarning = false,
                            onClick = { controller.push(DictRoute.BuiltIn(d.id)) },
                            trailing = { BuiltInBadge() },
                        )
                    }
                }
            }
            item { SectionLabel(stringResource(Res.string.dict_section_mine)) }
            if (controller.customs.isEmpty()) {
                if (controller.loaded) item { EmptyCustom(onNew = { controller.open(DictSheet.Create) }, onImport = onImport) }
            } else item {
                Panel {
                    controller.customs.forEachIndexed { i, d ->
                        if (i > 0) PanelDivider(inset = 72.dp)
                        val empty = d.wordCount == 0
                        DictionaryRow(
                            language = d.language,
                            name = d.name,
                            meta = if (empty) pluralStringResource(Res.plurals.categories, d.categoryCount, d.categoryCount) + " · " + stringResource(Res.string.dict_category_empty) else countsText(d.categoryCount, d.wordCount),
                            metaWarning = empty,
                            onClick = { controller.push(DictRoute.Custom(d.id)) },
                            trailing = { SpyIcon(SpyIcons.ChevronRight, c.textMuted, size = 18.dp) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryRow(
    language: WordLanguage,
    name: String,
    meta: String,
    metaWarning: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val c = Spy.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LanguageTile(language)
        Column(Modifier.weight(1f)) {
            Text(name, fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = c.text, maxLines = 2)
            Text(meta, style = Spy.type.body, color = if (metaWarning) c.warningText else c.textMuted)
        }
        trailing()
    }
}

@Composable
private fun BuiltInBadge() {
    val c = Spy.colors
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, c.line, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SpyIcon(SpyIcons.Lock, c.textMuted, size = 14.dp)
        Text(stringResource(Res.string.dict_builtin), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = c.textMuted)
    }
}

@Composable
private fun EmptyCustom(onNew: () -> Unit, onImport: () -> Unit) {
    val c = Spy.colors
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().dashedBorder(c.line, shape, 1.5.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpyIcon("M12 3l9 9-9 9-9-9z", c.textMuted, size = 40.dp)
        Text(stringResource(Res.string.dict_empty_title), style = Spy.type.headline.copy(fontSize = 18.sp), color = c.text, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(stringResource(Res.string.dict_empty_body), style = Spy.type.body, color = c.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        SpyButton(stringResource(Res.string.dict_new), onNew, small = true)
        SpyButton(stringResource(Res.string.dict_import), onImport, kind = ButtonKind.Secondary, small = true)
    }
}

// ───────────────────────────── g · built-in, read-only ─────────────────────────────

@Composable
fun BuiltInScreen(controller: DictionariesController, dictionary: Dictionary) {
    val c = Spy.colors
    val name = builtInName(dictionary)
    Column(Modifier.fillMaxSize()) {
        TopBar(onBack = { controller.back() })
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScreenTitle(name, Modifier.weight(1f, fill = false))
                        BuiltInBadge()
                    }
                    Text(countsText(dictionary.categories.size, dictionary.wordCount), style = Spy.type.body, color = c.textMuted)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpyIcon(SpyIcons.Lock, c.textMuted, size = 20.dp)
                    Text(stringResource(Res.string.dict_readonly), style = Spy.type.body, color = c.textMuted)
                }
            }
            item {
                Panel {
                    dictionary.categories.entries.forEachIndexed { i, (category, words) ->
                        if (i > 0) PanelDivider()
                        Row(Modifier.fillMaxWidth().heightIn(min = 55.dp).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(category, style = Spy.type.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                            Text(words.size.toString(), style = Spy.type.body.copy(fontFeatureSettings = "tnum"), color = c.textMuted)
                        }
                    }
                }
            }
        }
        StickyFooter {
            SpyButton(stringResource(Res.string.dict_copy), onClick = { controller.copyBuiltIn(dictionary, name) }, enabled = controller.loaded)
        }
    }
}

// ───────────────────────────── b · custom dictionary ─────────────────────────────

@Composable
fun CustomDictionaryScreen(controller: DictionariesController, dictionary: CustomDictionary) {
    val c = Spy.colors
    Column(Modifier.fillMaxSize()) {
        TopBar(
            onBack = { controller.back() },
            actions = listOf(
                BarAction(SpyIcons.Share, "square.and.arrow.up", stringResource(Res.string.dict_share), onClick = { controller.open(DictSheet.Export(dictionary.id)) }),
                BarAction(
                    SpyIcons.More, "ellipsis", stringResource(Res.string.dict_more),
                    menu = listOf(
                        BarAction(SpyIcons.More, "pencil", stringResource(Res.string.dict_edit_details), onClick = { controller.open(DictSheet.Edit(dictionary.id)) }),
                        BarAction(SpyIcons.More, "plus.square.on.square", stringResource(Res.string.dict_duplicate_dict), onClick = { controller.duplicate(dictionary.id) }),
                        BarAction(SpyIcons.Delete, "trash", stringResource(Res.string.dict_delete), danger = true, onClick = { controller.open(DictSheet.ConfirmDeleteDictionary(dictionary.id)) }),
                    ),
                ),
            ),
        )
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScreenTitle(dictionary.name)
                    Text(languageName(dictionary.language) + " · " + countsText(dictionary.categoryCount, dictionary.wordCount), style = Spy.type.body, color = c.textMuted)
                }
            }
            if (dictionary.categories.isEmpty()) {
                item { Text(stringResource(Res.string.dict_no_categories), style = Spy.type.body, color = c.textMuted, modifier = Modifier.padding(horizontal = 6.dp)) }
            } else item {
                Panel {
                    dictionary.categories.forEachIndexed { i, category ->
                        if (i > 0) PanelDivider()
                        NavRow(onClick = { controller.push(DictRoute.Category(dictionary.id, category.name)) }, minHeight = 55.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category.name, style = Spy.type.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                                Text(
                                    category.words.size.toString(),
                                    style = Spy.type.body.copy(fontFeatureSettings = "tnum"),
                                    color = if (category.words.isEmpty()) c.warningText else c.textMuted,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(22.dp)).border(1.dp, c.line, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button) { controller.open(DictSheet.NewCategory(dictionary.id)) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpyIcon(SpyIcons.Add, c.text, size = 20.dp)
                    Text(stringResource(Res.string.dict_new_category), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = c.text, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

// ───────────────────────────── c · category words ─────────────────────────────

/** Searching only helps once the list is long enough to scroll. */
private const val SEARCH_FROM = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(controller: DictionariesController, dictionary: CustomDictionary, categoryName: String) {
    val c = Spy.colors
    // Remembered so a deleted category's screen stays drawn while the native pop slides it out.
    val lastCategory = remember { arrayOfNulls<app.spijun.dictionaries.DictionaryCategory>(1) }
    dictionary.category(categoryName)?.let { lastCategory[0] = it }
    val category = lastCategory[0] ?: return
    var draft by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<TextProblem?>(null) }
    var query by remember { mutableStateOf("") }
    // Android: long-press starts a multi-select, shown in a contextual top bar (screens.md P1·09 c).
    var selected by remember { mutableStateOf(setOf<String>()) }
    // System back ends the selection first, like the bar's own back arrow.
    PlatformBackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }

    fun add() {
        if (draft.isBlank()) return
        problem = controller.addWord(dictionary.id, category.name, draft)
        if (problem == null) draft = ""
    }

    val shown = remember(category.words, query) {
        val key = DictionaryRules.key(query)
        val all = category.words.sortedWith(DictionaryRules.alphabetical)
        if (key.isEmpty()) all else all.filter { key in DictionaryRules.key(it) }
    }

    Column(Modifier.fillMaxSize()) {
        if (selected.isNotEmpty()) {
            TopBar(
                onBack = { selected = emptySet() },
                title = stringResource(Res.string.dict_selected, selected.size),
                actions = listOf(
                    BarAction(SpyIcons.Delete, "trash", stringResource(Res.string.dict_delete_short), danger = true, onClick = {
                        controller.deleteWords(dictionary.id, category.name, selected)
                        selected = emptySet()
                    }),
                ),
                cancels = true,
            )
        } else {
            TopBar(
                onBack = { controller.back() },
                title = category.name + " · " + category.words.size,
                actions = listOf(
                    BarAction(
                        SpyIcons.More, "ellipsis", stringResource(Res.string.dict_more),
                        menu = listOf(
                            BarAction(SpyIcons.More, "pencil", stringResource(Res.string.dict_rename_category), onClick = { controller.open(DictSheet.RenameCategory(dictionary.id, category.name)) }),
                            BarAction(SpyIcons.Delete, "trash", stringResource(Res.string.dict_delete_category), danger = true, onClick = { controller.open(DictSheet.ConfirmDeleteCategory(dictionary.id, category.name)) }),
                        ),
                    ),
                ),
            )
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SpyTextField(
                            value = draft,
                            onValue = { draft = it.take(DictionaryRules.MAX_WORD_LENGTH); problem = null },
                            placeholder = stringResource(Res.string.dict_new_word),
                            label = stringResource(Res.string.dict_new_word),
                            error = problem != null,
                            onImeAction = ::add,
                            modifier = Modifier.weight(1f),
                        )
                        val canAdd = draft.isNotBlank()
                        Box(
                            Modifier.height(52.dp).clip(RoundedCornerShape(12.dp)).background(if (canAdd) c.primary else c.surface2)
                                .clickable(enabled = canAdd, role = Role.Button, onClick = ::add).padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(Res.string.dict_add), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (canAdd) c.onPrimary else c.textMuted)
                        }
                    }
                    if (problem == TextProblem.Duplicate) {
                        Text(stringResource(Res.string.dict_duplicate, DictionaryRules.clean(draft)), style = Spy.type.body.copy(fontSize = 14.sp), color = c.danger, modifier = Modifier.padding(horizontal = 6.dp))
                    }
                }
            }
            if (category.words.size >= SEARCH_FROM) item {
                SpyTextField(
                    value = query,
                    onValue = { query = it },
                    placeholder = pluralStringResource(Res.plurals.search_words, category.words.size, category.words.size),
                    label = pluralStringResource(Res.plurals.search_words, category.words.size, category.words.size),
                    leading = { SpyIcon(SpyIcons.Search, c.textMuted, size = 20.dp) },
                )
            }
            when {
                category.words.isEmpty() -> item { Text(stringResource(Res.string.dict_empty_words), style = Spy.type.body, color = c.textMuted, modifier = Modifier.padding(6.dp)) }
                shown.isEmpty() -> item { Text(stringResource(Res.string.dict_no_results), style = Spy.type.body, color = c.textMuted, modifier = Modifier.padding(6.dp)) }
                else -> items(shown, key = { it }) { word ->
                    WordRow(
                        word = word,
                        selected = word in selected,
                        onTap = {
                            if (selected.isNotEmpty()) selected = if (word in selected) selected - word else selected + word
                            else controller.open(DictSheet.EditWord(dictionary.id, category.name, word))
                        },
                        onLongPress = { if (!isIos) selected = selected + word },
                        onSwipeDelete = { controller.deleteWords(dictionary.id, category.name, setOf(word)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WordRow(word: String, selected: Boolean, onTap: () -> Unit, onLongPress: () -> Unit, onSwipeDelete: () -> Unit) {
    val c = Spy.colors
    val row: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 51.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) c.surface2 else c.surface)
                .combinedClickable(role = Role.Button, onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(word, style = Spy.type.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
            if (selected) SpyIcon(SpyIcons.Check, c.text, size = 20.dp)
        }
    }
    if (isIos) {
        // Plain `remember`, not the saveable rememberSwipeToDismissBoxState: the list keeps saved
        // state per word, so a word brought back by Undo (or typed again) would return already
        // swiped away and be deleted again at once.
        val threshold = SwipeToDismissBoxDefaults.positionalThreshold
        val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
        androidx.compose.runtime.LaunchedEffect(state.currentValue) {
            if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onSwipeDelete()
        }
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(c.dangerFill).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(stringResource(Res.string.dict_delete_short), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, color = c.onDangerFill)
                }
            },
        ) { row() }
    } else {
        row()
    }
}
