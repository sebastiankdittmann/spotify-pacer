package dk.dittmann.spotifypacer.auth

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object AuthApiFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun create(
        baseUrl: String = AuthService.DEFAULT_AUTH_BASE_URL,
        okHttp: OkHttpClient = OkHttpClient(),
    ): SpotifyAuthApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SpotifyAuthApi::class.java)
}
