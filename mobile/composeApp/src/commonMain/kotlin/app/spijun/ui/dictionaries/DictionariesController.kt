package app.spijun.ui.dictionaries

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.spijun.dictionaries.CustomDictionary
import app.spijun.dictionaries.DictionaryCategory
import app.spijun.dictionaries.DictionaryEditing
import app.spijun.dictionaries.DictionaryTransfer
import app.spijun.dictionaries.Edit
import app.spijun.dictionaries.ImportPreview
import app.spijun.dictionaries.ImportResult
import app.spijun.dictionaries.ImportedDictionary
import app.spijun.dictionaries.TextProblem
import app.spijun.dictionaries.WordLanguage
import app.spijun.dictionaries.uniqueName
import app.spijun.ui.data.Dictionary
import kotlin.random.Random

/** Where the Rječnici tab is. [List] is the root, the only place the tab bar shows. */
sealed interface DictRoute {
    data object List : DictRoute
    data class BuiltIn(val id: String) : DictRoute
    data class Custom(val id: String) : DictRoute
    data class Category(val dictId: String, val name: String) : DictRoute
}

/** Things that cover the screen: sheets, dialogs and the full-screen QR. At most one at a time. */
sealed interface DictSheet {
    data object Create : DictSheet
    data class Edit(val dictId: String) : DictSheet
    data class Export(val dictId: String) : DictSheet
    data class Qr(val dictId: String) : DictSheet
    data class ConfirmDeleteDictionary(val dictId: String) : DictSheet
    data class ConfirmDeleteCategory(val dictId: String, val category: String) : DictSheet
    data class NewCategory(val dictId: String) : DictSheet
    data class RenameCategory(val dictId: String, val category: String) : DictSheet
    data class EditWord(val dictId: String, val category: String, val word: String) : DictSheet
    data class ImportPreviewSheet(val imported: ImportedDictionary, val preview: ImportPreview) : DictSheet
    data class ImportError(val problem: ImportResult) : DictSheet
}

/** What a toast says; the screen turns it into a string. [undo] puts things back. */
sealed interface ToastKind {
    data class WordAdded(val word: String) : ToastKind
    data class WordsDeleted(val first: String, val count: Int) : ToastKind
    data object LinkCopied : ToastKind
    data object CopyMade : ToastKind
    data object Imported : ToastKind
}

data class Toast(val id: Long, val kind: ToastKind, val undo: (() -> Unit)? = null)

enum class ImportChoice { Merge, SaveAsNew }

/**
 * The state and actions of the Rječnici tab. Every change to [customs] goes through here, so the
 * app only has to watch [customs] to know when to write the file. The text rules live in
 * `shared` (DictionaryEditing / DictionaryTransfer), where they are tested.
 */
@Stable
class DictionariesController(private val newId: () -> String = { "c" + Random.nextLong().toULong().toString(36) }) {
    private var library by mutableStateOf<List<CustomDictionary>>(emptyList())

    var customs: List<CustomDictionary>
        get() = library
        private set(value) {
            library = value
            // A screen whose dictionary or category is gone would draw nothing, and on iOS offer no way back.
            stack = stack.takeWhile { value.has(it) }
        }

    /**
     * False until the file has been read. Anything added before then would be replaced when the
     * file arrives, so the screens offer no adding and these actions ignore it.
     */
    var loaded by mutableStateOf(false)
        private set

    var stack by mutableStateOf<List<DictRoute>>(listOf(DictRoute.List))
        private set
    var sheet by mutableStateOf<DictSheet?>(null)
        private set
    var toast by mutableStateOf<Toast?>(null)
        private set

    private var toastCount = 0L

    val route: DictRoute get() = stack.last()
    val names: List<String> get() = customs.map { it.name }

    fun custom(id: String): CustomDictionary? = customs.firstOrNull { it.id == id }

    fun onLoaded(list: List<CustomDictionary>) {
        customs = list
        loaded = true
    }

    // Navigation

    fun push(route: DictRoute) { stack = stack + route }

    /** Pops one screen; false when already at the list, so the system back can leave the tab. */
    fun back(): Boolean {
        if (sheet != null) { sheet = null; return true }
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }

    fun open(sheet: DictSheet) { this.sheet = sheet }
    fun closeSheet() { sheet = null }

    fun showToast(kind: ToastKind, undo: (() -> Unit)? = null) { toast = Toast(++toastCount, kind, undo) }
    fun dismissToast(id: Long) { if (toast?.id == id) toast = null }

    // Dictionaries

    /** Null on success (the new dictionary opens); otherwise why the name was refused. */
    fun create(rawName: String, language: WordLanguage): TextProblem? {
        if (!loaded) return null
        return when (val edit = DictionaryEditing.create(newId(), rawName, language, names)) {
            is Edit.Refused -> edit.problem
            is Edit.Done -> {
                customs = customs + edit.dictionary
                sheet = null
                push(DictRoute.Custom(edit.dictionary.id))
                null
            }
        }
    }

    fun update(id: String, rawName: String, language: WordLanguage): TextProblem? {
        val dictionary = custom(id) ?: return null
        return when (val edit = DictionaryEditing.rename(dictionary, rawName, names - dictionary.name)) {
            is Edit.Refused -> edit.problem
            is Edit.Done -> {
                replace(DictionaryEditing.setLanguage(edit.dictionary, language))
                sheet = null
                null
            }
        }
    }

    /** Its screens close with it (see [customs]). */
    fun delete(id: String) {
        customs = customs.filter { it.id != id }
        sheet = null
    }

