# spotify-pacer

[![Latest release](https://img.shields.io/github/v/release/sebastiankdittmann/spotify-pacer?label=release&sort=semver)](https://github.com/sebastiankdittmann/spotify-pacer/releases/latest)
[![Release date](https://img.shields.io/github/release-date/sebastiankdittmann/spotify-pacer?label=released)](https://github.com/sebastiankdittmann/spotify-pacer/releases/latest)

BPM data powered by [GetSongBPM](https://getsongbpm.com/).

Android app that builds Spotify playlists paced to your run.

You pick a **distance** and **target time**. You pick a **pace strategy** (constant, linear ramp, or delayed-exponential ramp). The app queries your Spotify library, selects tracks whose BPM matches the pace curve minute-by-minute, shows you the lineup, and — on your approval — saves it as a new playlist to your account.

Login uses Spotify's **Authorization Code Flow with PKCE** so the app acts on behalf of the signed-in user; no personal API tokens to manage.

## Status

Pre-alpha, headless. The non-UI pieces of the MVP are in place; the Compose screens that wire them together are next. Work tracked under the [v0.1 MVP milestone](https://github.com/sebastiankdittmann/spotify-pacer/milestones).

- ✅ Spotify **PKCE auth** + encrypted refresh-token storage ([#2](https://github.com/sebastiankdittmann/spotify-pacer/issues/2))
- ✅ **Pace curve** generator — constant, linear, delayed-exponential ([#4](https://github.com/sebastiankdittmann/spotify-pacer/issues/4))
- ✅ **Track selector** — greedy minute-by-minute match with half/double-tempo substitution and widening tolerance ([#5](https://github.com/sebastiankdittmann/spotify-pacer/issues/5))
- ⏳ Spotify **Web API** client — `/me`, liked tracks, audio-features, playlist create/add ([#3](https://github.com/sebastiankdittmann/spotify-pacer/issues/3))
- ⏳ **UI screens** — login, run setup, preview ([#6](https://github.com/sebastiankdittmann/spotify-pacer/issues/6), [#7](https://github.com/sebastiankdittmann/spotify-pacer/issues/7), [#8](https://github.com/sebastiankdittmann/spotify-pacer/issues/8))
- ⏳ Save playlist to account ([#9](https://github.com/sebastiankdittmann/spotify-pacer/issues/9))

## Modules

All under `app/src/main/java/dk/dittmann/spotifypacer/`.

| Package | What's in it |
|---|---|
| `auth/` | `PkceGenerator`, `AuthService`, `SpotifyAuthApi` (Retrofit), `TokenStore` (+ `EncryptedTokenStore`), `AuthRedirectActivity`, `AuthLauncher` |
| `pacing/` | `PaceStrategy`, `generateCurve(...)`, `TrackSelector` |
| *(root)* | `SpotifyPacerApplication` (owns the lazy `AuthService`), `MainActivity` (placeholder) |

## Stack

- **Language:** Kotlin 2.1
- **UI:** Jetpack Compose (BOM 2024.12.01)
- **Min SDK:** 29 (Android 10), compile & target SDK 35
- **AGP:** 8.7.3
- **Auth:** Spotify Authorization Code Flow with PKCE
- **Tests:** JUnit 4 + Robolectric (Compose UI behavior tests on the JVM)
- **Screenshots:** `com.android.compose.screenshot` plugin (JVM-rendered `@Preview` → PNG)

## Docs

- [Requirements](docs/REQUIREMENTS.md)
- [Design](docs/DESIGN.md)
- [Releasing](docs/RELEASING.md)

## Installing a release

Side-load the latest signed APK on any Android 10+ device:

1. On the phone, allow installs from your browser: **Settings → Apps → Special access → Install unknown apps** → enable for the browser you'll download with.
2. Open the [latest release](https://github.com/sebastiankdittmann/spotify-pacer/releases/latest) and download the `spotify-pacer-<version>.apk` asset.
3. Tap the downloaded file in your notifications (or in **Files**) and confirm **Install**.
4. Launch **spotify-pacer** and sign in with your Spotify account.

To upgrade, repeat the steps with a newer release — Android keeps your data as long as the signing key matches.

## Prerequisites

- **Android Studio Ladybug** (2024.2.1) or newer — needed for the Compose screenshot-test plugin. [Download](https://developer.android.com/studio).
- **JDK 17** — bundled with Android Studio; Gradle uses it automatically.
- A Spotify account (free works) plus a **Spotify Developer app** — register at <https://developer.spotify.com/dashboard>. Redirect URI: `spotifypacer://callback`. The client ID goes in `local.properties` (see [Getting started](#getting-started)).

## Getting started

1. Clone the repo:
   ```bash
   git clone https://github.com/sebastiankdittmann/spotify-pacer.git
   cd spotify-pacer
   ```
2. Open the project folder in Android Studio.
3. Wait for **Gradle Sync** to finish. First sync downloads AGP, Kotlin, Compose, and the Android SDK components. The Gradle wrapper is pinned to 8.11.1 and committed.
4. Add your Spotify client ID to `local.properties` in the project root (create the file if it doesn't exist; keep any existing entries like `sdk.dir`):
   ```properties
   spotify.clientId=paste_your_public_client_id_here
   ```
   Register a Spotify Developer app at <https://developer.spotify.com/dashboard> with redirect URI `spotifypacer://callback`. `local.properties` is gitignored. There is no client secret — PKCE is used. The app reads `BuildConfig.SPOTIFY_CLIENT_ID` from this file (or the `SPOTIFY_CLIENT_ID` env var).

## Running the app

### On an emulator (recommended for dev)

1. **Tools → Device Manager** (or the sidebar icon) → **Create Virtual Device**.
2. Pick **Pixel 7** (or any recent profile), select system image **Android 15 · API 35 · Google APIs**, finish.
3. Click ▶ on the device entry to boot it.
4. With the emulator running, click the green **Run 'app'** button in the toolbar (or `⌃R` on macOS, `Shift+F10` on Windows/Linux).

### On a physical device

1. On the phone: **Settings → About phone** → tap **Build number** seven times to unlock developer mode → **Developer options → USB debugging: ON**.
2. Plug in via USB. Accept the debug prompt the phone shows.
3. The device appears in the Android Studio toolbar dropdown. Select it and hit **Run 'app'**.

## Running tests

### From Android Studio

- Right-click `app/src/test/java` → **Run 'Tests in 'test''** — JVM unit tests, including Robolectric-hosted Compose UI tests.
- Right-click a file under `app/src/screenshotTest/java` → **Run** — regenerates reference PNGs for the `@Preview` composables in that file.

### From the command line

With JDK 17 on the path (matches CI):

| Command | What it does |
|---|---|
| `./gradlew testDebugUnitTest` | Unit tests + Robolectric Compose UI tests |
| `./gradlew :app:updateDebugScreenshotTest` | (Re)generate `@Preview` reference PNGs |
| `./gradlew :app:validateDebugScreenshotTest` | Check committed references against fresh renders |
| `./gradlew lintDebug` | Android lint |
| `./gradlew spotlessCheck` / `spotlessApply` | Check / auto-format Kotlin |
| `./gradlew assembleDebug` | Build a debug APK |
| `./gradlew installDebug` | Install on the connected device/emulator |

CI runs `spotlessCheck → lintDebug → testDebugUnitTest → validateDebugScreenshotTest → assembleDebug` on every PR. `build` is a required status check on `main`.

## Contributing

This is a solo hobby project. Forks are welcome; only Seb (repo owner) merges. Coding rules live in [`.github/copilot-instructions.md`](.github/copilot-instructions.md); review rules in [`.github/copilot-code-review-instructions.md`](.github/copilot-code-review-instructions.md). These apply equally to humans and Copilot.
