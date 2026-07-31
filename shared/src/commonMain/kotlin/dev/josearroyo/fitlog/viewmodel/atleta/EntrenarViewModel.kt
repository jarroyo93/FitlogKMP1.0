package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.ui.util.BorradorLocalManager
import dev.josearroyo.fitlog.getCurrentTimeMillis // 🟢 Tu función de plataforma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class EntrenarState(
    val isLoading: Boolean = true,
    val rutina: RutinaAsignada? = null,
    val diaActual: DiaEntrenamientoAsignado? = null,
    val sesionEnProgreso: SesionEntrenamiento = SesionEntrenamiento(),
    val isFinished: Boolean = false,
    val error: String? = null
)

class EntrenarViewModel : ViewModel() {
    private val atletaRepository = AtletaRepository()
    private val userRepository = UserRepository()
    private val atletaProgresoRepository = AtletaProgresoRepository()

    private val _state = MutableStateFlow(EntrenarState())
    val state: StateFlow<EntrenarState> = _state.asStateFlow()

    fun cargarRutina(authUid: String, rutinaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val usuario = userRepository.obtenerUsuario(authUid)
                if (usuario == null) {
                    _state.update { it.copy(isLoading = false, error = "Usuario no encontrado.") }
                    return@launch
                }


                val borradorLocal = BorradorLocalManager.obtenerBorradorLocal()
                val rutina = atletaRepository.obtenerRutinaAsignada(usuario.id, rutinaId)

                if (rutina != null && rutina.diasEntrenamiento.isNotEmpty()) {
                    if (borradorLocal != null && borradorLocal.rutinaAsignadaId == rutinaId) {
                        val diaToca = rutina.diasEntrenamiento.find { it.idDia == borradorLocal.diaEntrenamientoId }
                            ?: rutina.diasEntrenamiento.first()

                        _state.update { it.copy(
                            isLoading = false,
                            rutina = rutina,
                            diaActual = diaToca,
                            sesionEnProgreso = borradorLocal
                        ) }
                    } else {
                        val diaToca = rutina.diasEntrenamiento.minByOrNull { it.ultimaVezEjecutada ?: 0L }
                            ?: rutina.diasEntrenamiento.first()

                        generarCuadernoParaElDia(rutina, diaToca)
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Rutina no encontrada o sin días.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun cambiarDiaSeleccionado(diaId: String) {
        val rutina = _state.value.rutina ?: return
        val nuevoDia = rutina.diasEntrenamiento.find { it.idDia == diaId } ?: return
        generarCuadernoParaElDia(rutina, nuevoDia)
    }

    private fun generarCuadernoParaElDia(rutina: RutinaAsignada, dia: DiaEntrenamientoAsignado) {
        val ejerciciosParaLlenar = dia.ejercicios.sortedBy { it.ordenSecuencia }.map { ejAsignado ->
            val listaSeries = ejAsignado.seriesPrescritas.mapIndexed { index, prescrita ->
                SerieRealizada(
                    numeroSerie = index + 1,
                    tipoSerie = prescrita.tipo,
                    pesoKg = 0.0,
                    repeticionesLogradas = 0,
                    pesoTarget = 0.0,
                    repsTarget = prescrita.repeticiones
                )
            }

            EjercicioRealizado(
                ejercicioGlobalId = ejAsignado.ejercicioGlobalId,
                nombreEjercicio = ejAsignado.nombre,
                ordenSecuencia = ejAsignado.ordenSecuencia,
                seriesRealizadas = listaSeries
            )
        }

        val nuevaSesion = SesionEntrenamiento(
            rutinaAsignadaId = rutina.id,
            diaEntrenamientoId = dia.idDia,
            nombreRutina = "${rutina.nombreRutina} - Día ${dia.ordenSecuencia}: ${dia.nombreDia}",
            ejerciciosRealizados = ejerciciosParaLlenar
        )

        _state.update { it.copy(
            isLoading = false,
            rutina = rutina,
            diaActual = dia,
            sesionEnProgreso = nuevaSesion
        ) }
    }

    fun actualizarSerie(ejercicioIndex: Int, serieIndex: Int, peso: Double, reps: Int) {
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            val ejercicioAModificar = nuevosEjercicios[ejercicioIndex]

            val nuevasSeries = ejercicioAModificar.seriesRealizadas.toMutableList()
            nuevasSeries[serieIndex] = nuevasSeries[serieIndex].copy(pesoKg = peso, repeticionesLogradas = reps)

            nuevosEjercicios[ejercicioIndex] = ejercicioAModificar.copy(seriesRealizadas = nuevasSeries)
            currentState.copy(sesionEnProgreso = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios))
        }
        BorradorLocalManager.guardarBorradorLocal(_state.value.sesionEnProgreso)
    }

    fun actualizarNotaAtleta(ejercicioIndex: Int, nota: String) {
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            nuevosEjercicios[ejercicioIndex] = nuevosEjercicios[ejercicioIndex].copy(notasAtleta = nota)
            currentState.copy(sesionEnProgreso = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios))
        }
        BorradorLocalManager.guardarBorradorLocal(_state.value.sesionEnProgreso)
    }

    fun toggleSaltarEjercicio(ejercicioIndex: Int, fueSaltado: Boolean, justificacion: String = "") {
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            nuevosEjercicios[ejercicioIndex] = nuevosEjercicios[ejercicioIndex].copy(
                fueSaltado = fueSaltado, justificacionSalto = justificacion
            )
            currentState.copy(sesionEnProgreso = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios))
        }
        BorradorLocalManager.guardarBorradorLocal(_state.value.sesionEnProgreso)
    }

    fun actualizarRpe(ejercicioIndex: Int, serieIndex: Int, rpe: Int) {
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            val ejercicioAModificar = nuevosEjercicios[ejercicioIndex]

            val nuevasSeries = ejercicioAModificar.seriesRealizadas.toMutableList()
            nuevasSeries[serieIndex] = nuevasSeries[serieIndex].copy(rpe = rpe)

            nuevosEjercicios[ejercicioIndex] = ejercicioAModificar.copy(seriesRealizadas = nuevasSeries)
            currentState.copy(sesionEnProgreso = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios))
        }
        BorradorLocalManager.guardarBorradorLocal(_state.value.sesionEnProgreso)
    }

    fun terminarEntrenamiento(authUid: String) {
        val currentState = _state.value
        val rutinaActual = currentState.rutina
        val diaActual = currentState.diaActual

        // 1. CORRECCIÓN: En lugar de salir en silencio, notificamos a la UI si falta contexto
        if (rutinaActual == null || diaActual == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "No se puede guardar: Error interno con los datos de la rutina."
                )
            }
            return
        }

        val contieneMensajesNuevos = currentState.sesionEnProgreso.ejerciciosRealizados.any { it.notasAtleta.isNotBlank() }
        val nuevoIdSesion = Uuid.random().toString()

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 2. CORRECCIÓN: Trasladamos el cálculo de métricas DENTRO del entorno seguro try-catch
                // Aquí aseguramos que el cálculo global se ejecute sin tumbar la aplicación
                val sesionFinal = currentState.sesionEnProgreso
                    .copy(
                        id = nuevoIdSesion,
                        fechaEjecucion = getCurrentTimeMillis()
                    )
                    .calcularMetricas() // 🚀 Extensión segura de repeticiones globales

                val usuario = userRepository.obtenerUsuario(authUid)

                if (usuario != null) {
                    val metaSesiones = rutinaActual.diasEntrenamiento.size

                    val exito = atletaProgresoRepository.registrarSesionYActualizarCiclo(
                        atletaId = usuario.id,
                        sesionProcesada = sesionFinal,
                        rutinaActual = rutinaActual,
                        diaActual = diaActual,
                        metaSesiones = metaSesiones
                    )

                    if (exito) {
                        BorradorLocalManager.eliminarBorradorLocal()

                        if (contieneMensajesNuevos) {
                            userRepository.actualizarPerfilUsuario(usuario.id, mapOf("tieneNotasNuevas" to true))
                        }

                        _state.update { it.copy(isLoading = false, isFinished = true) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Error al guardar el progreso en el servidor.") }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Usuario no encontrado.") }
                }
            } catch (e: Exception) {
                // Cualquier división por cero o fallo de red caerá aquí de manera segura, liberando la UI
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Fallo inesperado al procesar y guardar el entrenamiento."
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}