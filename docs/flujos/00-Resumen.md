# Arquitectura y Documentación Completa de Flujos — FitLog (KMP)

Este documento centraliza la arquitectura técnica, el mapa de componentes y la trazabilidad de llamadas (**quién llama a quién**) para los 10 flujos principales de la plataforma **FitLog**, estructurada bajo **Kotlin Multiplatform (KMP)**, **Compose Multiplatform**, **MVVM / Clean Architecture** y **Firebase (Auth & Firestore)**.

---

## Diagrama General de Capas KMP

```text
  ┌─────────────────────────────────────────────────────────────────────────┐
  │                         CAPA DE PRESENTACIÓN (UI)                       │
  │    Jetpack Compose Multiplatform (Screens, Components, BottomSheets)    │
  └────────────────────────────────────┬────────────────────────────────────┘
                                       │ Observa StateFlow / Invocaciones
                                       ▼
  ┌─────────────────────────────────────────────────────────────────────────┐
  │                         CAPA DE NEGOCIO (VIEWMODELS)                    │
  │  StateFlow, Coroutines, supervisorScope, Algoritmos de Métricas/Cálculos  │
  └────────────────────────────────────┬────────────────────────────────────┘
                                       │ Invocación de métodos suspend
                                       ▼
  ┌─────────────────────────────────────────────────────────────────────────┐
  │                           CAPA DE DATOS (REPOSITORIES)                  │
  │     WriteBatches, Transactions, Firestore Queries, Storage Local        │
  └───────────────────┬─────────────────────────────────┬───────────────────┘
                      │                                 │
                      ▼                                 ▼
   ┌─────────────────────────────────────┐   ┌─────────────────────────────┐
   │         FIREBASE BACKEND            │   │   PLATFORM EXPECT/ACTUAL    │
   │  Firebase Auth / Cloud Firestore    │   │ (AndroidPlatform / iOS)     │
   └─────────────────────────────────────┘   └─────────────────────────────┘
```

---

## Flujo 1: Autenticación, Login y Primer Acceso

### 1. Propósito
Gestionar la autenticación de usuarios, la validación de perfiles en Firestore, la persistencia local del último correo ingresado y el flujo obligatorio de cambio de clave temporal para atletas en su primer acceso.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `LoginScreen.kt` | Captura de credenciales, gestión del ciclo de vida de sesión y desmontado de estado. |
| **UI Screen** | `CambiarContrasenaScreen.kt` | Pantalla modal/obligatoria para actualización de contraseña de atletas. |
| **ViewModel** | `AuthViewModel.kt` | Orquestador de estados UI (`AuthState`, `ActivationState`), login y cambio de clave. |
| **Repository** | `AuthRepository.kt` | Acceso directo y exclusivo al SDK de Firebase Auth. |
| **Repository** | `UserRepository.kt` | Consulta y actualización de datos de usuario en la colección Firestore `users`. |
| **Local Storage** | `UserPreferencesManager` | Persistencia local multi-plataforma para almacenar el último correo ingresado. |

### 3. Cadena de Llamadas (Call Chain)

```text
[LoginScreen]
     │
     ▼ (Pulsar "Iniciar Sesión")
[AuthViewModel.login(email, clave)]
     │
     ├─► [AuthRepository.login(email, clave)] ──► Firebase Auth SDK
     │         └─► Retorna UID de autenticación
     │
     ├─► [UserRepository.obtenerUsuario(uid)] ──► Firestore collection("users")
     │         │
     │         ├── Si usuario == null ──► [AuthRepository.logout()] (Previene sesión huérfana)
     │         │                             └─► Emite AuthState.Error
     │         │
     │         └── Si usuario != null ──► [UserPreferencesManager.guardarUltimoCorreo(email)]
     │                                       └─► Emite AuthState.Success(uid, rol, requiereCambioContrasena)
     ▼
[LoginScreen] reacciona a AuthState.Success
     ├── Si requiereCambioContrasena == true ──► Navega a [CambiarContrasenaScreen]
     └── Si requiereCambioContrasena == false ──► Navega al Dashboard según Rol
```

