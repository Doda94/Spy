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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberTextStorage(): TextStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidTextStorage(context.filesDir) }
}

private class AndroidTextStorage(private val dir: File) : TextStorage {
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        File(dir, name).takeIf { it.isFile }?.readText()
    }

    override suspend fun write(name: String, text: String) = withContext(Dispatchers.IO) {
        val target = File(dir, name)
        val temp = File(dir, "$name.tmp")
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            target.delete()
            check(temp.renameTo(target)) { "Could not replace $name" }
        }
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
        val bytes = input.readNBytes(MAX_PICKED_BYTES + 1)
        if (bytes.size > MAX_PICKED_BYTES) "" else bytes.decodeToString()
    }
} catch (_: Exception) {
    ""
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
