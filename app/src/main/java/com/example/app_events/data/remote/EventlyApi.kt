package com.example.app_events.data.remote

import com.example.app_events.data.local.EventEntity
import com.example.app_events.data.local.SessionId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Tag

interface SessionApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): CredentialsDto

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): CredentialsDto

    @POST("auth/refresh-token")
    fun refresh(@Header("Authorization") token: String): Call<CredentialsDto>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>
}

interface EventsApi {
    @GET("events")
    suspend fun events(@Tag session: SessionId): List<EventDto>

    @GET("events/{id}")
    suspend fun event(@Path("id") id: String, @Tag session: SessionId): EventDto?

    @POST("events/{id}/subscribe")
    suspend fun subscribe(@Path("id") id: String, @Tag session: SessionId): SubscriptionDto

    @GET("me/subscriptions")
    suspend fun subscriptions(@Tag session: SessionId): List<SubscriptionDto>
}

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class RegisterRequest(val name: String, val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class CredentialsDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class EventDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val location: String,
    val date: String,
    val status: String,
    val availableSpots: Int
) {
    fun toEntity(listed: Boolean) =
        EventEntity(id, title, description.orEmpty(), location, date, status, availableSpots, listed)
}

@JsonClass(generateAdapter = true)
data class SubscriptionDto(val event: EventDto)
