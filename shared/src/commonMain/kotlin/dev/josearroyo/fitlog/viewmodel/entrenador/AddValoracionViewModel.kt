package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.repository.AtletaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddValoracionState(
    val valoracion: ValoracionFisica = ValoracionFisica(),
    val isGuardado: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null // Agregado 🟢
)

class AddValoracionViewModel(
    private val repository: AtletaRepository = AtletaRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AddValoracionState())
    val state = _state.asStateFlow()

    fun actualizarValoracion(nueva: ValoracionFisica) {
        _state.update { it.copy(valoracion = nueva) }
    }

    fun guardar(atletaId: String) {
        val actual = _state.value.valoracion
        if (actual.pesoKg <= 0.0 || actual.alturaCm <= 0.0 || actual.objetivoInicial.isBlank()) {
            _state.update { it.copy(error = "Por favor, completa correctamente el peso, altura y objetivo.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val exito = repository.guardarValoracion(atletaId, _state.value.valoracion)
                if (exito) {
                    _state.update { it.copy(isGuardado = true) }
                } else {
                    _state.update { it.copy(error = "No se pudieron almacenar los datos en el servidor.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Ocurrió un error inesperado de conexión.") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}