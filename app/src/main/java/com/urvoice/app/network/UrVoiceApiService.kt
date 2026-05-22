package com.urvoice.app.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

private const val BASE_URL = "https://urvoice-production.up.railway.app/"

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
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrVoiceApiService::class.java)
    }
}
