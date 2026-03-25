# Note Organizer (Native Android Kotlin)

This repository contains a native Android (Kotlin) offline-first notes app skeleton using:

- **Room** for local persistence
- **Kotlin Coroutines + Flow** for reactive data streams
- **ViewModel + Repository** architecture
- **Mock (in-memory) remote backend** interface for client-only sync
- **XML Views** (no Jetpack Compose)

## Project layout

- `android-app/` — Android Gradle project (the app)
- `notes_frontend/` — legacy Flutter project (kept for history; no longer used)

## Build & test

From `note-organizer-9603-9612-2417/android-app`:

- Build: `./gradlew :app:assembleDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Instrumented tests (if emulator/device available): `./gradlew :app:connectedDebugAndroidTest`

## Notes

This is an offline-first skeleton intended for further feature completion (UI screens, WorkManager scheduling, etc.).
It already includes the core data/repository/sync primitives and tests for local CRUD, reactivity, and conflict merge logic.