```text
[CambiarContrasenaScreen]
     │
     ▼ (Pulsar "Guardar Nueva Contraseña")
[AuthViewModel.actualizarContrasenaPrimeraVez(uid, nuevaContrasena)]
     │
     ├─► [AuthRepository.cambiarContrasenaPrimeraVez(nuevaContrasena)]
     │         └─► FirebaseUser.updatePassword(...)
     │
     └─► [UserRepository.actualizarPerfilUsuario(uid, mapOf("requiereCambioContrasena" to false))]
               └─► Firestore users.document(uid).update(...)
```

---

## Flujo 2: Registro y Alta Completa de Atletas (Onboarding Coach)

### 1. Propósito
Permitir a los entrenadores registrar atletas mediante un wizard multietapa (datos personales, valoración inicial, hábitos y plan de suscripción), creando la cuenta en Firebase Auth mediante una instancia secundaria nativa sin cerrar la sesión del entrenador.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `AddAtletaScreen.kt` | Formulario multietapa (wizard 4 pasos) con validaciones progresivas. |
| **ViewModel** | `AddAtletaViewModel.kt` | Mantiene `AddAtletaState`, valida longitud de documentos, precios y resetea errores reactivos. |
| **Repository** | `AtletaRepository.kt` | Ejecuta la creación atómica del atleta mediante Firestore `WriteBatch`. |
| **Repository** | `UserRepository.kt` | Consultas de pre-validación de unicidad de correo y documento filtrado por rol. |
| **Platform** | `Platform.kt` (`AndroidPlatform` / `IOSPlatform`) | Puentes nativos (`crearCuentaEnInstanciaSecundaria`) para registrar usuarios en Auth secundario. |

### 3. Cadena de Llamadas (Call Chain)

```text
[AddAtletaScreen] ──(Paso 1: Datos Personales)──► [AddAtletaViewModel.onEvent(NextStep)]
     │
     ▼
[AddAtletaViewModel.validarPaso1YContinuar()]
     ├─► [UserRepository.existeCorreo(correo)]
     └─► [UserRepository.existeDocumento(documento, RolUsuario.ATLETA)]
               └─► Si es válido ──► Avanza a Pasos 2, 3 y 4

──────────────────────────────────────────────────────────────────────────────────

[AddAtletaScreen] ──(Paso 4: Pulsar "Guardar Atleta")──► [AddAtletaViewModel.onEvent(SaveAtleta)]
     │
     ▼
[AddAtletaViewModel.guardarAtleta()]
     │
     ├─► Computa fechaInicio, fechaFin y estadoSuscripcion (ACTIVO / DIFERIDO)
     │
     ▼
[AtletaRepository.crearAtletaCompleto(usuario, valoracion, habitos, contrasena, primerPeriodo)]
     │
     ├─► [Platform.crearCuentaEnInstanciaSecundaria(correo, contrasena)] ──► Retorna authUid
     │
     └─► [Firebase Firestore WriteBatch]
               ├── set(usersCollection.document(authUid), usuario)
               ├── set(users.document(authUid).collection("valoraciones"), valoracion)
               ├── set(users.document(authUid).collection("habitos"), habitos)
               ├── set(users.document(authUid).collection("periodos_facturables"), primerPeriodo)
               └── set(db.collection("historial_facturacion_general"), reciboContable)
```

---

## Flujo 3: Expediente del Atleta, Semaforización y Salud del Equipo

