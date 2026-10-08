# RISE DC (code)

Kotlin Multiplatform + Compose Multiplatform apps for RISE DC: **Recipes** and **Transit**, on Android, iOS and web (wasmJs). Backend is Firebase (`functions/`).

## Specs and decisions live in the wiki

The wiki repo sits next to this one: `../rise-dc-wiki/wiki/`. Read the feature page before working on a feature, and the decision pages before changing architecture. **Update the wiki page when you change a feature.**

## Layout

```
apps/<app>/              KMP module: all screens + navigation, iOS framework, web entry point
apps/<app>/androidApp/   thin Android application wrapper (AGP 9 keeps it separate)
core/ui/                 RiseTheme, shared composables
core/data/               models, repositories, Ktor client to functions/
core/common/             DI bootstrap (initKoin), expect/actual platform services
iosApp/<App>/            Xcode wrappers (open in Xcode on macOS)
build-logic/             convention plugins: risedc.kmp.library, .compose, .kmp.app, .android.app, .quality
functions/               Cloud Functions; the only place API keys and prompts live
```

Navigation: each app has a `navigation/<App>Route.kt` (type-safe `@Serializable` routes, one per spec screen) and a `<App>NavHost.kt`. Every destination starts as a `PlaceholderScreen`; replace it with the real screen.

DI: Koin. Each app has a `<app>Module` in `<App>App.kt`; every entry point calls `init<App>Koin()`.

## Commands

```bash
./gradlew ktlintCheck detekt                   # lint (ktlintFormat to auto-fix)
./gradlew allTests                             # Android host + wasm tests (wasm needs Chrome)
./gradlew :apps:recipes:androidApp:installDebug
./gradlew :apps:recipes:wasmJsBrowserDevelopmentRun
cd functions && npm install && npm run build
```

iOS: open `iosApp/Recipes/iosApp.xcodeproj` (or Transit) in Xcode on macOS and run.

## Rules

- No API keys in the apps. External APIs (Gemini, Google Routes, WMATA) only through `functions/`.
- No participant names, photos, health or behavior details anywhere: code, fixtures, screenshots, commits.
- Accessibility rules in `../rise-dc-wiki/wiki/client/design-rules.md` are decisions, not suggestions.
