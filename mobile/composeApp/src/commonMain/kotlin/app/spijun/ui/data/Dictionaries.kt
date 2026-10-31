package app.spijun.ui.data

import app.spijun.dictionaries.SetupCategory
import app.spijun.dictionaries.SetupSource
import app.spijun.engine.Word
import app.spijun.resources.Res
import app.spijun.resources.dict_name_en
import app.spijun.resources.dict_name_hr
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

/** A dictionary contains categories, which contain words (decisions.md §6). Built-ins only for now. */
data class Dictionary(
    /** "hr" / "en": also picks the display name and the UI language it is the default for. */
    val id: String,
    /** Display name, from the strings now; from the server once built-ins are synced. */
    val name: String,
    val categories: Map<String, List<String>>,
) {
    val wordCount: Int get() = categories.values.sumOf { it.size }
    fun words(category: String): List<Word> = categories[category].orEmpty().map { Word(it, category) }
}

fun Dictionary.asSetupSource() =
    SetupSource(id, name, builtIn = true, categories = categories.map { (name, words) -> SetupCategory(name, words) })

private val json = Json { ignoreUnknownKeys = true }
private val serializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

private suspend fun load(id: String, name: String, file: String): Dictionary {
    val text = Res.readBytes("files/$file").decodeToString()
    return Dictionary(id, name, json.decodeFromString(serializer, text))
}

/** Bundled with the app. Later milestones sync these from the server and cache them. */
suspend fun loadBuiltInDictionaries(): List<Dictionary> = listOf(
    load("hr", getString(Res.string.dict_name_hr), "words_hr.json"),
    load("en", getString(Res.string.dict_name_en), "words_en.json"),
)
