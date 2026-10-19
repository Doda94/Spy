package app.spijun.ui.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

@Composable
actual fun rememberTextStorage(): TextStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidTextStorage(context.filesDir) }
}

/** Reads and writes through [AtomicFile], which syncs the new file to disk before swapping it in. */
private class AndroidTextStorage(private val dir: File) : TextStorage {
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val file = AtomicFile(File(dir, name))
        if (!file.baseFile.exists()) null else file.readFully().decodeToString(throwOnInvalidSequence = true)
    }

    override suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        val file = AtomicFile(File(dir, name))
        val out = file.startWrite()
        try {
            out.write(text.encodeToByteArray())
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
        file.finishWrite(out)
    }

    override suspend fun exists(name: String): Boolean = withContext(Dispatchers.IO) { File(dir, name).exists() }

    override suspend fun rename(from: String, to: String) = withContext(Dispatchers.IO) {
        check(File(dir, from).renameTo(File(dir, to))) { "Could not rename $from" }
    }
}

@Composable
actual fun rememberFileActions(onPicked: (String?) -> Unit): FileActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            latest.value(null)
        } else {
            scope.launch { latest.value(withContext(Dispatchers.IO) { readLimited(context, uri) }) }
        }
    }
    return remember(context, launcher) {
        FileActions(
            shareFile = { name, text -> shareFile(context, name, text) },
            shareText = { text ->
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            },
            pickFile = { launcher.launch(arrayOf("*/*")) },
            copy = { text ->
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("", text))
            },
        )
    }
}

/** A file bigger than [MAX_PICKED_BYTES] comes back as an unparsable stub, so it reads as "not a dictionary". */
private fun readLimited(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = input.readAtMost(MAX_PICKED_BYTES + 1)
        if (bytes.size > MAX_PICKED_BYTES) "" else bytes.decodeToString()
    }
} catch (_: Exception) {
    ""
}

/** `InputStream.readNBytes` only exists from API 33; this works on every version we support. */
private fun InputStream.readAtMost(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (out.size() < limit) {
        val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (n < 0) break
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}

private fun shareFile(context: Context, fileName: String, text: String) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName).apply { writeText(text) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/json")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}

@Composable
actual fun BrightnessBoost(active: Boolean) {
    val context = LocalContext.current
    DisposableEffect(active) {
        val window = context.findActivity()?.window
        val before = window?.attributes?.screenBrightness
        if (active && window != null) window.attributes = window.attributes.also { it.screenBrightness = 1f }
        onDispose {
            if (active && window != null && before != null) window.attributes = window.attributes.also { it.screenBrightness = before }
        }
    }
}
