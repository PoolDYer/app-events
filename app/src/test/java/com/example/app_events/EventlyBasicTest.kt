package com.example.app_events

import com.example.app_events.domain.Event
import com.example.app_events.domain.EventlyFailure
import com.example.app_events.domain.EventsRepository
import com.example.app_events.domain.FailureReason
import com.example.app_events.domain.LoadEvents
import com.example.app_events.domain.SubscribeToEvent
import com.example.app_events.presentation.EventsViewModel
import com.example.app_events.presentation.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventlyBasicTest {
    @Test
    fun listingShowsLocalEventsThenServerRefreshUpdatesTheSameFlow() = runTest {
        val repository = FakeEvents()
        val useCase = LoadEvents(repository)
        assertEquals(listOf(cached), useCase.observe().first())
        useCase.refresh()
        assertEquals(listOf(updated), useCase.observe().first())
        assertEquals(1, repository.refreshes)
    }

    @Test
    fun subscribingUpdatesTheObservedSubscriptionAndRejectsBlankIds() = runTest {
        val repository = FakeEvents()
        val useCase = SubscribeToEvent(repository)
        val invalid = runCatching { useCase(" ") }.exceptionOrNull() as EventlyFailure
        assertEquals(FailureReason.InvalidData, invalid.reason)
        assertEquals(emptyList<String>(), repository.submitted)

        useCase(cached.id)
        assertEquals(listOf(cached.id), repository.submitted)
        assertTrue(repository.observeSubscribed(cached.id).first())
    }

    @Test
    fun viewModelKeepsCachedEventsOnErrorAndReloadsAfterRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeEvents()
            val gate = CompletableDeferred<Unit>()
            repository.refreshGate = gate
            repository.failure = EventlyFailure(FailureReason.Offline)
            val viewModel = EventsViewModel(LoadEvents(repository))
            runCurrent()
            assertEquals(listOf(cached), viewModel.events.value)
            assertEquals(UiState.Loading, viewModel.result.value)

            viewModel.refresh()
            runCurrent()
            assertEquals(1, repository.refreshes)
            gate.complete(Unit)
            advanceUntilIdle()
            assertTrue(viewModel.result.value is UiState.Error)
            assertEquals(listOf(cached), viewModel.events.value)

            repository.failure = null
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(UiState.Ready, viewModel.result.value)
            assertEquals(listOf(updated), viewModel.events.value)
            assertEquals(2, repository.refreshes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeEvents : EventsRepository {
        val local = MutableStateFlow(listOf(cached))
        val subscribed = MutableStateFlow(false)
        val submitted = mutableListOf<String>()
        var refreshes = 0
        var refreshGate: CompletableDeferred<Unit>? = null
        var failure: EventlyFailure? = null

        override fun observeEvents() = local
        override fun observeEvent(id: String) = MutableStateFlow(local.value.find { it.id == id })
        override fun observeSubscriptions() = local
        override fun observeSubscribed(id: String) = subscribed

        override suspend fun refreshEvents() {
            refreshes++
            refreshGate?.await()
            failure?.let { throw it }
            local.value = listOf(updated)
        }

        override suspend fun refreshDetail(id: String) = Unit
        override suspend fun refreshSubscriptions() = Unit

        override suspend fun subscribe(id: String) {
            submitted += id
            subscribed.value = true
        }
    }

    private companion object {
        val cached = Event("example", "Evento guardado", "", "Ayacucho", "2026-12-01T14:00:00Z", "ACTIVE", 40)
        val updated = cached.copy(title = "Evento actualizado", availableSpots = 36)
    }
}
