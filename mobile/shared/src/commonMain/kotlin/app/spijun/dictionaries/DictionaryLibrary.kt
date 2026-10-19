package app.spijun.dictionaries

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class LibraryFile(val version: Int = 1, val dictionaries: List<CustomDictionary>)

/** The file the player's own dictionaries live in, on this phone only. */
object DictionaryLibrary {
    const val VERSION = 1

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(dictionaries: List<CustomDictionary>): String =
        json.encodeToString(LibraryFile(VERSION, dictionaries))

    /**
     * Null means the text can't be read (damaged, or from a newer app). The caller must then keep
     * the file as it is instead of overwriting it with an empty library.
     */
    fun decode(text: String): List<CustomDictionary>? = try {
        val file = json.decodeFromString<LibraryFile>(text)
        if (file.version > VERSION) null else file.dictionaries
    } catch (_: Exception) {
        null
    }
}
