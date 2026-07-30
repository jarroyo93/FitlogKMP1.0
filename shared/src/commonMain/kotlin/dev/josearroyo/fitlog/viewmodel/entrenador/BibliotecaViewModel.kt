package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Ejercicio
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.data.model.PlantillaRutina
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BibliotecaState(
    val listaCompleta: List<Ejercicio> = emptyList(),
    val listaFiltrada: List<Ejercicio> = emptyList(),
    val grupoSeleccionado: GrupoMuscular? = null,
    val textoBusqueda: String = "",
    val isLoading: Boolean = false,
    val listaPlantillas: List<PlantillaRutina> = emptyList(),
    val isLoadingPlantillas: Boolean = false,
    val tabSeleccionado: Int = 0,
    val error: String? = null // 🟢 Exponemos los errores de red a la UI
)

class BibliotecaViewModel : ViewModel() {
    private val repository = ExerciseRepository()

    private val _state = MutableStateFlow(BibliotecaState())
    val state = _state.asStateFlow()

    // ==========================================
    // 🚀 CARGA DE DATOS MULTIPLATAFORMA
    // ==========================================

    fun cargarBiblioteca(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val ejercicios = repository.obtenerBibliotecaCompleta(entrenadorId)

                // ⚡ OPTIMIZACIÓN: Calculamos y emitimos todo en un solo ciclo atómico (cero parpadeos)
                _state.update { currentState ->
                    val resultadoFiltrado = ejercicios.filter { ejercicio ->
                        val coincideNombre = ejercicio.nombre.contains(currentState.textoBusqueda, ignoreCase = true)
                        val coincideGrupo = currentState.grupoSeleccionado == null || ejercicio.grupoMuscular == currentState.grupoSeleccionado
                        coincideNombre && coincideGrupo
                    }
                    currentState.copy(
                        listaCompleta = ejercicios,
                        listaFiltrada = resultadoFiltrado,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar la biblioteca") }
            }
        }
    }

    fun cargarPlantillas(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlantillas = true, error = null) }
            try {
                val plantillas = repository.obtenerPlantillasDelEntrenador(entrenadorId)
                _state.update { it.copy(listaPlantillas = plantillas, isLoadingPlantillas = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingPlantillas = false, error = e.message ?: "Error al cargar las plantillas") }
            }
        }
    }

    // ==========================================
    // 🔍 FILTROS REACTIVOS EN MEMORIA
    // ==========================================

    fun filtrarEjercicios(texto: String, grupo: GrupoMuscular?) {
        aplicarFiltros(texto, grupo)
    }

    private fun aplicarFiltros(nuevoTexto: String, nuevoGrupo: GrupoMuscular?) {
        _state.update { currentState ->
            val resultado = currentState.listaCompleta.filter { ejercicio ->
                val coincideNombre = ejercicio.nombre.contains(nuevoTexto, ignoreCase = true)
                val coincideGrupo = nuevoGrupo == null || ejercicio.grupoMuscular == nuevoGrupo
                coincideNombre && coincideGrupo
            }
            currentState.copy(
                listaFiltrada = resultado,
                textoBusqueda = nuevoTexto,
                grupoSeleccionado = nuevoGrupo
            )
        }
    }

    fun cambiarPestana(nuevoIndex: Int) {
        _state.update { it.copy(tabSeleccionado = nuevoIndex) }
    }

    // ==========================================
    // 🛡️ BORRADO LÓGICO (SOFT DELETE) MULTIPLATAFORMA
    // ==========================================

    fun eliminarEjercicioPersonalizado(ejercicioId: String, entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {

                repository.actualizarEjercicioPersonalizado(ejercicioId, mapOf("activo" to false))
                cargarBiblioteca(entrenadorId)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "No se pudo ocultar el ejercicio") }
            }
        }
    }

    fun eliminarPlantilla(plantillaId: String, entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPlantillas = true, error = null) }
            try {
                repository.eliminarPlantillaFisica(plantillaId)
                cargarPlantillas(entrenadorId)
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingPlantillas = false, error = e.message ?: "No se pudo eliminar la plantilla") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}