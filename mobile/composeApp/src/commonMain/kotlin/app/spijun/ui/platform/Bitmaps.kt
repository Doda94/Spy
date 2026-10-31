package app.spijun.ui.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Call once nothing more will be drawn into this bitmap. iOS would otherwise copy and upload it
 * again every time it is recorded; Android gets mipmaps for drawing it smaller than its size.
 */
expect fun ImageBitmap.freeze()
