# Design notes

High-level design for spotify-pacer. Expect this to change once the prototype hits real data.

## Pace curves

Given total duration `T` (seconds), start BPM `b0`, and end BPM `b1`, each strategy defines BPM as a function of elapsed time `t ∈ [0, T]`.

Defaults (tunable later):
- `b0 = avg − 10 BPM`, `b1 = avg + 10 BPM` where `avg` is the BPM derived from average pace via the usual cadence heuristic (~170–180 BPM for typical recreational runners; app will let the user tune the midpoint).
- For **constant**, the single target is `avg`.

### Constant
```
bpm(t) = avg
```
One BPM target. Simplest to implement, simplest to match.

### Linear ramp
```
bpm(t) = b0 + (b1 − b0) · (t / T)
```
BPM rises at a steady rate from start to finish.

### Delayed exponential
```
bpm(t) = b0 + (b1 − b0) · (exp(k · t / T) − 1) / (exp(k) − 1)
```
- `k` controls how "delayed" the ramp is. `k ≈ 4` gives a noticeable flat stretch then a clear push at the end; `k → 0` collapses to linear.
- Start value matches `b0` at `t=0`, end value matches `b1` at `t=T`.
- Feels like a negative-split plan: cruise early, empty the tank at the end.

## Track selection

The curve is sampled at 1-minute resolution to get a target BPM sequence. For each target:

1. Filter candidate pool to tracks within ±3 BPM (consider half/double tempo).
2. Prefer tracks that fit the remaining time budget (don't pick a 5-min track with 1 min left).
3. Pick one, remove from pool, advance cursor by track duration.
4. Widen tolerance if no candidate matches; fall back to nearest BPM.

Greedy, one-pass. If results are poor we revisit with a DP/beam-search.

## Auth flow

Spotify **Authorization Code Flow with PKCE**:

1. App generates `code_verifier` + `code_challenge` (S256).
2. Launches Custom Tabs / browser to Spotify authorize URL with `code_challenge`.
3. Spotify redirects back to a registered redirect URI (custom scheme, e.g. `spotifypacer://callback`).
4. App exchanges `code + code_verifier` for access + refresh tokens at `/api/token`.
5. Refresh token stored in EncryptedSharedPreferences.

No client secret. Only the public client ID ships in the app.

Scopes needed:
- `user-library-read` — read liked songs.
- `playlist-read-private` / `playlist-read-collaborative` — read user's existing playlists as source material.
- `playlist-modify-private` (and possibly `playlist-modify-public`) — write the generated playlist.

## Error surfacing

The preview screen maps load-path failures from the Spotify Web API to user-visible buckets:

| Bucket | Trigger | Notes |
|---|---|---|
| `Network` | `IOException` (no connectivity, DNS, socket reset) | |
| `Forbidden` | HTTP `403` from Spotify | Usually scope mismatch or revoked refresh token. |
| `RateLimited` | HTTP `429` from Spotify, or `BpmProviderException.RateLimited` from GetSongBPM | The OkHttp `RateLimitInterceptor` retries once on `Retry-After`; only persistent throttling reaches the user. |
| `BpmProviderUnconfigured` | Missing or rejected GetSongBPM API key (HTTP `401`/`403` from `api.getsong.co`, or blank `BuildConfig.GETSONGBPM_API_KEY`) | Distinct from Spotify `Forbidden` so the user isn't told to re-sign-in to Spotify when the actual problem is the BPM key. |
| `BpmProviderUnavailable` | GetSongBPM `5xx`, transport failure, or unexpected HTTP code | |
| `EmptyPool` | Liked-tracks empty, or zero matches against the curve | |
| `SaveFailed` | `SavePlaylistResult.Failure` from `SavePlaylistUseCase` | |
| `Unknown` | Final fallback for the load path | All other throwables. Save path uses `SaveFailed` as its fallback instead. |

`PreviewViewModel` logs the throwable (tag `PreviewViewModel`) before mapping. Auth-token refresh failures log under `AuthTokenBridge`. The `SpotifyApiFactory` and `GetSongBpmApiFactory` wire an `HttpLoggingInterceptor` at `BASIC` level in debug builds — method, URL, response code, duration; no headers/bodies. Bearer tokens and the GetSongBPM API key stay out of bodies; the latter does appear in URL query params under `BASIC` — acceptable in debug only.

## BPM source

Spotify deprecated `/v1/audio-features` (and `/audio-analysis`, `/recommendations`, `/related-artists`) for new app registrations in November 2024. Extended quota mode is granted to organizations only, so a hobby app cannot get the endpoint back. The selector instead resolves BPM via [GetSongBPM](https://getsongbpm.com/) and caches results on disk.

**Matching strategy.** For each Spotify track, build the lookup `song:{normalised title} artist:{primary artist}` and call `GET /search/?type=song&...` on `api.getsong.co`. Title normalisation strips parenthetical/bracketed annotations and ` - …` suffixes (Remastered, Live, Radio Edit, feat. blocks). Among returned hits, prefer the first whose `song_length` is within ±10s of the Spotify track's duration; otherwise fall back to the first hit. Tracks with no usable hit are recorded as a negative cache entry.

**Throttling.** Sequential lookups, one request per ~1.1s, in a single in-flight call. The repository sleeps via an injected suspension function to keep tests deterministic.

**Cache.** `BpmCache` writes a versioned JSON file in `filesDir`. Positives never expire; negatives expire after 30 days so newly-added GetSongBPM entries (or fixed normalisation) get retried. Bumping `CACHE_VERSION` invalidates everything.

**API key.** Read from `getsongbpm.apiKey` in `local.properties` (or `GETSONGBPM_API_KEY` env), exposed as `BuildConfig.GETSONGBPM_API_KEY`. The key ships in the APK and is extractable; we treat it as public-by-design (analogous to the Spotify PKCE client ID), not as a secret.

**Library sampling.** `SpotifyCandidateLoader` pages through `/me/tracks` and resolves BPM page-by-page, stopping at `minPool` matched candidates or `maxLookupBudget` external lookups. This biases the pool toward recently-liked tracks for users with very large libraries — acceptable trade-off for first ship; tracked as a known limitation.

## High-level modules (tentative)

- `auth/` — PKCE flow, token storage, refresh.
- `spotify/` — thin Web API client (saved tracks, playlists).
- `bpm/` — GetSongBPM client, repository, on-disk cache.
- `pacing/` — curve generator + track selection.
- `ui/` — Compose screens: Login → Setup → Preview → Done.

## Open questions

- Re-roll: reshuffle within the same pool, or re-query?
- If user's library is small, do we allow recommendation seeding? (Currently out of scope — see REQUIREMENTS.)
- Quality gate when `minPool` produces an underfilled selection — surface a warning vs. silently proceed.
