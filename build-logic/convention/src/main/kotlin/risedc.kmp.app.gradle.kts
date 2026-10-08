import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The shared module of one RISE DC app (apps/recipes, apps/transit). It holds all screens and
// navigation, builds the iOS framework that iosApp/<App>/ links, and is itself the web (wasmJs) app.
plugins {
    id("risedc.compose")
    id("risedc.serialization")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

kotlin {
    android {
        androidResources {
            enable = true
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:data"))
            implementation(project(":core:common"))
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
