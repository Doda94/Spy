package app.spijun.ui.screens.dictionaries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.dictionaries.CustomDictionary
import app.spijun.dictionaries.DictionaryRules
import app.spijun.dictionaries.DictionaryTransfer
import app.spijun.dictionaries.ImportResult
import app.spijun.dictionaries.TextProblem
import app.spijun.dictionaries.WordLanguage
import app.spijun.dictionaries.uniqueName
import app.spijun.resources.*
import app.spijun.ui.design.ButtonKind
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyButton
import app.spijun.ui.design.SpyIcon
import app.spijun.ui.design.SpyIconButton
import app.spijun.ui.design.SpyIcons
import app.spijun.ui.design.SpySegmented
import app.spijun.ui.design.SpyTextField
import app.spijun.ui.dictionaries.DictSheet
import app.spijun.ui.dictionaries.DictionariesController
import app.spijun.ui.dictionaries.ImportChoice
import app.spijun.ui.platform.AlertButton
import app.spijun.ui.platform.AlertButtonKind
import app.spijun.ui.platform.BrightnessBoost
import app.spijun.ui.platform.NativeAlert
import app.spijun.ui.platform.isIos
import app.spijun.ui.platform.FileActions
import app.spijun.ui.platform.exportFileName
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpySheet(onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = Spy.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface2,
        contentColor = c.text,
        scrimColor = c.scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.imePadding().padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

/** Cancel · title · Save (screens.md P1·09 d). */
@Composable
private fun SheetHeader(title: String, onCancel: () -> Unit, saveEnabled: Boolean, onSave: () -> Unit) {
    val c = Spy.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(Res.string.common_cancel), color = c.textMuted, fontWeight = FontWeight.SemiBold)
        }
        Text(title, style = Spy.type.headline, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).semantics { heading() })
        Box(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).background(if (saveEnabled) c.primary else c.surface)
                .clickable(enabled = saveEnabled, role = Role.Button, onClick = onSave).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(Res.string.common_save), fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (saveEnabled) c.onPrimary else c.textMuted)
        }
    }
}

// ───────────────────────────── d · create / rename ─────────────────────────────

@Composable
fun DictionaryFormSheet(
    title: String,
    initialName: String,
    initialLanguage: WordLanguage,
    onSave: (String, WordLanguage) -> TextProblem?,
    onClose: () -> Unit,
) {
    val c = Spy.colors
    var name by remember { mutableStateOf(initialName) }
    var language by remember { mutableStateOf(initialLanguage) }
    var problem by remember { mutableStateOf<TextProblem?>(null) }
    val canSave = name.isNotBlank()
    fun save() { if (canSave) problem = onSave(name, language) }

    SpySheet(onClose) {
        SheetHeader(title, onClose, canSave, ::save)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.dict_name).uppercase(), style = Spy.type.label, color = c.textMuted, modifier = Modifier.padding(horizontal = 6.dp))
            SpyTextField(
                value = name,
                onValue = { name = it.take(DictionaryRules.MAX_NAME_LENGTH); problem = null },
                placeholder = stringResource(Res.string.dict_name),
                label = stringResource(Res.string.dict_name),
                error = problem != null,
                onImeAction = ::save,
            )
            problem?.let { Text(nameProblemText(it), style = Spy.type.body.copy(fontSize = 14.sp), color = c.danger, modifier = Modifier.padding(horizontal = 6.dp)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.dict_language).uppercase(), style = Spy.type.label, color = c.textMuted, modifier = Modifier.padding(horizontal = 6.dp))
            SpySegmented(WordLanguage.entries, language, { languageName(it) }, { language = it })
        }
        Row(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            SpyIcon(SpyIcons.Lock, c.textMuted, size = 18.dp, modifier = Modifier.padding(top = 2.dp))
            Text(stringResource(Res.string.dict_local_hint), style = Spy.type.body.copy(fontSize = 14.sp), color = c.textMuted)
        }
    }
}

/** One text field in a sheet: new category, rename category, edit word. */
@Composable
fun TextPromptSheet(
    title: String,
    label: String,
    initial: String,
    maxLength: Int,
    problemText: @Composable (TextProblem, String) -> String,
    onConfirm: (String) -> TextProblem?,
    onClose: () -> Unit,
) {
    val c = Spy.colors
    var text by remember { mutableStateOf(initial) }
    var problem by remember { mutableStateOf<TextProblem?>(null) }
    val canSave = text.isNotBlank()
    fun save() { if (canSave) problem = onConfirm(text) }

    SpySheet(onClose) {
        SheetHeader(title, onClose, canSave, ::save)
        SpyTextField(
            value = text,
            onValue = { text = it.take(maxLength); problem = null },
            placeholder = label,
            label = label,
            error = problem != null,
            onImeAction = ::save,
        )
        problem?.let { Text(problemText(it, text), style = Spy.type.body.copy(fontSize = 14.sp), color = c.danger, modifier = Modifier.padding(horizontal = 6.dp)) }
    }
}

