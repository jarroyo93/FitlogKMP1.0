# Documentación Técnica: Flujo 1 — Autenticación, Login y Primer Acceso

**Aplicación:** FitLog (Kotlin Multiplatform / Compose Multiplatform)  
**Módulo Auditado:** Flujo 1 — Autenticación, Inicio de Sesión y Primer Acceso  
**Arquitectura:** MVVM / Clean Architecture / Repository Pattern  
**Backend:** Firebase Auth & Cloud Firestore

---

## 1. Resumen Ejecutivo

En la auditoría del **Flujo 1**, se revisaron los flujos de datos, el manejo de errores asíncronos, la arquitectura de capas y el consumo de transacciones en Firebase.

**Objetivos alcanzados:**
* Erradicación de estados inconsistentes de sesión (sesiones huérfanas en Auth).
* Eliminación de escrituras redundantes en Firestore (ahorro directo del 50% de escrituras al cambiar contraseña).
* Desacoplamiento estricto entre la interfaz de usuario (UI) y los repositorios (cumplimiento MVVM).
* Eliminación de parpadeos de interfaz (flicker) durante la transición de pantallas tras login exitoso.

---

## 2. Componentes Involucrados

| Capa | Componente / Archivo | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI (Compose)** | `LoginScreen.kt` | Formulario de login, captura de credenciales y ciclo de vida de acceso. |
| **UI (Compose)** | `CambiarContrasenaScreen.kt` | Pantalla obligatoria de cambio de clave temporal para atletas. |
| **ViewModel** | `AuthViewModel.kt` | Orquestación de estados UI (`AuthState`, `ActivationState`) y reglas de negocio. |
| **Repository** | `AuthRepository.kt` | Gestión exclusiva de la API de Firebase Auth. |
| **Repository** | `UserRepository.kt` | Gestión de la base de datos Firestore (colección `users`). |
| **Local Storage** | `UserPreferencesManager` | Persistencia local del último correo ingresado. |

---

## 3. Matriz de Hallazgos y Soluciones Técnicas

### Hallazgo 1: Sesión Huérfana en Firebase Auth por Falla en Firestore
* **Problema:** Si Firebase Auth autenticaba con éxito pero la consulta a Firestore (`obtenerUsuario`) fallaba o retornaba `null`, el sistema mostraba un error pero dejaba la sesión abierta en el SDK de Firebase.
* **Solución:** Se agregó un `authRepository.logout()` preventivo inmediato dentro de los bloques de excepción y cuando el usuario no existe en la base de datos.
* **Impacto en Cuotas:** **0 lecturas / 0 escrituras**. El cierre de sesión en el SDK es una operación puramente local.

---

### Hallazgo 2: Doble Escritura Redundante al Cambiar Contraseña
* **Problema:** Al cambiar la clave temporal, `AuthRepository` ejecutaba un `.update()` a Firestore y, acto seguido, `AuthViewModel` volvía a llamar a `userRepository.actualizarPerfilUsuario()` para modificar el mismo campo (`requiereCambioContrasena`).
* **Solución:** Se eliminó toda manipulación de Firestore dentro de `AuthRepository.kt`, dejando esa responsabilidad exclusivamente en `UserRepository.kt`.
* **Impacto en Cuotas:** **Reducción del 50% de escrituras** en la operación de activación de contraseña (de 2 escrituras a 1).

---

### Hallazgo 3: Violación de Arquitectura MVVM en `CambiarContrasenaScreen`
* **Problema:** La pantalla instanciaba directamente el repositorio (`remember { AuthRepository() }`) e iniciaba corrutinas locales para cerrar sesión.
* **Solución:** Se expuso el método `logout(onSuccess)` en `AuthViewModel.kt` y se eliminó cualquier referencia directa a repositorios dentro del Composable.
* **Impacto en Cuotas:** **0 lecturas / 0 escrituras**.

---

### Hallazgo 4: Parpadeo de UI por Reseteo Prematuro en `LoginScreen`
* **Problema:** Al hacer login exitoso, `authViewModel.resetState()` se ejecutaba inmediatamente en `LaunchedEffect`, cambiando el estado a `Idle` mientras la animación de transición de pantalla aún estaba en curso.
* **Solución:** Se eliminó `resetState()` del bloque reactivo y se reubicó en un `DisposableEffect(Unit)` para que se ejecute únicamente cuando el Composable se desmonte del árbol de UI.
* **Impacto en Cuotas:** **0 lecturas / 0 escrituras**.

---

## 4. Archivos Modificados y Fragmentos Clave

### `AuthRepository.kt`
> **Ruta:** `shared/src/commonMain/kotlin/dev/josearroyo/fitlog/repository/AuthRepository.kt`

Responsabilidad acotada a operaciones de Firebase Auth.

```kotlin
// Cambio de contraseña desacoplado de Firestore
suspend fun cambiarContrasenaPrimeraVez(nuevaContrasena: String): Result<Boolean> {
    return try {
        val user = auth.currentUser ?: return Result.failure(Exception("No hay una sesión activa."))
        user.updatePassword(nuevaContrasena)
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}