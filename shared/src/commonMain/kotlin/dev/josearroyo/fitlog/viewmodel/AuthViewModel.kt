package dev.josearroyo.fitlog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.ui.util.UserPreferencesManager // 👈 IMPORTANTE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(
        val uid: String,
        val rol: RolUsuario,
        val requiereCambioContrasena: Boolean
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class ActivationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _activationState = MutableStateFlow(ActivationState())
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    // 🟢 Carga el último correo guardado al iniciar la pantalla
    fun obtenerUltimoCorreo(): String {
        return UserPreferencesManager.obtenerUltimoCorreo()
    }

    fun login(email: String, clave: String) {
        if (email.isBlank() || clave.isBlank()) {
            _authState.update { AuthState.Error("El correo y la contraseña son obligatorios") }
            return
        }
        _authState.update { AuthState.Loading }

        viewModelScope.launch {
            try {
                val uid = authRepository.login(email, clave)
                val usuario = userRepository.obtenerUsuario(uid)

                if (usuario != null) {
                    // 🟢 Guardar correo en preferencias locales del SO al hacer login exitoso
                    UserPreferencesManager.guardarUltimoCorreo(email)

                    _authState.update {
                        AuthState.Success(
                            uid = uid,
                            rol = usuario.rol,
                            requiereCambioContrasena = usuario.requiereCambioContrasena
                        )
                    }
                } else {
                    _authState.update { AuthState.Error("Usuario autenticado, pero sin perfil en la base de datos") }
                }
            } catch (e: Exception) {
                _authState.update { AuthState.Error(e.message ?: "Error al iniciar sesión") }
            }
        }
    }

    fun resetState() {
        _authState.update { AuthState.Idle }
    }

    fun actualizarContrasenaPrimeraVez(uid: String, contrasena: String) {
        viewModelScope.launch {
            _activationState.update { it.copy(isLoading = true, error = null) }

            authRepository.cambiarContrasenaPrimeraVez(uid, contrasena)
                .onSuccess {
                    userRepository.actualizarPerfilUsuario(uid, mapOf("requiereCambioContrasena" to false))
                    _activationState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { exception ->
                    _activationState.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }

    fun resetActivationState() {
        _activationState.update { ActivationState() }
    }
}