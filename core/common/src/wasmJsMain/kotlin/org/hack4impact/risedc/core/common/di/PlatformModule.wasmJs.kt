package org.hack4impact.risedc.core.common.di

import org.hack4impact.risedc.core.common.location.LocationProvider
import org.hack4impact.risedc.core.common.location.WebLocationProvider
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<LocationProvider> { WebLocationProvider() }
}
