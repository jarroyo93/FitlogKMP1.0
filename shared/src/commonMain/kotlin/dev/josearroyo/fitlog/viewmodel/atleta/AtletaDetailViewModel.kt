package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.ui.util.SemaforoCalculador
import dev.josearroyo.fitlog.getCurrentTimeMillis
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
                supervisorScope {
                    val atletaDeferred = async { atletaRepository.obtenerUsuario(atletaId) }
                    val cicloDeferred = async { progresoRepository.obtenerCicloActivo(atletaId) }
                    val rutinasDeferred = async { atletaRepository.obtenerRutinasActivas(atletaId) }

                    val atleta = atletaDeferred.await()
                    val cicloActivo = cicloDeferred.await()
                    val rutinas = rutinasDeferred.await()

                    if (atleta != null) {
                        val rutinaActiva = rutinas.firstOrNull { it.estaActiva }

                        // 🟢 Carga optimizada: solo sesiones del ciclo activo (evita descargar el historial completo de años)
                        val sesionesCicloActivo = if (cicloActivo != null) {
                            progresoRepository.obtenerEntrenamientosCicloActivo(atletaId, cicloActivo.fechaInicio)
                        } else emptyList()

                        // Extraer comentarios recientes únicamente de las últimas sesiones
                        val notasExtraidas = sesionesCicloActivo.flatMap { sesion ->
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

                        val ahora = getCurrentTimeMillis()

                        val metricaAdherencia = if (cicloActivo != null) {
                            val milisPorDia = 86_400_000L
                            val diasTranscurridos = (((ahora - cicloActivo.fechaInicio) / milisPorDia) + 1)
                                .toInt()
                                .coerceIn(1, cicloActivo.duracionDias)

                            SemaforoCalculador.evaluarAdherenciaProRata(
                                metaSesionesCiclo = cicloActivo.metaSesionesAsignadas,
                                duracionDiasCiclo = cicloActivo.duracionDias,
                                sesionesEjecutadas = cicloActivo.sesionesCompletadas,
                                diasTranscurridos = diasTranscurridos
                            )
                        } else MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "")

                        val metricaVolumen = if (cicloActivo != null) {
                            SemaforoCalculador.evaluarVolumenEfectivo(
                                repsMetaTotal = cicloActivo.repeticionesMetaTotal,
                                repsLogradasTotal = cicloActivo.repeticionesLogradasTotal,
                                metaSesionesCiclo = cicloActivo.metaSesionesAsignadas,
                                sesionesEjecutadas = cicloActivo.sesionesCompletadas
                            )
                        } else MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "")

                        val todasLasSeriesEfectivas = sesionesCicloActivo.flatMap { sesion ->
                            sesion.ejerciciosRealizados.flatMap { it.seriesRealizadas }
                        }
                        val metricaFatiga = SemaforoCalculador.evaluarFatigaRpe(todasLasSeriesEfectivas)

                        // 🟢 Cálculo seguro de RPE con protección contra NaN
                        val rpePorEj = sesionesCicloActivo
                            .flatMap { it.ejerciciosRealizados }
                            .filter { !it.fueSaltado }
                            .flatMap { ej ->
                                ej.seriesRealizadas
                                    .filter { it.tipoSerie != TipoSerie.APROXIMACION && (it.rpe ?: 0) > 0 }
                                    .map { serie -> ej.nombreEjercicio to serie.rpe!!.toDouble() }
                            }
                            .groupBy { it.first }
                            .mapValues { entry ->
                                val avg = entry.value.map { it.second }.average()
                                if (avg.isNaN()) 0.0 else avg
                            }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(3)
                            .toMap()

                        val informe = InformeCoach(
                            asistenciaPorcentaje = metricaAdherencia.valor,
                            cumplimientoVolumen = metricaVolumen.valor,
                            rpePromedioGlobal = metricaFatiga.valor,
                            rpePromedioPorEjercicio = rpePorEj,
                            totalSesiones = cicloActivo?.sesionesCompletadas ?: 0,
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