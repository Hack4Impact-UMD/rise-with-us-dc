import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The thin Android application wrapper that lives inside an app folder (apps/<app>/androidApp).
// AGP 9 keeps com.android.application separate from KMP modules, so the screens live in the parent.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("risedc.quality")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()
val appModule = checkNotNull(project.parent) { "risedc.android.app must be applied inside apps/<app>/" }

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "org.hack4impact.risedc.${appModule.name}"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.hack4impact.risedc.${appModule.name}"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    "implementation"(project(appModule.path))
    "implementation"(project(":core:common"))
    "implementation"(libs.androidx.activity.compose)
    "implementation"(platform(libs.koin.bom))
    "implementation"(libs.koin.android)
    "implementation"(libs.compose.uiToolingPreview)
    "debugImplementation"(libs.compose.uiTooling)
}
