# FYLO development instructions

FYLO is being rebuilt from zero for Andreas. User-facing text is German.
The current rebuilt version is 0.1.2. It contains a compact analog clock, date and battery alongside it, a status placeholder, a settings popup and external APK module installation/discovery. The clock is 25% smaller than 0.1.1 with 60% opacity. CPU monitoring, UI and related settings were removed at the user’s request. No AI model or previous modules are bundled. Do not claim planned capabilities are implemented.
Use Kotlin and Jetpack Compose when rebuilding the native Android app. Agree new features from the ongoing user instructions; do not automatically restore the previous UI or modules.
Use the agreed icon style: classic blue circle, orange symbol, subtle 3D effect, transparent background, consistent proportions and lighting.
Never commit credentials or signing keys. Keep future build dependencies pinned.

## Validation when an Android build exists

Run `gradle :app:lintDebug :app:testDebugUnitTest :app:assembleDebug` with JDK 17, Gradle 8.9 and Android SDK 35, or report which checks could not run. Add meaningful tests for persistence and business logic changes. Do not modify release signing or publish releases without a user instruction.

## Signing continuity

For future development APKs use the existing private FYLO-Android-Signierung.zip backup from the user's Library and its pinned certificate fingerprint. Never commit, log, or publish its credentials. Never distribute unsigned debug APKs or generate a replacement signing key for an update. If the backup is unavailable, stop before delivering an APK.
The last installed version used applicationId com.apylon26.fylo.app and launcher label FYLO Neu; retain the applicationId and signer for compatible builds. The legacy app used com.apylon26.fylo and has a different unavailable signer.

## Reset scope

The user authorized starting over including all old functions and data. The repository reset does not erase installed Android data or shared FYLO folders. Do not claim device data has been erased without actual device evidence. Preserve the old repository snapshot in backup/v0.7.0-before-reset-2026-10-31.
