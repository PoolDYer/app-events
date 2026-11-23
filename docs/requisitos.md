# Requisitos funcionales de Evently Básico

El actor de la app Android es el asistente. Puede crear una cuenta o usar una existente. La tabla describe el estado de cada requisito con el backend incluido.

| Código | Acción | Estado actual |
| --- | --- | --- |
| EB-01 | Registrar una cuenta o iniciar sesión. | Se validan el correo y la contraseña de al menos ocho caracteres. Si el servidor rechaza la operación, se muestra una explicación y se puede reintentar. |
| EB-02 | Abrir Evently Mobile. | El arranque comprueba la sesión guardada. Abre el catálogo con sesión o el formulario de acceso si hace falta. |
| EB-03 | Abrir o actualizar Próximos eventos. | Room emite los eventos guardados. La descarga de eventos falla al faltar `availableSpots` en la respuesta. |
| EB-04 | Seleccionar un evento por su ID. | Android tiene una ruta y una pantalla de detalle. La respuesta actual de la API carece del campo de cupos que el cliente exige. |
| EB-05 | Pulsar «Suscribirme». | El botón y el envío REST existen en Android. El flujo desde un catálogo nuevo queda bloqueado por el contrato del evento. |
| EB-06 | Abrir Mis suscripciones. | Android consulta Room y llama a `/me/subscriptions`. Si la API devuelve una inscripción con un evento, falta el campo de cupos que el cliente necesita para guardarla. |
| EB-07 | Consultar eventos o inscribirse cuando hay un error. | Hay un mensaje con opción de reintentar. Si hay datos locales, el catálogo, el detalle o las suscripciones siguen disponibles. |
| EB-08 | Confirmar el cierre de sesión. | Se vuelve al login y se limpian los tokens y la caché de Evently en el dispositivo. |
| EB-09 | Ejecutar las pruebas JVM. | Las pruebas con fakes cubren el caso de listado, el caso de inscripción y el ViewModel del catálogo. |

La inscripción requiere conexión y una sesión activa. El cliente habilita el botón cuando el evento está activo y tiene cupos. La API original carece de información de cupos y devuelve HTTP 400 si se repite una inscripción. [Contrato REST](README.md#32-contrato-rest)

## Requisitos comunes

| Códigos | Implementación y alcance de la comprobación |
| --- | --- |
| C-01 a C-03 | Estados de carga y error, mensajes en español, reintento y lectura de Room cuando hay datos previos. |
| C-04 y C-05 | Navigation Compose mantiene la ruta y los ViewModels se asocian a la entrada de navegación. El bloqueo de EB-04 afecta la verificación del recorrido por detalle con la API incluida. |
| S-01 y S-02 | Tokens cifrados con AES-GCM y clave en Android Keystore. OkHttp añade el access token y permite un reintento al renovar. |
| S-03 a S-05 | El cierre borra sesión y datos locales. Las claves del entorno se generan localmente. El cliente usa el permiso de Internet. |
| Q-01 | Tres pruebas JVM con fakes para listado, inscripción y recuperación del ViewModel tras un error. |
| Q-02 a Q-04 | Guías y commits en el repositorio. El comando de Gradle genera el APK de depuración. |

[Requisitos comunes del curso](curso/alcance_proyectos_android.md#3-requisitos-comunes-todos-los-proyectos) · [Pruebas y ejecución de Android](README.md#6-ejecutar-probar-y-mantener)
