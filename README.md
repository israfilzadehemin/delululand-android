# Delulutopia for Android

Native Android client of Delulutopia (internally Delululand). Kotlin, Jetpack Compose and Material 3,
following the design in the project doc "Delululand — Android App Design" and its canvas.

| | |
| --- | --- |
| Min / target SDK | 23 (Android 6.0) / 37 |
| Stack | Kotlin 2.4, Compose (BOM), Material 3, Navigation Compose (typed routes), Hilt, DataStore |
| Modules | `app` (shell, navigation) · `core:designsystem` (theme, fonts, components) · `build-logic` (convention plugins) |
| Flavours | `test` → https://test.delulutopia.com · `prod` → https://delulutopia.com (install side by side) |
| Application ID | `com.delulutopia.app` (`.test` and `.debug` suffixes) |

## Build

```
./gradlew assembleTestDebug          # test server, debug
./gradlew assembleProdRelease        # prod server, release (R8)
./gradlew bundleProdRelease          # Play Store bundle
./gradlew testProdDebugUnitTest :core:designsystem:testDebugUnitTest -Proborazzi.test.record=true
./gradlew ktlintCheck :app:lintProdDebug
```

CI (GitHub Actions) runs style, unit and screenshot tests, lint and every build on each push and PR, and
uploads the APKs, the bundle and the screenshots (`build/outputs/roborazzi`) as artifacts.

## Release signing

Release builds use the upload key when it is present, otherwise the debug key (so CI APKs still install).
Repository secrets: `DL_KEYSTORE_BASE64` (the .jks, base64), `DL_KEYSTORE_PASSWORD`, `DL_KEY_ALIAS`,
`DL_KEY_PASSWORD`. Locally, a git-ignored `keystore.properties` with `storeFile`, `storePassword`,
`keyAlias`, `keyPassword` does the same.

## Fonts

Bundled (P10): Manrope, Literata, Caveat (SIL OFL 1.1, licences in `tools/fonts/licenses`). Manrope has
no capital schwa Ə, so `tools/fonts/merge_schwa.py` copies it from Noto Sans into each Manrope weight,
the same rule the web uses (`DL Schwa`): 400–500 from Noto Sans 400, 600–800 from Noto Sans 700.
