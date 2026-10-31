package app.spijun.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private fun presenterFrom(host: UIViewController?): UIViewController? {
    var top = host ?: topViewController()
    while (top?.presentedViewController != null && top.presentedViewController?.isBeingDismissed() == false) {
        top = top.presentedViewController
    }
    return top
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeAlert(
    title: String?,
    message: String,
    buttons: List<AlertButton>,
    sheet: Boolean,
    onUnavailable: (() -> Unit)?,
) {
    val current by rememberUpdatedState(buttons)
    val unavailable by rememberUpdatedState(onUnavailable)
    val host = LocalUIViewController.current
    DisposableEffect(title, message, sheet) {
        val alert = UIAlertController.alertControllerWithTitle(
            title,
            message,
            if (sheet) UIAlertControllerStyleActionSheet else UIAlertControllerStyleAlert,
        )
        current.forEachIndexed { i, button ->
            val style = when (button.kind) {
                AlertButtonKind.Default -> UIAlertActionStyleDefault
                AlertButtonKind.Cancel -> UIAlertActionStyleCancel
                AlertButtonKind.Destructive -> UIAlertActionStyleDestructive
            }
            // Read at tap time: the buttons' lambdas change with every recomposition.
            alert.addAction(UIAlertAction.actionWithTitle(button.label, style) { _ -> current.getOrNull(i)?.onClick?.invoke() })
        }
        var disposed = false
        fun cancelOrReset() {
            val fallback = unavailable ?: current.firstOrNull { it.kind == AlertButtonKind.Cancel }?.onClick
            fallback?.invoke()
        }
        fun tryPresent(attemptsLeft: Int) {
            if (alert.presentingViewController != null || alert.isBeingPresented()) return
            if (disposed) return
            val top = presenterFrom(host)
            if (top == null || top.isBeingPresented() || top.isBeingDismissed() || top.presentedViewController != null) {
                if (attemptsLeft > 0) {
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 50_000_000L), dispatch_get_main_queue()) {
                        tryPresent(attemptsLeft - 1)
                    }
                } else {
                    cancelOrReset()
                }
                return
            }
            // iPad shows action sheets as popovers, which need an anchor. On iPhone an anchor would
            // turn it into a popover too, so it is left as the system's bottom sheet.
            if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) alert.popoverPresentationController?.let { popover ->
                val view = top.view
                popover.sourceView = view
                view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2, size.height, 0.0, 0.0) }
            }
            top.presentViewController(alert, animated = true, completion = null)
            if (alert.presentingViewController == null) {
                if (attemptsLeft > 0) {
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 50_000_000L), dispatch_get_main_queue()) {
                        tryPresent(attemptsLeft - 1)
                    }
                } else {
                    cancelOrReset()
                }
            }
        }
        tryPresent(attemptsLeft = 10)
        onDispose {
            disposed = true
            // Gone from composition without a tap (the game moved on): take it down too.
            if (alert.presentingViewController != null && !alert.isBeingDismissed()) alert.dismissViewControllerAnimated(true, completion = null)
        }
    }
}
