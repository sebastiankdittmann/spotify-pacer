package dk.dittmann.spotifypacer.spotify

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Attaches a Bearer access token to every request. If the response is 401, refreshes the token once
 * and retries.
 */
class SpotifyAuthInterceptor(private val tokens: SpotifyTokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.currentAccessToken() ?: tokens.refreshAccessToken()
        val response = chain.proceed(chain.request().withBearer(token))

        if (response.code != 401) return response

        response.close()
        val refreshed = tokens.refreshAccessToken()
        return chain.proceed(chain.request().withBearer(refreshed))
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
