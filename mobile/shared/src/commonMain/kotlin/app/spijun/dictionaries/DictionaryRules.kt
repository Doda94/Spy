package app.spijun.dictionaries

object DictionaryRules {
    const val MAX_WORD_LENGTH = 60
    const val MAX_CATEGORY_LENGTH = 40
    const val MAX_NAME_LENGTH = 40

    /** Link and QR carry the dictionary itself, so they stop at about 2 KB (decisions.md §6). */
    const val TRANSFER_MAX_BYTES = 2048

    /** Refuse to even parse a bigger file: no real dictionary comes close. */
    const val IMPORT_MAX_CHARS = 1_000_000

    private val whitespace = Regex("\\s+")

    /** Trims and collapses inner whitespace, so "  Zoološki   vrt " equals "Zoološki vrt". */
    fun clean(raw: String): String = raw.trim().replace(whitespace, " ")

    /** Comparison key: "Plaža" and "plaža" are the same word and the same category. */
    fun key(text: String): String = clean(text).lowercase()
}

/** Why a word, category or dictionary name was refused. */
enum class TextProblem { Empty, TooLong, Duplicate }

sealed interface TextCheck {
    /** [text] is the cleaned form to store. */
    data class Ok(val text: String) : TextCheck
    data class Rejected(val problem: TextProblem) : TextCheck
}

fun checkText(raw: String, maxLength: Int, existing: Collection<String>): TextCheck {
    val text = DictionaryRules.clean(raw)
    if (text.isEmpty()) return TextCheck.Rejected(TextProblem.Empty)
    if (text.length > maxLength) return TextCheck.Rejected(TextProblem.TooLong)
    val key = DictionaryRules.key(text)
    if (existing.any { DictionaryRules.key(it) == key }) return TextCheck.Rejected(TextProblem.Duplicate)
    return TextCheck.Ok(text)
}

/**
 * [DictionaryRules.key] equality is what makes names unique, so it is also what makes
 * "Party pack (2)" a name nobody already has.
 */
fun uniqueName(base: String, taken: Collection<String>): String {
    val takenKeys = taken.mapTo(HashSet()) { DictionaryRules.key(it) }
    val clean = DictionaryRules.clean(base)
    if (DictionaryRules.key(clean) !in takenKeys) return clean
    var n = 2
    while (true) {
        val candidate = "$clean ($n)"
        if (DictionaryRules.key(candidate) !in takenKeys) return candidate
        n++
    }
}
