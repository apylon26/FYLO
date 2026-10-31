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
