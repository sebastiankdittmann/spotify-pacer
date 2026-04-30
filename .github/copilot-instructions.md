# Copilot instructions — spotify-pacer

Project: Android app (Kotlin + Jetpack Compose) that builds Spotify playlists paced to a run. See [README](../README.md), [docs/REQUIREMENTS.md](../docs/REQUIREMENTS.md), [docs/DESIGN.md](../docs/DESIGN.md) for context.

## Coding principles

Apply these in every suggestion. They override any stylistic default.

- **SOLID** — single responsibility, open/closed, Liskov, interface segregation, dependency inversion.
- **KISS** — simple beats clever. No unnecessary abstraction.
- **DRY** — extract shared logic when it appears. But prefer two similar blocks over one premature abstraction.
- **YAGNI** — don't build for hypothetical future needs. Only what the current task requires.
- **Don't abstract too early** — let the code shape itself before factoring. Wait for natural cut points.
- **Chesterton's Fence** — understand why existing code exists before changing or deleting it. Ugly-but-working code is still working.
- **80/20** — prefer the Pareto solution: most value, least complexity.
- **Profile before optimizing** — no speculative performance work. Measure first. Network and I/O matter more than CPU in this app.
- **Self-documenting code** — good names over comments. Comment only when the *why* is non-obvious.
- **Never commit secrets** — no tokens, keys, credentials in source. Spotify client ID is public (PKCE); no client secret is used.

## What to avoid

- Error handling, fallbacks, or validation for scenarios that can't happen. Trust internal callers; validate only at boundaries (user input, Spotify API responses).
- Backwards-compatibility shims or feature flags for code that isn't shipped yet. Change the code directly.
- Comments that restate what the code does, reference the task/PR that introduced them, or narrate removed code.
- New abstractions introduced "for future flexibility" when the current caller count is 1.
- Generated files edited by hand (e.g. API clients under `frontend/src/api/` in other projects, equivalent here). Regenerate instead.

## Android / Kotlin specifics

- Kotlin idioms: `val` by default, data classes for models, sealed classes/interfaces for state, extension functions where they clarify.
- Compose: stateless composables where possible; hoist state; `remember` / `rememberSaveable` deliberately, not reflexively.
- No blocking calls on the main dispatcher. Use `viewModelScope` + `Dispatchers.IO` for network/disk.
- Min SDK 29 (Android 10). Prefer stdlib and AndroidX; don't pull a dependency to avoid writing ten lines.
- Secrets: never in source. Spotify client ID goes in `local.properties` or `BuildConfig`, not committed.

## Spotify integration

- Auth: **Authorization Code Flow with PKCE**. No client secret. Refresh token stored in `EncryptedSharedPreferences`.
- Scopes: request the minimum needed (see [DESIGN.md](../docs/DESIGN.md)).
- `audio-features` may be deprecated for new app registrations — flag if a suggestion depends on it without a fallback plan.
- Cache audio-features locally; Spotify rate-limits aggressively.

## UI behavior tests

Any PR that adds or changes a Compose UI **must** include or update a UI behavior test. Tests live in `app/src/test/java/dk/dittmann/spotifypacer/...` and run on the JVM via Robolectric — no emulator needed, no sandbox setup beyond what's already configured.

**Pattern:**

```kotlin
@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tapping_sign_in_shows_loading() {
        composeRule.setContent { LoginScreen(state = LoginState.Idle, onSignIn = {}) }
        composeRule.onNodeWithText("Sign in with Spotify").performClick()
        // assert state transition
    }
}
```

**What to test:**
- Each visible state the screen can render (idle/loading/error/success) — one test each.
- Every interactive affordance (button, input) — assert the state change it causes.
- Navigation triggers (e.g. button tap emits a nav event) — assert via a test double for the callback.

**What NOT to test here:**
- Pure logic already covered by unit tests (curve math, selection algorithm). Don't duplicate.
- Integration against real Spotify — use fakes/mocks.

**CI runs `gradle testDebugUnitTest` on every PR.** Failing UI behavior tests block merge.

## Screenshots for UI PRs

Any PR that adds or changes a Compose UI **must** include screenshots in the PR description. The repo is wired for headless, JVM-rendered screenshots via the `com.android.compose.screenshot` plugin — no emulator needed.

**Workflow:**

1. Write your composable in `app/src/main/java/...`.
2. Mirror each visual state as a `@Preview` function in `app/src/screenshotTest/java/dk/dittmann/spotifypacer/...` (e.g. `LoginPreviewTest.kt` with `LoginIdlePreview`, `LoginLoadingPreview`, `LoginErrorPreview`, `LoginSuccessPreview`).
3. Run `./gradlew :app:updateDebugScreenshotTest` to generate reference PNGs. They land under `app/src/debug/screenshotTest/reference/...`.
4. Commit both the source previews and the reference PNGs on the PR branch.
5. Embed each screenshot in the PR description using a relative path:

   ```markdown
   ## Screenshots
   | State | Image |
   |---|---|
   | Idle | ![idle](app/src/debug/screenshotTest/reference/dk/dittmann/spotifypacer/ui/login/LoginPreviewTest_LoginIdlePreview.png) |
   ```

**CI enforces this:** `gradle validateDebugScreenshotTest` runs on every PR. If the committed reference PNGs don't match what the `@Preview`s now render, CI fails — regenerate and re-commit.

## PR and commit conventions

- Small, focused PRs. One concern per PR.
- Commit/PR titles: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`. **No scope in parentheses** (`feat(auth):` is not used).
- Branch prefixes: `feat/`, `fix/`, `chore/`, `poc/`.
- Follow `.github/PULL_REQUEST_TEMPLATE.md` if present.
- Never `--no-verify`, never force-push `main`.

## Versioning

Every PR that ships user-visible code must bump [`version.txt`](../version.txt) following [semver](https://semver.org). The bump is part of the PR — do not open a separate version PR.

| Change | Bump | Examples |
|---|---|---|
| **Breaking** — anything that breaks an existing user's install or stored data (incompatible APK signing, schema migration without upgrade path, removed feature, redirect URI change, scope removal) | **MAJOR** | `1.4.2 → 2.0.0` |
| **Feature** (`feat:`) | **MINOR** | `1.4.2 → 1.5.0` |
| **Fix / chore / refactor / docs / test / perf** (`fix:`, `chore:`, …) | **PATCH** | `1.4.2 → 1.4.3` |

Pure-internal PRs that produce no shippable change (CI tweaks that don't touch the app, README typos, doc-only edits) may skip the bump — call this out explicitly in the PR description.

`versionCode` is derived automatically; minor and patch must each stay below 100. See [`docs/RELEASING.md`](../docs/RELEASING.md).

### Manual approval gate

The release workflow runs against the `Development` GitHub environment. **Major and minor releases require a human to approve the deployment** before the APK is built and published; patch releases run unattended. When the PR introduces a major or minor bump, mention in the PR description that the release will require manual approval after merge.

## When stuck

If a change requires guessing at the user's intent, stop and ask. Don't invent requirements.

## Documentation
- Before submitting a PR, review the documentation in the repository and add documentation updates where necessary
