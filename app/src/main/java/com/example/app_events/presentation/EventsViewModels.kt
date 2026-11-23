package com.example.app_events.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_events.domain.Event
import com.example.app_events.domain.EventsRepository
import com.example.app_events.domain.LoadEvents
import com.example.app_events.domain.SubscribeToEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EventsViewModel @Inject constructor(private val loadEvents: LoadEvents) : ViewModel() {
    val events = loadEvents.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val status = MutableStateFlow<UiState>(UiState.Ready)
    val result = status.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (status.value == UiState.Loading) return
        viewModelScope.launch {
            status.value = UiState.Loading
            status.value = outcome { loadEvents.refresh() }
        }
    }
}

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(private val repository: EventsRepository) : ViewModel() {
    val events = repository.observeSubscriptions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val status = MutableStateFlow<UiState>(UiState.Ready)
    val result = status.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (status.value == UiState.Loading) return
        viewModelScope.launch {
            status.value = UiState.Loading
            status.value = outcome { repository.refreshSubscriptions() }
        }
    }
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    state: SavedStateHandle,
    private val repository: EventsRepository,
    private val subscribeToEvent: SubscribeToEvent
) : ViewModel() {
    private val id: String = checkNotNull(state["id"])
    val event: StateFlow<Event?> = repository.observeEvent(id).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val subscribed = repository.observeSubscribed(id).stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val loading = MutableStateFlow<UiState>(UiState.Ready)
    val result = loading.asStateFlow()
    private val submission = MutableStateFlow<UiState?>(null)
    val subscription = submission.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (loading.value == UiState.Loading) return
        viewModelScope.launch {
            loading.value = UiState.Loading
            loading.value = outcome { repository.refreshDetail(id) }
        }
    }

    fun subscribe() {
        if (submission.value == UiState.Loading || subscribed.value) return
        viewModelScope.launch {
            submission.value = UiState.Loading
            submission.value = outcome { subscribeToEvent(id) }
        }
    }
}
