# Documentación Técnica: Flujo 4 — Vinculación, Suscripciones y Periodos Facturables

**Aplicación:** FitLog (Kotlin Multiplatform / Compose Multiplatform)  
**Módulo Auditado:** Flujo 4 — Vinculación, Suscripciones y Periodos Facturables  
**Arquitectura:** MVVM / Clean Architecture / Repository Pattern  
**Backend:** Cloud Firestore

---

## 1. Resumen Ejecutivo

El **Flujo 4** gestiona el ciclo comercial entre entrenadores y atletas. Abarca la generación y consumo de códigos de vinculación de 6 dígitos con ventana de caducidad (15 minutos), la administración de suscripciones activas/diferidas/suspendidas, la detección de solapamiento de fechas y la autorreparación/promoción atómica de estados mediante la subcolección `periodos_facturables`.

---

## 2. Estructura de Componentes e Interfaz de Usuario

| Componente / Archivo | Ubicación | Descripción y Función |
| :--- | :--- | :--- |
| `Usuario.kt` | `data/model/` | Declara la entidad principal `Usuario` y las estructuras asociadas: `PeriodoFacturable`, `EstadoSuscripcion`, `EstadoPeriodo` y `TipoPlanSuscripcion`[cite: 1]. |
| `AtletaMainScreen.kt` | `ui/dashboard/atleta/` | Pantalla principal del atleta. Integra `PantallaHuerfano` para el ingreso de credenciales de vinculación, gestión de estados de restricción (Vencido, Pausado, Diferido) y cierre de sesión. |
| `AsignarPlanDialog.kt` | `ui/components/` | Modal unificado para el entrenador. Permite asignar o renovar planes con selección de fecha normalizada (`normalizarFechaDatePicker`) e inspección de colisiones[cite: 9]. |
| `FacturacionScreen.kt` | `ui/dashboard/entrenador/` | Consola comercial del entrenador para consultar estados de los atletas, activar/pausar planes y generar códigos de vinculación[cite: 8]. |
| `HistorialFacturacionScreen.kt` | `ui/dashboard/entrenador/` | Vista de detalle de la cola de planes (Activos, Diferidos, Completados, Cancelados) de un atleta específico[cite: 8]. |
| `InformeFacturacionGlobalScreen.kt` | `ui/dashboard/entrenador/` | Tablero contable con el registro histórico de cobros y transacciones generales del entrenador[cite: 8]. |
| `UserRepository.kt` | `repository/` | Capa de datos y operaciones atómicas (`WriteBatch`) contra Firestore para vinculación, reactivación, pausa y re-evaluación de suscripciones[cite: 8]. |

---

## 3. Flujo de Datos y Lógica de Negocio

### 3.1 Vinculación Atleta - Entrenador
1. El entrenador genera un código alfanumérico aleatorio de 6 dígitos con vencimiento de 15 minutos en `UserRepository.generarCodigoVinculacion`[cite: 8].
2. El atleta ingresa el correo del entrenador y el código de 6 dígitos desde `PantallaHuerfano` en `AtletaMainScreen.kt`.
3. `UserRepository.vincularConEntrenador` valida la vigencia del código, cancela los periodos vigentes previos y asigna el `entrenadorId`[cite: 8].

### 3.2 Administración de Periodos y Promoción Atómica
* **Encolamiento Diferido**: Si un atleta posee una suscripción activa, la asignación de un nuevo plan se encola como `DIFERIDO` iniciando en `ultimaFechaFinCadena + 1ms`[cite: 8].
* **Pausa / Congelamiento**: Al pausar la cuenta, el saldo de días en milisegundos se resguarda (`saldoMilisegundosRestantes`) y el periodo pasa a `CONGELADO`[cite: 8].
* **Reactivación**: Al reanudar, se ajustan las fechas del periodo congelado y los diferidos en cola se desplazan proporcionalmente[cite: 8].
* **Promoción Atómica**: En `evaluarYActualizarEstadoSuscripcion`, si un plan activo vence o se anula, se promueve inmediatamente el diferido más antiguo a `ACTIVO` mediante un `WriteBatch`[cite: 8].

---

## 4. Archivos Obsoletos / Deprecados
* `RenovarSuscripcionDialog.kt`: Eliminado por redundancia y riesgo de desfase horaria al añadir desplazamientos manuales. Se consolida el 100% de la funcionalidad comercial en `AsignarPlanDialog.kt`[cite: 8, 9].