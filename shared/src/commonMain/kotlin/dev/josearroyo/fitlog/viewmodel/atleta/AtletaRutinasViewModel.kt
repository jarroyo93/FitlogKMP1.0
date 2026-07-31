package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AtletaRutinasState(
    val isLoading: Boolean = true,
    val rutinas: List<RutinaAsignada> = emptyList(),
    val error: String? = null
)

class AtletaRutinasViewModel : ViewModel() {
    private val atletaRepository = AtletaRepository()
    private val userRepository = UserRepository()

    private val _state = MutableStateFlow(AtletaRutinasState())
    val state: StateFlow<AtletaRutinasState> = _state.asStateFlow()

    fun cargarRutinas(authUid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val usuario = userRepository.obtenerUsuario(authUid)

                if (usuario == null) {
                    _state.update { it.copy(isLoading = false, error = "Usuario no encontrado en la base de datos.") }
                    return@launch
                }

                val listaRutinas = atletaRepository.obtenerRutinasActivas(usuario.id)
                _state.update { it.copy(isLoading = false, rutinas = listaRutinas) }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}