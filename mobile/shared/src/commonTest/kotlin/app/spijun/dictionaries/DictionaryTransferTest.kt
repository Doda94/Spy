package app.spijun.dictionaries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryTransferTest {
    private val mine = CustomDictionary(
        id = "d1",
        name = "Party pack",
        language = WordLanguage.Croatian,
        categories = listOf(
            DictionaryCategory("Filmovi", listOf("Titanic", "Shrek")),
            DictionaryCategory("Hrana", listOf("Pizza")),
        ),
    )

    private fun parsed(text: String): ImportedDictionary =
        assertIs<ImportResult.Success>(DictionaryTransfer.parse(text)).dictionary

    private fun file(vararg fields: String) = "{" + fields.joinToString(",") + "}"
    private val ok = """"format":"spijun-dictionary","version":1"""

    @Test
    fun exportThenParseRoundTripsWithoutAnId() {
        val text = DictionaryTransfer.export(mine)
        assertFalse("d1" in text, "the id must not leave the phone")
        val back = parsed(text)
        assertEquals(ImportedDictionary("Party pack", WordLanguage.Croatian, mine.categories), back)
    }

    @Test
    fun exportKeepsCroatianLettersAndNamesTheLanguage() {
        val text = DictionaryTransfer.export(mine.copy(categories = listOf(DictionaryCategory("Šuma", listOf("Čežnja")))))
        assertTrue("Čežnja" in text)
        assertTrue(""""language":"hr"""" in text)
        assertTrue(""""format":"spijun-dictionary"""" in text)
    }

    @Test
    fun linkAndQrStopAtAboutTwoKilobytes() {
        assertTrue(DictionaryTransfer.fitsInLinkOrQr(mine))
        // Distinct words so nothing is de-duplicated; 80 short words is the design's own example.
        val eighty = DictionaryCategory("Riječi", List(80) { "riječ$it" })
        assertTrue(DictionaryTransfer.fitsInLinkOrQr(mine.copy(categories = listOf(eighty))))
        val many = DictionaryCategory("Riječi", List(400) { "riječ broj $it" })
        assertFalse(DictionaryTransfer.fitsInLinkOrQr(mine.copy(categories = listOf(many))))
    }

    @Test
    fun transferLimitIsOnTheLinkTheQrCarries() {
        // The link is base64 of the UTF-8 bytes, about 4/3 of the JSON ("č" is two bytes), so a
        // dictionary whose JSON is under the limit can still make a link that is over it.
        val heavy = mine.copy(categories = listOf(DictionaryCategory("K", List(100) { "ččččč$it" })))
        val json = DictionaryTransfer.export(heavy)
        val link = DictionaryTransfer.toLink(heavy)
        assertTrue(json.encodeToByteArray().size < DictionaryRules.TRANSFER_MAX_BYTES)
        assertTrue(link.length > DictionaryRules.TRANSFER_MAX_BYTES)
        assertFalse(DictionaryTransfer.fitsInLinkOrQr(heavy))
        assertFalse(DictionaryTransfer.fitsInLinkOrQr(link))
        assertTrue(DictionaryTransfer.fitsInLinkOrQr(DictionaryTransfer.toLink(mine)))
    }

    @Test
    fun notJsonOrNotOursIsInvalid() {
        val invalid = listOf(
            "",
            "pizza",
            "[]",
            "\"x\"",
            "{}",
            file(""""format":"something-else","version":1,"name":"A","language":"hr","categories":[]"""),
            file(""""format":"spijun-dictionary","name":"A","language":"hr","categories":[]"""),
            file(""""format":"spijun-dictionary","version":"1","name":"A","language":"hr","categories":[]"""),
            file(""""format":"spijun-dictionary","version":0,"name":"A","language":"hr","categories":[]"""),
            file("""$ok,"name":"A","language":"fr","categories":[{"name":"K","words":["a"]}]"""),
            file("""$ok,"name":"A","language":"hr","categories":"nope""""),
            file("""$ok,"name":"   ","language":"hr","categories":[{"name":"K","words":["a"]}]"""),
        )
        for (text in invalid) {
            assertEquals(ImportResult.Invalid, DictionaryTransfer.parse(text), "should be invalid: $text")
        }
    }

    @Test
    fun aNewerVersionIsReportedBeforeLookingAtItsShape() {
        assertEquals(
            ImportResult.NewerVersion,
            DictionaryTransfer.parse("""{"format":"spijun-dictionary","version":2,"wholly":"different"}"""),
        )
    }

    @Test
    fun oversizedInputIsRefusedWithoutParsing() {
        assertEquals(ImportResult.Invalid, DictionaryTransfer.parse(" ".repeat(DictionaryRules.IMPORT_MAX_CHARS + 1)))
    }

    @Test
    fun noUsableWordsIsEmptyNotInvalid() {
        assertEquals(ImportResult.Empty, DictionaryTransfer.parse(file("""$ok,"name":"A","language":"hr","categories":[]""")))
        assertEquals(
            ImportResult.Empty,
            DictionaryTransfer.parse(file("""$ok,"name":"A","language":"hr","categories":[{"name":"K","words":["  ", ""]}]""")),
        )
    }

    @Test
    fun importCleansShortensDropsAndFoldsDuplicates() {
        val tooLongWord = "w".repeat(DictionaryRules.MAX_WORD_LENGTH + 1)
        val tooLongName = "c".repeat(DictionaryRules.MAX_CATEGORY_LENGTH + 1)
        val text = file(
            """$ok,"name":"  Moj   paket ","language":"en","unknown":true,"categories":[""" +
                """{"name":" Filmovi ","words":["Titanic","TITANIC","  Zov   divljine ","","$tooLongWord"]},""" +
                """{"name":"","words":["x"]},""" +
                """{"name":"$tooLongName","words":["x"]},""" +
                """{"name":"filmovi","words":["Shrek","titanic"]},""" +
                """{"name":"Prazno","words":[]}]""",
        )
        val imported = parsed(text)
        assertEquals("Moj paket", imported.name)
        assertEquals(WordLanguage.English, imported.language)
        assertEquals(
            listOf(
                DictionaryCategory("Filmovi", listOf("Titanic", "Zov divljine", "Shrek")),
                // An overlong category name is shortened, not dropped with its words.
                DictionaryCategory("c".repeat(DictionaryRules.MAX_CATEGORY_LENGTH), listOf("x")),
                DictionaryCategory("Prazno", emptyList()),
            ),
            imported.categories,
        )
        assertEquals(3, imported.categoryCount)
        assertEquals(4, imported.wordCount)
    }

    @Test
    fun importTruncatesAnOverlongName() {
        val imported = parsed(
            file("""$ok,"name":"${"n".repeat(100)}","language":"hr","categories":[{"name":"K","words":["a"]}]"""),
        )
        assertEquals(DictionaryRules.MAX_NAME_LENGTH, imported.name.length)

        // The cut never leaves a trailing space.
        val spaced = parsed(
            file("""$ok,"name":"${"n".repeat(39)} ${"m".repeat(20)}","language":"hr","categories":[{"name":"K","words":["a"]}]"""),
        )
        assertEquals("n".repeat(39), spaced.name)
    }

    private val incoming = ImportedDictionary(
        "party PACK",
        WordLanguage.English,
        listOf(
            DictionaryCategory("filmovi", listOf("TITANIC", "Matrix", "Up")),
            DictionaryCategory("Glazba", listOf("Jazz")),
        ),
    )

    @Test
    fun previewCountsWordsYouAlreadyHaveOnlyOnANameMatch() {
        val clash = DictionaryTransfer.preview(incoming, listOf(mine))
        assertEquals(mine, clash.conflict)
        assertEquals(4, clash.wordCount)
        assertEquals(2, clash.categoryCount)
        assertEquals(1, clash.alreadyHave)
        assertEquals(3, clash.newWords)

        val free = DictionaryTransfer.preview(incoming, listOf(mine.copy(name = "Drugi")))
        assertNull(free.conflict)
        assertEquals(0, free.alreadyHave)
        assertEquals(4, free.newWords)
    }

    @Test
    fun mergeAddsNewWordsAndCategoriesAndKeepsYourNameAndLanguage() {
        val merged = DictionaryTransfer.merge(mine, incoming)
        assertEquals(3, merged.addedWords)
        val dict = merged.dictionary
        assertEquals("Party pack", dict.name)
        assertEquals(WordLanguage.Croatian, dict.language)
        assertEquals("d1", dict.id)
        assertEquals(listOf("Titanic", "Shrek", "Matrix", "Up"), dict.category("Filmovi")!!.words)
        assertEquals(listOf("Pizza"), dict.category("Hrana")!!.words)
        assertEquals(listOf("Jazz"), dict.category("Glazba")!!.words)
    }

    @Test
    fun mergingTheSameDictionaryTwiceChangesNothing() {
        val once = DictionaryTransfer.merge(mine, incoming).dictionary
        val twice = DictionaryTransfer.merge(once, incoming)
        assertEquals(0, twice.addedWords)
        assertEquals(once, twice.dictionary)
    }

    @Test
    fun saveAsNewPicksAFreeNameAndKeepsTheContent() {
        val saved = DictionaryTransfer.saveAsNew(incoming, "d9", listOf("Party pack"))
        assertEquals("d9", saved.id)
        assertEquals("party PACK (2)", saved.name)
        assertEquals(WordLanguage.English, saved.language)
        assertEquals(incoming.categories, saved.categories)
        assertNotNull(saved.category("Glazba"))
        assertEquals("party PACK", DictionaryTransfer.saveAsNew(incoming, "d9", emptyList()).name)
    }
}

class DictionaryLinkAndLibraryTest {
    private val dict = CustomDictionary(
        "d1", "Ljeto na Visu", WordLanguage.Croatian,
        listOf(DictionaryCategory("Šuma", listOf("Čežnja", "Žaba")), DictionaryCategory("Hrana", listOf("Pizza"))),
    )

    @Test
    fun linkRoundTripsAndCarriesNoPaddingOrSlashes() {
        val link = DictionaryTransfer.toLink(dict)
        assertTrue(link.startsWith(DictionaryTransfer.LINK_PREFIX))
        val payload = link.removePrefix(DictionaryTransfer.LINK_PREFIX)
        assertTrue(payload.none { it == '=' || it == '/' || it == '+' || it == ' ' })
        val json = assertNotNull(DictionaryTransfer.fromLink("  $link \n"))
        assertEquals(
            ImportedDictionary("Ljeto na Visu", WordLanguage.Croatian, dict.categories),
            assertIs<ImportResult.Success>(DictionaryTransfer.parse(json)).dictionary,
        )
    }

    @Test
    fun foreignOrDamagedLinksAreNull() {
        assertNull(DictionaryTransfer.fromLink("https://example.com/?d=abc"))
        assertNull(DictionaryTransfer.fromLink(DictionaryTransfer.LINK_PREFIX + "***not base64***"))
        // Valid base64, but not UTF-8.
        assertNull(DictionaryTransfer.fromLink(DictionaryTransfer.LINK_PREFIX + "_w"))
    }

    @Test
    fun libraryRoundTrips() {
        val text = DictionaryLibrary.encode(listOf(dict))
        assertEquals(listOf(dict), DictionaryLibrary.decode(text))
        assertEquals(emptyList(), DictionaryLibrary.decode(DictionaryLibrary.encode(emptyList())))
    }

    @Test
    fun unreadableLibraryIsNullNotEmpty() {
        assertNull(DictionaryLibrary.decode(""))
        assertNull(DictionaryLibrary.decode("{broken"))
        assertNull(DictionaryLibrary.decode("""{"version":2,"dictionaries":[]}"""))
        assertNull(DictionaryLibrary.decode("""{"version":1,"dictionaries":[{"id":1}]}"""))
    }
}
