package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.repository.AtletaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorialState(
    val lista: List<ValoracionFisica> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HistorialValoracionViewModel : ViewModel() {
    private val repository = AtletaRepository()
    private val _state = MutableStateFlow(HistorialState())
    val state = _state.asStateFlow()

    fun cargarHistorial(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val lista = repository.obtenerHistorialValoraciones(atletaId)
                _state.update { it.copy(lista = lista, isLoading = false) }
            } catch (e: Exception) {
                println("🔥 [HistorialValoracionViewModel] Error al cargar historial: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message ?: "No se pudo cargar el historial") }
            }
        }
    }
}