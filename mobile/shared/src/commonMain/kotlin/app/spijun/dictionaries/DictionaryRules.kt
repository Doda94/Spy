package app.spijun.dictionaries

object DictionaryRules {
    const val MAX_WORD_LENGTH = 60
    const val MAX_CATEGORY_LENGTH = 40
    const val MAX_NAME_LENGTH = 40

    /**
     * The longest link, which is also exactly what a QR code carries: about 2 KB (decisions.md §6).
     * A link is ASCII, so characters are bytes; the JSON inside it is about three quarters of this.
     */
    const val TRANSFER_MAX_BYTES = 2048

    /** Refuse to even parse a bigger file: no real dictionary comes close. */
    const val IMPORT_MAX_CHARS = 1_000_000

    private val whitespace = Regex("\\s+")

    /** Trims and collapses inner whitespace, so "  Zoološki   vrt " equals "Zoološki vrt". */
    fun clean(raw: String): String = raw.trim().replace(whitespace, " ")

    /** Comparison key: "Plaža" and "plaža" are the same word and the same category. */
    fun key(text: String): String = clean(text).lowercase()

    /** Cuts [text] to at most [max] characters without splitting an emoji or ending on a space. */
    fun truncate(text: String, max: Int): String {
        if (text.length <= max) return text
        val end = if (max > 0 && text[max - 1].isHighSurrogate()) max - 1 else max
        return text.substring(0, end).trimEnd()
    }

    private const val ALPHABET = "abcčćdđefghijklmnopqrsštuvwxyzž"

    /** Letters in alphabet order, then everything before "a" (digits, spaces) first and other letters last. */
    private fun weight(c: Char): Int {
        val i = ALPHABET.indexOf(c)
        return when {
            i >= 0 -> 'a'.code + i
            c < 'a' -> c.code
            else -> 0x10000 + c.code
        }
    }

    /**
     * Word-list order, ignoring case: č, ć, đ, š and ž sit next to c, d, s and z instead of after
     * "z". Digraphs (lj, nj, dž) compare letter by letter, as phone lists do.
     */
    val alphabetical: Comparator<String> = Comparator { a, b ->
        val x = a.lowercase()
        val y = b.lowercase()
        for (i in 0 until minOf(x.length, y.length)) {
            val d = weight(x[i]) - weight(y[i])
            if (d != 0) return@Comparator d
        }
        if (x.length != y.length) x.length - y.length else a.compareTo(b)
    }
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
 * "Party pack (2)" a name nobody already has. The result always fits [maxLength], so it can be
 * saved again unchanged.
 */
fun uniqueName(base: String, taken: Collection<String>, maxLength: Int = DictionaryRules.MAX_NAME_LENGTH): String {
    val takenKeys = taken.mapTo(HashSet()) { DictionaryRules.key(it) }
    val clean = DictionaryRules.truncate(DictionaryRules.clean(base), maxLength)
    if (DictionaryRules.key(clean) !in takenKeys) return clean
    var n = 2
    while (true) {
        val suffix = " ($n)"
        // Shorten the name, never the number.
        val candidate = DictionaryRules.truncate(clean, maxLength - suffix.length) + suffix
        if (DictionaryRules.key(candidate) !in takenKeys) return candidate
        n++
    }
}
