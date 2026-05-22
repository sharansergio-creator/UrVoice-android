package com.urvoice.app.network

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://urvoice-production.up.railway.app/"
private const val TAG = "UrVoice/API"

data class FetchBusinessContextRequest(
    @SerializedName("gbp_url")     val gbpUrl: String,
    @SerializedName("website_url") val websiteUrl: String,
    @SerializedName("user_id")     val userId: String,
)

interface UrVoiceApiService {
    @POST("fetch-business-context")
    suspend fun fetchBusinessContext(
        @Body request: FetchBusinessContextRequest
    ): Response<ResponseBody>
}

object UrVoiceApi {
    val service: UrVoiceApiService by lazy {
        val logging = HttpLoggingInterceptor { msg -> Log.d(TAG, msg) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrVoiceApiService::class.java)
    }
}
