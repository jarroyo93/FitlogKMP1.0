package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.DiaEntrenamientoAsignado
import dev.josearroyo.fitlog.data.model.EjercicioAsignado
import dev.josearroyo.fitlog.data.model.PlantillaRutina
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AsignarRutinaState(
    val plantillas: List<PlantillaRutina> = emptyList(),
    val plantillasSeleccionadas: List<PlantillaRutina> = emptyList(),
    val nombreRutina: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalUuidApi::class)
class AsignarRutinaViewModel(
    private val exerciseRepo: ExerciseRepository = ExerciseRepository(),
    private val atletaRepo: AtletaRepository = AtletaRepository(),
    private val progresoRepo: AtletaProgresoRepository = AtletaProgresoRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AsignarRutinaState())
    val state: StateFlow<AsignarRutinaState> = _state.asStateFlow()

    fun cargarBiblioteca(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val lista = exerciseRepo.obtenerPlantillasDelEntrenador(entrenadorId)
            _state.update { it.copy(plantillas = lista, isLoading = false) }
        }
    }

    fun actualizarNombreRutina(nombre: String) {
        _state.update { it.copy(nombreRutina = nombre) }
    }

    fun agregarPlantilla(plantilla: PlantillaRutina) {
        _state.update { currentState ->
            val actual = currentState.plantillasSeleccionadas.toMutableList()
            actual.add(plantilla)
            currentState.copy(plantillasSeleccionadas = actual)
        }
    }

    fun removerPlantillaSeleccionada(index: Int) {
        _state.update { currentState ->
            val actual = currentState.plantillasSeleccionadas.toMutableList()
            if (index in actual.indices) actual.removeAt(index)
            currentState.copy(plantillasSeleccionadas = actual)
        }
    }

    fun moverPlantillaSeleccionada(index: Int, direccion: Int) {
        _state.update { currentState ->
            val actual = currentState.plantillasSeleccionadas.toMutableList()
            val nuevoIndex = index + direccion
            if (nuevoIndex in actual.indices) {
                val temp = actual[index]
                actual[index] = actual[nuevoIndex]
                actual[nuevoIndex] = temp
            }
            currentState.copy(plantillasSeleccionadas = actual)
        }
    }

    fun construirYAsignarRutina(atletaId: String) {
        val currentState = _state.value
        if (currentState.nombreRutina.isBlank() || currentState.plantillasSeleccionadas.isEmpty()) {
            _state.update { it.copy(error = "Debe asignar un nombre y agregar al menos un día al programa.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val diasGenerados = currentState.plantillasSeleccionadas.mapIndexed { indexDia, plantilla ->
                    val ejerciciosDelDia = plantilla.ejercicios.mapIndexed { indexEj, ej ->
                        EjercicioAsignado(
                            idInterno = Uuid.random().toString(),
                            ejercicioGlobalId = ej.ejercicioId,
                            nombre = ej.nombreEjercicio,
                            seriesPrescritas = ej.seriesPrescritas,
                            descansoSegundos = ej.descansoSegundos,
                            notasEspecificas = ej.notas,
                            ordenSecuencia = indexEj + 1
                        )
                    }

                    DiaEntrenamientoAsignado(
                        idDia = Uuid.random().toString(),
                        plantillaOriginalId = plantilla.id,
                        nombreDia = plantilla.nombre,
                        ordenSecuencia = indexDia + 1,
                        ejercicios = ejerciciosDelDia
                    )
                }

                val nuevaRutina = RutinaAsignada(
                    nombreRutina = currentState.nombreRutina,
                    fechaAsignacion = getCurrentTimeMillis(),
                    estaActiva = true,
                    diasEntrenamiento = diasGenerados
                )

                val exito = atletaRepo.asignarRutina(atletaId, nuevaRutina)

                if (exito) {
                    progresoRepo.forzarCierreCicloActivo(atletaId)
                }

                _state.update { it.copy(isLoading = false, isSuccess = exito) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Fallo: ${e.message}") }
            }
        }
    }
}