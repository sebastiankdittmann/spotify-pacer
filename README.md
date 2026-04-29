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

## Next steps

1. Generate Android Studio project scaffold (Kotlin + Compose).
2. Register the app in the Spotify Developer Dashboard and wire up PKCE auth.
3. Prototype the pacing engine (BPM curve → track selection).
4. Minimal UI: distance/time input → pace preview → track list → approve → save.
