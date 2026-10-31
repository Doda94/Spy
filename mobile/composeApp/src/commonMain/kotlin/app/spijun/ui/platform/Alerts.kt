package app.spijun.ui.platform

import androidx.compose.runtime.Composable

enum class AlertButtonKind { Default, Cancel, Destructive }

class AlertButton(val label: String, val kind: AlertButtonKind, val onClick: () -> Unit)

/**
 * The system's own alert, up while this is composed; [sheet] makes it an action sheet, which is
 * how iOS confirms anything destructive (components.md › Confirm dialog). The safe choice is the
 * [AlertButtonKind.Cancel] button. Android draws Material dialogs instead and never calls this.
 */
@Composable
expect fun NativeAlert(title: String?, message: String, buttons: List<AlertButton>, sheet: Boolean)
