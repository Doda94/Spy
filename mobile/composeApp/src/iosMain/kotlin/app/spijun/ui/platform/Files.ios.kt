package app.spijun.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPasteboard
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import androidx.compose.runtime.DisposableEffect
import platform.CoreGraphics.CGRectMake

@Composable
actual fun rememberTextStorage(): TextStorage = remember { IosTextStorage() }

@OptIn(ExperimentalForeignApi::class)
private class IosTextStorage : TextStorage {
    private val dir: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        NSFileManager.defaultManager.createDirectoryAtPath(base, withIntermediateDirectories = true, attributes = null, error = null)
        base
    }

    // stringWithContentsOfFile is also null for a file that exists but isn't UTF-8, so check first.
    override suspend fun read(name: String): String? {
        val path = "$dir/$name"
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        return checkNotNull(NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)) { "Could not read $name" }
    }

    // `atomically` writes to a temporary file and swaps it in, so a crash keeps the old file.
    override suspend fun write(name: String, text: String) {
        val ok = NSString.create(string = text).writeToFile("$dir/$name", true, NSUTF8StringEncoding, null)
        check(ok) { "Could not write $name" }
    }

    override suspend fun exists(name: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath("$dir/$name")

    override suspend fun rename(from: String, to: String) {
        check(NSFileManager.defaultManager.moveItemAtPath("$dir/$from", toPath = "$dir/$to", error = null)) { "Could not rename $from" }
    }
}

private fun topViewController(): UIViewController? {
    @Suppress("DEPRECATION")
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private class PickerDelegate(private val onPicked: (String?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) { onPicked(null); return }
        val size = (NSFileManager.defaultManager.attributesOfItemAtPath(url.path ?: "", null)?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
        // Too big to be a dictionary: hand back an unparsable stub rather than reading it.
        onPicked(if (size > MAX_PICKED_BYTES) "" else NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null) ?: "")
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onPicked(null)
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberFileActions(onPicked: (String?) -> Unit): FileActions {
    val latest = rememberUpdatedState(onPicked)
    // The picker only holds its delegate weakly, so this object keeps it alive.
    val delegate = remember { PickerDelegate { latest.value(it) } }
    return remember {
        FileActions(
            shareFile = { name, text ->
                val path = NSTemporaryDirectory() + name
                NSString.create(string = text).writeToFile(path, true, NSUTF8StringEncoding, null)
                present(listOf(NSURL.fileURLWithPath(path)))
            },
            shareText = { text -> present(listOf(text)) },
            pickFile = {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = true)
                picker.delegate = delegate
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            },
            copy = { text -> UIPasteboard.generalPasteboard.string = text },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun present(items: List<Any>) {
    val top = topViewController() ?: return
    val sheet = UIActivityViewController(activityItems = items, applicationActivities = null)
    // iPad shows the share sheet as a popover, which needs an anchor.
    sheet.popoverPresentationController?.sourceView = top.view
    sheet.popoverPresentationController?.sourceRect = top.view.bounds.useContents { CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
    top.presentViewController(sheet, animated = true, completion = null)
}

@Composable
actual fun BrightnessBoost(active: Boolean) {
    DisposableEffect(active) {
        val before = UIScreen.mainScreen.brightness
        if (active) UIScreen.mainScreen.brightness = 1.0
        onDispose { if (active) UIScreen.mainScreen.brightness = before }
    }
}
