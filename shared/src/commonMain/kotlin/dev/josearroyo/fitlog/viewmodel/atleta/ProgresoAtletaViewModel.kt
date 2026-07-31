package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.data.model.EjercicioRealizado
import dev.josearroyo.fitlog.data.model.Pesaje
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.formatearFechaHistorial
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Modelos UI para mapeo limpio en commonMain
data class DetalleEjercicioUI(
    val fechaFormat: String,
    val nombreRutina: String,
    val detalle: EjercicioRealizado
)

data class RecordPersonalUI(
    val nombreEjercicio: String,
    val pesoMaximo: Double,
    val repeticiones: Int,
    val fechaFormateada: String
)

data class ImpactoFisicoUI(
    val pesoInicial: Double? = null,
    val pesoFinal: Double? = null,
    val deltaPeso: Double? = null,
    val abdomenInicial: Double? = null,
    val abdomenFinal: Double? = null,
    val deltaAbdomen: Double? = null,
    val tieneDatos: Boolean = false
)

data class ResumenCicloUI(
    val rpePromedio: Double = 0.0,
    val tonelajeTotalKg: Double = 0.0,
    val porcentajeAsistencia: Double = 0.0,
    val porcentajeVolumen: Double = 0.0,
    val sesionesCompletadas: Int = 0,
    val metaSesiones: Int = 0,
    val impactoFisico: ImpactoFisicoUI = ImpactoFisicoUI()
)

data class ProgresoAtletaState(
    val isLoading: Boolean = true,
    val rachaSemana: List<Pair<String, Boolean>> = emptyList(),
    val entrenosMes: Int = 0,
    val volumenSemanal: Double = 0.0,
    val ejerciciosDisponibles: List<String> = emptyList(),
    val historialEjercicioFiltrado: List<DetalleEjercicioUI> = emptyList(),
    val historialSesiones: List<SesionEntrenamiento> = emptyList(), // Sesiones del ciclo activo/seleccionado
    val recordsPersonales: List<RecordPersonalUI> = emptyList(),

    // --- NUEVAS PROPIEDADES PARA HISTORIAL DE CICLOS ---
    val historialCiclos: List<CicloEntrenamiento> = emptyList(),
    val cicloSeleccionado: CicloEntrenamiento? = null,
    val resumenCicloSeleccionado: ResumenCicloUI = ResumenCicloUI()
)

