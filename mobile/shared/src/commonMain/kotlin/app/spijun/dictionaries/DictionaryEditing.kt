package app.spijun.dictionaries

/** The result of an edit that takes typed text: the new dictionary, or why the text was refused. */
sealed interface Edit {
    /** [text] is the cleaned text that was stored, for the "added" toast. */
    data class Done(val dictionary: CustomDictionary, val text: String) : Edit
    data class Refused(val problem: TextProblem) : Edit
}

/** Pure edits of a custom dictionary (decisions.md §6). Persisting the result is the caller's job. */
object DictionaryEditing {
    fun create(id: String, rawName: String, language: WordLanguage, existingNames: Collection<String>): Edit =
        when (val name = checkText(rawName, DictionaryRules.MAX_NAME_LENGTH, existingNames)) {
            is TextCheck.Rejected -> Edit.Refused(name.problem)
            is TextCheck.Ok -> Edit.Done(CustomDictionary(id, name.text, language, emptyList()), name.text)
        }

    /** [otherNames] are the names of the player's other dictionaries; the dictionary may keep its own. */
    fun rename(dictionary: CustomDictionary, rawName: String, otherNames: Collection<String>): Edit =
        when (val name = checkText(rawName, DictionaryRules.MAX_NAME_LENGTH, otherNames)) {
            is TextCheck.Rejected -> Edit.Refused(name.problem)
            is TextCheck.Ok -> Edit.Done(dictionary.copy(name = name.text), name.text)
        }

    fun setLanguage(dictionary: CustomDictionary, language: WordLanguage): CustomDictionary =
        dictionary.copy(language = language)

    /** "Duplicate" in the ⋯ menu and "Napravi kopiju" on a built-in: same content, new id and name. */
    fun copyOf(dictionary: CustomDictionary, newId: String, existingNames: Collection<String>): CustomDictionary =
        dictionary.copy(id = newId, name = uniqueName(dictionary.name, existingNames))

    fun addCategory(dictionary: CustomDictionary, rawName: String): Edit =
        when (val name = checkText(rawName, DictionaryRules.MAX_CATEGORY_LENGTH, dictionary.categories.map { it.name })) {
            is TextCheck.Rejected -> Edit.Refused(name.problem)
            is TextCheck.Ok -> Edit.Done(
                dictionary.copy(categories = dictionary.categories + DictionaryCategory(name.text, emptyList())),
                name.text,
            )
        }

    fun renameCategory(dictionary: CustomDictionary, category: String, rawName: String): Edit {
        val others = dictionary.categories.map { it.name }.filter { it != category }
        return when (val name = checkText(rawName, DictionaryRules.MAX_CATEGORY_LENGTH, others)) {
            is TextCheck.Rejected -> Edit.Refused(name.problem)
            is TextCheck.Ok -> Edit.Done(
                dictionary.mapCategory(category) { it.copy(name = name.text) },
                name.text,
            )
        }
    }

    fun deleteCategory(dictionary: CustomDictionary, category: String): CustomDictionary =
        dictionary.copy(categories = dictionary.categories.filter { it.name != category })

    /** A word is a duplicate when its category already has it, ignoring case (screens.md P1·09 c). */
    fun addWord(dictionary: CustomDictionary, category: String, rawWord: String): Edit {
        val current = requireNotNull(dictionary.category(category)) { "No category \"$category\"" }
        return when (val word = checkText(rawWord, DictionaryRules.MAX_WORD_LENGTH, current.words)) {
            is TextCheck.Rejected -> Edit.Refused(word.problem)
            is TextCheck.Ok -> Edit.Done(
                dictionary.mapCategory(category) { it.copy(words = it.words + word.text) },
                word.text,
            )
        }
    }

    fun editWord(dictionary: CustomDictionary, category: String, word: String, rawWord: String): Edit {
        val current = requireNotNull(dictionary.category(category)) { "No category \"$category\"" }
        return when (val checked = checkText(rawWord, DictionaryRules.MAX_WORD_LENGTH, current.words.filter { it != word })) {
            is TextCheck.Rejected -> Edit.Refused(checked.problem)
            is TextCheck.Ok -> Edit.Done(
                dictionary.mapCategory(category) { c -> c.copy(words = c.words.map { if (it == word) checked.text else it }) },
                checked.text,
            )
        }
    }

    fun deleteWord(dictionary: CustomDictionary, category: String, word: String): CustomDictionary =
        dictionary.mapCategory(category) { c -> c.copy(words = c.words.filter { it != word }) }

    private fun CustomDictionary.mapCategory(name: String, change: (DictionaryCategory) -> DictionaryCategory) =
        copy(categories = categories.map { if (it.name == name) change(it) else it })
}
