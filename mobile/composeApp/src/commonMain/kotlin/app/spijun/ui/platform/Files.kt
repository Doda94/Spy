package app.spijun.ui.platform

import androidx.compose.runtime.Composable

/** Small private text files that live only on this phone (the player's own dictionaries). */
interface TextStorage {
    /** Null only when the file doesn't exist yet; throws when it exists but can't be read as UTF-8. */
    suspend fun read(name: String): String?

    /** Replaces the whole file; a crash mid-write must leave the old file intact. */
    suspend fun write(name: String, text: String)

    suspend fun exists(name: String): Boolean

    /** Renames [from] to [to]; throws when that fails. [to] must not exist. */
    suspend fun rename(from: String, to: String)
}

@Composable
expect fun rememberTextStorage(): TextStorage

/**
 * The few system hand-offs the dictionary screens need. They are plain functions so common code
 * never touches a platform type; [pickFile] reports through the `onPicked` given to
 * [rememberFileActions] (null when the player cancelled).
 */
class FileActions(
    /** Opens the share sheet with [text] as a `.json` file called [fileName]. */
    val shareFile: (fileName: String, text: String) -> Unit,
    /** Opens the share sheet with [text] as plain text (a link). */
    val shareText: (text: String) -> Unit,
    val pickFile: () -> Unit,
    val copy: (text: String) -> Unit,
)

@Composable
expect fun rememberFileActions(onPicked: (String?) -> Unit): FileActions

/** Turns the screen brightness up while [active] (the QR screen), and puts it back after. */
@Composable
expect fun BrightnessBoost(active: Boolean)

/** A file name that is safe on both platforms; the dictionary's own name, plus ".json". */
fun exportFileName(dictionaryName: String): String {
    val base = dictionaryName.map { if (it.isLetterOrDigit() || it in " ._-") it else '_' }.joinToString("").trim().ifEmpty { "spijun" }
    return "$base.json"
}

/** Largest file the picker will read; real dictionaries are a few KB. */
const val MAX_PICKED_BYTES = 2_000_000
