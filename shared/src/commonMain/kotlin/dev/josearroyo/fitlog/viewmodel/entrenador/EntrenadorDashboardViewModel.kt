package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.SemaforoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SemaforoDashboardUiState(
    val isLoading: Boolean = true,
    val resumen: ResumenGerencial = ResumenGerencial(0, 0, 0, 0),
    val atletasTotales: List<AtletaSemaforoItem> = emptyList(),
    val atletasFiltrados: List<AtletaSemaforoItem> = emptyList(),
    val filtroEstado: EstadoSemaforo? = null,
    val error: String? = null
)

class EntrenadorDashboardViewModel(
    private val semaforoRepository: SemaforoRepository = SemaforoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SemaforoDashboardUiState())
    val uiState: StateFlow<SemaforoDashboardUiState> = _uiState.asStateFlow()

    fun cargarDashboard(entrenadorId: String, forzarRecarga: Boolean = false) {
        // ⚡ CACHÉ EN MEMORIA: Evita re-consultar a la red si la lista ya existe y no se forzó recarga
        if (!forzarRecarga && _uiState.value.atletasTotales.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val atletas = semaforoRepository.obtenerEvaluacionAtletas(entrenadorId)
                val resumen = semaforoRepository.calcularResumenGerencial(atletas)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resumen = resumen,
                        atletasTotales = atletas,
                        atletasFiltrados = aplicarFiltro(atletas, it.filtroEstado)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error al cargar dashboard")
                }
            }
        }
    }

    fun filtrarPorEstado(estado: EstadoSemaforo?) {
        _uiState.update { estadoActual ->
            val nuevoFiltro = if (estadoActual.filtroEstado == estado) null else estado
            estadoActual.copy(
                filtroEstado = nuevoFiltro,
                atletasFiltrados = aplicarFiltro(estadoActual.atletasTotales, nuevoFiltro)
            )
        }
    }

    private fun aplicarFiltro(
        lista: List<AtletaSemaforoItem>,
        filtro: EstadoSemaforo?
    ): List<AtletaSemaforoItem> {
        return if (filtro == null) lista else lista.filter { it.estado == filtro }
    }
}