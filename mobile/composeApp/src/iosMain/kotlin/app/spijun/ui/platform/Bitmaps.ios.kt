package app.spijun.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap

actual fun ImageBitmap.freeze() {
    asSkiaBitmap().setImmutable()
}
