# Integraciones y datos

## REST de Evently

La base de las rutas de la app es `/api/`. El backend del curso usa NestJS, MySQL para usuarios, eventos e inscripciones y Redis para revocar refresh tokens.

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `/auth/register` | Crea una cuenta con nombre, correo y contraseña. Devuelve los tokens. |
| POST | `/auth/login` | Valida correo y contraseña. Devuelve los tokens. |
| POST | `/auth/refresh-token` | Rota el par de tokens con el refresh token como Bearer. |
| POST | `/auth/logout` | Revoca el refresh token como Bearer si hay conexión. |
| GET | `/events` | Descarga eventos próximos. |
| GET | `/events/{id}` | Descarga el detalle de un evento, incluida su disponibilidad. |
| POST | `/events/{id}/subscribe` | Usa access token. Confirma o devuelve una inscripción existente. |
| GET | `/me/subscriptions` | Usa access token. Devuelve las inscripciones de la cuenta activa. |

La columna `capacity` se agrega con una migración de TypeORM. Los eventos previos reciben un límite inicial de 100 plazas. La API devuelve `availableSpots` a partir de la capacidad y del número de inscripciones persistidas. El servidor bloquea la fila del evento durante la asignación para que dos solicitudes simultáneas no gasten el mismo cupo.

## Room y uso sin conexión

- `event` guarda el detalle y una marca de pertenencia al catálogo de próximos eventos.
- `subscription` referencia `event` por ID. Los DAO publican `Flow` y las operaciones que afectan las dos tablas usan una transacción.
- El repositorio conserva los últimos datos válidos si falla la descarga. Al confirmar una inscripción se guardan juntos el evento y la relación de suscripción.
- El botón de inscripción requiere autorización y confirmación del servidor. Si la red falla, se puede volver a intentar. La operación es idempotente para la misma cuenta y evento.

## Sesión

El par de tokens se cifra con AES de 256 bits en modo GCM y se guarda en una sola entrada de SharedPreferences. La clave reside en Android Keystore y se deshabilita la copia de seguridad de la app. El cliente de autenticación tiene un OkHttp separado para que el refresh jamás vuelva a activar el autenticador.

En un 401 de una petición de eventos, el autenticador sincroniza la renovación. Reutiliza el token más nuevo si otra solicitud ya lo renovó. Una respuesta de reintento con 401 cierra la sesión local. Un fallo de conectividad durante la renovación se puede reintentar y preserva la caché. Al cerrar sesión, se invalidan las operaciones ligadas a esa cuenta, se borran las dos tablas y se intenta revocar el refresh token en el servidor.
