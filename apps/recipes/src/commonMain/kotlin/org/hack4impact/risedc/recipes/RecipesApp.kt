package org.hack4impact.risedc.recipes

import androidx.compose.runtime.Composable
import org.hack4impact.risedc.core.common.di.initKoin
import org.hack4impact.risedc.core.data.di.dataModule
import org.hack4impact.risedc.core.ui.theme.RiseTheme
import org.hack4impact.risedc.recipes.navigation.RecipesNavHost
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Root composable for Android, iOS and web. Call [initRecipesKoin] first. */
@Composable
fun RecipesApp() {
    RiseTheme {
        RecipesNavHost()
    }
}

/** Recipes-only bindings (ViewModels, repositories). Register them here. */
val recipesModule = module { }

fun initRecipesKoin(config: KoinAppDeclaration = {}) = initKoin(dataModule, recipesModule, config = config)
