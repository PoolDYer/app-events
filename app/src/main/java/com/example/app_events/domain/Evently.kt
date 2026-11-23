package com.example.app_events.domain

import kotlinx.coroutines.flow.Flow

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val date: String,
    val status: String,
    val availableSpots: Int
)

interface EventsRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(id: String): Flow<Event?>
    fun observeSubscriptions(): Flow<List<Event>>
    fun observeSubscribed(id: String): Flow<Boolean>
    suspend fun refreshEvents()
    suspend fun refreshDetail(id: String)
    suspend fun refreshSubscriptions()
    suspend fun subscribe(id: String)
}

interface SessionRepository {
    val active: Flow<Boolean>
    suspend fun restore()
    suspend fun authenticate(name: String?, email: String, password: String)
    suspend fun logout()
}

enum class FailureReason {
    Offline, Timeout, InvalidCredentials, DuplicateEmail, InvalidData,
    ExpiredSession, MissingEvent, FullEvent, ClosedEvent, Unavailable
}

class EventlyFailure(val reason: FailureReason) : RuntimeException()
