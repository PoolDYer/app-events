package com.example.app_events.presentation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val LOGIN = "login"
private const val REGISTER = "register"
private const val EVENTS = "events"
private const val SUBSCRIPTIONS = "subscriptions"
private const val DETAIL = "events/{id}"

@Composable
fun EventlyApp(session: SessionViewModel = hiltViewModel()) {
    val state by session.screen.collectAsStateWithLifecycle()
    when (val screen = state) {
        SessionScreen.Checking -> Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Loading("Preparando Evently")
            }
        }
        is SessionScreen.Error -> Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ErrorBox(UiState.Error(screen.message), session::restore)
            }
        }
        SessionScreen.SignedIn, SessionScreen.SignedOut -> {
            val signedIn = state == SessionScreen.SignedIn
            key(signedIn) { EventlyNavigation(signedIn, session) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventlyNavigation(signedIn: Boolean, session: SessionViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val logout by session.logoutState.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Evently Mobile") },
            navigationIcon = {
                if (route in listOf(REGISTER, DETAIL, SUBSCRIPTIONS)) {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            },
            actions = {
                if (signedIn) {
                    TextButton(
                        onClick = { confirmLogout = true },
                        enabled = logout != UiState.Loading
                    ) { Text("Cerrar sesión") }
                }
            }
        )
    }) { padding ->
        NavHost(nav, startDestination = if (signedIn) EVENTS else LOGIN, modifier = Modifier.padding(padding)) {
            if (signedIn) {
                val openEvent: (String) -> Unit = { id -> nav.navigate("events/" + Uri.encode(id)) { launchSingleTop = true } }
                composable(EVENTS) {
                    val viewModel = hiltViewModel<EventsViewModel>()
                    val events by viewModel.events.collectAsStateWithLifecycle()
                    val state by viewModel.result.collectAsStateWithLifecycle()
                    EventListScreen(events, state, viewModel::refresh, openEvent) {
                        nav.navigate(SUBSCRIPTIONS) { launchSingleTop = true }
                    }
                }
                composable(SUBSCRIPTIONS) {
                    val viewModel = hiltViewModel<SubscriptionsViewModel>()
                    val events by viewModel.events.collectAsStateWithLifecycle()
                    val state by viewModel.result.collectAsStateWithLifecycle()
                    EventListScreen(events, state, viewModel::refresh, openEvent)
                }
                composable(DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    EventDetailScreen(hiltViewModel())
                }
            } else {
                composable(LOGIN) { AuthScreen(register = false, viewModel = hiltViewModel()) { nav.navigate(REGISTER) } }
                composable(REGISTER) { AuthScreen(register = true, viewModel = hiltViewModel()) { nav.navigateUp() } }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se borrarán los eventos, las suscripciones y los datos guardados por versiones anteriores de Evently en este dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    session.logout()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } }
        )
    }
    if (signedIn && logout is UiState.Error) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cierre de sesión") },
            text = { Text((logout as UiState.Error).message) },
            confirmButton = { TextButton(onClick = session::logout) { Text("Reintentar") } }
        )
    }
}
