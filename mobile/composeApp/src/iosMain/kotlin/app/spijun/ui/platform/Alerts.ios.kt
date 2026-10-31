package app.spijun.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeAlert(title: String?, message: String, buttons: List<AlertButton>, sheet: Boolean) {
    val current by rememberUpdatedState(buttons)
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
        val top = topViewController()
        // iPad shows action sheets as popovers, which need an anchor. On iPhone an anchor would
        // turn it into a popover too, so it is left as the system's bottom sheet.
        if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) alert.popoverPresentationController?.let { popover ->
            top?.view?.let { view ->
                popover.sourceView = view
                view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2, size.height, 0.0, 0.0) }
            }
        }
        top?.presentViewController(alert, animated = true, completion = null)
        onDispose {
            // Gone from composition without a tap (the game moved on): take it down too.
            if (alert.presentingViewController != null && !alert.isBeingDismissed()) alert.dismissViewControllerAnimated(true, completion = null)
        }
    }
}
