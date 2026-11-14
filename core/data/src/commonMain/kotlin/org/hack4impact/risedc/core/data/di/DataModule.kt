package org.hack4impact.risedc.core.data.di

import com.russhwolf.settings.Settings
import org.hack4impact.risedc.core.data.BuildKonfig
import org.hack4impact.risedc.core.data.grocery.GroceryListStore
import org.hack4impact.risedc.core.data.network.ApiConfig
import org.hack4impact.risedc.core.data.network.RiseJson
import org.hack4impact.risedc.core.data.network.riseHttpClient
import org.hack4impact.risedc.core.data.recipes.RecipeStore
import org.hack4impact.risedc.core.data.snap.GeminiSnapAndCook
import org.hack4impact.risedc.core.data.snap.GenerationBudget
import org.hack4impact.risedc.core.data.snap.HttpGeminiClient
import org.hack4impact.risedc.core.data.snap.SnapAndCook
import org.hack4impact.risedc.core.data.transit.PlaceStore
import org.hack4impact.risedc.core.data.transit.TripStore
import org.koin.dsl.module

/** Device-local stores and external API clients (decisions 0008/0009). */
val dataModule = module {
    single { ApiConfig.Dev }
    single { riseHttpClient(get()) }
    single<Settings> { Settings() }
    single { RecipeStore(get(), RiseJson) }
    single { GroceryListStore(get(), RiseJson) }
    single { TripStore(get(), RiseJson) }
    single { PlaceStore(get(), RiseJson) }
    single { GenerationBudget(get()) }
    single<SnapAndCook> {
        GeminiSnapAndCook(
            client = HttpGeminiClient(get(), BuildKonfig.GEMINI_API_KEY),
            budget = get(),
            policy = null, // Enable only after the project lead confirms the never-use ingredients and tools.
            hasKey = BuildKonfig.GEMINI_API_KEY.isNotBlank(),
        )
    }
}
