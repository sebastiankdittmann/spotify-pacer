package dk.dittmann.spotifypacer.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object AuthLauncher {

    fun launch(context: Context, authService: AuthService) {
        val url = authService.prepareAuthorize()
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url.toString()))
    }
}
