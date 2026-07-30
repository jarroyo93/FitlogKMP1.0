package dev.josearroyo.fitlog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de Login actualizado con la bandera de primer ingreso 🟢
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

// Estado separado para la pantalla de Activación / Primer Ingreso
data class ActivationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // --- Flujo de Login ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // --- Flujo de Activación ---
    private val _activationState = MutableStateFlow(ActivationState())
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    // ==========================================
    // LÓGICA DE LOGIN MULTIPLATAFORMA
    // ==========================================
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

    // ==========================================
    // LÓGICA DE PRIMER INGRESO (CAMBIO DE CONTRASEÑA)
    // ==========================================
    fun actualizarContrasenaPrimeraVez(uid: String, contrasena: String) {
        viewModelScope.launch {
            _activationState.update { it.copy(isLoading = true, error = null) }

            authRepository.cambiarContrasenaPrimeraVez(uid, contrasena)
                .onSuccess {
                    // Actualizamos en Firestore para que no vuelva a pedir el cambio en futuros logins 🟢
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