### 1. Propósito
Monitorear el rendimiento global del equipo del entrenador mediante un algoritmo de semáforo de salud (Adherencia, Volumen, RPE/Fatiga) y la consulta detallada del expediente individual del atleta.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `EntrenadorDashboardScreen.kt` | Tablero tri-pestaña ("Mis Atletas", "Semáforo", "Asistencia Hoy"). |
| **UI Screen** | `AtletaDetailScreen.kt` | Expediente del atleta con KPIs de adherencia, fatiga RPE y notas del coach. |
| **ViewModel** | `EntrenadorDashboardViewModel.kt` | Carga el resumen gerencial y lista de evaluación del semáforo con caché en memoria. |
| **ViewModel** | `AtletaDetailViewModel.kt` | Computa KPIs del expediente acotados al ciclo activo para minimizar lecturas. |
| **Repository** | `SemaforoRepository.kt` | Evaluación multihilo paralela (`coroutineScope` + `async`) de la salud de todos los atletas. |
| **Utils** | `SemaforoCalculador.kt` | Motor de reglas puras para evaluar adherencia pro-rata, volumen efectivo y fatiga RPE. |

### 3. Cadena de Llamadas (Call Chain)

```text
[EntrenadorDashboardScreen]
     │
     ▼
[EntrenadorDashboardViewModel.cargarDashboard(entrenadorId)]
     │
     ▼
[SemaforoRepository.obtenerEvaluacionAtletas(entrenadorId)]
     │
     ├─► [UserRepository.obtenerAtletasPorEntrenador(entrenadorId)]
     │
     └─► For-each atleta (en paralelo con async/awaitAll):
               ├─► [AtletaRepository.obtenerRutinasActivas(atletaId)]
               ├─► [AtletaProgresoRepository.obtenerCicloActivo(atletaId)]
               ├─► [AtletaProgresoRepository.obtenerEntrenamientosCicloActivo(...)]
               │
               ▼
         [SemaforoCalculador.evaluarAdherenciaProRata()]
         [SemaforoCalculador.evaluarVolumenEfectivo()]
         [SemaforoCalculador.evaluarFatigaRpe()]
               │
               ▼
         Retorna `List<AtletaSemaforoItem>` al StateFlow
```

```text
[AtletaDetailScreen]
     │
     ▼
[AtletaDetailViewModel.cargarExpedienteAtleta(atletaId)]
     │
     └─► (supervisorScope concurrente):
               ├── async ──► [AtletaRepository.obtenerUsuario(atletaId)]
               ├── async ──► [AtletaProgresoRepository.obtenerCicloActivo(atletaId)]
               ├── async ──► [AtletaRepository.obtenerRutinasActivas(atletaId)]
               └── async ──► [AtletaProgresoRepository.obtenerEntrenamientosCicloActivo(...)]
     │
     ▼
[SemaforoCalculador] extrae adherencia, volumen, RPE y notas recientes
     │
     ▼
Actualiza `AtletaDetailState` ──► Renderiza KPIs en [AtletaDetailScreen]
```

---

## Flujo 4: Vinculación, Suscripciones y Periodos Facturables

### 1. Propósito
Administrar la vinculación atleta-entrenador mediante códigos temporales (15 minutos), controlar la cola de suscripciones (Activas, Diferidas, Suspendidas, Vencidas) y ejecutar promociones atómicas de planes.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `PantallaHuerfano` (en `AtletaMainScreen.kt`) | Vista de ingreso de credenciales de vinculación para atletas huérfanos. |
| **UI Dialog** | `AsignarPlanDialog.kt` | Modal unificado para renovación y asignación de suscripciones con normalización de timezone. |
| **UI Screen** | `FacturacionScreen.kt`<br>`HistorialFacturacionScreen.kt` | Consola comercial del entrenador e historial de periodos del atleta. |
| **ViewModel** | `FacturacionViewModel.kt`<br>`HistorialFacturacionViewModel.kt` | Control de estados comerciales, prevención de solapamientos y cálculo de fechas fin. |
| **Repository** | `UserRepository.kt` | Operaciones en Firestore para vincular, pausar, reactivar y autorreparar suscripciones. |

### 3. Cadena de Llamadas (Call Chain)

