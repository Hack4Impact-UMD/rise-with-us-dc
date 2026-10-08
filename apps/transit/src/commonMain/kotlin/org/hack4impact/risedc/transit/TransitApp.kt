package org.hack4impact.risedc.transit

import androidx.compose.runtime.Composable
import org.hack4impact.risedc.core.common.di.initKoin
import org.hack4impact.risedc.core.data.di.dataModule
import org.hack4impact.risedc.core.ui.theme.RiseTheme
import org.hack4impact.risedc.transit.navigation.TransitNavHost
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Root composable for Android, iOS and web. Call [initTransitKoin] first. */
@Composable
fun TransitApp() {
    RiseTheme {
        TransitNavHost()
    }
}

/** Transit-only bindings (ViewModels, repositories). Register them here. */
val transitModule = module { }

fun initTransitKoin(config: KoinAppDeclaration = {}) = initKoin(dataModule, transitModule, config = config)
