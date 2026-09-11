# Documentación Técnica: Flujo 5 — Biblioteca de Ejercicios y Plantillas de Rutinas

**Aplicación:** FitLog (Kotlin Multiplatform / Compose Multiplatform)  
**Módulo Auditado:** Flujo 5 — Biblioteca de Ejercicios y Plantillas de Rutinas  
**Arquitectura:** MVVM / Clean Architecture / Repository Pattern  
**Backend:** Cloud Firestore

---

## 1. Resumen Ejecutivo

El **Flujo 5** gestiona el catálogo maestro de entrenamiento de la plataforma. Permite administrar la biblioteca global e individual de ejercicios estructurados por `GrupoMuscular`, así como la creación, edición y estructuración de plantillas máster (`PlantillaRutina`) compuestas por una secuencia ordenada de `ElementoRutina` con prescripción detallada de series, repeticiones y tiempos de descanso.

---

## 2. Mapa de Componentes y Responsabilidades

| Componente / Archivo | Ubicación | Función Principal |
| :--- | :--- | :--- |
| `Ejercicio.kt` | `data/model/` | Modelo de datos para ejercicios (globales o creados por un coach) con enum `GrupoMuscular`. |
| `PlantillaRutina.kt` | `data/model/` | Entidad máster de plantillas compuestas por una lista ordenada de `ElementoRutina`. |
| `PrescripcionSerie.kt` | `data/model/` | Define el número de serie, rangos de repeticiones (`repsMin`, `repsMax`) y el `TipoSerie` (Efectiva, Aproximación, Drop Set, etc.). |
| `BibliotecaScreen.kt` | `ui/dashboard/entrenador/` | Pantalla principal con pestañas (*Ejercicios* y *Plantillas*), búsqueda por texto y filtrado por `GrupoMuscular`. |
| `AddEjercicioScreen.kt` | `ui/dashboard/entrenador/` | Formulario para alta y modificación de ejercicios personalizados por el entrenador. |
| `AddPlantillaScreen.kt` | `ui/dashboard/entrenador/` | Editor de plantillas máster con selector dinámico desde la biblioteca y reordenamiento de secuencias. |
| `EditorNotasLista.kt` | `ui/components/` | Componente reutilizable para administrar notas, viñetas y recomendaciones técnicas. |
| `BibliotecaViewModel.kt` | `viewmodel/entrenador/` | Manejo de estado del catálogo, filtrado en memoria y borrado lógico (*soft delete*). |
| `AddEjercicioViewModel.kt` | `viewmodel/entrenador/` | Control del formulario y persistencia de la entidad `Ejercicio`. |
| `AddPlantillaViewModel.kt` | `viewmodel/entrenador/` | Construcción y actualización de `PlantillaRutina` preservando la integridad de IDs. |
| `ExerciseRepository.kt` | `repository/` | Capa de datos para colecciones `biblioteca_global`, `ejercicios_personalizados` y `plantillas_rutinas`. |

---

## 3. Reglas de Negocio e Integridad de Datos

1. **Aislamiento de la Biblioteca Global**: Los ejercicios pertenecientes a la colección `biblioteca_global` son de lectura general para todos los usuarios y no pueden ser editados ni eliminados por los entrenadores.
2. **Identificadores Únicos y Edición**: Al guardar o actualizar una `PlantillaRutina`, el `AddPlantillaViewModel` preserva de forma atómica el ID del documento Firestore para evitar duplicados o desvincular los elementos del carrito.
3. **Consistencia en Reordenamiento**: Los ejercicios dentro de una plantilla mantienen una secuencia explícita gracias a llaves de renderizado estables en las vistas `LazyColumn`.
4. **Optimización KMP**: El filtrado por grupos musculares utiliza iteradores directos `GrupoMuscular.entries` para evitar la sobrecarga de memoria en plataformas móviles y de escritorio.

---

## 4. Delimitación de Alcance (Desambiguación)
* **Pertenecen al Flujo 5**: Gestión de la biblioteca máster, ejercicios personalizados y plantillas base de entrenamiento.
* **Excluidos del Flujo 5** *(Pertenecen al Flujo de Prescripción/Asignación)*: Archivos como `SeleccionarPlantillaScreen.kt`, `EditRutinaAsignadaScreen.kt`, `AsignarRutinaViewModel.kt` y `EditRutinaAsignadaViewModel.kt` corresponden a la asignación particular de rutinas en la colección `rutinas_asignadas` de cada atleta.