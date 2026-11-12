package org.hack4impact.risedc.core.common.di

import org.hack4impact.risedc.core.common.location.IosLocationProvider
import org.hack4impact.risedc.core.common.location.LocationProvider
import org.hack4impact.risedc.core.common.speech.IosSpeaker
import org.hack4impact.risedc.core.common.speech.Speaker
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<LocationProvider> { IosLocationProvider() }
    single<Speaker> { IosSpeaker() }
}
