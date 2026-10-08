// Every module configures itself through the convention plugins in build-logic/:
//   risedc.kmp.library   – KMP library (Android, iOS, wasmJs) + ktlint/detekt
//   risedc.compose       – Compose Multiplatform on top of a KMP module
//   risedc.kmp.app       – an app's shared module: library + compose + iOS framework + wasm executable
//   risedc.android.app   – the thin Android application wrapper for an app
