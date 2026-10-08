plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.composeCompiler)
    implementation(libs.gradlePlugin.composeMultiplatform)
    implementation(libs.gradlePlugin.serialization)
    implementation(libs.gradlePlugin.ktlint)
    implementation(libs.gradlePlugin.detekt)
    // Lets precompiled script plugins read the version catalog as `libs`.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
