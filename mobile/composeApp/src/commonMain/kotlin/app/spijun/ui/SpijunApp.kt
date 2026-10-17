package app.spijun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import app.spijun.ui.data.loadBuiltInDictionaries
import app.spijun.ui.design.Spy
import app.spijun.ui.design.SpyTheme
import app.spijun.ui.platform.rememberReduceMotion
import app.spijun.ui.platform.rememberSpyHaptics
import app.spijun.ui.screens.GameScreen
import app.spijun.ui.screens.HomeScreen
import app.spijun.ui.screens.SetupScreen

@Composable
fun SpijunApp() {
    val controller = remember { AppController() }
    val language = Locale.current.language

    LaunchedEffect(Unit) {
        if (controller.dictionaries == null) {
            controller.onDictionariesLoaded(loadBuiltInDictionaries(), preferredId = if (language == "en") "en" else "hr")
        }
    }

    SpyTheme(haptics = rememberSpyHaptics(enabled = true), reduceMotion = rememberReduceMotion()) {
        Box(Modifier.fillMaxSize().background(Spy.colors.bg)) {
            when (controller.route) {
                Route.Home -> HomeScreen(onOnePhone = controller::openSetup)
                Route.Setup -> SetupScreen(controller, onBack = controller::goHome, onStart = { controller.startGame(it) })
                Route.Game -> controller.game?.let { GameScreen(it, controller) }
            }
        }
    }
}