    /** "Duplicate" in the ⋯ menu. */
    fun duplicate(id: String) {
        val dictionary = custom(id) ?: return
        customs = customs + DictionaryEditing.copyOf(dictionary, newId(), names)
        showToast(ToastKind.CopyMade)
    }

    /** "Napravi kopiju" on a built-in dictionary: an editable copy under Moji. [displayName] is its localised name. */
    fun copyBuiltIn(builtIn: Dictionary, displayName: String) {
        if (!loaded) return
        val copy = CustomDictionary(
            id = newId(),
            name = uniqueName(displayName, names),
            language = if (builtIn.id == "en") WordLanguage.English else WordLanguage.Croatian,
            categories = builtIn.categories.map { (name, words) -> DictionaryCategory(name, words) },
        )
        customs = customs + copy
        stack = listOf(DictRoute.List, DictRoute.Custom(copy.id))
        showToast(ToastKind.CopyMade)
    }

    // Categories

    fun addCategory(dictId: String, raw: String): TextProblem? = applyText(dictId) { DictionaryEditing.addCategory(it, raw) }

    fun renameCategory(dictId: String, category: String, raw: String): TextProblem? {
        val dictionary = custom(dictId) ?: return null
        return when (val edit = DictionaryEditing.renameCategory(dictionary, category, raw)) {
            is Edit.Refused -> edit.problem
            is Edit.Done -> {
                // The open category screen follows the new name. Renamed first, so the change
                // below doesn't close it as gone.
                stack = stack.map {
                    if (it is DictRoute.Category && it.dictId == dictId && it.name == category) DictRoute.Category(dictId, edit.text) else it
                }
                replace(edit.dictionary)
                sheet = null
                null
            }
        }
    }

    /** Its screen closes with it (see [customs]). */
    fun deleteCategory(dictId: String, category: String) {
        val dictionary = custom(dictId) ?: return
        replace(DictionaryEditing.deleteCategory(dictionary, category))
        sheet = null
    }

    // Words
    // Undo reverses only its own change on the dictionary as it is now, so edits made while the
    // toast is up are kept.

    fun addWord(dictId: String, category: String, raw: String): TextProblem? {
        val dictionary = custom(dictId) ?: return null
        return when (val edit = DictionaryEditing.addWord(dictionary, category, raw)) {
            is Edit.Refused -> edit.problem
            is Edit.Done -> {
                replace(edit.dictionary)
                showToast(ToastKind.WordAdded(edit.text)) { change(dictId) { DictionaryEditing.deleteWord(it, category, edit.text) } }
                null
            }
        }
    }

    fun editWord(dictId: String, category: String, word: String, raw: String): TextProblem? {
        val result = applyText(dictId) { DictionaryEditing.editWord(it, category, word, raw) }
        if (result == null) sheet = null
        return result
    }

    fun deleteWords(dictId: String, category: String, words: Set<String>) {
        val dictionary = custom(dictId) ?: return
        val before = dictionary.category(category)?.words.orEmpty()
        val present = before.filter { it in words }
        if (present.isEmpty()) return
        replace(present.fold(dictionary) { d, w -> DictionaryEditing.deleteWord(d, category, w) })
        showToast(ToastKind.WordsDeleted(present.first(), present.size)) {
            change(dictId) { DictionaryEditing.restoreWords(it, category, before, present) }
        }
    }

    // Import

    /** [text] is what a file, link or QR gave us; null means the player cancelled the picker. */
    fun beginImport(text: String?) {
        if (text == null || !loaded) return
        val json = DictionaryTransfer.fromLink(text) ?: text
        sheet = when (val result = DictionaryTransfer.parse(json)) {
            is ImportResult.Success -> DictSheet.ImportPreviewSheet(result.dictionary, DictionaryTransfer.preview(result.dictionary, customs))
            else -> DictSheet.ImportError(result)
        }
    }

    fun confirmImport(choice: ImportChoice) {
        val shown = sheet as? DictSheet.ImportPreviewSheet ?: return
        val conflict = shown.preview.conflict?.let { custom(it.id) }
        val saved = if (conflict != null && choice == ImportChoice.Merge) {
            val merged = DictionaryTransfer.merge(conflict, shown.imported).dictionary
            replace(merged)
            merged
        } else {
            val fresh = DictionaryTransfer.saveAsNew(shown.imported, newId(), names)
            customs = customs + fresh
            fresh
        }
        sheet = null
        stack = listOf(DictRoute.List, DictRoute.Custom(saved.id))
        showToast(ToastKind.Imported)
    }

    // Helpers

    private fun applyText(dictId: String, edit: (CustomDictionary) -> Edit): TextProblem? {
        val dictionary = custom(dictId) ?: return null
        return when (val result = edit(dictionary)) {
            is Edit.Refused -> result.problem
            is Edit.Done -> { replace(result.dictionary); null }
        }
    }

    private fun replace(dictionary: CustomDictionary) {
        customs = customs.map { if (it.id == dictionary.id) dictionary else it }
    }

    /** Applies [edit] to the dictionary as it is now; nothing when it has been deleted since. */
    private fun change(dictId: String, edit: (CustomDictionary) -> CustomDictionary) {
        custom(dictId)?.let { replace(edit(it)) }
    }
}

/** Whether [route]'s dictionary and category still exist. */
private fun List<CustomDictionary>.has(route: DictRoute): Boolean = when (route) {
    DictRoute.List, is DictRoute.BuiltIn -> true
    is DictRoute.Custom -> any { it.id == route.id }
    is DictRoute.Category -> firstOrNull { it.id == route.dictId }?.category(route.name) != null
}
