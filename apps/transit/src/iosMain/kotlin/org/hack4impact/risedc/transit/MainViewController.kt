package org.hack4impact.risedc.transit

import androidx.compose.ui.window.ComposeUIViewController
import org.hack4impact.risedc.core.common.di.iosLocationModule
import platform.UIKit.UIViewController

/** Called from iosApp/Transit/iosApp/ContentView.swift. */
@Suppress("FunctionName", "ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    initTransitKoin { modules(iosLocationModule()) }
    return ComposeUIViewController { TransitApp() }
}
