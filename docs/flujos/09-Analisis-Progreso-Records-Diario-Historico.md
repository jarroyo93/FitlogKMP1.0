# Documentación Técnica — Flujo 9: Análisis de Progreso, Récords y Diario Histórico

## 1. Visión General

El **Flujo 9** provee al atleta un centro de analítica deportiva para evaluar su evolución de carga y rendimiento a lo largo del tiempo. Incluye la visualización de progresiones de carga por ejercicio con estimación de $1\text{RM}$, consistencia semanal de entrenamiento (racha), récords personales (PRs), resumen de impacto físico por ciclo y un diario histórico detallado de sesiones pasadas.

---

## 2. Arquitectura de Componentes

| Capa | Archivo / Componente | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI / Presentación** | `ProgresoAtletaScreen.kt` | Pantalla principal basada en un `TabRow` reactivo con 3 pestañas ("Evolución", "Diario de Ciclos", "Récords"). |
| **UI / Componentes** | `EvolucionEjercicioSection` | Módulo de análisis de fuerza por ejercicio. Incluye buscador por grupos musculares y selector de métricas (Carga Máxima vs. Volumen Total). |
| **UI / Componentes** | `GraficaProgresoEjercicio` | Gráfica vectorial personalizada renderizada mediante Compose `Canvas` con curvas de nivel, degradado dinámico y etiquetas de carga. |
| **UI / Componentes** | `TarjetaEncabezadoCiclo` | Muestra KPIs del ciclo seleccionado: asistencia (%), RPE promedio, tonelaje total ($t$) e impacto físico (deltas de peso y cintura). |
| **UI / Componentes** | `ModalBottomSheetHistoricoCiclos` | Modal para filtrado de ciclos pasados por Año y Mes. |
| **UI / Componentes** | `DetalleSesionDialog` | Modal emergente para inspeccionar series, pesos, repeticiones, RPE y comparativa contra pauta prescrita de una sesión. |
| **ViewModel / Estado** | `ProgresoAtletaViewModel.kt` | Orquesta la carga concurrente (`supervisorScope`), computación de PRs, KPIs semanales/mensuales y mapeo de modelos UI (`ResumenCicloUI`, `ImpactoFisicoUI`). |
| **Capa de Datos** | `AtletaProgresoRepository.kt` | Consultas históricas de entrenamientos, ciclo activo y registros de pesajes. |
| **Capa de Datos** | `AtletaRepository.kt` | Consulta histórica de valoraciones físicas antropométricas. |
| **Platform / Utils** | `AndroidPlatform.kt` / `IOSPlatform.kt` | Estandarización de marcas de tiempo y zona horaria local (`timeZone = TimeZone.getDefault()` / `NSTimeZone.defaultTimeZone`) para el formateo de fechas y cálculo de iniciales de días. |

---

## 3. Flujo de Datos y Secuencia Operativa

```text
                               ┌──► AtletaProgresoRepository.obtenerHistorialEntrenamientos()
[ProgresoAtletaViewModel] ─────┼──► AtletaProgresoRepository.obtenerHistorialCiclos()
 (Carga concurrente vía        ├──► AtletaProgresoRepository.obtenerUltimosPesajes()
   supervisorScope)            └──► AtletaRepository.obtenerHistorialValoraciones()
          │
          ├─► Mapea PRs por Ejercicio (Peso Máximo × Reps)
          ├─► Asigna Ciclo Activo Inicial
          └─► Ejecuta seleccionarCiclo(ciclo)
                    │
                    ▼
          Calcula KPIs del Ciclo (RPE Promedio, Tonelaje, Deltas Físicos)
                    │
                    ▼
          [ProgresoAtletaState] ──► Renderizado en [ProgresoAtletaScreen]