package com.example.app_events.presentation

import com.example.app_events.domain.EventlyFailure
import com.example.app_events.domain.FailureReason
import kotlinx.coroutines.CancellationException

sealed interface UiState {
    data object Loading : UiState
    data object Ready : UiState
    data class Error(val message: String, val offline: Boolean = false) : UiState
}

internal suspend fun outcome(action: suspend () -> Unit): UiState {
    return try {
        action()
        UiState.Ready
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: EventlyFailure) {
        when (failure.reason) {
            FailureReason.Offline -> UiState.Error("Sin conexión. Comprueba tu red y vuelve a intentar.", offline = true)
            FailureReason.Timeout -> UiState.Error("La conexión tardó demasiado. Reintenta en unos segundos.")
            FailureReason.InvalidCredentials -> UiState.Error("El correo o la contraseña son incorrectos.")
            FailureReason.DuplicateEmail -> UiState.Error("Ese correo ya está registrado.")
            FailureReason.InvalidData -> UiState.Error("Revisa los datos ingresados.")
            FailureReason.ExpiredSession -> UiState.Error("Tu sesión venció. Vuelve a iniciar sesión.")
            FailureReason.MissingEvent -> UiState.Error("El evento ya no está disponible.")
            FailureReason.FullEvent -> UiState.Error("Se agotaron los cupos de este evento.")
            FailureReason.ClosedEvent -> UiState.Error("Las inscripciones de este evento están cerradas.")
            FailureReason.Unavailable -> UiState.Error("Evently no está disponible en este momento.")
        }
    } catch (_: Exception) {
        UiState.Error("No se pudo completar la acción. Inténtalo de nuevo.")
    }
}
