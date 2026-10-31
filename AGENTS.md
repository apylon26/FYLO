# FYLO development instructions

FYLO is a native Android personal organizer for Andreas. User-facing text is German.
Use Kotlin and Jetpack Compose. Keep features separated under feature/ as they grow.
The initial app works offline and has no AI provider or API credentials.
Do not describe planned modules as implemented. Never commit credentials or signing keys.
For each change run `gradle :app:lintDebug :app:testDebugUnitTest :app:assembleDebug`
with JDK 17, Gradle 8.9 and Android SDK 35, or report which checks could not run.
Keep build dependencies pinned; do not upgrade unrelated dependencies.
Preserve stored tasks and notes when changing persistence. Document migrations.
Add meaningful tests for persistence and business logic changes.
Do not modify release signing or publish releases without a user instruction.

## Installable development APKs
CI deliberately builds unsigned debug APKs. Never distribute them directly or generate a new key for an update. Use tools/sign_apk.py and the existing private FYLO-Android-Signierung.zip backup from the user's Library. That backup contains credentials: never commit, log, or publish it. Verify the pinned certificate fingerprint. If the backup is unavailable, stop before delivering an APK. This concerns development signing only; release signing is unchanged.

Version 0.3.1 uses applicationId com.apylon26.fylo.app and launcher label FYLO Neu to coexist with the legacy com.apylon26.fylo app, whose ephemeral CI signing key is unavailable. Legacy data is preserved in that app and is NOT automatically migrated. Keep this new applicationId and signer stable for every subsequent update.
