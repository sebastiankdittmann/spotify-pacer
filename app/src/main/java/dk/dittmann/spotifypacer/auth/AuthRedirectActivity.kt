package dk.dittmann.spotifypacer.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dk.dittmann.spotifypacer.SpotifyPacerApplication
import kotlinx.coroutines.launch

class AuthRedirectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data == null) {
            finish()
            return
        }
        val service = (application as SpotifyPacerApplication).authService
        lifecycleScope.launch {
            runCatching {
                service.completeAuthorize(
                    code = data.getQueryParameter("code"),
                    state = data.getQueryParameter("state"),
                    error = data.getQueryParameter("error"),
                )
            }
            finish()
        }
    }
}
