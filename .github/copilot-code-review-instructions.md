# Copilot code-review instructions — spotify-pacer

Review the diff against the principles below. Comment only when you have a concrete concern; silence is preferred over filler praise. Cite file and line. Every comment should help the author ship a smaller, simpler, safer change.

## Reject / flag

- **Secrets in the diff** — API keys, tokens, client secrets, personal access tokens, keystore contents, `google-services.json` with real IDs. Even in tests. Even "temporarily."
- **Hand-edits to generated files** — any file under a clearly generated path (e.g. `frontend/src/api/`, Kotlin `generated/` dirs, DataBinding outputs). Ask the author to regenerate.
- **Broken secret hygiene** — Spotify client ID hard-coded in source when `BuildConfig` / `local.properties` exists; client secret present at all (PKCE is used, so there shouldn't be one).
- **Skipped pre-commit hooks / force-pushes to main** — if the PR description or commit trailers hint at it.

## Complexity smells — flag and suggest the simpler path

- **Premature abstraction** — new interfaces, base classes, or factories with a single caller. Ask: is there a second caller today? If not, inline it.
- **Speculative configurability** — feature flags, strategy patterns, DI wiring added for a use case that isn't in this PR.
- **Over-broad error handling** — `try/catch` around code that can't realistically fail, or that swallows and logs without recovery. Validate at the boundary (user input, Spotify API), trust internal code.
- **Defensive `null` checks / validation** on values a caller guarantees are non-null.
- **New dependency for a small helper** — if the added library replaces <50 lines of stdlib/AndroidX code, flag it.

## Clarity

- **Comments that restate the code** — `// increment counter` on `counter++`. Remove.
- **Stale context comments** — references to the task, PR number, or "added for the X flow". These rot. Suggest removing; the git history has this.
- **Obscure naming** — `data`, `tmp`, `x1`, single-letter non-iterator vars. Suggest a better name.
- **Functions doing two things** — if the name needs "and" to describe it, suggest splitting.

## Android / Kotlin correctness

- **Main-thread IO** — network, disk, or Spotify API calls not dispatched off the main thread. Flag and suggest `Dispatchers.IO`.
- **Compose pitfalls** — state read inside a `remember { }` block (won't recompose), unstable lambdas captured in composable keys, missing `rememberSaveable` for state that must survive process death.
- **Leaky coroutine scopes** — `GlobalScope` in UI/ViewModel code. Suggest `viewModelScope` / `lifecycleScope`.
- **Mutable public state** — `var` on a public property when `val` + exposed flow would do.

## Spotify/auth specifics

- **Token handling** — access/refresh tokens logged, stored in plaintext `SharedPreferences`, or included in analytics. Must be `EncryptedSharedPreferences` or Android Keystore-backed.
- **Scope creep** — OAuth scopes requested beyond what the PR actually uses. Flag and suggest narrowing.
- **Dependence on `audio-features`** — deprecated for newer app registrations; flag if the PR adds a new code path relying on it with no fallback.
- **Unbounded Spotify calls** — no caching, no pagination, no rate-limit handling on loops that hit the API per track.

## PR hygiene

- **Mixed concerns** — unrelated changes bundled in one PR. Suggest splitting.
- **Missing Jira reference** — description should reference an `LRMP-xxx` ticket. If none, suggest creating one.
- **Title format** — must use `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`. No scope in parentheses.
- **PR description restating the diff** — the description should explain *why*, not narrate *what*. Flag filler.

## Don't comment on

- Style issues a formatter would catch (Spotless/ktfmt will handle it).
- Subjective preference when the existing code in the file follows a different convention — defer to the surrounding style.
- Hypothetical future problems that YAGNI covers. Review the code in this PR, not the code that might exist one day.
