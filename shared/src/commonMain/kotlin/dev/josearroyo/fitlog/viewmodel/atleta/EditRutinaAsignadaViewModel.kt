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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class EditRutinaState(
    val rutina: RutinaAsignada? = null,
    val bibliotecaEjercicios: List<Ejercicio> = emptyList(),
    val plantillasDisponibles: List<PlantillaRutina> = emptyList(),
    val duracionTexto: String = "4",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalUuidApi::class)
class EditRutinaAsignadaViewModel : ViewModel() {
    private val repository = AtletaRepository()
    private val exerciseRepository = ExerciseRepository()
    private val progresoRepository = AtletaProgresoRepository()

    private val _state = MutableStateFlow(EditRutinaState())
    val state = _state.asStateFlow()

    fun cargarRutinaYBiblioteca(atletaId: String, rutinaId: String, entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val rut = repository.obtenerRutinaAsignada(atletaId, rutinaId)
                val listaEjercicios = exerciseRepository.obtenerBibliotecaCompleta(entrenadorId)
                val listaPlantillas = exerciseRepository.obtenerPlantillasDelEntrenador(entrenadorId)

                val duracionVisual = if (rut?.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL) {
                    ((rut.duracionDias) / 7).coerceAtLeast(1).toString()
                } else {
                    (rut?.duracionDias ?: 28).toString()
                }

                _state.update {
                    it.copy(
                        rutina = rut,
                        bibliotecaEjercicios = listaEjercicios,
                        plantillasDisponibles = listaPlantillas,
                        duracionTexto = duracionVisual,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun actualizarModoCiclo(nuevoModo: ModoCiclo) {
        _state.update { currentState ->
            val rutinaActual = currentState.rutina ?: return@update currentState
            if (rutinaActual.modoCiclo == nuevoModo) return@update currentState

            val numeroActual = currentState.duracionTexto.toIntOrNull() ?: 1
            val nuevaDuracionVisual = if (nuevoModo == ModoCiclo.CALENDARIO_SEMANAL) {
                (numeroActual / 7).coerceAtLeast(1).toString()
            } else {
                (numeroActual * 7).toString()
            }

            val duracionTotalDias = if (nuevoModo == ModoCiclo.CALENDARIO_SEMANAL) {
                (nuevaDuracionVisual.toIntOrNull() ?: 1) * 7
            } else {
                nuevaDuracionVisual.toIntOrNull() ?: 1
            }

            currentState.copy(
                rutina = rutinaActual.copy(
                    modoCiclo = nuevoModo,
                    duracionDias = duracionTotalDias
                ),
                duracionTexto = nuevaDuracionVisual
            )
        }
    }

    fun actualizarDuracion(duracion: String) {
        if (duracion.all { it.isDigit() }) {
            _state.update { currentState ->
                val rutinaActual = currentState.rutina ?: return@update currentState
                val numeroInt = duracion.toIntOrNull() ?: 0
                val duracionTotalDias = if (rutinaActual.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL) {
                    numeroInt * 7
                } else {
                    numeroInt
                }

                currentState.copy(
                    duracionTexto = duracion,
                    rutina = rutinaActual.copy(duracionDias = duracionTotalDias)
                )
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
            if (diaIndex !in dias.indices) return@update state

            val dia = dias[diaIndex]

            val yaExiste = dia.ejercicios.any { ej ->
                (ej.ejercicioGlobalId.isNotBlank() && ej.ejercicioGlobalId == ejercicioGlobal.id) ||
                        ej.nombre.trim().equals(ejercicioGlobal.nombre.trim(), ignoreCase = true)
            }

            if (yaExiste) {
                return@update state.copy(
                    error = "El ejercicio '${ejercicioGlobal.nombre}' ya está incluido en este día."
                )
            }

            val nuevoEjercicio = EjercicioAsignado(
                idInterno = Uuid.random().toString(),
                ejercicioGlobalId = ejercicioGlobal.id,
                nombre = ejercicioGlobal.nombre,
                seriesPrescritas = listOf(PrescripcionSerie(numeroSerie = 1, repsMin = 8, repsMax = 12, tipo = TipoSerie.EFECTIVA)),
                descansoSegundos = 60,
                ordenSecuencia = dia.ejercicios.size + 1
            )

            dias[diaIndex] = dia.copy(ejercicios = dia.ejercicios + nuevoEjercicio)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias), error = null)
        }
    }

    fun actualizarNombreONotas(nombre: String, notas: String) {
        _state.update { it.copy(rutina = it.rutina?.copy(nombreRutina = nombre.uppercase(), notasEntrenador = notas)) }
    }

    fun guardarCambios(atletaId: String) {
        // 🔴 1. Bloqueo SÍNCRONO contra reentradas
        if (_state.value.isLoading) return

        val actual = _state.value.rutina ?: return
        val duracionNumero = _state.value.duracionTexto.toIntOrNull() ?: 0

        if (duracionNumero <= 0) {
            _state.update { it.copy(error = "Debe ingresar una duración válida para la rutina.") }
            return
        }

        val rutinaFinal = actual.copy(nombreRutina = actual.nombreRutina.trim().uppercase())

        // 🔴 2. Establecer isLoading = true SÍNCRONAMENTE antes de lanzar la corrutina
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val exito = repository.actualizarRutinaAsignada(atletaId, rutinaFinal)

                if (exito) {
                    progresoRepository.sincronizarCicloActivoConRutina(atletaId, rutinaFinal)
                }
                _state.update { it.copy(isSaved = exito, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al actualizar la rutina.") }
            }
        }
    }

    fun eliminarRutinaCompleta(atletaId: String) {
        // 🔴 1. Bloqueo SÍNCRONO contra reentradas
        if (_state.value.isLoading) return

        val actual = _state.value.rutina ?: return

        // 🔴 2. Establecer isLoading = true SÍNCRONAMENTE antes de lanzar la corrutina
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val exito = repository.eliminarRutinaAsignada(atletaId, actual.id)
                if (exito) {
                    progresoRepository.forzarCierreCicloActivo(atletaId)
                }
                _state.update { it.copy(isDeleted = exito, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al eliminar la rutina.") }
            }
        }
    }

    fun agregarDiaDesdePlantilla(plantilla: PlantillaRutina) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val diasActuales = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            val nuevoOrdenSecuencia = if (diasActuales.isEmpty()) 1 else diasActuales.maxOf { it.ordenSecuencia } + 1

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

            val nuevoDia = DiaEntrenamientoAsignado(
                idDia = Uuid.random().toString(),
                plantillaOriginalId = plantilla.id,
                nombreDia = plantilla.nombre,
                ordenSecuencia = nuevoOrdenSecuencia,
                ejercicios = ejerciciosDelDia
            )

            diasActuales.add(nuevoDia)
            state.copy(rutina = actual.copy(diasEntrenamiento = diasActuales))
        }
    }

