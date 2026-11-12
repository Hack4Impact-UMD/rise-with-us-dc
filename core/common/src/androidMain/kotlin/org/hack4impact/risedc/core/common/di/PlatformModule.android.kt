package org.hack4impact.risedc.core.common.di

import org.hack4impact.risedc.core.common.location.AndroidLocationProvider
import org.hack4impact.risedc.core.common.location.LocationProvider
import org.hack4impact.risedc.core.common.speech.AndroidSpeaker
import org.hack4impact.risedc.core.common.speech.Speaker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<Speaker> { AndroidSpeaker(androidContext()) }
}