class ProgresoAtletaViewModel(
    private val progresoRepository: AtletaProgresoRepository = AtletaProgresoRepository(),
    private val atletaRepository: AtletaRepository = AtletaRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgresoAtletaState())
    val uiState: StateFlow<ProgresoAtletaState> = _uiState.asStateFlow()

    private var todasLasSesiones: List<SesionEntrenamiento> = emptyList()
    private var todosLosPesajes: List<Pesaje> = emptyList()
    private var todasLasValoraciones: List<ValoracionFisica> = emptyList()

    fun cargarDatosProgreso(authUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val usuario = userRepository.obtenerUsuario(authUid)
                if (usuario != null) {
                    val atletaId = usuario.id

                    // Carga concurrente paralela para alta velocidad
                    kotlinx.coroutines.supervisorScope {
                        val sesionesDef = async { progresoRepository.obtenerHistorialEntrenamientos(atletaId) }
                        val ciclosDef = async { progresoRepository.obtenerHistorialCiclos(atletaId) }
                        val pesajesDef = async { progresoRepository.obtenerUltimosPesajes(atletaId, limite = 50) }
                        val valoracionesDef = async { atletaRepository.obtenerHistorialValoraciones(atletaId) }

                        todasLasSesiones = sesionesDef.await()
                        val ciclos = ciclosDef.await()
                        todosLosPesajes = pesajesDef.await()
                        todasLasValoraciones = valoracionesDef.await()

                        // Seleccionar ciclo activo por defecto, o el más reciente
                        val cicloInicial = ciclos.firstOrNull { it.estaActivo } ?: ciclos.firstOrNull()

                        _uiState.update { state ->
                            state.copy(
                                historialCiclos = ciclos,
                                cicloSeleccionado = cicloInicial
                            )
                        }

                        // Calcular métricas generales y del ciclo seleccionado
                        calcularKPIsYEjercicios()
                        cicloInicial?.let { seleccionarCiclo(it) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun seleccionarCiclo(ciclo: CicloEntrenamiento) {
        val toleranciaMilis = 300_000L
        val inicio = (ciclo.fechaInicio - toleranciaMilis).coerceAtLeast(0L)
        val fin = if (ciclo.fechaCierre > 0L) ciclo.fechaCierre else Long.MAX_VALUE

        // 1. Filtrar sesiones pertenecientes exclusivamente a este ciclo
        val sesionesDelCiclo = todasLasSesiones.filter { it.fechaEjecucion in inicio..fin }

        // 2. Calcular RPE Promedio Saneado (sin divisiones por cero ni NaN)
        val seriesConRpe = sesionesDelCiclo
            .flatMap { it.ejerciciosRealizados }
            .filter { !it.fueSaltado }
            .flatMap { it.seriesRealizadas }
            .mapNotNull { it.rpe?.toDouble() }

        val rpePromedioRaw = if (seriesConRpe.isNotEmpty()) seriesConRpe.average() else 0.0
        val rpePromedio = if (rpePromedioRaw.isNaN() || rpePromedioRaw.isInfinite()) 0.0 else (rpePromedioRaw * 10).roundToInt() / 10.0

        // 3. Tonelaje Total del Ciclo
        var tonelajeTotal = 0.0
        sesionesDelCiclo.forEach { sesion ->
            sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ej ->
                ej.seriesRealizadas.forEach { serie ->
                    tonelajeTotal += (serie.pesoKg * serie.repeticionesLogradas)
                }
            }
        }

        // 4. Porcentajes de Asistencia y Volumen Saneados
        val pctAsistencia = if (ciclo.metaSesionesAsignadas > 0) {
            ((ciclo.sesionesCompletadas.toDouble() / ciclo.metaSesionesAsignadas.toDouble()) * 100.0).coerceAtMost(100.0)
        } else 0.0

        val pctVolumen = if (ciclo.repeticionesMetaTotal > 0) {
            ((ciclo.repeticionesLogradasTotal.toDouble() / ciclo.repeticionesMetaTotal.toDouble()) * 100.0).coerceAtMost(100.0)
        } else 0.0

        // 5. Impacto Físico (Deltas de Peso y Abdomen)
        val pesajesCiclo = todosLosPesajes.filter { it.fecha in inicio..fin }.sortedBy { it.fecha }
        val valoracionesCiclo = todasLasValoraciones.filter { it.fechaRegistro in inicio..fin }.sortedBy { it.fechaRegistro }

        val pInicial = pesajesCiclo.firstOrNull()?.pesoKg ?: valoracionesCiclo.firstOrNull()?.pesoKg
        val pFinal = pesajesCiclo.lastOrNull()?.pesoKg ?: valoracionesCiclo.lastOrNull()?.pesoKg
        val deltaPeso = if (pInicial != null && pFinal != null) (pFinal - pInicial) else null

        val abdInicial = valoracionesCiclo.firstOrNull()?.abdomen1
        val abdFinal = valoracionesCiclo.lastOrNull()?.abdomen1
        val deltaAbdomen = if (abdInicial != null && abdFinal != null) (abdFinal - abdInicial) else null

        val impacto = ImpactoFisicoUI(
            pesoInicial = pInicial,
            pesoFinal = pFinal,
            deltaPeso = deltaPeso?.let { (it * 10).roundToInt() / 10.0 },
            abdomenInicial = abdInicial,
            abdomenFinal = abdFinal,
            deltaAbdomen = deltaAbdomen?.let { (it * 10).roundToInt() / 10.0 },
            tieneDatos = pInicial != null || abdInicial != null
        )

        val resumen = ResumenCicloUI(
            rpePromedio = rpePromedio,
            tonelajeTotalKg = tonelajeTotal,
            porcentajeAsistencia = (pctAsistencia * 10).roundToInt() / 10.0,
            porcentajeVolumen = (pctVolumen * 10).roundToInt() / 10.0,
            sesionesCompletadas = ciclo.sesionesCompletadas,
            metaSesiones = ciclo.metaSesionesAsignadas,
            impactoFisico = impacto
        )

        _uiState.update { state ->
            state.copy(
                cicloSeleccionado = ciclo,
                historialSesiones = sesionesDelCiclo, // ¡El Diario de Cargas ahora responde al ciclo activo!
                resumenCicloSeleccionado = resumen
            )
        }
    }

    private fun calcularKPIsYEjercicios() {
        val ahoraMilis = getCurrentTimeMillis()
        val unDiaMilis = 86400000L
        val unMesMilis = unDiaMilis * 30L

        // 1. Cómputo de entrenamientos del mes
        val conteoMes = todasLasSesiones.count {
            it.fechaEjecucion in (ahoraMilis - unMesMilis)..ahoraMilis
        }

        // 2. Cómputo de Racha Semanal y Volumen
        var volumenTotal = 0.0
        val racha = mutableListOf<Pair<String, Boolean>>()
        val nombresDiasSemana = listOf("D", "L", "M", "M", "J", "V", "S")

        for (i in 6 downTo 0) {
            val objetivoDiaTimestamp = ahoraMilis - (i * unDiaMilis)
            val inicioDia = objetivoDiaTimestamp - (objetivoDiaTimestamp % unDiaMilis)
            val finDia = inicioDia + unDiaMilis - 1

            val sesionesDelDia = todasLasSesiones.filter { it.fechaEjecucion in inicioDia..finDia }
            val diaIndex = (((inicioDia / unDiaMilis) + 4) % 7).toInt()
            val letraDia = nombresDiasSemana[if (diaIndex < 0) diaIndex + 7 else diaIndex]

            racha.add(Pair(letraDia, sesionesDelDia.isNotEmpty()))

            sesionesDelDia.forEach { sesion ->
                sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ejercicio ->
                    ejercicio.seriesRealizadas.forEach { serie ->
                        volumenTotal += (serie.pesoKg * serie.repeticionesLogradas)
                    }
                }
            }
        }

        // 3. Extraer ejercicios únicos
        val nombresUnicos = todasLasSesiones
            .flatMap { it.ejerciciosRealizados }
            .map { it.nombreEjercicio }
            .distinct()
            .sorted()

        // 4. Récords Personales (PRs)
        val recordsMap = mutableMapOf<String, RecordPersonalUI>()
        todasLasSesiones.forEach { sesion ->
            sesion.ejerciciosRealizados.filter { !it.fueSaltado }.forEach { ej ->
                ej.seriesRealizadas.forEach { serie ->
                    val recordActual = recordsMap[ej.nombreEjercicio]

                    if (recordActual == null ||
                        serie.pesoKg > recordActual.pesoMaximo ||
                        (serie.pesoKg == recordActual.pesoMaximo && serie.repeticionesLogradas > recordActual.repeticiones)
                    ) {
                        recordsMap[ej.nombreEjercicio] = RecordPersonalUI(
                            nombreEjercicio = ej.nombreEjercicio,
                            pesoMaximo = serie.pesoKg,
                            repeticiones = serie.repeticionesLogradas,
                            fechaFormateada = formatearFechaHistorial(sesion.fechaEjecucion)
                        )
                    }
                }
            }
        }

        // 5. Historial inicial del primer ejercicio
        val historialInicialFiltrado = if (nombresUnicos.isNotEmpty()) {
            val primerEjercicio = nombresUnicos.first()
            todasLasSesiones.mapNotNull { sesion ->
                val ejercicioEncontrado = sesion.ejerciciosRealizados.find {
                    it.nombreEjercicio == primerEjercicio && !it.fueSaltado
                }
                if (ejercicioEncontrado != null) {
                    DetalleEjercicioUI(
                        fechaFormat = formatearFechaHistorial(sesion.fechaEjecucion),
                        nombreRutina = sesion.nombreRutina,
                        detalle = ejercicioEncontrado
                    )
                } else null
            }
        } else emptyList()

        _uiState.update { state ->
            state.copy(
                entrenosMes = conteoMes,
                rachaSemana = racha,
                volumenSemanal = volumenTotal,
                ejerciciosDisponibles = nombresUnicos,
                recordsPersonales = recordsMap.values.sortedByDescending { it.pesoMaximo },
                historialEjercicioFiltrado = historialInicialFiltrado,
                isLoading = false
            )
        }
    }

    fun filtrarPorEjercicio(nombreEjercicio: String) {
        val historialFiltrado = todasLasSesiones.mapNotNull { sesion ->
            val ejercicioEncontrado = sesion.ejerciciosRealizados.find {
                it.nombreEjercicio == nombreEjercicio && !it.fueSaltado
            }

            if (ejercicioEncontrado != null) {
                DetalleEjercicioUI(
                    fechaFormat = formatearFechaHistorial(sesion.fechaEjecucion),
                    nombreRutina = sesion.nombreRutina,
                    detalle = ejercicioEncontrado
                )
            } else null
        }

        _uiState.update { it.copy(historialEjercicioFiltrado = historialFiltrado) }
    }
}