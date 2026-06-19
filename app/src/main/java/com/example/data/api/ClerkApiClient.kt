package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ClerkUser(
    val id: String,
    val first_name: String?,
    val last_name: String?,
    val image_url: String?,
    val email_addresses: List<ClerkEmail>?,
    val phone_numbers: List<ClerkPhoneNumber>?
) {
    val primaryEmail: String?
        get() = email_addresses?.firstOrNull()?.email_address

    val primaryPhone: String?
        get() = phone_numbers?.firstOrNull()?.phone_number
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ClerkEmail(
    val id: String,
    val email_address: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ClerkPhoneNumber(
    val id: String,
    val phone_number: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ClerkCreateUserRequest(
    val first_name: String?,
    val last_name: String?,
    val email_address: List<String>?,
    val phone_number: List<String>?,
    val password: String? = null,
    val skip_password_requirement: Boolean? = true
)

interface ClerkApiService {
    @GET("v1/users")
    suspend fun listUsers(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 50
    ): List<ClerkUser>

    @POST("v1/users")
    suspend fun createUser(
        @Header("Authorization") authHeader: String,
        @Body request: ClerkCreateUserRequest
    ): ClerkUser
}

object ClerkApiClient {
    private const val BASE_URL = "https://api.clerk.com/"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: ClerkApiService = retrofit.create(ClerkApiService::class.java)

    fun getSecretKey(): String {
        return BuildConfig.CLERK_SECRET_KEY
    }

    fun getPublishableKey(): String {
        return BuildConfig.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY
    }

    fun getAuthHeader(): String {
        return "Bearer ${getSecretKey()}"
    }
}
