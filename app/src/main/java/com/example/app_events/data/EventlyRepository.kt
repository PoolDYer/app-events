package com.example.app_events.data

import android.content.Context
import androidx.room.withTransaction
import com.example.app_events.data.local.EncryptedSession
import com.example.app_events.data.local.EventlyDatabase
import com.example.app_events.data.local.SessionId
import com.example.app_events.data.local.SubscriptionEntity
import com.example.app_events.data.remote.EventDto
import com.example.app_events.data.remote.EventsApi
import com.example.app_events.data.remote.LoginRequest
import com.example.app_events.data.remote.RegisterRequest
import com.example.app_events.data.remote.SessionApi
import com.example.app_events.data.remote.SubscriptionDto
import com.example.app_events.domain.EventlyFailure
import com.example.app_events.domain.EventsRepository
import com.example.app_events.domain.FailureReason
import com.example.app_events.domain.SessionRepository
import com.squareup.moshi.JsonDataException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class EventlyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: EventlyDatabase,
    private val store: EncryptedSession,
    private val sessionApi: SessionApi,
    private val eventsApi: EventsApi
) : SessionRepository, EventsRepository {
    private val sessionLock = Mutex()
    private val operations = Mutex()

    override val active = store.tokens.map { it != null }.distinctUntilChanged()

    override suspend fun restore() = withContext(Dispatchers.IO) {
        sessionLock.withLock {
            val session = store.read()
            if (session == null) database.clearAllTables() else store.restore(session)
        }
    }

    override suspend fun authenticate(name: String?, email: String, password: String) {
        val credentials = request {
            if (name == null) sessionApi.login(LoginRequest(email, password))
            else sessionApi.register(RegisterRequest(name, email, password))
        }
        withContext(Dispatchers.IO) {
            sessionLock.withLock {
                if (store.tokens.value != null) return@withLock
                database.clearAllTables()
                store.start(credentials)
            }
        }
    }

    override suspend fun logout() {
        val token = withContext(Dispatchers.IO + NonCancellable) {
            sessionLock.withLock {
                clearPreviousDemoData()
                database.clearAllTables()
                store.clear()?.refresh
            }
        }
        if (token != null) {
            try {
                sessionApi.logout("Bearer " + token).errorBody()?.close()
            } catch (_: IOException) {
                // The device session has already been closed.
            }
        }
    }

    private fun clearPreviousDemoData() {
        val databaseName = "app_events_database"
        val previousDatabase = context.getDatabasePath(databaseName)
        if (previousDatabase.exists()) {
            check(context.deleteDatabase(databaseName))
        }

        val previousPreferences = context.filesDir.resolve("datastore/user_settings.preferences_pb")
        if (previousPreferences.exists()) {
            check(previousPreferences.delete())
        }
    }

    override fun observeEvents() = database.events.observeEvents().map { events -> events.map { it.toDomain() } }
    override fun observeEvent(id: String) = database.events.observeEvent(id).map { it?.toDomain() }
    override fun observeSubscriptions() = database.subscriptions.observeEvents().map { events -> events.map { it.toDomain() } }
    override fun observeSubscribed(id: String) = database.subscriptions.observeSubscribed(id)

    override suspend fun refreshEvents() = withSession { session ->
        val events = eventsApi.events(session)
        write(session) {
            database.events.clearListing()
            database.events.upsert(events.map { it.toEntity(listed = true) })
        }
    }

    override suspend fun refreshDetail(id: String) = withSession { session ->
        val event = eventsApi.event(id, session) ?: throw EventlyFailure(FailureReason.MissingEvent)
        write(session) { upsertDetails(listOf(event)) }
        val subscriptions = eventsApi.subscriptions(session)
        write(session) { saveSubscriptions(subscriptions) }
    }

    override suspend fun refreshSubscriptions() = withSession { session ->
        val subscriptions = eventsApi.subscriptions(session)
        write(session) { saveSubscriptions(subscriptions) }
    }

    override suspend fun subscribe(id: String) = withSession { session ->
        val subscription = eventsApi.subscribe(id, session)
        write(session) {
            upsertDetails(listOf(subscription.event))
            database.subscriptions.insert(listOf(SubscriptionEntity(subscription.event.id)))
        }
    }

    private suspend fun upsertDetails(events: List<EventDto>) {
        database.events.upsert(events.map { it.toEntity(database.events.isListed(it.id) == true) })
    }

    private suspend fun saveSubscriptions(subscriptions: List<SubscriptionDto>) {
        upsertDetails(subscriptions.map(SubscriptionDto::event))
        database.subscriptions.clear()
        database.subscriptions.insert(subscriptions.map { SubscriptionEntity(it.event.id) })
    }

    private suspend fun write(session: SessionId, block: suspend () -> Unit) {
        sessionLock.withLock {
            checkSession(session)
            database.withTransaction { block() }
        }
    }

    private fun checkSession(session: SessionId) {
        if (store.tokens.value?.id != session) throw EventlyFailure(FailureReason.ExpiredSession)
    }

    private suspend fun withSession(block: suspend (SessionId) -> Unit) {
        val session = store.tokens.value?.id ?: throw EventlyFailure(FailureReason.ExpiredSession)
        operations.withLock {
            checkSession(session)
            try {
                request { block(session) }
            } catch (failure: EventlyFailure) {
                if (failure.reason == FailureReason.ExpiredSession) {
                    withContext(Dispatchers.IO + NonCancellable) {
                        sessionLock.withLock {
                            if (store.tokens.value?.id == session) {
                                database.clearAllTables()
                                store.clear()
                            }
                        }
                    }
                }
                throw failure
            }
        }
    }

    private suspend fun <T> request(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            val response = exception.response()?.errorBody()?.use { it.string() }.orEmpty()
            val reason = when (exception.code()) {
                400 -> if ("Email already registered" in response) FailureReason.DuplicateEmail else FailureReason.InvalidData
                401 -> if (exception.response()?.raw()?.request?.tag(SessionId::class.java) != null) {
                    FailureReason.ExpiredSession
                } else FailureReason.InvalidCredentials
                403 -> FailureReason.ExpiredSession
                404 -> FailureReason.MissingEvent
                409 -> if ("EVENT_FULL" in response) FailureReason.FullEvent else FailureReason.ClosedEvent
                else -> FailureReason.Unavailable
            }
            throw EventlyFailure(reason)
        } catch (_: InterruptedIOException) {
            throw EventlyFailure(FailureReason.Timeout)
        } catch (_: IOException) {
            throw EventlyFailure(FailureReason.Offline)
        } catch (_: JsonDataException) {
            throw EventlyFailure(FailureReason.Unavailable)
        }
    }
}
