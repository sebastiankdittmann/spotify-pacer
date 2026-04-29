# spotify-pacer

Android app that builds Spotify playlists paced to your run.

You pick a **distance** and **target time**. You pick a **pace strategy** (constant, linear ramp, or delayed-exponential ramp). The app queries your Spotify library, selects tracks whose BPM matches the pace curve minute-by-minute, shows you the lineup, and — on your approval — saves it as a new playlist to your account.

Login uses Spotify's **Authorization Code Flow with PKCE** so the app acts on behalf of the signed-in user; no personal API tokens to manage.

## Status

Pre-alpha. Scaffold + requirements only. Android project not yet generated.

## Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Min SDK**: TBD (target: Android 10+, API 29)
- **Auth**: Spotify Authorization Code Flow with PKCE
- **Spotify API**: Web API (search, audio-features, playlists)

## Docs

- [Requirements](docs/REQUIREMENTS.md) — what the app does, in detail
- [Design](docs/DESIGN.md) — high-level architecture, pace curves, auth flow

## Getting started

Project uses the Gradle **version catalog** (`gradle/libs.versions.toml`) and ships without the Gradle Wrapper binaries. First time after cloning:

```bash
# Option A — open in Android Studio; "Sync Project" generates the wrapper.
# Option B — from the command line, with Gradle installed locally:
gradle wrapper --gradle-version 8.11.1
./gradlew assembleDebug
```

**Build targets:**
- `./gradlew spotlessCheck` — formatting check
- `./gradlew spotlessApply` — auto-format
- `./gradlew lintDebug` — Android lint
- `./gradlew testDebugUnitTest` — unit tests
- `./gradlew assembleDebug` — build the APK

**Spotify client ID:**
The Spotify client ID is read from `local.properties` (not committed). Add:

```
SPOTIFY_CLIENT_ID=your_public_client_id_here
```

There is no client secret — we use Authorization Code Flow with PKCE.

## Next steps

Tracked in the [v0.1 MVP milestone](https://github.com/sebastiankdittmann/spotify-pacer/milestones) and the tracking issue (once opened).
