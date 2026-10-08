package org.hack4impact.risedc.transit

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

// Web entry point. Run: ./gradlew :apps:transit:wasmJsBrowserDevelopmentRun
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initTransitKoin()
    ComposeViewport {
        TransitApp()
    }
}
