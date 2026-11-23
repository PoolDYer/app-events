# Requisitos funcionales de Evently Básico

El actor de la app Android es el asistente. Puede crear una cuenta o usar una existente. Una sesión válida permite consultar eventos e inscribirse.

| Código | Acción | Resultado esperado |
| --- | --- | --- |
| EB-01 | Registrar una cuenta o iniciar sesión. | Se validan el correo y la contraseña de al menos ocho caracteres. Si el servidor rechaza la operación, se muestra una explicación y se puede reintentar. |
| EB-02 | Abrir Evently Mobile. | El arranque comprueba la sesión guardada. Abre el catálogo con sesión o el formulario de acceso si hace falta. |
| EB-03 | Abrir o actualizar Próximos eventos. | Se muestran los eventos almacenados en Room y se intenta actualizar desde la red. |
| EB-04 | Seleccionar un evento por su ID. | El detalle muestra el título, la fecha, el lugar, la descripción y los cupos disponibles. Atrás devuelve al listado de origen. |
| EB-05 | Pulsar «Suscribirme». | La app muestra la carga. Una confirmación del servidor cambia el botón a «Suscrito» y actualiza los cupos. |
| EB-06 | Abrir Mis suscripciones. | La pantalla muestra las inscripciones guardadas y las actualiza desde la cuenta activa. |
| EB-07 | Consultar eventos o inscribirse cuando hay un error. | Hay un mensaje con opción de reintentar. Si hay datos locales, el catálogo, el detalle o las suscripciones siguen disponibles. |
| EB-08 | Confirmar el cierre de sesión. | Se vuelve al login y se limpian los tokens y la caché de Evently en el dispositivo. |
| EB-09 | Ejecutar las pruebas JVM. | Las pruebas con fakes cubren el caso de listado, el caso de inscripción y el ViewModel del catálogo. |

La inscripción requiere conexión y una sesión activa. El cliente habilita el botón cuando el evento está activo y tiene cupos. La API original carece de información de cupos y devuelve HTTP 400 si se repite una inscripción. [Contrato REST](README.md#32-contrato-rest)
