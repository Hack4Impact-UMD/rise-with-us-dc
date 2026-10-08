import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A Kotlin Multiplatform library targeting Android, iOS and wasmJs (the targets in the app spec).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("risedc.quality")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

kotlin {
    android {
        // :core:ui -> org.hack4impact.risedc.core.ui, :apps:recipes -> org.hack4impact.risedc.apps.recipes
        namespace = "org.hack4impact.risedc" + project.path.replace(':', '.')
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