    // ==========================================
// MÉTODOS DE AGRUPACIÓN (N EJERCIOS)
// ==========================================

    /**
     * Agrupa N ejercicios contiguos en un bloque (Biserie, Triserie, Serie Gigante).
     */
    fun agruparEjercicios(diaIndex: Int, indicesSeleccionados: List<Int>) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            if (diaIndex !in dias.indices) return@update state

            val dia = dias[diaIndex]
            val ejercicios = dia.ejercicios.sortedBy { it.ordenSecuencia }.toMutableList()

            if (indicesSeleccionados.size < 2) {
                return@update state.copy(error = "Debes seleccionar al menos 2 ejercicios para agrupar.")
            }

            val indicesOrdenados = indicesSeleccionados.sorted()
            val esContiguo = indicesOrdenados.zipWithNext().all { (a, b) -> b == a + 1 }
            if (!esContiguo) {
                return@update state.copy(error = "Los ejercicios seleccionados deben ser contiguos en la lista.")
            }

            val nuevoBloqueId = Uuid.random().toString()

            // Asignar temporalmente el bloque id a los seleccionados
            indicesOrdenados.forEach { ejIndex ->
                val ej = ejercicios[ejIndex]
                ejercicios[ejIndex] = ej.copy(bloqueId = nuevoBloqueId)
            }

