package dk.dittmann.spotifypacer.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dk.dittmann.spotifypacer.MainActivity
import dk.dittmann.spotifypacer.SpotifyPacerApplication
import kotlinx.coroutines.launch

/**
 * Theme.NoDisplay activity that handles the OAuth redirect. Must call finish() before onResume()
 * returns or Android crashes the app, so the token exchange is dispatched onto the application
 * scope and this activity finishes synchronously. Login UI observes [AuthService.isAuthenticated]
 * to react when the exchange completes.
 *
 * After processing, MainActivity is brought back to the front so the user returns to the app
 * instead of staring at the now-stale Custom Tabs page.
 */
class AuthRedirectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null) {
            val app = application as SpotifyPacerApplication
            app.applicationScope.launch {
                runCatching {
                    app.authService.completeAuthorize(
                        code = data.getQueryParameter("code"),
                        state = data.getQueryParameter("state"),
                        spotifyError = data.getQueryParameter("error"),
                    )
                }
            }
        }
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }
}
