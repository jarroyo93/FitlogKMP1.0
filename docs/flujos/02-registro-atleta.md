# Documentación Técnica: Flujo 2 — Registro y Alta Completa de Atletas (Onboarding Coach)

**Aplicación:** FitLog (Kotlin Multiplatform / Compose Multiplatform)  
**Módulo Auditado:** Flujo 2 — Registro y Alta Completa de Atletas (Onboarding Coach)  
**Arquitectura:** MVVM / Clean Architecture / Repository Pattern  
**Backend:** Firebase Auth (Instancia Secundaria Nativa) & Cloud Firestore (Batches)

---

## 1. Resumen Ejecutivo

En la auditoría del **Flujo 2**, se optimizó el proceso de registro multietapa (wizard de 4 pasos) utilizado por los entrenadores para dar de alta a nuevos atletas.

**Objetivos alcanzados:**
* Eliminación de lecturas redundantes en Firestore previo a la creación del usuario (ahorro directo de transacciones).
* Flexibilización de la regla de negocio para permitir que una misma persona física pueda poseer cuenta de Entrenador y cuenta de Atleta utilizando el mismo documento de identidad en roles distintos.
* Validación estricta en el ViewModel para garantizar que la contraseña temporal generada a partir del número de documento cumpla con los 6 caracteres mínimos requeridos por Firebase Auth.
* Validación de duración mínima (≥ 1 día) para suscripciones de tipo plan personalizado.
* Sanitización de texto (`.trim()`) en nombres, apellidos, documento y teléfono.
* Corrección de errores "pegajosos" (Sticky Errors) en el estado de la UI mediante reseteo dinámico al modificar cualquier campo del formulario.

---

## 2. Componentes Involucrados

| Capa | Componente / Archivo | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI (Compose)** | `AddAtletaScreen.kt` | Formulario por pasos (wizard) para captura de datos personales, valoración física, hábitos y plan de suscripción. |
| **ViewModel** | `AddAtletaViewModel.kt` | Manejo del estado del formulario (`AddAtletaState`), validaciones de cada paso, reseteo dinámico de errores y orquestación del guardado. |
| **Repository** | `AtletaRepository.kt` | Invocación de Auth secundario y ejecución del lote atómico (`WriteBatch`) en Firestore (`users`, `valoraciones`, `habitos`, `periodos_facturables`, `historial_facturacion_general`). |
| **Repository** | `UserRepository.kt` | Consultas de validación previa de existencia de correo y número de documento por rol. |
| **Platform** | `Platform.kt` (Android / iOS) | Puentes nativos (`crearCuentaEnInstanciaSecundaria`, `eliminarCuentaEnInstanciaSecundaria`) para crear usuarios en Auth sin cerrar la sesión activa del entrenador. |

---

## 3. Matriz de Hallazgos y Soluciones Técnicas

### Hallazgo 1: Lecturas Redundantes en Firestore en `crearAtletaCompleto`
* **Problema:** `AtletaRepository.crearAtletaCompleto()` ejecutaba dos consultas `.where()` a Firestore para validar correo y documento antes de registrar al usuario, a pesar de que `AddAtletaViewModel` ya las había ejecutado en el Paso 1.
* **Solución:** Se eliminaron las 2 consultas redundantes en `AtletaRepository`, delegando la validación previa al Paso 1 y apoyándose en la salvaguarda nativa de Firebase Auth (`email-already-in-use`).
* **Impacto en Cuotas:** **Ahorro directo de 2 lecturas en Firestore** por cada alta exitosa de atleta.

---

### Hallazgo 2: Restricción Global de Número de Documento por Rol
* **Problema:** `UserRepository.existeDocumento()` consultaba la colección `users` de forma global, impidiendo que un usuario con perfil de `ENTRENADOR` pudiera registrarse como `ATLETA` usando su mismo número de cédula/documento.
* **Solución:** Se parametrizó `existeDocumento(documento, rol)` para verificar la unicidad del número de documento **específicamente por el rol** (`ATLETA`), habilitando la convivencia de ambos roles para una misma persona física con correos diferentes.

---

### Hallazgo 3: Inconsistencia en Clave Temporal para Documentos Cortos (< 6 caracteres)
* **Problema:** Si el número de documento de un atleta tenía menos de 6 dígitos, la validación del Paso 1 lo permitía, pero al guardarlo se rellenaba silenciosamente con ceros (`padEnd`), creando una contraseña en Firebase Auth que no coincidía con el documento real ingresado por el entrenador.
* **Solución:** Se añadió la validación `documento.length >= 6` directamente en el Paso 1 (`validarPaso1YContinuar`), bloqueando el avance si el documento no cumple con la longitud mínima de contraseña exigida por Firebase Auth.

---

### Hallazgo 4: Sanitización de Cadenas de Texto (`.trim()`) y Validación de Plan Personalizado
* **Problema:** Nombres y apellidos guardaban espacios en blanco accidentales al final de la cadena, y se permitían planes personalizados con 0 días de duración.
* **Solución:** Aplicación de `.trim()` en la captura de todos los campos de texto del usuario y validación de `diasPersonalizados >= 1` en `guardarAtleta()`.

---

### Hallazgo 5: Persistencia Involuntaria de Mensajes de Error (Sticky Errors)
* **Problema:** Cuando se mostraba una alerta de validación (ej. "El correo ya está registrado") y el usuario modificaba un campo para corregirlo, el mensaje de error permanecía visible en pantalla.
* **Solución:** Se añadió `error = null` en los eventos de actualización del formulario en `onEvent` y al inicio de `validarPaso1YContinuar()`.

---

## 4. Archivos Modificados y Fragmentos Clave

### `UserRepository.kt`
> **Ruta:** `shared/src/commonMain/kotlin/dev/josearroyo/fitlog/repository/UserRepository.kt`

```kotlin
// Validación de existencia de documento filtrada por rol de usuario
suspend fun existeDocumento(documento: String, rol: RolUsuario = RolUsuario.ATLETA): Boolean = try {
    val result = usersCollection
        .where("numeroDocumento", equalTo = documento.trim())
        .where("rol", equalTo = rol.name)
        .limit(1)
        .get()
    result.documents.isNotEmpty()
} catch (e: Exception) {
    println("🔥 [UserRepository] Error en existeDocumento ($documento):${e.message}")
    e.printStackTrace()
    false
}