```text
[PantallaHuerfano] ──(Ingresar Correo y Código)──► [UserRepository.vincularConEntrenador(...)]
     │
     ▼
[UserRepository] consulta Firestore collection("users") filtrando por correo y codigoVinculacion
     ├── Si el código expiró ──► Retorna false
     └── Si es válido ──► Firestore WriteBatch:
               ├── update(atletaRef, "entrenadorId" to entrenadorDocId, "estadoSuscripcion" to "VENCIDO")
               └── update(periodosVigentes, "estado" to "CANCELADO")
```

```text
[AsignarPlanDialog] ──(Guardar Plan)──► [FacturacionViewModel.renovarAtleta(...)]
     │
     ├─► [UserRepository.obtenerUltimaFechaFinCadena(atletaId)]
     ├─► [UserRepository.existeSolapamientoPeriodo(atletaId, fechaInicio, fechaFin)]
     │
     ▼ (Si no hay solapamiento)
[UserRepository.renovarSuscripcion(...)]
     │
     └─► Firestore WriteBatch:
               ├── update(userRef, "estadoSuscripcion", "planActivo", "vencimientoSuscripcion")
               ├── set(userRef.collection("periodos_facturables").document(id), periodo)
               └── set(db.collection("historial_facturacion_general").document(id), reciboContable)
```

---

## Flujo 5: Biblioteca de Ejercicios y Plantillas de Rutinas

### 1. Propósito
Administrar el catálogo maestro de ejercicios (globales y personalizados por el entrenador) y la creación de plantillas de rutinas reutilizables con prescripción de series y repeticiones.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `BibliotecaScreen.kt` | Pestañas de Ejercicios y Plantillas con búsqueda y filtro por `GrupoMuscular`. |
| **UI Screen** | `AddEjercicioScreen.kt`<br>`AddPlantillaScreen.kt` | Formularios de creación y edición de ejercicios y plantillas base. |
| **ViewModel** | `BibliotecaViewModel.kt`<br>`AddEjercicioViewModel.kt`<br>`AddPlantillaViewModel.kt` | Filtrado reactivo en memoria y control de formularios. |
| **Repository** | `ExerciseRepository.kt` | Operaciones sobre `biblioteca_global`, `ejercicios_personalizados` y `plantillas_rutinas`. |
| **Modelos** | `Ejercicio.kt`<br>`PlantillaRutina.kt`<br>`ElementoRutina.kt` | Modelos de dominio serializables para ejercicios y plantillas. |

### 3. Cadena de Llamadas (Call Chain)

```text
[AddPlantillaScreen] ──(Pulsar "Guardar Plantilla")──► [AddPlantillaViewModel.guardarPlantilla(entrenadorId)]
     │
     ▼
[ExerciseRepository.guardarPlantillaRutina(plantilla)]
     │
     ├── Si es plantilla nueva ──► Firestore collection("plantillas_rutinas").add(plantilla)
     └── Si es edición ───────► Firestore collection("plantillas_rutinas").document(id).set(plantilla)
     │
     ▼
[AddPlantillaViewModel] emite `isGuardado = true` ──► Navega de vuelta a [BibliotecaScreen]
```

---

## Flujo 6: Asignación y Edición del Programa de Entrenamiento

### 1. Propósito
Prescribir y reconfigurar la rutina asignada activa del atleta (`RutinaAsignada`), reordenar días y ejercicios, prescribir tipos de serie (`EFECTIVA`, `DROP_SET`, `FALLO`) y sincronizar el `CicloEntrenamiento`.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `SeleccionarPlantillaScreen.kt` | Constructor de rutina activa combinando plantillas de la biblioteca. |
| **UI Screen** | `EditRutinaAsignadaScreen.kt` | Editor interactivo de días, secuencias, prescripción de series y descanso. |
| **ViewModel** | `AsignarRutinaViewModel.kt`<br>`EditRutinaAsignadaViewModel.kt` | Lógica de estructuración de programas, reordenamiento de días y cálculo de duraciones. |
| **Repository** | `AtletaRepository.kt` | Persistencia en la subcolección `rutinas_asignadas` del atleta. |
| **Repository** | `AtletaProgresoRepository.kt` | Sincronización automática de metas del ciclo activo con la nueva rutina. |

