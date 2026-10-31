package app.spijun.ui.screens.dictionaries

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.spijun.dictionaries.DictionaryRules
import app.spijun.resources.*
import app.spijun.ui.data.Dictionary
import app.spijun.ui.dictionaries.DictRoute
import app.spijun.ui.dictionaries.DictSheet
import app.spijun.ui.dictionaries.DictionariesController
import app.spijun.ui.dictionaries.ToastKind
import app.spijun.ui.platform.FileActions
import app.spijun.ui.platform.LocalNativeBar
import app.spijun.ui.platform.PlatformBackHandler
import app.spijun.ui.platform.rememberFileActions
import org.jetbrains.compose.resources.stringResource

/** The Rječnici tab: its screens, sheets and toast (decisions.md §6, screens.md P1·09 and P2·10). */
@Composable
fun DictionariesScreen(controller: DictionariesController, builtIns: List<Dictionary>) {
    val files = rememberFileActions(onPicked = controller::beginImport)
    PlatformBackHandler(enabled = controller.route != DictRoute.List || controller.sheet != null) { controller.back() }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Crossfade(targetState = controller.route, label = "dictionaries") { route ->
            DictionaryPage(controller, builtIns, route, files)
        }
        ToastHost(controller.toast, controller::dismissToast, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
    DictionarySheets(controller, files)
}

/** One screen of the tab. iOS hosts each in its own NavigationStack destination. */
@Composable
fun DictionaryPage(controller: DictionariesController, builtIns: List<Dictionary>, route: DictRoute, files: FileActions) {
    when (route) {
        DictRoute.List -> DictionaryListScreen(controller, builtIns, onImport = files.pickFile)
        is DictRoute.BuiltIn -> builtIns.firstOrNull { it.id == route.id }?.let { BuiltInScreen(controller, it) }
        is DictRoute.Custom -> {
            // Remembered so a deleted dictionary's screen stays drawn while the native pop slides it out.
            val last = androidx.compose.runtime.remember(route.id) { arrayOfNulls<app.spijun.dictionaries.CustomDictionary>(1) }
            controller.custom(route.id)?.let { last[0] = it }
            last[0]?.let { CustomDictionaryScreen(controller, it) }
        }
        is DictRoute.Category -> {
            val last = androidx.compose.runtime.remember(route.dictId) { arrayOfNulls<app.spijun.dictionaries.CustomDictionary>(1) }
            controller.custom(route.dictId)?.let { last[0] = it }
            last[0]?.let { CategoryScreen(controller, it, route.name) }
        }
    }
}

/** The tab's sheets, dialogs and full-screen QR, drawn over whichever screen is on top. */
@Composable
fun DictionarySheets(controller: DictionariesController, files: FileActions) {
    when (val sheet = controller.sheet) {
        null -> Unit
        DictSheet.Create -> DictionaryFormSheet(
            title = stringResource(Res.string.dict_new),
            initialName = "",
            initialLanguage = defaultLanguage(),
            onSave = controller::create,
            onClose = controller::closeSheet,
        )
        is DictSheet.Edit -> controller.custom(sheet.dictId)?.let { d ->
            DictionaryFormSheet(
                title = stringResource(Res.string.dict_edit_title),
                initialName = d.name,
                initialLanguage = d.language,
                onSave = { name, language -> controller.update(d.id, name, language) },
                onClose = controller::closeSheet,
            )
        }
        is DictSheet.NewCategory -> TextPromptSheet(
            title = stringResource(Res.string.dict_new_category),
            label = stringResource(Res.string.dict_category_name),
            initial = "",
            maxLength = DictionaryRules.MAX_CATEGORY_LENGTH,
            problemText = { problem, _ -> categoryProblemText(problem) },
            onConfirm = { name -> controller.addCategory(sheet.dictId, name).also { if (it == null) controller.closeSheet() } },
            onClose = controller::closeSheet,
        )
        is DictSheet.RenameCategory -> TextPromptSheet(
            title = stringResource(Res.string.dict_rename_category),
            label = stringResource(Res.string.dict_category_name),
            initial = sheet.category,
            maxLength = DictionaryRules.MAX_CATEGORY_LENGTH,
            problemText = { problem, _ -> categoryProblemText(problem) },
            onConfirm = { name -> controller.renameCategory(sheet.dictId, sheet.category, name) },
            onClose = controller::closeSheet,
        )
        is DictSheet.EditWord -> TextPromptSheet(
            title = stringResource(Res.string.dict_edit_word),
            label = stringResource(Res.string.dict_new_word),
            initial = sheet.word,
            maxLength = DictionaryRules.MAX_WORD_LENGTH,
            problemText = { _, typed -> stringResource(Res.string.dict_duplicate, DictionaryRules.clean(typed)) },
            onConfirm = { word -> controller.editWord(sheet.dictId, sheet.category, sheet.word, word) },
            onClose = controller::closeSheet,
        )
        is DictSheet.ConfirmDeleteDictionary -> controller.custom(sheet.dictId)?.let { d ->
            DeleteDictionaryDialog(d, onConfirm = { controller.delete(d.id) }, onDismiss = controller::closeSheet)
        }
        is DictSheet.ConfirmDeleteCategory -> controller.custom(sheet.dictId)?.category(sheet.category)?.let { category ->
            ConfirmDeleteDialog(
                title = stringResource(Res.string.dict_delete_title, category.name),
                body = stringResource(
                    Res.string.dict_delete_category_body,
                    org.jetbrains.compose.resources.pluralStringResource(Res.plurals.words, category.words.size, category.words.size),
                ),
                confirmLabel = stringResource(Res.string.dict_delete_category),
                onConfirm = { controller.deleteCategory(sheet.dictId, sheet.category) },
                onDismiss = controller::closeSheet,
            )
        }
        is DictSheet.Export -> controller.custom(sheet.dictId)?.let { d ->
            ExportSheet(d, files, onQr = { controller.open(DictSheet.Qr(d.id)) }, onClose = controller::closeSheet)
        }
        is DictSheet.Qr -> controller.custom(sheet.dictId)?.let { d ->
            QrOverlay(d, files, controller)
        }
        is DictSheet.ImportPreviewSheet -> ImportPreviewSheet(
            sheet,
            existingNames = controller.names,
            onChoose = controller::confirmImport,
            onClose = controller::closeSheet,
        )
        is DictSheet.ImportError -> ImportErrorSheet(
            sheet.problem,
            onPickOther = { controller.closeSheet(); files.pickFile() },
            onClose = controller::closeSheet,
        )
    }
}

@Composable
private fun QrOverlay(dictionary: app.spijun.dictionaries.CustomDictionary, files: FileActions, controller: DictionariesController) {
    // Covers the whole tab, tab bar included (it is hidden while this is up). On iOS the native
    // bars hide too, so the QR draws its own back.
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CompositionLocalProvider(LocalNativeBar provides null) {
            QrScreen(dictionary, files, onCopied = { controller.showToast(ToastKind.LinkCopied) }, onClose = controller::closeSheet)
        }
    }
}

@Composable
private fun defaultLanguage(): app.spijun.dictionaries.WordLanguage =
    if (androidx.compose.ui.text.intl.Locale.current.language == "en") app.spijun.dictionaries.WordLanguage.English else app.spijun.dictionaries.WordLanguage.Croatian
