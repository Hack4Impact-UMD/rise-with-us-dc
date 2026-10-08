package org.hack4impact.risedc.core.data.di

import org.hack4impact.risedc.core.data.network.ApiConfig
import org.hack4impact.risedc.core.data.network.riseHttpClient
import org.koin.dsl.module

/** Shared data layer: HTTP client now; repositories (Firestore, local cache) get bound here. */
val dataModule = module {
    single { ApiConfig.Dev }
    single { riseHttpClient(get()) }
}