### 3. Cadena de Llamadas (Call Chain)

```text
[SeleccionarPlantillaScreen] ──(Asignar Rutina)──► [AsignarRutinaViewModel.construirYAsignarRutina(atletaId)]
     │
     ├─► [AtletaRepository.obtenerRutinasActivas(atletaId)] (Valida que no haya otra activa)
     │
     ▼
[AtletaRepository.asignarRutina(atletaId, nuevaRutina)]
     │
     ├─► Firestore users.document(atletaId).collection("rutinas_asignadas").add(nuevaRutina)
     │
     ▼
[AtletaProgresoRepository.forzarCierreCicloActivo(atletaId)]
     │
     └─► Marca el ciclo previo como `estaActivo = false` e inicia un nuevo `CicloEntrenamiento`
```

---

## Flujo 7: Ejecución del Entrenamiento en Tiempo Real (Live Workout)

### 1. Propósito
Coordinar la ejecución interactiva de una sesión de entrenamiento, registrando repeticiones, peso y RPE, sincronizando un cronómetro de descanso con notificaciones del sistema y garantizando resiliencia ante cierres mediante borrador local.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `EntrenarScreen.kt` | Cuaderno de entrenamiento interactivo con inputs sanitizados y modales RPE. |
| **ViewModel** | `EntrenarViewModel.kt` | Orquestador de la sesión, temporizador basado en tiempo real (`targetEndTimeMs`) y borrador local. |
| **Repository** | `AtletaProgresoRepository.kt` | Ejecución de la transacción atómica en Firestore (`registrarSesionYActualizarCiclo`). |
| **Local Storage** | `BorradorLocalManager` | Guardado automático del estado de la sesión en almacenamiento del dispositivo. |
| **Android / Native**| `TimerReceiver.kt`<br>`ReproductorAudio.kt` | AlarmManager, canal de notificaciones y alertas sonoras de fin de descanso. |

### 3. Cadena de Llamadas (Call Chain)

```text
[EntrenarScreen] ──(Ingresar Peso/Reps/RPE)──► [EntrenarViewModel.actualizarSerie(...)]
     │                                                    │
     │                                                    ▼
     │                                      [BorradorLocalManager.guardarBorradorLocal(sesion)]
     │
     ├──(Completar Serie)──► [EntrenarViewModel.iniciarCronometro(segundos)]
     │                             │
     │                             ├─► [Platform.programarNotificacionTimer(segundos)]
     │                             └─► Bucle de polling que calcula diferencia con `targetEndTimeMs`
     │                                       └─► Al llegar a 0 ──► [ReproductorAudio.reproducirSonidoFinTiempo()]
     │
     ▼ (Pulsar "Terminar Entrenamiento")
[EntrenarViewModel.terminarEntrenamiento(authUid)]
     │
     ├─► [UserRepository.evaluarYActualizarEstadoSuscripcion(usuario)] (Guarda de seguridad)
     │
     ▼
[AtletaProgresoRepository.registrarSesionYActualizarCiclo(atletaId, sesionFinal, ...)]
     │
     └─► [Firebase Firestore runTransaction]:
               ├── Guarda documento en subcolección "historial_entrenamientos"
               ├── Incrementa `sesionesCompletadas` y `repeticionesLogradasTotal` en "ciclos_entrenamiento"
               └── Actualiza `ultimaVezEjecutada` en la rutina asignada
     │
     ▼
[BorradorLocalManager.eliminarBorradorLocal()] ──► Emite `isFinished = true`
```

---

## Flujo 8: Dashboard del Atleta, Control Físico y Estilo de Vida

