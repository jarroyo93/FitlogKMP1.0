package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Ejercicio
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AddEjercicioState(
    val nombre: String = "",
    val grupoMuscular: GrupoMuscular = GrupoMuscular.PECHO,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AddEjercicioViewModel(
    private val repository: ExerciseRepository = ExerciseRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AddEjercicioState())
    val state: StateFlow<AddEjercicioState> = _state.asStateFlow()

    private var ejercicioIdActual: String? = null

    fun actualizarNombre(nuevoNombre: String) {
        _state.update { it.copy(nombre = nuevoNombre) }
    }

    fun actualizarGrupo(nuevoGrupo: GrupoMuscular) {
        _state.update { it.copy(grupoMuscular = nuevoGrupo) }
    }

    fun cargarEjercicioSiExiste(ejercicioId: String?) {
        if (ejercicioId == null || ejercicioId == "null") return

        ejercicioIdActual = ejercicioId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val ejercicioExistente = repository.obtenerEjercicioPorId(ejercicioId)
                if (ejercicioExistente != null) {
                    _state.update {
                        it.copy(
                            nombre = ejercicioExistente.nombre,
                            grupoMuscular = ejercicioExistente.grupoMuscular,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al cargar el ejercicio") }
            }
        }
    }

    fun guardarEjercicio(entrenadorId: String) {
        val currentState = _state.value
        if (currentState.nombre.isBlank()) {
            _state.update { it.copy(error = "El nombre del ejercicio no puede estar vacío.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                if (ejercicioIdActual != null) {
                    val datosActualizados = mapOf(
                        "nombre" to currentState.nombre,
                        "grupoMuscular" to currentState.grupoMuscular.name
                    )
                    repository.actualizarEjercicioPersonalizado(ejercicioIdActual!!, datosActualizados)
                } else {
                    val nuevoEjercicio = Ejercicio(
                        nombre = currentState.nombre,
                        grupoMuscular = currentState.grupoMuscular,
                        esPersonalizado = true,
                        creadorId = entrenadorId,
                        activo = true
                    )
                    repository.guardarEjercicioPersonalizado(nuevoEjercicio)
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar el ejercicio") }
            }
        }
    }
}