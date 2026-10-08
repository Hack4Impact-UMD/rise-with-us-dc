rootProject.name = "rise-dc"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Shared platform
include(":core:ui")
include(":core:data")
include(":core:common")

// Apps: each app is a KMP module (commonMain/androidMain/iosMain/wasmJsMain, also the web entry point)
// plus a thin Android application module. The iOS wrappers live in iosApp/<App>/.
include(":apps:recipes")
include(":apps:recipes:androidApp")
include(":apps:transit")
include(":apps:transit:androidApp")
