package dk.dittmann.spotifypacer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dk.dittmann.spotifypacer.auth.AuthLauncher
import dk.dittmann.spotifypacer.ui.nav.AppNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SpotifyPacerApplication
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        authService = app.authService,
                        tokenStore = app.tokenStore,
                        signInLauncher = { AuthLauncher.launch(this, app.authService) },
                    )
                }
            }
        }
    }
}
