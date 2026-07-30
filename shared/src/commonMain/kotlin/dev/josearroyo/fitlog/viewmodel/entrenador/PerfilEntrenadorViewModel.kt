package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Usuario // 🔥 Importación al nuevo paquete de modelos
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Modificado el nombre para alinearlo al contexto del Entrenador
data class PerfilEntrenadorUiState(
    val usuarioLogueado: Usuario? = null,
    val entrenadorAsignado: Usuario? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val exitoGuardado: Boolean = false
)

class PerfilEntrenadorViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(PerfilEntrenadorUiState())
    val uiState: StateFlow<PerfilEntrenadorUiState> = _uiState.asStateFlow()

    fun cargarPerfil(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) } // 🟢 Cambiado a .update
            try {
                val user = userRepository.obtenerUsuario(uid)
                if (user != null) {
                    _uiState.update { it.copy(usuarioLogueado = user, isLoading = false) }
                    if (user.entrenadorId != null) {
                        cargarEntrenadorAsignado(user.entrenadorId)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se encontró el perfil.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun cargarEntrenadorAsignado(entrenadorId: String) {
        viewModelScope.launch {
            try {
                val entrenador = userRepository.obtenerUsuario(entrenadorId)
                _uiState.update { it.copy(entrenadorAsignado = entrenador) }
            } catch (e: Exception) {
                println("🔥 [PerfilEntrenadorViewModel] Error al cargar entrenador asignado: ${e.message}")
                _uiState.update { it.copy(error = e.message ?: "Error al cargar la información del entrenador") }
            }
        }
    }

    fun guardarPerfilEntrenador(uid: String, especialidad: String, biografia: String, certificaciones: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, exitoGuardado = false) }
            val campos = mapOf(
                "especialidad" to especialidad,
                "biografia" to biografia,
                "certificaciones" to certificaciones
            )
            try {
                val exito = userRepository.actualizarPerfilUsuario(uid, campos)
                if (exito) {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            exitoGuardado = true,
                            usuarioLogueado = state.usuarioLogueado?.copy(
                                especialidad = especialidad,
                                biografia = biografia,
                                certificaciones = certificaciones
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSaving = false, error = "Error al actualizar la base de datos.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // 🚀 CORREGIDO: Agregamos el parámetro 'tipoDocumento' a la firma del método
    fun guardarDatosPersonales(
        uid: String,
        nombres: String,
        apellidos: String,
        tipoDocumento: String, // Recibe el tipo
        documento: String,    // Recibe el número
        telefono: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, exitoGuardado = false, error = null) }

            val exito = userRepository.actualizarDatosPersonales(
                uid = uid,
                nombres = nombres,
                apellidos = apellidos,
                tipoDocumento = tipoDocumento,
                documento = documento,
                telefono = telefono
            )

            if (exito) {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        exitoGuardado = true,
                        usuarioLogueado = state.usuarioLogueado?.copy(
                            nombres = nombres,
                            apellidos = apellidos,
                            tipoDocumento = tipoDocumento,
                            numeroDocumento = documento,
                            telefono = telefono
                        )
                    )
                }
            } else {
                _uiState.update { it.copy(isSaving = false, error = "Error al actualizar los datos.") }
            }
        }
    }

    fun resetExito() {
        _uiState.value = _uiState.value.copy(exitoGuardado = false)
    }
}