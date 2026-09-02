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
    val planesActivosContador: Int = 0,      // 🟢 En curso actualmente
    val planesDiferidosContador: Int = 0,    // 🟢 Encolados / Futuros
    val planesCompletadosContador: Int = 0,  // 🟢 Ya finalizados
    val planesCanceladosContador: Int = 0,   // 🟢 Anulados
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

                // Desglose preciso según el estado real de cada PeriodoFacturable
                val activos = lista.count { it.estado == "ACTIVO" }
                val diferidos = lista.count { it.estado == "DIFERIDO" }
                val completados = lista.count { it.estado == "COMPLETADO" }
                val cancelados = lista.count { it.estado == "CANCELADO" }

                _state.update { it.copy(
                    isLoading = false,
                    registros = lista,
                    planesActivosContador = activos,
                    planesDiferidosContador = diferidos,
                    planesCompletadosContador = completados,
                    planesCanceladosContador = cancelados
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}