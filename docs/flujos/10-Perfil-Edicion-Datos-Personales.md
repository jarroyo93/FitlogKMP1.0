# Documentación Técnica — Flujo 10: Perfil y Edición de Datos Personales

## 1. Visión General

El **Flujo 10** gestiona la visualización y edición de la información personal de los usuarios en la plataforma FitLog. La pantalla se adapta de forma dinámica según el rol del usuario (`RolUsuario.ATLETA` o `RolUsuario.ENTRENADOR`), habilitando o restringiendo la edición de la ficha fisiológica (fecha de nacimiento, tipo de sangre y nacionalidad).

---

## 2. Arquitectura de Componentes

| Capa | Archivo / Componente | Responsabilidad Principal |
| :--- | :--- | :--- |
| **UI / Presentación** | `EditarDatosPersonalesScreen.kt` | Pantalla adaptable para edición de perfiles con DatePicker y DropdownMenus contextuales. |
| **ViewModel / Estado** | `PerfilAtletaViewModel.kt` | Maneja la carga y actualización atómica del perfil de los usuarios con rol `ATLETA`. |
| **ViewModel / Estado** | `PerfilEntrenadorViewModel.kt` | Maneja la carga y actualización de datos de perfil e identificación para rol `ENTRENADOR`. |
| **Capa de Datos** | `UserRepository.kt` | Provee los métodos `actualizarPerfilUsuario` y `actualizarDatosPersonales` en Firestore. |
| **Modelo de Datos** | `Usuario.kt` | Entidad principal conteniendo campos de identificación y fisiológicos. |

---

## 3. Matriz de Campos por Rol de Usuario

| Campo de Perfil | Rol Entrenador | Rol Atleta | Validación UI |
| :--- | :---: | :---: | :--- |
| **Nombres** | Editable | Editable | Obligatorio (`isNotBlank()`) |
| **Apellidos** | Editable | Editable | Obligatorio (`isNotBlank()`) |
| **Tipo Documento** | Editable | Editable | Dropdown (`C.C.`, `C.E.`, `Pasaporte`, `D.N.I.`) |
| **Número Documento**| Editable | Editable | Obligatorio, teclado numérico |
| **Teléfono** | Editable | Editable | Teclado telefónico |
| **Fecha Nacimiento** | Oculto | Editable | DatePicker modal, persistido en epoch millis |
| **Tipo de Sangre** | Oculto | Editable | Dropdown (`A+`, `O+`, `AB-`, etc.) |
| **Nacionalidad** | Oculto | Editable | Obligatorio para atletas |

---

## 4. Puntos de Verificación para Pruebas (QA)

* **Adaptación por Rol**: Iniciar sesión como Entrenador y verificar que la sección "Ficha Fisiológica del Atleta" no aparezca en pantalla.
* **DatePicker Modal**: Abrir la pantalla como Atleta, pulsar en el campo "Fecha de Nacimiento" y verificar que el diálogo de selección de fecha cargue la fecha guardada y actualice la fecha corta (`dd/MM/yyyy`).
* **Habilitación del Botón de Guardado**: Confirmar que el botón "Guardar Cambios" permanece deshabilitado hasta que todos los campos requeridos por rol tengan contenido válido.
* **Resiliencia de Red**: Simular desconexión antes de guardar y verificar que la pantalla muestre el mensaje de error de Firestore sin desconfigurar las entradas de texto.