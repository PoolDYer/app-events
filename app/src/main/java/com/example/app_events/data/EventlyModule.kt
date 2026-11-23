package com.example.app_events.data

import android.content.Context
import androidx.room.Room
import com.example.app_events.BuildConfig
import com.example.app_events.data.local.EventlyDatabase
import com.example.app_events.data.remote.AccessTokenInterceptor
import com.example.app_events.data.remote.EventsApi
import com.example.app_events.data.remote.RefreshAuthenticator
import com.example.app_events.data.remote.SessionApi
import com.example.app_events.domain.EventsRepository
import com.example.app_events.domain.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class EventlyModule {
    @Binds
    abstract fun events(implementation: EventlyRepository): EventsRepository

    @Binds
    abstract fun session(implementation: EventlyRepository): SessionRepository

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): EventlyDatabase =
            Room.databaseBuilder(context, EventlyDatabase::class.java, "evently_basic.db").build()

        @Provides
        @Singleton
        fun sessionApi(): SessionApi = retrofit(httpClient().build()).create(SessionApi::class.java)

        @Provides
        @Singleton
        fun eventsApi(interceptor: AccessTokenInterceptor, authenticator: RefreshAuthenticator): EventsApi =
            retrofit(httpClient().addInterceptor(interceptor).authenticator(authenticator).build())
                .create(EventsApi::class.java)

        private fun httpClient() = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .followRedirects(false)

        private fun retrofit(client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(BuildConfig.EVENTLY_API_URL.ifBlank { "https://evently.invalid/api/" })
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
}
