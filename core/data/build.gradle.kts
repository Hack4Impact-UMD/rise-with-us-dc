import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

plugins {
    id("risedc.kmp.library")
    id("risedc.serialization")
    id("risedc.buildkonfig")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

// Gemini key: from local.properties (never committed) or the GEMINI_API_KEY env var in CI.
val geminiApiKey: String = Properties()
    .apply { rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load) }
    .getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY") ?: ""

buildkonfig {
    packageName = "org.hack4impact.risedc.core.data"
    defaultConfigs { buildConfigField(STRING, "GEMINI_API_KEY", geminiApiKey) }
    // The web bundle is public, so it never carries the key (decision 0008).
    targetConfigs { create("wasmJs") { buildConfigField(STRING, "GEMINI_API_KEY", "") } }
}