### 1. Propósito
Proporcionar al atleta su pantalla de inicio diaria, con recomendación automática del día de entrenamiento a ejecutar, control rápido de peso corporal y acceso a historiales antropométricos y de hábitos.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `AtletaInicioScreen.kt`<br>`HistorialValoracionScreen.kt`<br>`HistorialHabitosScreen.kt` | Dashboard principal del atleta e historiales de valoraciones físicas y hábitos. |
| **ViewModel** | `AtletaInicioViewModel.kt`<br>`HistorialValoracionViewModel.kt`<br>`HistorialHabitosViewModel.kt` | Carga paralela de rutinas, pesajes y ciclos con validación de estado de suscripción. |
| **Repository** | `AtletaRepository.kt`<br>`AtletaProgresoRepository.kt` | Consultas de valoraciones, hábitos, pesajes y cierre automático de ciclos expirados. |

### 3. Cadena de Llamadas (Call Chain)

```text
[AtletaInicioScreen]
     │
     ▼
[AtletaInicioViewModel.cargarDashboard(authUid)]
     │
     ├─► [UserRepository.evaluarYActualizarEstadoSuscripcion(usuario)]
     │
     └─► (supervisorScope concurrente):
               ├── async ──► [AtletaRepository.obtenerRutinasActivas(atletaId)]
               ├── async ──► [AtletaProgresoRepository.obtenerUltimosPesajes(atletaId, limite = 4)]
               └── async ──► [AtletaProgresoRepository.obtenerCicloActivo(atletaId)]
                                 └─► Si `ahora > fechaCierre` ──► Cierra ciclo expirado automáticamente
     │
     ▼
Actualiza `AtletaInicioState` ──► Renderiza tarjeta de Rutina Sugerida y Peso Actual
```

```text
[AtletaInicioScreen] ──(Registrar Nuevo Peso)──► [AtletaInicioViewModel.registrarPeso(pesoKg, notas)]
     │
     ├─► Valida `usuario.estadoSuscripcion == ACTIVO`
     │
     └─► [AtletaProgresoRepository.registrarPesaje(atletaId, nuevoPesaje)]
               └─► Firestore users.document(atletaId).collection("pesajes").add(nuevoPesaje)
```

---

## Flujo 9: Análisis de Progreso, Récords y Diario Histórico

### 1. Propósito
Ofrecer analítica avanzada del atleta: evolución de carga por ejercicio con $1\text{RM}$ estimado mediante la fórmula de Epley ($1\text{RM} = \text{Peso} \times \left(1 + \frac{\text{Repeticiones}}{30}\right)$), racha semanal, tonelaje total, récords personales (PRs) e impacto físico por ciclo pasados y activos.

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `ProgresoAtletaScreen.kt` | Vista analítica tri-pestaña ("Evolución", "Diario de Ciclos", "Récords"). |
| **UI Componentes** | `GraficaProgresoEjercicio.kt`<br>`TarjetaEncabezadoCiclo.kt`<br>`DetalleSesionDialog.kt` | Renderizado de curva en Compose `Canvas`, tarjetas de impacto y modales de detalle. |
| **ViewModel** | `ProgresoAtletaViewModel.kt` | Mapeo de modelos UI (`ResumenCicloUI`, `ImpactoFisicoUI`), cálculo de PRs y racha. |
| **Repository** | `AtletaProgresoRepository.kt`<br>`AtletaRepository.kt` | Carga de entrenamientos, ciclos, pesajes y valoraciones. |
| **Platform** | `AndroidPlatform.kt`<br>`IOSPlatform.kt` | Helpers timezone-safe (`esMismoDia`, `obtenerLetraDiaSemana`, `formatearFechaHistorial`). |

### 3. Cadena de Llamadas (Call Chain)

