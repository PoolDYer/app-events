package com.example.app_events.data.remote

import com.example.app_events.data.local.EncryptedSession
import com.example.app_events.data.local.SessionId
import java.io.IOException
import javax.inject.Inject
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

const val AUTHORIZATION = "Authorization"

class AccessTokenInterceptor @Inject constructor(private val store: EncryptedSession) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val id = request.tag(SessionId::class.java) ?: return chain.proceed(request)
        val tokens = store.tokens.value?.takeIf { it.id == id } ?: throw IOException("Session changed")
        return chain.proceed(request.newBuilder().header(AUTHORIZATION, "Bearer " + tokens.access).build())
    }
}

class RefreshAuthenticator @Inject constructor(
    private val sessions: SessionApi,
    private val store: EncryptedSession
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val id = response.request.tag(SessionId::class.java) ?: return null
        if (response.priorResponse != null) return null

        val updated = synchronized(refreshLock) {
            val tokens = store.tokens.value?.takeIf { it.id == id } ?: return null
            if (response.request.header(AUTHORIZATION) != "Bearer " + tokens.access) {
                tokens
            } else {
                val refreshed = sessions.refresh("Bearer " + tokens.refresh).execute()
                if (!refreshed.isSuccessful) {
                    refreshed.errorBody()?.close()
                    when (refreshed.code()) {
                        400, 401, 403 -> return null
                        else -> throw IOException("Session renewal unavailable")
                    }
                }
                val credentials = refreshed.body() ?: throw IOException("Missing session renewal")
                store.rotate(tokens, credentials)
            }
        } ?: return null

        return response.request.newBuilder().header(AUTHORIZATION, "Bearer " + updated.access).build()
    }
}
