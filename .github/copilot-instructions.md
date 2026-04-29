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

## PR and commit conventions

- Small, focused PRs. One concern per PR.
- Commit/PR titles: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`. **No scope in parentheses** (`feat(auth):` is not used).
- Branch prefixes: `feat/`, `fix/`, `chore/`, `poc/`.
- Follow `.github/PULL_REQUEST_TEMPLATE.md` if present.
- Never `--no-verify`, never force-push `main`.

## When stuck

If a change requires guessing at the user's intent, stop and ask. Don't invent requirements.
