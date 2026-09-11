# Documentación Técnica: Flujo 3 — Expediente del Atleta, Semaforización y Salud del Equipo

**Aplicación:** FitLog (Kotlin Multiplatform / Compose Multiplatform)  
**Módulo Auditado:** Flujo 3 — Expediente del Atleta, Semaforización y Salud del Equipo  
**Arquitectura:** MVVM / Clean Architecture / Repository Pattern  
**Backend:** Cloud Firestore

---

## 1. Resumen Ejecutivo

En la auditoría del **Flujo 3**, se optimizó la carga de métricas del expediente individual del atleta y el motor de evaluación paralela del semáforo de salud del equipo para entrenadores.

**Objetivos alcanzados:**
* Optimización de consultas a Firestore en `AtletaDetailViewModel`, reemplazando la descarga completa del historial histórico por consultas acotadas al ciclo activo.
* Eliminación de consultas innecesarias a subcolecciones en `SemaforoRepository` al ordenar condicionalmente las verificaciones de rutinas y ciclos.
* Corrección de falsos positivos en el indicador de cumpleaños (`esCumpleanosHoy`) para marcas de tiempo vacías (`0L`).
* Protección contra valores `Double.NaN` en los agregados de RPE promedio por ejercicio.

---

## 2. Componentes Involucrados

| Capa | Componente / Archivo | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI (Compose)** | `AtletaDetailScreen.kt` | Expediente del atleta, tarjetas KPI (Asistencia, Volumen, RPE), accesos directos e historial de notas. |
| **UI (Compose)** | `EntrenadorDashboardScreen.kt` | Dashboard tri-pestanas ("Mis Atletas", "Semáforo", "Asistencia Hoy") con refresco por navegación reactiva. |
| **UI (Compose)** | `PerfilAtletaScreen.kt` | Ficha detallada de identificación y estado comercial del usuario. |
| **ViewModel** | `AtletaDetailViewModel.kt` | Carga asíncrona del expediente, orquestación de métricas con `SemaforoCalculador` y extracción de comentarios. |
| **Repository** | `SemaforoRepository.kt` | Evaluación multihilo paralela (`coroutineScope` + `async`) del estado de salud del equipo. |
| **Platform** | `Platform.kt` | Helpers de fecha nativos en iOS y Android (`esCumpleanosHoy`). |

---

## 3. Matriz de Hallazgos y Soluciones Técnicas

### Hallazgo 1: Descarga Desmedida de Historial en `AtletaDetailViewModel`
* **Problema:** Se leían todas las sesiones históricas registradas por un atleta para calcular las métricas del ciclo activo.
* **Solución:** Se reemplazó la llamada por `obtenerEntrenamientosCicloActivo(atletaId, cicloActivo.fechaInicio)`, leyendo únicamente las sesiones vigentes.
* **Impacto en Cuotas:** Reducción sustancial en el consumo de lecturas para atletas con alto historial de entrenamiento.

---

### Hallazgo 2: Falso Positivo de Cumpleaños en `esCumpleanosHoy` (1 de Enero)
* **Problema:** Para atletas sin fecha de nacimiento (`0L`), la fecha por defecto era `01/01/1970`, activando la alerta de cumpleaños cada 1 de enero.
* **Solución:** Inclusión del chequeo `if (fechaNacimiento <= 0L) return false` en la implementación nativa de Android e iOS.

---

### Hallazgo 3: Consultas Prematuras a Firestore en `SemaforoRepository`
* **Problema:** Se consultaba `obtenerCicloActivo` antes de verificar si el atleta tenía alguna rutina asignada, desperdiciando lecturas en atletas en estado "Requiere Gestión".
* **Solución:** Reordenamiento condicional: solo se consulta el ciclo activo si la lista de rutinas activas no está vacía.

---

### Hallazgo 4: Manejo Seguro de RPE Promedio (`Double.NaN`)
* **Problema:** Grupos vacíos en la agrupación de RPE por ejercicio producían `NaN` en las propiedades del `InformeCoach`.
* **Solución:** Validación explícita de `avg.isNaN()` retornando `0.0` por defecto.

---

## 4. Dictamen Técnico Final
* **Estado:** ✅ **Aprobado y Producción-Ready**.
* **Rendimiento:** Carga optimizada en paralelo sin lecturas huérfanas en Firestore.
* **Estabilidad UI:** Eliminación de banderas falsas de cumpleaños y prevención de errores de cálculo en Compose.