package app.spijun.ui.data

import app.spijun.engine.Word
import app.spijun.resources.Res
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** A dictionary contains categories, which contain words (decisions.md §6). Built-ins only for now. */
data class Dictionary(
    /** "hr" / "en": also picks the display name and the UI language it is the default for. */
    val id: String,
    val categories: Map<String, List<String>>,
) {
    val wordCount: Int get() = categories.values.sumOf { it.size }
    fun words(category: String): List<Word> = categories[category].orEmpty().map { Word(it, category) }
}

/** Selection key: the same category name may exist in several dictionaries. */
fun categoryKey(dictId: String, category: String) = "$dictId/$category"

private val json = Json { ignoreUnknownKeys = true }
private val serializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

private suspend fun load(id: String, file: String): Dictionary {
    val text = Res.readBytes("files/$file").decodeToString()
    return Dictionary(id, json.decodeFromString(serializer, text))
}

/** Bundled with the app. Later milestones sync these from the server and cache them. */
suspend fun loadBuiltInDictionaries(): List<Dictionary> = listOf(
    load("hr", "words_hr.json"),
    load("en", "words_en.json"),
)
