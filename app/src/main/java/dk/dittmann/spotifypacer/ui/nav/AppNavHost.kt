package dk.dittmann.spotifypacer.ui.nav

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dk.dittmann.spotifypacer.BuildConfig
import dk.dittmann.spotifypacer.auth.AuthService
import dk.dittmann.spotifypacer.auth.TokenStore
import dk.dittmann.spotifypacer.bpm.BpmCache
import dk.dittmann.spotifypacer.bpm.GetSongBpmApiFactory
import dk.dittmann.spotifypacer.bpm.GetSongBpmRepository
import dk.dittmann.spotifypacer.save.SavePlaylistUseCase
import dk.dittmann.spotifypacer.spotify.SpotifyApiFactory
import dk.dittmann.spotifypacer.spotify.SpotifyClient
import dk.dittmann.spotifypacer.ui.login.SignInLauncher
import dk.dittmann.spotifypacer.ui.preview.SpotifyCandidateLoader
import java.io.File

/**
 * Top-level NavHost. Wires the three v0.1 destinations together and constructs the long-lived
 * Spotify dependencies once, sharing them between the setup → preview transition.
 *
 * Auth-guarded start: we skip Login when a refresh token is already stored. The first authenticated
 * Spotify call that fails on refresh will surface as a network error in the calling screen — not
 * elegant, but acceptable for v0.1.
 */
@Composable
fun AppNavHost(
    authService: AuthService,
    tokenStore: TokenStore,
    signInLauncher: SignInLauncher,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val spotifyClient =
        remember(authService) {
            SpotifyClient(SpotifyApiFactory.create(AuthTokenBridge(authService)))
        }
    val bpmRepository =
        remember(context) {
            GetSongBpmRepository(
                api = GetSongBpmApiFactory.create(),
                cache = BpmCache(File(context.filesDir, "bpm-cache.json")),
                apiKey = BuildConfig.GETSONGBPM_API_KEY,
            )
        }
    val candidateLoader =
        remember(spotifyClient, bpmRepository) {
            SpotifyCandidateLoader(spotifyClient, bpmRepository)
        }
    val savePlaylist = remember(spotifyClient) { SavePlaylistUseCase(spotifyClient) }

    NavHost(
        navController = navController,
        startDestination = chooseStartRoute(tokenStore),
        modifier = modifier,
    ) {
        composable(Routes.Login) {
            LoginRoute(
                authService = authService,
                signInLauncher = signInLauncher,
                onSignedIn = {
                    navController.navigate(Routes.Setup) {
                        popUpTo(Routes.Login) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.Setup) {
            SetupRoute(onConfirm = { config -> navController.navigate(Routes.preview(config)) })
        }
        composable(
            route = Routes.PreviewPattern,
            arguments = listOf(navArgument(Routes.PreviewArgConfig) { type = NavType.StringType }),
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString(Routes.PreviewArgConfig)
            val config =
                checkNotNull(encoded) { "Preview destination requires a config argument." }
                    .let(RunConfigArg::decode)
                    .toRunConfig()
            PreviewRoute(
                config = config,
                candidateLoader = candidateLoader,
                save = savePlaylist::invoke,
                onOpenPlaylist = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )
        }
    }
}
