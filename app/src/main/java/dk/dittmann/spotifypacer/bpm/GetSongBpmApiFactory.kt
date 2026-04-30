package dk.dittmann.spotifypacer.bpm

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dk.dittmann.spotifypacer.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/** Retrofit factory for [GetSongBpmApi]. Mirrors the Spotify factory's logging policy. */
object GetSongBpmApiFactory {

    const val DEFAULT_BASE_URL = "https://api.getsong.co/"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun create(
        baseUrl: String = DEFAULT_BASE_URL,
        okHttp: OkHttpClient = OkHttpClient(),
    ): GetSongBpmApi {
        val builder = okHttp.newBuilder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(builder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GetSongBpmApi::class.java)
    }
}
