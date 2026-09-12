# Documentación Técnica — Flujo 7: Ejecución del Entrenamiento en Tiempo Real (Live Workout)

## 1. Visión General

El **Flujo 7** coordina la ejecución interactiva de una sesión de entrenamiento dentro de FitLog. Abarca desde la entrada de datos en la interfaz (peso, repeticiones, RPE), la sincronización con el temporizador de descanso en segundo plano, la prevención de pérdida de progreso mediante borrador local y la persistencia atómica de la sesión en Firestore.

---

## 2. Arquitectura de Componentes

| Capa | Archivo / Componente | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI / Presentación** | `EntrenarScreen.kt` | Renderizado reactivo en Compose Multiplatform. Maneja foco del teclado (`LocalFocusManager`), sanitización de datos numéricos (`SeriePesoInput`), tarjeta de ejercicio (`EjercicioInteractivoCard`), widget flotante del temporizador (`CronometroFlotanteWidget`) y selección de RPE (`SelectorRpeBottomSheet`). |
| **ViewModel / Estado** | `EntrenarViewModel.kt` | Mantiene el estado reactivo (`StateFlow`). Ejecuta el temporizador de precisión basado en tiempo objetivo absoluto (`targetEndTimeMs`), gestiona la limpieza en `onCleared()` y coordina la persistencia local. |
| **Capa de Datos** | `AtletaProgresoRepository.kt` | Ejecuta la transacción atómica en Firestore (`registrarSesionYActualizarCiclo`). Calcula la diferencia de repeticiones efectivas (`deltaRepsLogradas`), verifica expiración de ciclos y actualiza marcas de tiempo de ejecución de la rutina. |
| **Capa de Datos** | `UserRepository.kt` | Invocado por `EntrenarViewModel` para validar el estado de la suscripción (`evaluarYActualizarEstadoSuscripcion`) y actualizar marcas de notificaciones del usuario. |
| **Resiliencia Local** | `BorradorLocalManager` | Almacena temporalmente el progreso de la sesión activa en el almacenamiento local del dispositivo. Protege el progreso frente a cierres inesperados o interrupciones por llamadas. |
| **Audio** | `ReproductorAudio.kt` | Emite las alertas sonoras al finalizar los intervalos de descanso. |
| **Android Native** | `TimerReceiver.kt` | `BroadcastReceiver` que recibe el Intent de `AlarmManager` cuando finaliza el descanso, disparando la notificación emergente (`Heads-Up`) y sonido en segundo plano. |
| **Android Native** | `MainActivity.kt` | Punto de entrada en Android. Configura permisos en tiempo de ejecución (`POST_NOTIFICATIONS`), crea el canal de notificaciones (`descanso_timer_channel_v2`) e inicializa context-aware managers. |

---

## 3. Flujo de Datos y Secuencia Operativa

```text
[EntrenarScreen] ──(Inputs/Reps)──> [EntrenarViewModel] ──(Guardado previo)──> [BorradorLocalManager]
       │                                     │
       │ (Completa serie)                    │ (Inicio descanso)
       ▼                                     ▼
[TimerReceiver] <──(AlarmManager)─── [ targetEndTimeMs ]
       │
       ├─► Notificación (descanso_timer_channel_v2)
       └─► ReproductorAudio.reproducirSonidoFinTiempo()
                                             │
                                     (Finalizar Sesión)
                                             ▼
                               [AtletaProgresoRepository]
                                             │
                                   (runTransaction Firestore)
                                             ├─► Registra Historial
                                             ├─► Actualiza Ciclo/Reps
                                             └─► Actualiza Rutina
                                             │
                                             ▼
                               [Limpiar Borrador Local]