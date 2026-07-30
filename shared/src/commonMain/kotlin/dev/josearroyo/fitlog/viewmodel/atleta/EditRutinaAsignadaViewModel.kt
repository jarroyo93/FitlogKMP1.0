package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class EditRutinaState(
    val rutina: RutinaAsignada? = null,
    val bibliotecaEjercicios: List<Ejercicio> = emptyList(),
    val plantillasDisponibles: List<PlantillaRutina> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

class EditRutinaAsignadaViewModel : ViewModel() {
    private val repository = AtletaRepository()
    private val exerciseRepository = ExerciseRepository()
    private val progresoRepository = AtletaProgresoRepository()

    private val _state = MutableStateFlow(EditRutinaState())
    val state = _state.asStateFlow()

    fun cargarRutinaYBiblioteca(atletaId: String, rutinaId: String, entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val rut = repository.obtenerRutinaAsignada(atletaId, rutinaId)
                val listaEjercicios = exerciseRepository.obtenerBibliotecaCompleta(entrenadorId)
                val listaPlantillas = exerciseRepository.obtenerPlantillasDelEntrenador(entrenadorId)

                _state.update {
                    it.copy(
                        rutina = rut,
                        bibliotecaEjercicios = listaEjercicios,
                        plantillasDisponibles = listaPlantillas,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }


    fun eliminarDia(diaIndex: Int) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            if (diaIndex !in dias.indices) return@update state

            dias.removeAt(diaIndex)
            val reorganizados = dias.mapIndexed { index, dia -> dia.copy(ordenSecuencia = index + 1) }
            state.copy(rutina = actual.copy(diasEntrenamiento = reorganizados))
        }
    }


    fun moverDia(diaIndex: Int, direccion: Int) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            val nuevoIndex = diaIndex + direccion


            if (diaIndex in dias.indices && nuevoIndex in dias.indices) {
                val temp = dias[diaIndex]
                dias[diaIndex] = dias[nuevoIndex]
                dias[nuevoIndex] = temp
                val reorganizados = dias.mapIndexed { index, dia -> dia.copy(ordenSecuencia = index + 1) }
                state.copy(rutina = actual.copy(diasEntrenamiento = reorganizados))
            } else state
        }
    }

    fun eliminarEjercicio(diaIndex: Int, ejercicioIndex: Int) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            if (diaIndex !in dias.indices) return@update state

            val dia = dias[diaIndex]
            val ejercicios = dia.ejercicios.sortedBy { it.ordenSecuencia }.toMutableList()
            if (ejercicioIndex !in ejercicios.indices) return@update state

            ejercicios.removeAt(ejercicioIndex)
            val ejReorganizados = ejercicios.mapIndexed { index, ej -> ej.copy(ordenSecuencia = index + 1) }
            dias[diaIndex] = dia.copy(ejercicios = ejReorganizados)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias))
        }
    }


    fun moverEjercicio(diaIndex: Int, ejercicioIndex: Int, direccion: Int) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            if (diaIndex !in dias.indices) return@update state

            val dia = dias[diaIndex]
            val ejercicios = dia.ejercicios.sortedBy { it.ordenSecuencia }.toMutableList()
            val nuevoIndex = ejercicioIndex + direccion

            if (ejercicioIndex in ejercicios.indices && nuevoIndex in ejercicios.indices) {
                val temp = ejercicios[ejercicioIndex]
                ejercicios[ejercicioIndex] = ejercicios[nuevoIndex]
                ejercicios[nuevoIndex] = temp
                val ejReorganizados = ejercicios.mapIndexed { index, ej -> ej.copy(ordenSecuencia = index + 1) }
                dias[diaIndex] = dia.copy(ejercicios = ejReorganizados)
                state.copy(rutina = actual.copy(diasEntrenamiento = dias))
            } else state
        }
    }

    fun actualizarEjercicio(diaIndex: Int, ejercicioIndex: Int, actualizado: EjercicioAsignado) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            val dia = dias[diaIndex]
            val ejercicios = dia.ejercicios.sortedBy { it.ordenSecuencia }.toMutableList()
            ejercicios[ejercicioIndex] = actualizado
            dias[diaIndex] = dia.copy(ejercicios = ejercicios)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias))
        }
    }

    fun agregarEjercicioDesdeBiblioteca(diaIndex: Int, ejercicioGlobal: Ejercicio) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            val dia = dias[diaIndex]
            val nuevoEjercicio = EjercicioAsignado(
                idInterno = Uuid.random().toString(),
                ejercicioGlobalId = ejercicioGlobal.id,
                nombre = ejercicioGlobal.nombre,
                seriesPrescritas = listOf(PrescripcionSerie(numeroSerie = 1, repeticiones = 10, tipo = TipoSerie.EFECTIVA)),
                descansoSegundos = 60,
                ordenSecuencia = dia.ejercicios.size + 1
            )
            dias[diaIndex] = dia.copy(ejercicios = dia.ejercicios + nuevoEjercicio)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias))
        }
    }

    fun actualizarNombreONotas(nombre: String, notas: String) {
        _state.update { it.copy(rutina = it.rutina?.copy(nombreRutina = nombre, notasEntrenador = notas)) }
    }

    // --- GUARDAR Y BORRAR RUTINA ---

    fun guardarCambios(atletaId: String) {
        val actual = _state.value.rutina ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val exito = repository.actualizarRutinaAsignada(atletaId, actual)

            if (exito) {
                val nuevaMetaSesiones = actual.diasEntrenamiento.size
                var nuevasRepsMetaTotal = 0
                actual.diasEntrenamiento.forEach { dia ->
                    dia.ejercicios.forEach { ejercicio ->
                        ejercicio.seriesPrescritas.forEach { serie ->
                            if (serie.tipo != TipoSerie.APROXIMACION) {
                                nuevasRepsMetaTotal += serie.repeticiones
                            }
                        }
                    }
                }
                progresoRepository.actualizarMetaCicloActivo(atletaId, nuevaMetaSesiones, nuevasRepsMetaTotal)
            }
            _state.update { it.copy(isSaved = exito, isLoading = false) }
        }
    }

    fun eliminarRutinaCompleta(atletaId: String) {
        val actual = _state.value.rutina ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val exito = repository.eliminarRutinaAsignada(atletaId, actual.id)
            if (exito) {
                progresoRepository.forzarCierreCicloActivo(atletaId)
            }
            _state.update { it.copy(isDeleted = exito, isLoading = false) }
        }
    }

    fun agregarDiaDesdePlantilla(plantilla: PlantillaRutina) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val diasActuales = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            val nuevoOrdenSecuencia = if (diasActuales.isEmpty()) 1 else diasActuales.maxOf { it.ordenSecuencia } + 1

            val ejerciciosDelDia = plantilla.ejercicios.mapIndexed { indexEj, ej ->
                EjercicioAsignado(
                    idInterno = Uuid.random().toString(), // 🚀 ID Multiplataforma
                    ejercicioGlobalId = ej.ejercicioId,
                    nombre = ej.nombreEjercicio,
                    seriesPrescritas = ej.seriesPrescritas,
                    descansoSegundos = ej.descansoSegundos,
                    notasEspecificas = ej.notas,
                    ordenSecuencia = indexEj + 1
                )
            }

            val nuevoDia = DiaEntrenamientoAsignado(
                idDia = Uuid.random().toString(), // 🚀 ID Multiplataforma
                plantillaOriginalId = plantilla.id,
                nombreDia = plantilla.nombre,
                ordenSecuencia = nuevoOrdenSecuencia,
                ejercicios = ejerciciosDelDia
            )

            diasActuales.add(nuevoDia)
            state.copy(rutina = actual.copy(diasEntrenamiento = diasActuales))
        }
    }
}