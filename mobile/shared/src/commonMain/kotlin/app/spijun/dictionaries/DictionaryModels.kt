package app.spijun.dictionaries

import app.spijun.engine.Word
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The language of a dictionary's words (decisions.md §6). Independent of the UI language. */
@Serializable
enum class WordLanguage(val code: String) {
    @SerialName("hr") Croatian("hr"),
    @SerialName("en") English("en"),
}

@Serializable
data class DictionaryCategory(val name: String, val words: List<String>)

/**
 * A dictionary the player owns: stored only on this phone, never sent to the server
 * (decisions.md §6). Immutable, so every edit returns a new value and the UI can undo by
 * keeping the previous one.
 */
@Serializable
data class CustomDictionary(
    val id: String,
    val name: String,
    val language: WordLanguage,
    val categories: List<DictionaryCategory>,
) {
    val categoryCount: Int get() = categories.size
    val wordCount: Int get() = categories.sumOf { it.words.size }

    /** Names are unique ignoring case, so "filmovi" finds "Filmovi". */
    fun category(name: String): DictionaryCategory? =
        categories.firstOrNull { DictionaryRules.key(it.name) == DictionaryRules.key(name) }

    /** The engine's view of one category, for drawing the secret word. */
    fun words(category: String): List<Word> = category(category)?.words.orEmpty().map { Word(it, category) }
}
