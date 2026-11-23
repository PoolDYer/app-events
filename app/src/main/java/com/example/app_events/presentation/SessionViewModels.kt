package com.example.app_events.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_events.domain.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SessionScreen {
    data object Checking : SessionScreen
    data object SignedOut : SessionScreen
    data object SignedIn : SessionScreen
    data class Error(val message: String) : SessionScreen
}

@HiltViewModel
class SessionViewModel @Inject constructor(private val repository: SessionRepository) : ViewModel() {
    private val startup = MutableStateFlow<UiState>(UiState.Loading)
    val screen = combine(startup, repository.active) { state, active ->
        when (state) {
            UiState.Loading -> SessionScreen.Checking
            UiState.Ready -> if (active) SessionScreen.SignedIn else SessionScreen.SignedOut
            is UiState.Error -> SessionScreen.Error(state.message)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionScreen.Checking)

    private val mutableLogout = MutableStateFlow<UiState>(UiState.Ready)
    val logoutState = mutableLogout.asStateFlow()

    init {
        restore()
    }

    fun restore() {
        viewModelScope.launch {
            startup.value = UiState.Loading
            startup.value = outcome { repository.restore() }
        }
    }

    fun logout() {
        if (mutableLogout.value == UiState.Loading) return
        viewModelScope.launch {
            mutableLogout.value = UiState.Loading
            mutableLogout.value = outcome { repository.logout() }
        }
    }
}

data class AuthForm(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val result: UiState = UiState.Ready
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: SessionRepository) : ViewModel() {
    private val mutableForm = MutableStateFlow(AuthForm())
    val form = mutableForm.asStateFlow()

    fun name(value: String) = mutableForm.update { it.copy(name = value.take(150), result = UiState.Ready) }
    fun email(value: String) = mutableForm.update { it.copy(email = value.take(180), result = UiState.Ready) }
    fun password(value: String) = mutableForm.update { it.copy(password = value.take(128), result = UiState.Ready) }

    fun submit(register: Boolean) {
        val values = mutableForm.value
        if (values.result == UiState.Loading) return
        val email = values.email.trim()
        val name = values.name.trim()
        val validation = when {
            register && name.length < 2 -> "Ingresa tu nombre."
            !EMAIL.matches(email) -> "Ingresa un correo electrónico válido."
            values.password.length < 8 -> "Usa una contraseña de al menos 8 caracteres."
            else -> null
        }
        if (validation != null) {
            mutableForm.update { it.copy(result = UiState.Error(validation)) }
            return
        }
        viewModelScope.launch {
            mutableForm.update { it.copy(result = UiState.Loading) }
            val status = outcome { repository.authenticate(name.takeIf { register }, email, values.password) }
            mutableForm.update { it.copy(password = if (status == UiState.Ready) "" else it.password, result = status) }
        }
    }

    private companion object {
        val EMAIL = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
    }
}
