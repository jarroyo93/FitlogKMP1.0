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
                    val sesionesDeferred = async { progresoRepository.obtenerHistorialEntrenamientos(atletaId) }

                    val atleta = atletaDeferred.await()
                    val cicloActivo = cicloDeferred.await()
                    val rutinas = rutinasDeferred.await()
                    val sesionesHistorial = sesionesDeferred.await()

                    if (atleta != null) {
                        val rutinaActiva = rutinas.firstOrNull { it.estaActiva }

                        // 1. Extraer comentarios de las sesiones
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

                        // 2. Filtrar historial de sesiones correspondientes al ciclo activo
                        val sesionesCicloActivo = if (cicloActivo != null) {
                            sesionesHistorial.filter { it.fechaEjecucion >= cicloActivo.fechaInicio }
                        } else emptyList()

                        // 3. RECÁLCULO UNIFICADO CON SEMÁFORO CALCULADOR 🟢
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

                        // Extraer series efectivas del ciclo para RPE (excluyendo aproximaciones)
                        val todasLasSeriesEfectivas = sesionesCicloActivo.flatMap { sesion ->
                            sesion.ejerciciosRealizados.flatMap { it.seriesRealizadas }
                        }
                        val metricaFatiga = SemaforoCalculador.evaluarFatigaRpe(todasLasSeriesEfectivas)

                        // Top 3 ejercicios con mayor RPE del ciclo activo
                        val rpePorEj = sesionesCicloActivo
                            .flatMap { it.ejerciciosRealizados }
                            .filter { !it.fueSaltado }
                            .flatMap { ej ->
                                ej.seriesRealizadas
                                    .filter { it.tipoSerie != TipoSerie.APROXIMACION && (it.rpe ?: 0) > 0 }
                                    .map { serie -> ej.nombreEjercicio to serie.rpe!!.toDouble() }
                            }
                            .groupBy { it.first }
                            .mapValues { entry -> entry.value.map { it.second }.average() }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(3)
                            .toMap()

                        // 4. Estructurar Informe Sincronizado
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