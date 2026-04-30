package dk.dittmann.spotifypacer.spotify

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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
        val client =
            okHttp
                .newBuilder()
                .addInterceptor(SpotifyAuthInterceptor(tokens))
                .addInterceptor(RateLimitInterceptor())
                .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotifyApi::class.java)
    }
}
