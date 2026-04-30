package dk.dittmann.spotifypacer.spotify

import okhttp3.Interceptor
import okhttp3.Response

/**
 * On HTTP 429, sleeps for the duration suggested by the Retry-After header (seconds) and retries
 * the request once. Subsequent 429s propagate to the caller.
 */
class RateLimitInterceptor(
    private val sleeper: (Long) -> Unit = Thread::sleep,
    private val maxRetryDelayMs: Long = DEFAULT_MAX_RETRY_DELAY_MS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != 429) return response

        val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 0L
        val delayMs = (retryAfterSeconds * 1000L).coerceIn(0L, maxRetryDelayMs)
        response.close()
        if (delayMs > 0L) sleeper(delayMs)
        return chain.proceed(chain.request())
    }

    private companion object {
        const val DEFAULT_MAX_RETRY_DELAY_MS = 60_000L
    }
}
