# Documentación Técnica: Flujo 6 — Asignación y Edición del Programa de Entrenamiento

**Aplicación:** FitLog (Kotlin Multiplatform / Compose Multiplatform)  
**Módulo Auditado:** Flujo 6 — Asignación y Edición del Programa de Entrenamiento  
**Arquitectura:** MVVM / Clean Architecture / Repository Pattern  
**Backend:** Cloud Firestore

---

## 1. Resumen Ejecutivo

El **Flujo 6** gestiona la prescripción y reconfiguración directa del plan de entrenamiento activo de un atleta. Permite estructurar bloques de entrenamiento (`RutinaAsignada`), reordenar la secuencia de días y ejercicios, prescribir series con rangos (`repsMin`, `repsMax`) y tipos de esfuerzo (`EFECTIVA`, `APROXIMACION`, `DROP_SET`, `FALLO`, `REST_PAUSE`), y mantener la sincronización reactiva con la entidad `CicloEntrenamiento`.

---

## 2. Componentes del Módulo

| Componente / Archivo | Ubicación | Función Principal |
| :--- | :--- | :--- |
| `RutinaAsignada.kt` | `data/model/` | Entidad con la rutina activa del atleta, días asignados (`DiaEntrenamientoAsignado`) y ejercicios (`EjercicioAsignado`). |
| `CicloEntrenamiento.kt` | `data/model/` | Modelo que representa el microciclo activo, metas de volumen e indicadores de asistencia. |
| `PrescripcionSerie.kt` | `data/model/` | Estructura para configurar series, repeticiones y tipología de esfuerzo. |
| `SeleccionarPlantillaScreen.kt` | `ui/dashboard/entrenador/` | Pantalla de asignación inicial de rutinas a partir de plantillas máster. |
| `EditRutinaAsignadaScreen.kt` | `ui/dashboard/` | Editor completo de rutina activa con reordenamiento visual de días y ejercicios. |
| `ModoCicloSection.kt` | `ui/components/` | Componente reutilizable para configurar la modalidad (`CALENDARIO_SEMANAL` o `SECUENCIAL_RODANTE`) y duración. |
| `EditRutinaAsignadaViewModel.kt` | `viewmodel/atleta/` | Lógica de presentación para edición, reordenamiento, adición desde biblioteca y borrado. |
| `AtletaRepository.kt` | `repository/` | Persistencia en la subcolección `rutinas_asignadas`. |
| `AtletaProgresoRepository.kt` | `repository/` | Sincronización del ciclo activo (`sincronizarCicloActivoConRutina`) y cierre forzado al reemplazar rutinas. |

---

## 3. Reglas de Negocio e Integridad

1. **Unicidad de Rutina Activa**: Un atleta solo puede tener una rutina con `estaActiva = true`. La asignación o eliminación forza el cierre del `CicloEntrenamiento` previo.
2. **Consistencia de Índices Visuales**: El reordenamiento de días y ejercicios en la UI opera sobre `visualDiaIndex` y `visualEjIndex` para garantizar que la mutación se aplique sobre el arreglo ordenado por `ordenSecuencia`.
3. **Sincronización de Metas**: Al actualizar un programa asignado, `AtletaProgresoRepository.sincronizarCicloActivoConRutina` recalculas las metas de repeticiones y valida si el ciclo actual debe marcarse como completado (`estaActivo = false`).