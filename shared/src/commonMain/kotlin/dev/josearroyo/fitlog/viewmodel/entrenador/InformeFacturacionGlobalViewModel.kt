package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.RegistroContable
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InformeGlobalState(
    val isLoading: Boolean = true,
    val registros: List<RegistroContable> = emptyList(),
    val planesActivosContador: Int = 0,
    val planesCanceladosContador: Int = 0,
    val error: String? = null
)

class InformeFacturacionGlobalViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _state = MutableStateFlow(InformeGlobalState())
    val state = _state.asStateFlow()

    fun cargarInformeGlobal(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val lista = userRepository.obtenerInformeFacturacionEntrenador(entrenadorId)

                val activos = lista.count { it.estado == "ACTIVO" || it.estado == "DIFERIDO" }
                val cancelados = lista.count { it.estado == "CANCELADO" }

                _state.update { it.copy(
                    isLoading = false,
                    registros = lista,
                    planesActivosContador = activos,
                    planesCanceladosContador = cancelados
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}