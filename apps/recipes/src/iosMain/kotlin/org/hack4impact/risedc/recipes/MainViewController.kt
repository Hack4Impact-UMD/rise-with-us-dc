package org.hack4impact.risedc.recipes

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Called from iosApp/Recipes/iosApp/ContentView.swift. */
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    initRecipesKoin()
    return ComposeUIViewController { RecipesApp() }
}
