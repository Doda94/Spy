package app.spijun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.spijun.dictionaries.DictionaryTransfer

/**
 * Where the platform shells drop a `spijun://dictionary?d=…` link that opened the app. The app
 * picks it up when it can show the import preview; a link never changes anything on its own.
 */
object IncomingLinks {
    var pending by mutableStateOf<String?>(null)
        private set

    /** Called from MainActivity and from SwiftUI's onOpenURL; links that aren't ours are dropped. */
    fun deliver(url: String) {
        if (url.trim().startsWith(DictionaryTransfer.LINK_PREFIX)) pending = url
    }

    fun consume() { pending = null }
}
