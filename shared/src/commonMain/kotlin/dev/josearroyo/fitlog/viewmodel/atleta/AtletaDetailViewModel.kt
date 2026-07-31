package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class NotaReciente(
    val fecha: Long,
    val rutinaNombre: String,
    val ejercicioNombre: String,
    val mensaje: String
)

data class InformeCoach(
    val asistenciaPorcentaje: Double = 0.0,
    val cumplimientoVolumen: Double = 0.0,
    val rpePromedioGlobal: Double = 0.0,
    val rpePromedioPorEjercicio: Map<String, Double> = emptyMap(),
    val totalSesiones: Int = 0,
    val fechaInicio: Long? = null,
    val fechaFin: Long? = null
)

data class AtletaDetailState(
    val atleta: Usuario? = null,
    val cicloActivo: CicloEntrenamiento? = null,
    val rutinaActiva: RutinaAsignada? = null,
    val notasRecientes: List<NotaReciente> = emptyList(),
    val informeCoach: InformeCoach = InformeCoach(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AtletaDetailViewModel(
    private val atletaRepository: AtletaRepository = AtletaRepository(),
    private val progresoRepository: AtletaProgresoRepository = AtletaProgresoRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AtletaDetailState())
    val state: StateFlow<AtletaDetailState> = _state.asStateFlow()

    fun cargarExpedienteAtleta(atletaId: String) {
        if (atletaId.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // supervisorScope aísla las fallas de red individuales impidiendo crashes
                supervisorScope {
                    val atletaDeferred = async { atletaRepository.obtenerUsuario(atletaId) }
                    val cicloDeferred = async { progresoRepository.obtenerCicloActivo(atletaId) }
                    val rutinasDeferred = async { atletaRepository.obtenerRutinasActivas(atletaId) }
                    val sesionesDeferred = async { progresoRepository.obtenerHistorialEntrenamientos(atletaId) }

                    val atleta = atletaDeferred.await()
                    val cicloActivo = cicloDeferred.await()
                    val rutinas = rutinasDeferred.await()
                    val sesionesHistorial = sesionesDeferred.await()

                    if (atleta != null) {
                        val rutinaActiva = rutinas.firstOrNull { it.estaActiva }

                        // 1. Extraer comentarios
                        val notasExtraidas = sesionesHistorial.flatMap { sesion ->
                            sesion.ejerciciosRealizados
                                .filter { it.notasAtleta.isNotBlank() }
                                .map { ej ->
                                    NotaReciente(
                                        fecha = sesion.fechaEjecucion,
                                        rutinaNombre = sesion.nombreRutina,
                                        ejercicioNombre = ej.nombreEjercicio,
                                        mensaje = ej.notasAtleta
                                    )
                                }
                        }.take(3)

                        // 2. Procesar RPE con sanado explícito contra NaN / Infinity
                        val todasLasSeriesConRpe = sesionesHistorial
                            .flatMap { it.ejerciciosRealizados }
                            .filter { !it.fueSaltado }
                            .flatMap { ej ->
                                ej.seriesRealizadas.mapNotNull { serie ->
                                    serie.rpe?.let { rpe -> ej.nombreEjercicio to rpe.toDouble() }
                                }
                            }

                        val rpeGlobal = if (todasLasSeriesConRpe.isNotEmpty()) {
                            val avg = todasLasSeriesConRpe.map { it.second }.average()
                            if (avg.isNaN() || avg.isInfinite()) 0.0 else avg
                        } else 0.0

                        val rpePorEj = todasLasSeriesConRpe.groupBy { it.first }
                            .mapValues { entry ->
                                val avg = entry.value.map { it.second }.average()
                                if (avg.isNaN() || avg.isInfinite()) 0.0 else avg
                            }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(3)
                            .toMap()

                        // 3. Saneamiento de porcentajes del ciclo antes de enviar a la UI
                        val asistenciaSanada = cicloActivo?.porcentajeAsistencia?.let {
                            if (it.isNaN() || it.isInfinite()) 0.0 else it
                        } ?: 0.0

                        val volumenSanado = cicloActivo?.porcentajeVolumenGlobal?.let {
                            if (it.isNaN() || it.isInfinite()) 0.0 else it
                        } ?: 0.0

                        // 4. Estructurar Informe seguro
                        val informe = InformeCoach(
                            asistenciaPorcentaje = asistenciaSanada,
                            cumplimientoVolumen = volumenSanado,
                            rpePromedioGlobal = rpeGlobal,
                            rpePromedioPorEjercicio = rpePorEj,
                            totalSesiones = sesionesHistorial.size,
                            fechaInicio = cicloActivo?.fechaInicio,
                            fechaFin = cicloActivo?.fechaCierre
                        )

                        _state.update {
                            it.copy(
                                atleta = atleta,
                                cicloActivo = cicloActivo,
                                rutinaActiva = rutinaActiva,
                                notasRecientes = notasExtraidas,
                                informeCoach = informe,
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "No se encontró al atleta.") }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al procesar el expediente") }
            }
        }
    }
}