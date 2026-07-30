package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddPlantillaState(
    val nombrePlantilla: String = "",
    val ejerciciosEnCarrito: List<ElementoRutina> = emptyList(),
    val bibliotecaDisponible: List<Ejercicio> = emptyList(),
    val isLoading: Boolean = false, // Agregado para feedback visual 🟢
    val isGuardado: Boolean = false,
    val error: String? = null        // Agregado para fallos de red 🟢
)

class AddPlantillaViewModel(
    private val repository: ExerciseRepository = ExerciseRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AddPlantillaState())
    val state = _state.asStateFlow()

    private var plantillaIdActual: String? = null

    fun cargarBiblioteca(entrenadorId: String) {
        viewModelScope.launch {
            try {
                val lista = repository.obtenerBibliotecaCompleta(entrenadorId)
                _state.update { it.copy(bibliotecaDisponible = lista) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo cargar la biblioteca de ejercicios.") }
            }
        }
    }

    fun cargarPlantillaSiExiste(plantillaId: String?, entrenadorId: String) {
        cargarBiblioteca(entrenadorId)
        if (plantillaId == null) return

        plantillaIdActual = plantillaId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val existente = repository.obtenerPlantillaPorId(plantillaId)
                if (existente != null) {
                    _state.update {
                        it.copy(
                            nombrePlantilla = existente.nombre,
                            ejerciciosEnCarrito = existente.ejercicios,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al descargar la plantilla.") }
            }
        }
    }

    fun actualizarNombre(nuevoNombre: String) { _state.update { it.copy(nombrePlantilla = nuevoNombre) } }

    fun agregarEjercicioAlCarrito(ejercicio: Ejercicio) {
        val nuevo = ElementoRutina(
            ejercicioId = ejercicio.id,
            nombreEjercicio = ejercicio.nombre,
            seriesPrescritas = listOf(PrescripcionSerie(numeroSerie = 1, repeticiones = 10, tipo = TipoSerie.EFECTIVA)),
            descansoSegundos = 60
        )
        _state.update { it.copy(ejerciciosEnCarrito = it.ejerciciosEnCarrito + nuevo) }
    }

    fun eliminarEjercicio(index: Int) {
        _state.update { s -> s.copy(ejerciciosEnCarrito = s.ejerciciosEnCarrito.toMutableList().apply { removeAt(index) }) }
    }

    fun moverEjercicio(index: Int, direccion: Int) {
        _state.update { s ->
            val lista = s.ejerciciosEnCarrito.toMutableList()
            val destino = index + direccion
            if (destino in lista.indices) {
                val temp = lista[index]
                lista[index] = lista[destino]
                lista[destino] = temp
            }
            s.copy(ejerciciosEnCarrito = lista)
        }
    }

    fun actualizarElemento(index: Int, elementoModificado: ElementoRutina) {
        _state.update { s ->
            val lista = s.ejerciciosEnCarrito.toMutableList()
            lista[index] = elementoModificado
            s.copy(ejerciciosEnCarrito = lista)
        }
    }

    fun guardarPlantilla(entrenadorId: String) {
        val currentState = _state.value
        if (currentState.nombrePlantilla.isBlank() || currentState.ejerciciosEnCarrito.isEmpty()) {
            _state.update { it.copy(error = "Completa el nombre y añade al menos un ejercicio.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val nuevaPlantilla = PlantillaRutina(
                nombre = currentState.nombrePlantilla,
                entrenadorId = entrenadorId,
                ejercicios = currentState.ejerciciosEnCarrito,
                activo = true
            )
            try {
                if (plantillaIdActual != null) {
                    repository.actualizarPlantilla(plantillaIdActual!!, mapOf("nombre" to nuevaPlantilla.nombre, "ejercicios" to nuevaPlantilla.ejercicios))
                } else {
                    repository.guardarPlantillaRutina(nuevaPlantilla)
                }
                _state.update { it.copy(isLoading = false, isGuardado = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar la plantilla") }
            }
        }
    }
}