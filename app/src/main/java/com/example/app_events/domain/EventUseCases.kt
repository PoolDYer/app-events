package com.example.app_events.domain

import javax.inject.Inject

class LoadEvents @Inject constructor(private val repository: EventsRepository) {
    fun observe() = repository.observeEvents()
    suspend fun refresh() = repository.refreshEvents()
}

class SubscribeToEvent @Inject constructor(private val repository: EventsRepository) {
    suspend operator fun invoke(id: String) {
        if (id.isBlank()) throw EventlyFailure(FailureReason.InvalidData)
        repository.subscribe(id)
    }
}
