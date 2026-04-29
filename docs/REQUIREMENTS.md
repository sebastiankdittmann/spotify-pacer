# Requirements

## Goal

Generate a Spotify playlist whose track tempo (BPM) matches a user-selected pacing curve for a run of chosen distance and target time.

## User flow

1. **Sign in** with Spotify (Authorization Code Flow with PKCE — no personal tokens required from the user).
2. **Pick distance** (e.g. 5 km, 10 km, half marathon, custom).
3. **Pick target time** (total duration, e.g. 28 min for 5 km).
4. **Pick pace strategy**:
   - **Constant** — single target BPM for the whole run.
   - **Linear ramp** — BPM rises linearly from start to finish.
   - **Delayed exponential** — BPM stays gentle for most of the run, then ramps up sharply near the end (negative-split style).
5. **Preview the playlist** — see selected tracks, in order, with each track's BPM and its position on the pace curve.
6. **Approve** — save the playlist to the user's Spotify account with a generated name (e.g. "Pacer 5km · 28:00 · delayed-exp").

## Functional requirements

### Auth
- **F-A1** *(implemented, [#2](https://github.com/sebastiankdittmann/spotify-pacer/issues/2))*: Use Spotify Authorization Code Flow with PKCE. The app must not require users to paste tokens or register their own app credentials.
- **F-A2** *(implemented, [#2](https://github.com/sebastiankdittmann/spotify-pacer/issues/2))*: Store refresh token securely on device (EncryptedSharedPreferences or Android Keystore-backed).
- **F-A3** *(implemented, [#2](https://github.com/sebastiankdittmann/spotify-pacer/issues/2))*: Sign-out clears stored tokens.

### Input
- **F-I1**: User inputs distance (km, with decimal) and target time (hh:mm:ss or mm:ss).
- **F-I2**: User picks one of the three pace strategies.
- **F-I3**: Sensible validation: positive distance, positive time, resulting average pace in a plausible running range (warn outside 3:00–10:00 min/km).

### Pacing engine
- **F-P1** *(implemented, [#4](https://github.com/sebastiankdittmann/spotify-pacer/issues/4))*: Convert (distance, time, strategy) into a BPM-over-time curve covering the full run duration.
- **F-P2**: Total playlist duration should be within ±30 s of the target time.
- **F-P3** *(implemented, [#4](https://github.com/sebastiankdittmann/spotify-pacer/issues/4))*: Strategy definitions in [DESIGN.md](DESIGN.md#pace-curves).

### Track selection
- **F-T1**: Source candidate tracks from the user's liked songs and playlists.
- **F-T2**: Fetch `audio-features` for candidates to get `tempo` (BPM).
- **F-T3** *(implemented, [#5](https://github.com/sebastiankdittmann/spotify-pacer/issues/5))*: Select tracks greedily, minute by minute, whose BPM best matches the curve at that moment, subject to:
  - No repeats within a playlist.
  - Prefer tracks within ±3 BPM of the target at that point; widen tolerance if no match.
  - Allow half/double-tempo substitution (a 90 BPM track can stand in for 180 BPM and vice versa).

### Preview & save
- **F-S1**: Show ordered list of picked tracks with title, artist, BPM, duration, and cumulative run-time marker.
- **F-S2**: Allow the user to re-roll (regenerate with a different random seed) before saving.
- **F-S3**: On approve, create a new playlist in the user's account via Spotify Web API.
- **F-S4**: Playlist name follows the template: `Pacer · {distance} · {target time} · {strategy}`.

## Non-functional requirements

- **N-1**: Offline-tolerant preview — once candidates and audio-features are fetched, pace generation and re-roll run locally without network.
- **N-2**: No secrets in the repo. Spotify client ID is public (required for PKCE); no client secret is used.
- **N-3**: Minimum Android version: API 29 (Android 10). Target latest stable.
- **N-4**: The app is personal/hobby scope — no analytics, no account system beyond Spotify's.

## Out of scope (for now)

- iOS / web clients.
- Real-time BPM adjustment during a run (GPS-driven).
- Social features / sharing.
- Recommendations from outside the user's library.
- Wear OS / watch companion.
