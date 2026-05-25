package com.urvoice.app.network

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @POST("provision-number")
    suspend fun provisionNumber(@Body body: Map<String, String>): Response<Map<String, Any>>

    @Multipart
    @POST("clone-voice")
    suspend fun cloneVoice(
        @Part("user_id") userId: RequestBody,
        @Part("language") language: RequestBody,
        @Part audio: MultipartBody.Part
    ): Response<Map<String, Any>>

    @POST("delete-voice")
    suspend fun deleteVoice(@Body body: Map<String, String>): Response<Map<String, Any>>
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
