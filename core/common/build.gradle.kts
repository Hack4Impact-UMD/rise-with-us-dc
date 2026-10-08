plugins {
    id("risedc.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
        }
    }
}
