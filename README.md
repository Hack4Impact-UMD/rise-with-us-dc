# rise-with-us-dc

RISE DC apps (Recipes, Transit) for Android, iOS and web. Kotlin Multiplatform + Compose Multiplatform, Firebase backend.

Scaffolded with the Kotlin Multiplatform wizard (the same generator as Android Studio's KMP wizard), then split into the `apps/` + `core/` + `build-logic/` layout from the app spec.

## Getting started

1. Open this folder in Android Studio (with the Kotlin Multiplatform plugin). Gradle downloads JDK 21 on its own.
2. Run the `androidApp` configuration of `apps/recipes` or `apps/transit`, or:
   - Android: `./gradlew :apps:recipes:androidApp:installDebug`
   - Web: `./gradlew :apps:recipes:wasmJsBrowserDevelopmentRun`
   - iOS (macOS): open `iosApp/Recipes/iosApp.xcodeproj` in Xcode
3. Click through the placeholder screens. Each route in `apps/<app>/.../navigation/` maps to one spec screen.

See [AGENTS.md](AGENTS.md) for the layout, commands and rules. Specs live in `../rise-dc-wiki/wiki/`.

## Not set up yet

- Firebase projects `rise-dc-dev` / `rise-dc-prod` (`.firebaserc` names them), App Check, Secret Manager keys
- Firebase SDK in the apps (repositories in `core/data`)
- Preview and release workflows (App Distribution, Hosting channels), branch protection, CODEOWNERS
