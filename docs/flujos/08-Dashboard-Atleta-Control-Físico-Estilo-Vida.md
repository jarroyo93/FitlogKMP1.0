# Documentación Técnica — Flujo 8: Dashboard del Atleta, Control Físico y Estilo de Vida

## 1. Visión General

El **Flujo 8** administra la pantalla principal del atleta (`AtletaInicioScreen`), sirviendo como centro de control para la consulta del ciclo de entrenamiento activo, la recomendación automática de la rutina a ejecutar, el pesaje rápido y la revisión histórica del perfil físico y hábitos de vida.

---

## 2. Arquitectura de Componentes

| Capa | Archivo / Componente | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI / Presentación** | `AtletaInicioScreen.kt` | Renderiza el saludo personalizado, el estado del ciclo activo, la sugerencia del día de entrenamiento y la tarjeta de control de peso corporal con modal de registro. |
| **UI / Presentación** | `HistorialValoracionScreen.kt` | Lista cronológica de evaluaciones antropométricas y métricas de bioimpedancia con tarjetas colapsables (`rememberSaveable`) y optimización de renderizado (`key`). |
| **UI / Presentación** | `HistorialHabitosScreen.kt` | Lista cronológica del historial de estilo de vida (sueño, disponibilidad, horarios) con soporte de tarjetas colapsables. |
| **ViewModel / Estado** | `AtletaInicioViewModel.kt` | Carga paralela con `supervisorScope` de rutinas, pesajes y ciclo activo. Valida el estado de la suscripción (`EstadoSuscripcion.ACTIVO`) antes de permitir el registro de pesaje. |
| **ViewModel / Estado** | `HistorialValoracionViewModel.kt` | Gestiona el estado reactivo (`HistorialState`) para consultar el historial antropométrico del atleta. |
| **ViewModel / Estado** | `HistorialHabitosViewModel.kt` | Gestiona el estado reactivo (`HistorialHabitosState`) para la consulta de hábitos. |
| **ViewModel / Estado** | `AddHabitosViewModel.kt` | Administra la captura, actualización y persistencia de nuevos hábitos en el repositorio. |
| **Capa de Datos** | `AtletaRepository.kt` | Persistencia en subcolecciones `valoraciones` y `habitos`. |
| **Capa de Datos** | `AtletaProgresoRepository.kt` | Lectura/escritura de `pesajes` y control automático de expiración de ciclos activos (`estaActivo = false` si `ahora > fechaCierre`). |
| **Platform / Utils** | `Platform.kt` (Android/iOS) | Formateo nativo de fechas (`formatearFechaHistorial`, `formatearFechaHora`). |

---

## 3. Flujo de Datos y Secuencia Operativa

```text
                               ┌──► AtletaRepository.obtenerRutinasActivas()
                               │
[AtletaInicioViewModel] ──────┼──► AtletaProgresoRepository.obtenerUltimosPesajes()
 (Carga en paralelo via        │
   supervisorScope)            └──► AtletaProgresoRepository.obtenerCicloActivo()
                                        │
                                        ▼ (Si fechaCierre < ahora)
                                  [Cierra ciclo expirado automáticamente]

──────────────────────────────────────────────────────────────────────────────────

[AtletaInicioScreen] ──(Nuevo Peso)──► [registrarPeso()] ──(Valida Suscripción Activa)
                                                 │
                                                 ▼
                                  [AtletaProgresoRepository.registrarPesaje()]