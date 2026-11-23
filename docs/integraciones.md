# Integraciones y datos

## REST de Evently

La base de las rutas de la app es `/api/`. El backend del curso usa NestJS, MySQL para usuarios, eventos e inscripciones y Redis para revocar refresh tokens.

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `/auth/register` | Crea una cuenta con nombre, correo y contraseña. Devuelve los tokens. |
| POST | `/auth/login` | Valida correo y contraseña. Devuelve los tokens. |
| POST | `/auth/refresh-token` | Rota el par de tokens con el refresh token como Bearer. |
| POST | `/auth/logout` | La API original exige el access token como Bearer. |
| GET | `/events` | Descarga eventos próximos. |
| GET | `/events/{id}` | Descarga el detalle de un evento. |
| POST | `/events/{id}/subscribe` | Usa access token. Devuelve un error si ya existe una inscripción. |
| GET | `/me/subscriptions` | Usa access token. Devuelve las inscripciones de la cuenta activa. |

El contrato original de la API entrega eventos sin `capacity` ni `availableSpots`. El cliente Android requiere `availableSpots` y falla al leer esas respuestas.

## Room y uso sin conexión

- `event` guarda el detalle y una marca de pertenencia al catálogo de próximos eventos.
- `subscription` referencia `event` por ID. Los DAO publican `Flow` y las operaciones que afectan las dos tablas usan una transacción.
- El repositorio conserva los últimos datos válidos si falla la descarga. Al confirmar una inscripción se guardan juntos el evento y la relación de suscripción.
- El botón de inscripción requiere autorización y confirmación del servidor. Si la red falla, se puede volver a intentar.

## Sesión

El par de tokens se cifra con AES de 256 bits en modo GCM y se guarda en una sola entrada de SharedPreferences. La clave reside en Android Keystore y se deshabilita la copia de seguridad de la app. El cliente de autenticación tiene un OkHttp separado para que el refresh jamás vuelva a activar el autenticador.

En un 401 de una petición de eventos, el autenticador sincroniza la renovación. Reutiliza el token más nuevo si otra solicitud ya lo renovó. Una respuesta de reintento con 401 cierra la sesión local. Un fallo de conectividad durante la renovación se puede reintentar y preserva la caché. Al cerrar sesión, se borran las dos tablas y se eliminan los tokens. Las operaciones iniciadas con esa sesión ya no pueden escribir en Room. Si el dispositivo se actualizó desde el prototipo, el cierre confirmado también limpia su base de datos y sus preferencias locales antiguas. La app envía el refresh token a `/auth/logout`, por lo que la API original rechaza la solicitud de revocación.
