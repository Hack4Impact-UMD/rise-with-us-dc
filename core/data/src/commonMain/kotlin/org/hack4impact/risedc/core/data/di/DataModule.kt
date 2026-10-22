package org.hack4impact.risedc.core.data.di

import com.russhwolf.settings.Settings // the import for the settings
import org.hack4impact.risedc.core.data.network.ApiConfig
import org.hack4impact.risedc.core.data.network.RiseJson
import org.hack4impact.risedc.core.data.network.riseHttpClient
import org.hack4impact.risedc.core.data.recipes.RecipeStore
import org.hack4impact.risedc.core.data.recipes.SettingsRecipeStore
import org.koin.dsl.module

/** Shared data layer: HTTP client now; repositories (Firestore, local cache) get bound here. */
val dataModule = module {
    single { ApiConfig.Dev }
    single { riseHttpClient(get()) }
    single<Settings> { Settings() }
    single<RecipeStore> { SettingsRecipeStore(get(), RiseJson) }
}
