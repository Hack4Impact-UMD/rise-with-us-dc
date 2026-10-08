package org.hack4impact.risedc.core.common.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

/**
 * Platform services (expect/actual), e.g. text-to-speech, camera, location.
 * Each platform source set provides its own bindings.
 */
internal expect val platformModule: Module

/**
 * Starts Koin once per process with [platformModule] plus [modules]. Each app wraps this
 * (initRecipesKoin, initTransitKoin) and every entry point calls the wrapper: the Android
 * Application, the iOS MainViewController and the web main().
 */
fun initKoin(vararg modules: Module, config: KoinAppDeclaration = {}): KoinApplication? {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return null
    return startKoin {
        config()
        modules(platformModule, *modules)
    }
}