// ───────────────────────────── e · delete confirmation ─────────────────────────────

@Composable
fun ConfirmDeleteDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = Spy.colors
    if (isIos) {
        NativeAlert(
            title = title,
            message = body,
            buttons = listOf(
                AlertButton(confirmLabel, AlertButtonKind.Destructive, onConfirm),
                AlertButton(stringResource(Res.string.common_cancel), AlertButtonKind.Cancel, onDismiss),
            ),
            sheet = true,
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2,
        title = { Text(title, style = Spy.type.titleMedium, color = c.text) },
        text = { Text(body, style = Spy.type.body, color = c.textMuted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = c.danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel), color = c.text) } },
    )
}

@Composable
fun DeleteDictionaryDialog(dictionary: CustomDictionary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDeleteDialog(
        title = stringResource(Res.string.dict_delete_title, dictionary.name),
        body = stringResource(
            Res.string.dict_delete_body_counts,
            pluralStringResource(Res.plurals.categories, dictionary.categoryCount, dictionary.categoryCount),
            pluralStringResource(Res.plurals.words, dictionary.wordCount, dictionary.wordCount),
        ),
        confirmLabel = stringResource(Res.string.dict_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

// ───────────────────────────── P2·10 · export ─────────────────────────────

/** "0,6 KB": one decimal, the separator of the app language. */
private fun kilobytes(bytes: Int, language: String): String {
    val tenths = (bytes * 10 + 512) / 1024
    return "${tenths / 10}${if (language == "hr") ',' else '.'}${tenths % 10}"
}

@Composable
fun ExportSheet(dictionary: CustomDictionary, files: FileActions, onQr: () -> Unit, onClose: () -> Unit) {
    val c = Spy.colors
    // Built once per dictionary: the file, the link and the size all come from these two strings.
    val json = remember(dictionary) { DictionaryTransfer.export(dictionary) }
    val link = remember(dictionary) { DictionaryTransfer.toLink(dictionary) }
    val bytes = remember(json) { json.encodeToByteArray().size }
    val fits = DictionaryTransfer.fitsInLinkOrQr(link)
    val size = kilobytes(bytes, Locale.current.language)
    SpySheet(onClose) {
        Text(stringResource(Res.string.export_title), style = Spy.type.titleLarge.copy(fontSize = 28.sp, lineHeight = 34.sp), color = c.text, modifier = Modifier.semantics { heading() })
        Text(
            stringResource(Res.string.export_meta, dictionary.name, pluralStringResource(Res.plurals.words, dictionary.wordCount, dictionary.wordCount), size),
            style = Spy.type.body,
            color = c.textMuted,
        )
        OptionCard(
            selected = !fits,
            onClick = { files.shareFile(exportFileName(dictionary.name), json); onClose() },
            leading = { IconTile(SpyIcons.File, c.text) },
        ) {
            OptionTitle(stringResource(Res.string.export_file), true)
            OptionDesc(stringResource(if (fits) Res.string.export_file_desc else Res.string.export_file_best))
        }
        OptionCard(
            enabled = fits,
            dashed = !fits,
            onClick = { files.shareText(link); onClose() },
            leading = { IconTile(SpyIcons.Link, if (fits) c.text else c.textMuted) },
        ) {
            OptionTitle(stringResource(Res.string.export_link), fits)
            OptionDesc(stringResource(if (fits) Res.string.export_link_desc else Res.string.export_too_big))
        }
        OptionCard(
            enabled = fits,
            dashed = !fits,
            onClick = onQr,
            leading = { IconTile(SpyIcons.JoinCode, if (fits) c.text else c.textMuted) },
        ) {
            OptionTitle(stringResource(Res.string.export_qr), fits)
            OptionDesc(stringResource(if (fits) Res.string.export_qr_desc else Res.string.export_too_big))
        }
    }
}

@Composable
private fun OptionTitle(text: String, enabled: Boolean) {
    val c = Spy.colors
    Text(text, fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (enabled) c.text else c.textMuted)
}

@Composable
private fun OptionDesc(text: String) {
    Text(text, style = Spy.type.body.copy(fontSize = 14.sp), color = Spy.colors.textMuted)
}

/** The QR as a full screen, always on top of the tab content; brightness is boosted while it is shown. */
@Composable
fun QrScreen(dictionary: CustomDictionary, files: FileActions, onCopied: () -> Unit, onClose: () -> Unit) {
    val c = Spy.colors
    val link = remember(dictionary) { DictionaryTransfer.toLink(dictionary) }
    val painter = rememberQrCodePainter(link)
    BrightnessBoost(true)
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(onBack = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(dictionary.name, style = Spy.type.titleLarge.copy(fontSize = 30.sp), color = c.text, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(
                pluralStringResource(Res.plurals.words, dictionary.wordCount, dictionary.wordCount) + " · " + languageName(dictionary.language),
                style = Spy.type.body,
                color = c.textMuted,
            )
            // Always black on white, in both themes, with a quiet zone: that is what cameras read best.
            Box(Modifier.size(300.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(20.dp)) {
                androidx.compose.foundation.Image(painter, contentDescription = stringResource(Res.string.export_qr), modifier = Modifier.fillMaxSize())
            }
            Text(stringResource(Res.string.export_qr_how), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
        }
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SpyButton(stringResource(Res.string.export_copy_link), onClick = { files.copy(link); onCopied() }, kind = ButtonKind.Secondary)
            SpyButton(stringResource(Res.string.export_done), onClick = onClose)
        }
    }
}

// ───────────────────────────── P2·10 · import ─────────────────────────────

@Composable
fun ImportPreviewSheet(shown: DictSheet.ImportPreviewSheet, existingNames: List<String>, onChoose: (ImportChoice) -> Unit, onClose: () -> Unit) {
    val c = Spy.colors
    val p = shown.preview
    var choice by remember { mutableStateOf(ImportChoice.Merge) }
    val conflict = p.conflict != null
    val newName = remember(shown) { uniqueName(p.name, existingNames) }

    SpySheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.import_title), style = Spy.type.titleLarge.copy(fontSize = 28.sp, lineHeight = 34.sp), color = c.text, modifier = Modifier.weight(1f).semantics { heading() })
            SpyIconButton(SpyIcons.Close, stringResource(Res.string.common_close), onClose)
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LanguageTile(p.language, size = 48.dp)
                Column {
                    Text(p.name, style = Spy.type.titleMedium, color = c.text)
                    Text(languageName(p.language), style = Spy.type.body, color = c.textMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(p.categoryCount.toString(), stringResource(Res.string.import_stat_categories), Modifier.weight(1f))
                StatTile(p.wordCount.toString(), stringResource(Res.string.import_stat_words), Modifier.weight(1f))
                if (conflict) StatTile(p.alreadyHave.toString(), stringResource(Res.string.import_already_have), Modifier.weight(1f), warn = p.alreadyHave > 0)
            }
            if (conflict) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SpyIcon(SpyIcons.Warning, c.warningText, size = 20.dp)
                    Text(stringResource(Res.string.import_conflict, p.name), style = Spy.type.body, color = c.text)
                }
                OptionCard(selected = choice == ImportChoice.Merge, onClick = { choice = ImportChoice.Merge }, leading = {}) {
                    OptionTitle(stringResource(Res.string.import_merge), true)
                    OptionDesc(
                        stringResource(
                            Res.string.import_merge_desc_counts,
                            pluralStringResource(Res.plurals.new_words, p.newWords, p.newWords),
                            pluralStringResource(Res.plurals.words, p.alreadyHave, p.alreadyHave),
                        ),
                    )
                }
                OptionCard(selected = choice == ImportChoice.SaveAsNew, onClick = { choice = ImportChoice.SaveAsNew }, leading = {}) {
                    OptionTitle(stringResource(Res.string.import_save_new), true)
                    OptionDesc(stringResource(Res.string.import_save_new_desc, newName))
                }
            }
        }
        val label = when {
            !conflict -> stringResource(Res.string.dict_import)
            choice == ImportChoice.Merge -> stringResource(Res.string.import_merge_button, pluralStringResource(Res.plurals.new_words, p.newWords, p.newWords))
            else -> stringResource(Res.string.import_save_new)
        }
        SpyButton(label, onClick = { onChoose(if (conflict) choice else ImportChoice.SaveAsNew) })
        SpyButton(stringResource(Res.string.common_cancel), onClick = onClose, kind = ButtonKind.Tertiary)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, warn: Boolean = false) {
    val c = Spy.colors
    // `surface`, not `surface2`: the sheet itself is surface2, so the tile would vanish.
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(c.surface).padding(horizontal = 12.dp, vertical = 12.dp), horizontalAlignment = Alignment.Start) {
        Text(value, fontFamily = Spy.type.body.fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = if (warn) c.warningText else c.text)
        Text(label, style = Spy.type.caption, color = c.textMuted)
    }
}

@Composable
fun ImportErrorSheet(problem: ImportResult, onPickOther: () -> Unit, onClose: () -> Unit) {
    val c = Spy.colors
    SpySheet(onClose) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).border(2.dp, c.danger, CircleShape), contentAlignment = Alignment.Center) {
                SpyIcon(SpyIcons.FileInvalid, c.danger, size = 32.dp)
            }
            Text(
                stringResource(
                    when (problem) {
                        ImportResult.NewerVersion -> Res.string.import_err_version
                        ImportResult.Empty -> Res.string.import_err_empty
                        else -> Res.string.import_err_invalid
                    },
                ),
                style = Spy.type.titleLarge.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (problem == ImportResult.Invalid) {
                Text(stringResource(Res.string.import_err_invalid_body), style = Spy.type.body, color = c.textMuted, textAlign = TextAlign.Center)
            }
        }
        SpyButton(stringResource(Res.string.import_pick_other), onClick = onPickOther)
        SpyButton(stringResource(Res.string.common_close), onClick = onClose, kind = ButtonKind.Tertiary)
    }
}
