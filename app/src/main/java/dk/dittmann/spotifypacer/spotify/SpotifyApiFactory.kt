package dk.dittmann.spotifypacer.spotify

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dk.dittmann.spotifypacer.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object SpotifyApiFactory {

    const val DEFAULT_BASE_URL = "https://api.spotify.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun create(
        tokens: SpotifyTokenProvider,
        baseUrl: String = DEFAULT_BASE_URL,
        okHttp: OkHttpClient = OkHttpClient(),
    ): SpotifyApi {
        val builder =
            okHttp
                .newBuilder()
                .addInterceptor(SpotifyAuthInterceptor(tokens))
                .addInterceptor(RateLimitInterceptor())
        if (BuildConfig.DEBUG) {
            // BASIC logs method, URL, response code, duration. No headers/bodies — bearer tokens
            // and refresh-token form bodies stay out of logcat. URLs (including ?ids=… for
            // audio-features) are logged; acceptable for a debug build.
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(builder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotifyApi::class.java)
    }
}
