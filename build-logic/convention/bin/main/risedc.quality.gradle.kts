import dev.detekt.gradle.Detekt

// ktlint + detekt for every Kotlin module. CI runs `./gradlew ktlintCheck detekt`.
// Generated code (Compose resource accessors etc.) lives under build/ and is skipped. The exclude
// lambdas must not reference script-level values, or the configuration cache cannot store them.
plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

ktlint {
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    exclude { it.file.invariantSeparatorsPath.contains("/build/") }
}

// On KMP modules the plain `detekt` task has no sources; the work is in detekt<SourceSet>SourceSet.
tasks.named("detekt") {
    dependsOn(tasks.withType<Detekt>().matching { it.name.endsWith("SourceSet") })
}
