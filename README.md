# Note Organizer (Native Android Kotlin)

This repository contains a native Android (Kotlin) offline-first notes app skeleton using:

- **Room** for local persistence
- **Kotlin Coroutines + Flow** for reactive data streams
- **ViewModel + Repository** architecture
- **Mock (in-memory) remote backend** interface for client-only sync
- **XML Views** (no Jetpack Compose)

## Project layout

- `android-app/` — Android Gradle project (the app)

## Build & test

From `note-organizer-9603-9612-2417/android-app`:

### Build APKs (skip tests)
- Debug APK: `./gradlew :app:assembleDebug -x test --no-daemon`
- Release APK (unsigned by default): `./gradlew :app:assembleRelease -x test --no-daemon`

### APK output locations
- Debug: `android-app/app/build/outputs/apk/debug/app-debug.apk`
- Release: `android-app/app/build/outputs/apk/release/app-release.apk`

### (Optional) Run tests later
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Instrumented tests (if emulator/device available): `./gradlew :app:connectedDebugAndroidTest`

## Notes

This is an offline-first skeleton intended for further feature completion (UI screens, WorkManager scheduling, etc.).
It already includes the core data/repository/sync primitives and tests for local CRUD, reactivity, and conflict merge logic.
