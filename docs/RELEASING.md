# Releasing

Continuous delivery is driven by [`version.txt`](../version.txt) at the repo root. Bumping it on `main` triggers `.github/workflows/release.yml`, which builds a signed release APK and publishes it as a GitHub Release.

## Cutting a release

1. Decide the next [semver](https://semver.org) — `MAJOR.MINOR.PATCH`.
2. Update `version.txt` to that string (single line, nothing else).
3. Commit on a branch, open a PR, get it merged into `main`.
4. The `Release` workflow runs on the merge commit, builds `app-release.apk`, and creates the `vX.Y.Z` tag + GitHub Release with auto-generated notes from commits since the previous tag.
5. Re-running the workflow on the same version is a no-op — it skips when the tag already exists.

`versionName` and `versionCode` are derived from `version.txt` in [`app/build.gradle.kts`](../app/build.gradle.kts):

- `versionName` = the file content (e.g. `1.2.3`).
- `versionCode` = `MAJOR * 10_000 + MINOR * 100 + PATCH`. Minor and patch must each stay below 100. The build fails fast if they don't.

## Required configuration

### Environment: `Development`

Until v1.0 the workflow runs against the GitHub `Development` environment. Configured there:

| Type | Name | Purpose |
|---|---|---|
| Variable | `SPOTIFY_CLIENT_ID` | Public PKCE client ID baked into `BuildConfig`. |
| Variable | `GETSONGBPM_API_KEY` | GetSongBPM API key. Public-by-design (extractable from APK; quota-limited). |
| Secret | `KEYSTORE_BASE64` | Release keystore, base64-encoded. |
| Secret | `KEYSTORE_PASSWORD` | Keystore password. |
| Secret | `KEY_ALIAS` | Signing key alias inside the keystore. |
| Secret | `KEY_PASSWORD` | Password for the signing key. |

The client ID and the GetSongBPM API key are intentionally variables, not secrets — both are public and ship in the APK. The Spotify client ID is exposed under PKCE; the GetSongBPM key is a quota-limited identifier the provider issues per requester.

### Generating a release keystore (one-off)

```bash
keytool -genkey -v \
  -keystore spotify-pacer-release.jks \
  -alias spotify-pacer \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -storetype JKS
```

Encode it for the secret:

```bash
base64 -i spotify-pacer-release.jks | pbcopy   # macOS
# or: base64 -w 0 spotify-pacer-release.jks    # Linux
```

Paste into the `KEYSTORE_BASE64` secret on the `Development` environment. Set `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` to match what `keytool` asked for.

**Keep the original `.jks` file out of git.** Losing it means future releases can't be installed as upgrades — Android rejects APKs signed by a different key.

## Verifying a release locally

You don't need the production keystore to smoke-test the release pipeline — without env vars, `:app:assembleRelease` falls back to debug signing.

```bash
./gradlew :app:assembleRelease
ls app/build/outputs/apk/release/app-release.apk
```

To dry-run with a real keystore:

```bash
RELEASE_KEYSTORE_PATH=/abs/path/to/spotify-pacer-release.jks \
RELEASE_KEYSTORE_PASSWORD=... \
RELEASE_KEY_ALIAS=spotify-pacer \
RELEASE_KEY_PASSWORD=... \
SPOTIFY_CLIENT_ID=your_client_id \
./gradlew :app:assembleRelease
```

## Installing a release on a device

See the [Installing a release](../README.md#installing-a-release) section in the README.