            val ejerciciosRecalculados = recalcularEtiquetasBloques(ejercicios)
            dias[diaIndex] = dia.copy(ejercicios = ejerciciosRecalculados)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias), error = null)
        }
    }

    /**
     * Elimina la agrupación de un bloque completo dentro de un día.
     */
    fun desagruparBloque(diaIndex: Int, bloqueId: String) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            if (diaIndex !in dias.indices) return@update state

            val dia = dias[diaIndex]
            val ejercicios = dia.ejercicios.map { ej ->
                if (ej.bloqueId == bloqueId) ej.copy(bloqueId = null, bloqueNombre = null) else ej
            }

            val ejerciciosRecalculados = recalcularEtiquetasBloques(ejercicios)
            dias[diaIndex] = dia.copy(ejercicios = ejerciciosRecalculados)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias))
        }
    }

    // Modificación en eliminarEjercicio para limpiar grupos inconsistentes al borrar
    fun eliminarEjercicioConReajuste(diaIndex: Int, ejercicioIndex: Int) {
        _state.update { state ->
            val actual = state.rutina ?: return@update state
            val dias = actual.diasEntrenamiento.sortedBy { it.ordenSecuencia }.toMutableList()
            if (diaIndex !in dias.indices) return@update state
            val dia = dias[diaIndex]
            val ejercicios = dia.ejercicios.sortedBy { it.ordenSecuencia }.toMutableList()
            if (ejercicioIndex !in ejercicios.indices) return@update state

            ejercicios.removeAt(ejercicioIndex)
            val ejReorganizados = ejercicios.mapIndexed { index, ej -> ej.copy(ordenSecuencia = index + 1) }
            val ejerciciosFinales = recalcularEtiquetasBloques(ejReorganizados)

            dias[diaIndex] = dia.copy(ejercicios = ejerciciosFinales)
            state.copy(rutina = actual.copy(diasEntrenamiento = dias))
        }
    }

    // Modificación en moverEjercicio para mantener la integridad de las etiquetas
    fun moverEjercicioConReajuste(diaIndex: Int, ejercicioIndex: Int, direccion: Int) {
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
                val ejerciciosFinales = recalcularEtiquetasBloques(ejReorganizados)

                dias[diaIndex] = dia.copy(ejercicios = ejerciciosFinales)
                state.copy(rutina = actual.copy(diasEntrenamiento = dias))
            } else state
        }
    }

    private fun recalcularEtiquetasBloques(ejercicios: List<EjercicioAsignado>): List<EjercicioAsignado> {
        // 1. Contar cuántos ejercicios hay por cada bloqueId
        val conteoPorBloque = ejercicios.mapNotNull { it.bloqueId }.groupingBy { it }.eachCount()

        // 2. Desagrupar si un bloque quedó con solo 1 ejercicio
        val ejerciciosSaneados = ejercicios.map { ej ->
            if (ej.bloqueId != null && (conteoPorBloque[ej.bloqueId] ?: 0) < 2) {
                ej.copy(bloqueId = null, bloqueNombre = null)
            } else ej
        }

        // 3. Asignar letras secuenciales (A, B, C...) a los bloques válidos
        val bloquesUnicos = ejerciciosSaneados.mapNotNull { it.bloqueId }.distinct()
        val abecedario = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val mapaLetras = bloquesUnicos.mapIndexed { index, bId ->
            bId to abecedario.getOrElse(index) { 'A' }.toString()
        }.toMap()

        val contadorPosicion = mutableMapOf<String, Int>()

        return ejerciciosSaneados.map { ej ->
            if (ej.bloqueId != null) {
                val letra = mapaLetras[ej.bloqueId] ?: "A"
                val pos = (contadorPosicion[ej.bloqueId] ?: 0) + 1
                contadorPosicion[ej.bloqueId!!] = pos
                ej.copy(bloqueNombre = "$letra$pos")
            } else {
                ej.copy(bloqueId = null, bloqueNombre = null)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}