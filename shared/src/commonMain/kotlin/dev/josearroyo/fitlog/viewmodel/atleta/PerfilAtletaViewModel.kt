package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilAtletaState(
    val usuarioLogueado: Usuario? = null,
    val entrenadorAsignado: Usuario? = null,
    val rutinaActiva: RutinaAsignada? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false
)

class PerfilAtletaViewModel : ViewModel() {
    private val atletaRepository = AtletaRepository()
    private val userRepository = UserRepository()
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(PerfilAtletaState())
    val uiState: StateFlow<PerfilAtletaState> = _uiState.asStateFlow()

    fun cargarPerfil(atletaId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try { // 🟢 Try-catch global de lectura
                val atleta = atletaRepository.obtenerUsuario(atletaId)

                if (atleta != null) {
                    // 🚀 Concurrencia paralela para acelerar la carga del perfil
                    kotlinx.coroutines.supervisorScope {
                        val coachDeferred = async {
                            if (!atleta.entrenadorId.isNullOrBlank()) {
                                userRepository.obtenerUsuario(atleta.entrenadorId)
                            } else null
                        }
                        val rutinasDeferred = async { atletaRepository.obtenerRutinasActivas(atletaId) }

                        val coach = coachDeferred.await()
                        val rutinas = rutinasDeferred.await()
                        val rutinaActiva = rutinas.firstOrNull { it.estaActiva }

                        _uiState.update { state ->
                            state.copy(
                                usuarioLogueado = atleta,
                                entrenadorAsignado = coach,
                                rutinaActiva = rutinaActiva,
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el perfil.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    // 🚀 NUEVA FUNCIÓN: Permite actualizar todos los datos desde la pantalla unificada
    fun actualizarDatosAtleta(
        uid: String,
        nombres: String,
        apellidos: String,
        tipoDocumento: String,
        numeroDocumento: String,
        telefono: String,
        fechaNacimiento: Long?,
        tipoSangre: String?,
        nacionalidad: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val campos = mutableMapOf<String, Any>(
                    "nombres" to nombres.trim(),
                    "apellidos" to apellidos.trim(),
                    "tipoDocumento" to tipoDocumento.trim(),
                    "numeroDocumento" to numeroDocumento.trim(),
                    "telefono" to telefono.trim()
                )

                fechaNacimiento?.let { campos["fechaNacimiento"] = it }
                tipoSangre?.let { campos["tipoSangre"] = it.trim() }
                nacionalidad?.let { campos["nacionalidad"] = it.trim() }

                val exitoFirestore = userRepository.actualizarPerfilUsuario(uid, campos)
                if (!exitoFirestore) {
                    throw Exception("No se pudieron guardar los cambios en el servidor.")
                }

                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        error = null,
                        guardadoExitoso = true,
                        usuarioLogueado = state.usuarioLogueado?.copy(
                            nombres = nombres.trim(),
                            apellidos = apellidos.trim(),
                            tipoDocumento = tipoDocumento.trim(),
                            numeroDocumento = numeroDocumento.trim(),
                            telefono = telefono.trim(),
                            // 🚀 CORREGIDO: Si es nulo, mantiene el que ya tenía en el estado, si no, usa 0L
                            fechaNacimiento = fechaNacimiento ?: state.usuarioLogueado?.fechaNacimiento ?: 0L,
                            tipoSangre = tipoSangre ?: state.usuarioLogueado?.tipoSangre ?: "",
                            nacionalidad = nacionalidad ?: state.usuarioLogueado?.nacionalidad ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Ocurrió un error inesperado.")
                }
            }
        }
    }

    // 🚀 NUEVA FUNCIÓN: Restablece el flag de guardado para evitar pops infinitos en la UI
    fun resetExito() {
        _uiState.update { it.copy(guardadoExitoso = false) }
    }

    fun guardarPerfilAtleta(uid: String, nombres: String, apellidos: String, nuevoCorreo: String, nacionalidad: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val correoLimpio = nuevoCorreo.trim()
                val correoActual = _uiState.value.usuarioLogueado?.correo ?: ""

                if (correoLimpio != correoActual) {
                    val correoDuplicado = userRepository.existeCorreo(correoLimpio)
                    if (correoDuplicado) {
                        _uiState.update {
                            it.copy(isSaving = false, error = "El correo electrónico ya se encuentra registrado por otro usuario.")
                        }
                        return@launch
                    }
                }

                val userAuth = auth.currentUser
                if (userAuth != null && userAuth.email != correoLimpio) {
                    try {
                        userAuth.updateEmail(correoLimpio)
                    } catch (e: Exception) {
                        if (e.message?.contains("RECENT_LOGIN_REQUIRED", ignoreCase = true) == true ||
                            e.message?.contains("requires recent authentication", ignoreCase = true) == true) {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    error = "Por seguridad, cambiar tu correo requiere que hayas iniciado sesión recientemente. Cierra sesión e ingresa de nuevo."
                                )
                            }
                            return@launch
                        }
                        throw e
                    }
                }

                val campos = mapOf(
                    "nombres" to nombres.trim(),
                    "apellidos" to apellidos.trim(),
                    "correo" to correoLimpio,
                    "nacionalidad" to nacionalidad.trim()
                )

                val exitoFirestore = userRepository.actualizarPerfilUsuario(uid, campos)
                if (!exitoFirestore) {
                    throw Exception("No se pudieron guardar los cambios en el servidor. Inténtalo de nuevo.")
                }

                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        error = null,
                        guardadoExitoso = true,
                        usuarioLogueado = state.usuarioLogueado?.copy(
                            nombres = nombres.trim(),
                            apellidos = apellidos.trim(),
                            correo = correoLimpio,
                            nacionalidad = nacionalidad.trim()
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Ocurrió un error inesperado.")
                }
            }
        }
    }
}