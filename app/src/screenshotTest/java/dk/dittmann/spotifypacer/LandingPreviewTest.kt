package dk.dittmann.spotifypacer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Preview screenshot tests. Every @Preview here is rendered to PNG by
 * `./gradlew :app:updateDebugScreenshotTest` (reference images) and compared by
 * `./gradlew :app:validateDebugScreenshotTest` (CI).
 *
 * When you add a new screen, mirror its @Preview variants in this source set so
 * screenshots show up in PRs.
 */
@Preview(name = "landing_light", showBackground = true)
@Composable
fun LandingLightPreview() {
    MaterialTheme { Landing() }
}
