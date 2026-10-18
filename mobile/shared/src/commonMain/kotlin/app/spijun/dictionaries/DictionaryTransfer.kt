package app.spijun.dictionaries

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * What a dictionary looks like on its way between phones (decisions.md §6): a file, a link or a
 * QR code all carry this same JSON, and nothing passes through our server. It deliberately has
 * no id, owner or sender: the receiving phone makes its own id.
 */
@Serializable
private data class Envelope(
    val format: String,
    val version: Int,
    val name: String,
    val language: WordLanguage,
    val categories: List<DictionaryCategory>,
)

/** A dictionary that has been read from a file, link or QR but not yet saved (P2·10 d). */
data class ImportedDictionary(
    val name: String,
    val language: WordLanguage,
    val categories: List<DictionaryCategory>,
) {
    val categoryCount: Int get() = categories.size
    val wordCount: Int get() = categories.sumOf { it.words.size }
}

sealed interface ImportResult {
    data class Success(val dictionary: ImportedDictionary) : ImportResult

    /** Not JSON, not ours, or the wrong shape: `import_err_invalid`. */
    data object Invalid : ImportResult

    /** Written by a newer app: `import_err_version`. */
    data object NewerVersion : ImportResult

    /** Valid, but there is not a single usable word: `import_err_empty`. */
    data object Empty : ImportResult
}

/** The numbers on the import preview card, and whether a conflict choice is needed. */
data class ImportPreview(
    val name: String,
    val language: WordLanguage,
    val categoryCount: Int,
    val wordCount: Int,
    /** Words the player already has in the dictionary with the same name ("već imaš"). */
    val alreadyHave: Int,
    /** The dictionary with the same name, if any: only then does the player pick Merge or Save as new. */
    val conflict: CustomDictionary?,
) {
    val newWords: Int get() = wordCount - alreadyHave
}

/** What "Spoji" did, for the button label and the `toast_imported` toast. */
data class MergeResult(val dictionary: CustomDictionary, val addedWords: Int)

object DictionaryTransfer {
    const val FORMAT = "spijun-dictionary"
    const val VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }
    private val compact = Json { encodeDefaults = true }

    /** The JSON for a file, link or QR. Minified, so the size limit is about words, not whitespace. */
    fun export(dictionary: CustomDictionary): String =
        compact.encodeToString(
            Envelope(FORMAT, VERSION, dictionary.name, dictionary.language, dictionary.categories),
        )

    /** Link and QR are only offered under the limit; above it the file route is promoted (P2·10). */
    fun fitsInLinkOrQr(dictionary: CustomDictionary): Boolean =
        export(dictionary).encodeToByteArray().size <= DictionaryRules.TRANSFER_MAX_BYTES

    /**
     * Reads untrusted text. Anything that is not exactly our format is [ImportResult.Invalid]; a
     * newer `version` is reported as such before its shape is looked at, since a newer app may
     * have changed it. Words and categories are cleaned and de-duplicated the same way editing does.
     */
    fun parse(text: String): ImportResult {
        if (text.length > DictionaryRules.IMPORT_MAX_CHARS) return ImportResult.Invalid
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: Exception) {
            null
        } ?: return ImportResult.Invalid

        if ((root["format"] as? JsonPrimitive)?.content != FORMAT) return ImportResult.Invalid
        val versionField = root["version"] as? JsonPrimitive
        if (versionField == null || versionField.isString) return ImportResult.Invalid
        val version = versionField.intOrNull ?: return ImportResult.Invalid
        if (version > VERSION) return ImportResult.NewerVersion
        if (version < 1) return ImportResult.Invalid

        val envelope = try {
            json.decodeFromJsonElement(Envelope.serializer(), root)
        } catch (_: Exception) {
            return ImportResult.Invalid
        }

        val name = DictionaryRules.clean(envelope.name).take(DictionaryRules.MAX_NAME_LENGTH)
        if (name.isEmpty()) return ImportResult.Invalid

        val categories = sanitize(envelope.categories)
        if (categories.none { it.words.isNotEmpty() }) return ImportResult.Empty
        return ImportResult.Success(ImportedDictionary(name, envelope.language, categories))
    }

    fun preview(imported: ImportedDictionary, existing: List<CustomDictionary>): ImportPreview {
        val conflict = existing.firstOrNull { DictionaryRules.key(it.name) == DictionaryRules.key(imported.name) }
        val alreadyHave = if (conflict == null) 0 else imported.categories.sumOf { c ->
            val have = conflict.category(c.name)?.words.orEmpty().mapTo(HashSet()) { DictionaryRules.key(it) }
            c.words.count { DictionaryRules.key(it) in have }
        }
        return ImportPreview(
            name = imported.name,
            language = imported.language,
            categoryCount = imported.categoryCount,
            wordCount = imported.wordCount,
            alreadyHave = alreadyHave,
            conflict = conflict,
        )
    }

    /** "Spoji": adds the new categories and words, skips what the player already has, keeps their name and language. */
    fun merge(existing: CustomDictionary, imported: ImportedDictionary): MergeResult {
        var result = existing
        var added = 0
        for (incoming in imported.categories) {
            val current = result.category(incoming.name)
            if (current == null) {
                result = result.copy(categories = result.categories + incoming)
                added += incoming.words.size
                continue
            }
            val have = current.words.mapTo(HashSet()) { DictionaryRules.key(it) }
            val fresh = incoming.words.filter { DictionaryRules.key(it) !in have }
            added += fresh.size
            result = result.copy(
                categories = result.categories.map { if (it === current) it.copy(words = it.words + fresh) else it },
            )
        }
        return MergeResult(result, added)
    }

    /** "Spremi kao novi": a separate dictionary named "Party pack (2)" if "Party pack" is taken. */
    fun saveAsNew(imported: ImportedDictionary, newId: String, existingNames: Collection<String>): CustomDictionary =
        CustomDictionary(newId, uniqueName(imported.name, existingNames), imported.language, imported.categories)

    private fun sanitize(categories: List<DictionaryCategory>): List<DictionaryCategory> {
        val seen = LinkedHashMap<String, DictionaryCategory>()
        for (raw in categories) {
            val name = DictionaryRules.clean(raw.name)
            if (name.isEmpty() || name.length > DictionaryRules.MAX_CATEGORY_LENGTH) continue
            val words = LinkedHashMap<String, String>()
            for (w in raw.words) {
                val word = DictionaryRules.clean(w)
                if (word.isEmpty() || word.length > DictionaryRules.MAX_WORD_LENGTH) continue
                if (DictionaryRules.key(word) !in words) words[DictionaryRules.key(word)] = word
            }
            val key = DictionaryRules.key(name)
            val earlier = seen[key]
            // The same category listed twice: fold it together instead of keeping two.
            val merged = if (earlier == null) words.values.toList() else {
                val have = earlier.words.mapTo(HashSet()) { DictionaryRules.key(it) }
                earlier.words + words.values.filter { DictionaryRules.key(it) !in have }
            }
            seen[key] = DictionaryCategory(earlier?.name ?: name, merged)
        }
        return seen.values.toList()
    }
}
