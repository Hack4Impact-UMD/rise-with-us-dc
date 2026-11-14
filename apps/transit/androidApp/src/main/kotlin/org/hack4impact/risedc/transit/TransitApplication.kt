package org.hack4impact.risedc.transit

import android.app.Application
import org.hack4impact.risedc.core.common.di.androidLocationModule
import org.koin.android.ext.koin.androidContext

class TransitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initTransitKoin {
            androidContext(this@TransitApplication)
            modules(androidLocationModule())
        }
    }
}
