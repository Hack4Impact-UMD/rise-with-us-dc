package org.hack4impact.risedc.recipes

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

// Web entry point. Run: ./gradlew :apps:recipes:wasmJsBrowserDevelopmentRun
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initRecipesKoin()
    ComposeViewport {
        RecipesApp()
    }
}
