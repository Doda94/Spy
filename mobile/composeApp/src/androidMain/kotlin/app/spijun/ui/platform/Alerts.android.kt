package app.spijun.ui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun NativeAlert(
    title: String?,
    message: String,
    buttons: List<AlertButton>,
    sheet: Boolean,
    onUnavailable: (() -> Unit)?,
) {
    // Android shows Material AlertDialogs, drawn by the screens themselves.
}
