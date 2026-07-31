package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.repository.AtletaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddHabitosState(
    val habitos: Habitos = Habitos(),
    val isGuardado: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Inyección por constructor recomendada para desacoplar FitLog 🛠️
class AddHabitosViewModel(
    private val repository: AtletaRepository = AtletaRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(AddHabitosState())
    val state = _state.asStateFlow()

    fun actualizarHabitos(nuevo: Habitos) {
        _state.update { it.copy(habitos = nuevo) }
    }

    fun guardar(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val exito = repository.guardarHabitos(atletaId, _state.value.habitos)
                if (exito) {
                    _state.update { it.copy(isLoading = false, isGuardado = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Error al guardar los datos.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }
}

data class HistorialHabitosState(
    val lista: List<Habitos> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null // Añadido para avisar fallos en la UI 🟢
)

class HistorialHabitosViewModel(
    private val repository: AtletaRepository = AtletaRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(HistorialHabitosState())
    val state = _state.asStateFlow()

    fun cargarHistorial(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val lista = repository.obtenerHistorialHabitos(atletaId)
                _state.update { it.copy(lista = lista, isLoading = false) }
            } catch (e: Exception) {
                // Evitamos que la pantalla se quede cargando infinitamente 🟢
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar historial") }
            }
        }
    }
}