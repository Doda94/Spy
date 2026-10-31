package app.spijun.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

actual fun ImageBitmap.freeze() {
    val bitmap = asAndroidBitmap()
    bitmap.setHasMipMap(true)
    bitmap.prepareToDraw()
}
