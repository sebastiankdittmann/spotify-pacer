package dk.dittmann.spotifypacer.auth

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object AuthLauncher {

    fun launch(activity: Activity, authService: AuthService) {
        val url = authService.prepareAuthorize()
        CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(url.toString()))
    }
}
