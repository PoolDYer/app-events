package com.example.app_events.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_events.domain.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun AuthScreen(register: Boolean, viewModel: AuthViewModel, onSwitch: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val busy = form.result == UiState.Loading
    ContentColumn(Modifier.imePadding()) {
        Title(if (register) "Crea tu cuenta" else "Inicia sesión")
        if (register) {
            OutlinedTextField(
                form.name, viewModel::name, Modifier.fillMaxWidth(),
                label = { Text("Nombre") }, enabled = !busy, singleLine = true
            )
        }
        OutlinedTextField(
            form.email, viewModel::email, Modifier.fillMaxWidth(),
            label = { Text("Correo electrónico") }, enabled = !busy, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            form.password, viewModel::password, Modifier.fillMaxWidth(),
            label = { Text("Contraseña") }, enabled = !busy, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        when (val result = form.result) {
            UiState.Loading -> Loading(if (register) "Creando cuenta" else "Iniciando sesión")
            is UiState.Error -> ErrorBox(result, retry = { viewModel.submit(register) })
            UiState.Ready -> Unit
        }
        Button(
            onClick = { viewModel.submit(register) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !busy
        ) { Text(if (register) "Crear cuenta" else "Entrar") }
        TextButton(onClick = onSwitch, enabled = !busy) {
            Text(if (register) "Volver al inicio de sesión" else "Crear una cuenta")
        }
    }
}

@Composable
internal fun EventListScreen(
    events: List<Event>,
    state: UiState,
    refresh: () -> Unit,
    openEvent: (String) -> Unit,
    openSubscriptions: (() -> Unit)? = null
) {
    val isCatalog = openSubscriptions != null
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Title(if (isCatalog) "Próximos eventos" else "Mis suscripciones")
                if (openSubscriptions != null) {
                    OutlinedButton(onClick = openSubscriptions, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Mis suscripciones")
                    }
                }
            }
            item {
                when (state) {
                    UiState.Loading -> Loading(if (events.isEmpty()) "Cargando eventos" else "Actualizando")
                    is UiState.Error -> ErrorBox(state, refresh, hasCache = events.isNotEmpty())
                    UiState.Ready -> {
                        if (events.isEmpty()) {
                            Text(if (isCatalog) "Aún no hay eventos próximos." else "Tus inscripciones aparecerán aquí.")
                        }
                        TextButton(onClick = refresh) { Text("Actualizar") }
                    }
                }
            }
            items(events, key = Event::id) { event ->
                Surface(
                    onClick = { openEvent(event.id) },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(formatDate(event.date), style = MaterialTheme.typography.bodyMedium)
                        Text(event.location, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EventDetailScreen(viewModel: EventDetailViewModel) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val subscribed by viewModel.subscribed.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val details = event
    ContentColumn {
        when (val state = result) {
            UiState.Loading -> Loading("Cargando detalle")
            is UiState.Error -> ErrorBox(state, viewModel::refresh, hasCache = details != null)
            UiState.Ready -> Unit
        }
        if (details != null) {
            Title(details.title)
            DetailField("Fecha", formatDate(details.date))
            DetailField("Lugar", details.location)
            DetailField("Descripción", details.description.ifBlank { "Sin descripción." })
            DetailField("Cupos disponibles", details.availableSpots.toString())

            when (val status = subscription) {
                UiState.Loading -> Loading("Guardando inscripción")
                is UiState.Error -> ErrorBox(status, viewModel::subscribe)
                UiState.Ready -> Text(
                    "Inscripción confirmada.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                null -> Unit
            }
            val closed = details.status != "ACTIVE" || details.availableSpots <= 0
            if (closed && !subscribed) {
                Text(if (details.status == "CANCELLED") "Evento cancelado." else "No hay cupos disponibles.")
            }
            Button(
                onClick = viewModel::subscribe,
                enabled = !subscribed && !closed && result != UiState.Loading && subscription != UiState.Loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (subscribed) "Suscrito" else "Suscribirme") }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ContentColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
}

@Composable
internal fun Loading(label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(label)
    }
}

@Composable
internal fun ErrorBox(error: UiState.Error, retry: () -> Unit, hasCache: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(error.message)
            if (hasCache) Text("Mostrando lo guardado en este dispositivo.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = retry) { Text("Reintentar") }
        }
    }
}

private fun formatDate(iso: String): String =
    runCatching { DATE_FORMAT.format(Instant.parse(iso).atZone(ZoneId.systemDefault())) }.getOrDefault(iso)

private val DATE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy · HH:mm", Locale.forLanguageTag("es-PE"))
