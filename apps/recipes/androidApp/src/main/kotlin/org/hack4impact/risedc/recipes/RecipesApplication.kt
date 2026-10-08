package org.hack4impact.risedc.recipes

import android.app.Application
import org.koin.android.ext.koin.androidContext

class RecipesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initRecipesKoin { androidContext(this@RecipesApplication) }
    }
}