```text
[ProgresoAtletaScreen]
     │
     ▼
[ProgresoAtletaViewModel.cargarDatosProgreso(authUid)]
     │
     └─► (supervisorScope concurrente):
               ├── async ──► [AtletaProgresoRepository.obtenerHistorialEntrenamientos(atletaId)]
               ├── async ──► [AtletaProgresoRepository.obtenerHistorialCiclos(atletaId)]
               ├── async ──► [AtletaProgresoRepository.obtenerUltimosPesajes(atletaId, limite = 50)]
               └── async ──► [AtletaRepository.obtenerHistorialValoraciones(atletaId)]
     │
     ▼
[ProgresoAtletaViewModel.calcularKPIsYEjercicios()]
     ├─► Recorre 7 días: usa [Platform.esMismoDia()] y [Platform.obtenerLetraDiaSemana()] (Racha)
     ├─► Procesa Récords Personales (PRs): Peso Máximo × Repeticiones Logradas
     │
     ▼
[ProgresoAtletaViewModel.seleccionarCiclo(cicloSeleccionado)]
     ├─► Calcula 1RM Estimado: 1RM = Peso * (1 + Reps / 30)
     ├─► Calcula Tonelaje Total: Sumatoria (Peso * Reps Logradas)
     ├─► Calcula Deltas Físicos: ΔPeso (PesoFinal - PesoInicial) y ΔCintura
     │
     ▼
Actualiza `ProgresoAtletaState` ──► Renderiza Canvas y Tablas en [ProgresoAtletaScreen]
```

---

## Flujo 10: Perfil y Edición de Datos Personales

### 1. Propósito
Permitir la consulta y edición de los datos de identificación del usuario y su ficha fisiológica (fecha de nacimiento, tipo de sangre, nacionalidad), adaptando dinámicamente los campos según el rol (`RolUsuario.ATLETA` o `RolUsuario.ENTRENADOR`).

### 2. Mapa de Componentes e Interacciones

| Capa | Archivo | Función / Responsabilidad |
| :--- | :--- | :--- |
| **UI Screen** | `EditarDatosPersonalesScreen.kt` | Formulario adaptable con DatePicker modal, DropdownMenus y validación de formulario. |
| **ViewModel** | `PerfilAtletaViewModel.kt` | Carga y actualización de perfil extendido para usuarios con rol `ATLETA`. |
| **ViewModel** | `PerfilEntrenadorViewModel.kt` | Carga y actualización de datos personales para usuarios con rol `ENTRENADOR`. |
| **Repository** | `UserRepository.kt` | Operaciones en Firestore via `actualizarPerfilUsuario` y `actualizarDatosPersonales`. |
| **Platform** | `Platform.kt` | Funciones nativas `formatearFechaCorto`, `getCurrentTimeMillis` y `normalizarFechaDatePicker`. |

### 3. Cadena de Llamadas (Call Chain)

#### Rol Atleta:

```text
[EditarDatosPersonalesScreen] ──(Seleccionar DatePicker)──► [Platform.normalizarFechaDatePicker(utcMillis)]
     │                                                                   │
     │                                                                   ▼
     │                                                   Asigna `fechaNacimientoMilis`
     ▼ (Pulsar "Guardar Cambios")
[PerfilAtletaViewModel.actualizarDatosAtleta(uid, nombres, apellidos, ..., fechaNac, tipoSangre, nacionalidad)]
     │
     ▼
[UserRepository.actualizarPerfilUsuario(uid, camposMap)]
     │
     ├─► Firestore usersCollection.document(uid).update(camposMap)
     │
     ▼ (Respuesta exitosa)
Actualiza `_uiState.usuarioLogueado` conservando inmutabilidad
     │
     ▼
[EditarDatosPersonalesScreen] detecta `guardadoExitoso == true` y ejecuta `onBack()`
```

#### Rol Entrenador:

```text
[EditarDatosPersonalesScreen] (Ficha fisiológica oculta por Rol)
     │
     ▼ (Pulsar "Guardar Cambios")
[PerfilEntrenadorViewModel.guardarDatosPersonales(uid, nombres, apellidos, tipoDoc, doc, tel)]
     │
     ▼
[UserRepository.actualizarDatosPersonales(uid, nombres, apellidos, tipoDoc, doc, tel)]
     │
     ├─► Firestore usersCollection.document(uid).update(campos)
     │
     ▼
Actualiza `_uiState.usuarioLogueado` y cierra la pantalla
```