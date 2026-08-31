package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.ui.util.BorradorLocalManager
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.esMismoDiaLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import dev.josearroyo.fitlog.ui.util.ReproductorAudio
import dev.josearroyo.fitlog.ui.util.vibrarDispositivo
import dev.josearroyo.fitlog.ui.util.programarNotificacionTimer
import dev.josearroyo.fitlog.ui.util.cancelarNotificacionTimer

data class EntrenarState(
    val isLoading: Boolean = true,
    val rutina: RutinaAsignada? = null,
    val diaActual: DiaEntrenamientoAsignado? = null,
    val sesionEnProgreso: SesionEntrenamiento = SesionEntrenamiento(),
    val historialPrevioEjercicios: Map<String, AtletaProgresoRepository.RegistroEjercicioPrevio> = emptyMap(),
    val isFinished: Boolean = false,
    val error: String? = null,
    val mostrarDialogoEdicionHoy: Boolean = false,
    val sesionGuardadaHoy: SesionEntrenamiento? = null,

    // ⏱️ ESTADO DEL CRONÓMETRO DE DESCANSO
    val tiempoRestanteSegundos: Int = 0,
    val tiempoTotalSegundos: Int = 0,
    val cronometroActivo: Boolean = false,
    val cronometroEnPausa: Boolean = false,
    val estaSonandoAlarma: Boolean = false
)

@OptIn(ExperimentalUuidApi::class)
class EntrenarViewModel : ViewModel() {
    private val atletaRepository = AtletaRepository()
    private val userRepository = UserRepository()
    private val atletaProgresoRepository = AtletaProgresoRepository()

    private val _state = MutableStateFlow(EntrenarState())
    val state: StateFlow<EntrenarState> = _state.asStateFlow()

    private var currentAtletaId: String = ""
    private var timerJob: Job? = null

    // 🟢 MARCA DE TIEMPO REAL PARA SINCRONIZACIÓN CON PANTALLA BLOQUEADA
    private var targetEndTimeMs: Long = 0L

    fun cargarRutina(authUid: String, rutinaId: String) {
        currentAtletaId = authUid
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val usuario = userRepository.obtenerUsuario(authUid)
                if (usuario == null) {
                    _state.update { it.copy(isLoading = false, error = "Usuario no encontrado.") }
                    return@launch
                }

                val rutina = atletaRepository.obtenerRutinaAsignada(usuario.id, rutinaId)

                if (rutina != null && rutina.diasEntrenamiento.isNotEmpty()) {
                    prepararOCargarSesionInicial(usuario.id, rutina)
                } else {
                    _state.update { it.copy(isLoading = false, error = "Rutina no encontrada o sin días.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun prepararOCargarSesionInicial(
        atletaId: String,
        rutina: RutinaAsignada
    ) {
        val borradorLocal = BorradorLocalManager.obtenerBorradorLocal()

        if (borradorLocal != null &&
            borradorLocal.rutinaAsignadaId == rutina.id &&
            borradorLocal.estado == EstadoSesion.EN_PROGRESO
        ) {
            val diaCorrespondiente = rutina.diasEntrenamiento.find { it.idDia == borradorLocal.diaEntrenamientoId }
                ?: rutina.diasEntrenamiento.first()

            val historialPrevio = cargarHistorialPrevio(atletaId, diaCorrespondiente)

            val sesionSincronizada = sincronizarBorradorConRutina(borradorLocal, diaCorrespondiente, historialPrevio)
            BorradorLocalManager.guardarBorradorLocal(sesionSincronizada)

            _state.update { it.copy(
                isLoading = false,
                rutina = rutina,
                diaActual = diaCorrespondiente,
                sesionEnProgreso = sesionSincronizada,
                historialPrevioEjercicios = historialPrevio
            ) }
            return
        }

        val diaToca = rutina.diasEntrenamiento.minByOrNull { it.ultimaVezEjecutada ?: 0L }
            ?: rutina.diasEntrenamiento.first()

        prepararOCargarSesionDiaEspecifico(atletaId, rutina, diaToca)
    }

    fun cambiarDiaSeleccionado(diaId: String) {
        detenerCronometro()
        val rutina = _state.value.rutina ?: return
        val nuevoDia = rutina.diasEntrenamiento.find { it.idDia == diaId } ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                prepararOCargarSesionDiaEspecifico(currentAtletaId, rutina, nuevoDia)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun cargarHistorialPrevio(
        atletaId: String,
        dia: DiaEntrenamientoAsignado,
        sesionActualIdExcluir: String? = null
    ): Map<String, AtletaProgresoRepository.RegistroEjercicioPrevio> {
        return atletaProgresoRepository.obtenerUltimosRegistrosPorEjercicio(
            atletaId = atletaId,
            ejercicios = dia.ejercicios,
            sesionActualIdExcluir = sesionActualIdExcluir
        )
    }

    private suspend fun prepararOCargarSesionDiaEspecifico(
        atletaId: String,
        rutina: RutinaAsignada,
        dia: DiaEntrenamientoAsignado
    ) {
        val borradorLocal = BorradorLocalManager.obtenerBorradorLocal()
        val ahoraMs = getCurrentTimeMillis()
        val historialPrevio = cargarHistorialPrevio(atletaId, dia)

        if (borradorLocal != null &&
            borradorLocal.rutinaAsignadaId == rutina.id &&
            borradorLocal.diaEntrenamientoId == dia.idDia &&
            borradorLocal.estado == EstadoSesion.EN_PROGRESO
        ) {
            val sesionSincronizada = sincronizarBorradorConRutina(borradorLocal, dia, historialPrevio)
            BorradorLocalManager.guardarBorradorLocal(sesionSincronizada)

            _state.update { it.copy(
                isLoading = false,
                rutina = rutina,
                diaActual = dia,
                sesionEnProgreso = sesionSincronizada,
                historialPrevioEjercicios = historialPrevio
            ) }
            return
        }

        val ultimaSesion = atletaProgresoRepository.obtenerUltimaSesion(atletaId, rutina.id, dia.idDia)

        if (ultimaSesion != null &&
            ultimaSesion.estado == EstadoSesion.COMPLETADA &&
            esMismoDiaLocal(ahoraMs, ultimaSesion.fechaEjecucion)
        ) {
            _state.update { it.copy(
                isLoading = false,
                rutina = rutina,
                diaActual = dia,
                mostrarDialogoEdicionHoy = true,
                sesionGuardadaHoy = ultimaSesion,
                historialPrevioEjercicios = historialPrevio
            ) }
        } else {
            generarCuadernoParaElDia(rutina, dia, historialPrevio)
        }
    }

    fun confirmarEdicionSesionHoy() {
        val sesionHoy = _state.value.sesionGuardadaHoy ?: return
        val diaActual = _state.value.diaActual ?: return
        val historialPrevio = _state.value.historialPrevioEjercicios

        val sesionSincronizada = sincronizarBorradorConRutina(
            borrador = sesionHoy,
            dia = diaActual,
            historialPrevio = historialPrevio
        ).copy(estado = EstadoSesion.EN_PROGRESO)

        _state.update { it.copy(
            mostrarDialogoEdicionHoy = false,
            sesionEnProgreso = sesionSincronizada,
            sesionGuardadaHoy = null
        ) }
        BorradorLocalManager.guardarBorradorLocal(sesionSincronizada)
    }

    fun cancelarEdicionSesionHoy() {
        _state.update { it.copy(mostrarDialogoEdicionHoy = false, sesionGuardadaHoy = null) }
    }

    private fun generarCuadernoParaElDia(
        rutina: RutinaAsignada,
        dia: DiaEntrenamientoAsignado,
        historialPrevio: Map<String, AtletaProgresoRepository.RegistroEjercicioPrevio>
    ) {
        val ejerciciosParaLlenar = dia.ejercicios.sortedBy { it.ordenSecuencia }.map { ejAsignado ->
            val registroPrevioObj = historialPrevio[ejAsignado.ejercicioGlobalId] ?: historialPrevio[ejAsignado.nombre]
            val registroPrevio = registroPrevioObj?.ejercicioLog

            val listaSeries = ejAsignado.seriesPrescritas.mapIndexed { index, prescrita ->
                val seriePrevia = registroPrevio?.seriesRealizadas?.getOrNull(index)
                val pesoReferencia = seriePrevia?.pesoKg ?: 0.0

                SerieRealizada(
                    numeroSerie = index + 1,
                    tipoSerie = prescrita.tipo,
                    pesoKg = 0.0,
                    repeticionesLogradas = 0,
                    pesoTarget = pesoReferencia,
                    repsMinTarget = prescrita.minReps,
                    repsMaxTarget = prescrita.maxReps
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
            id = Uuid.random().toString(),
            rutinaAsignadaId = rutina.id,
            diaEntrenamientoId = dia.idDia,
            nombreRutina = "${rutina.nombreRutina} - Día ${dia.ordenSecuencia}: ${dia.nombreDia}",
            fechaInicio = getCurrentTimeMillis(),
            estado = EstadoSesion.EN_PROGRESO,
            ejerciciosRealizados = ejerciciosParaLlenar
        )

        _state.update { it.copy(
            isLoading = false,
            rutina = rutina,
            diaActual = dia,
            sesionEnProgreso = nuevaSesion,
            historialPrevioEjercicios = historialPrevio
        ) }

        BorradorLocalManager.guardarBorradorLocal(nuevaSesion)
    }

    fun actualizarSerie(ejercicioIndex: Int, serieIndex: Int, peso: Double, reps: Int) {
        var sesionGuardar: SesionEntrenamiento? = null
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            val ejercicioAModificar = nuevosEjercicios[ejercicioIndex]

            val nuevasSeries = ejercicioAModificar.seriesRealizadas.toMutableList()
            nuevasSeries[serieIndex] = nuevasSeries[serieIndex].copy(pesoKg = peso, repeticionesLogradas = reps)

            nuevosEjercicios[ejercicioIndex] = ejercicioAModificar.copy(seriesRealizadas = nuevasSeries)
            val sesionActualizada = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios)
            sesionGuardar = sesionActualizada
            currentState.copy(sesionEnProgreso = sesionActualizada)
        }
        sesionGuardar?.let { BorradorLocalManager.guardarBorradorLocal(it) }
    }

    fun actualizarNotaAtleta(ejercicioIndex: Int, nota: String) {
        var sesionGuardar: SesionEntrenamiento? = null
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            nuevosEjercicios[ejercicioIndex] = nuevosEjercicios[ejercicioIndex].copy(notasAtleta = nota)
            val sesionActualizada = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios)
            sesionGuardar = sesionActualizada
            currentState.copy(sesionEnProgreso = sesionActualizada)
        }
        sesionGuardar?.let { BorradorLocalManager.guardarBorradorLocal(it) }
    }

    fun toggleSaltarEjercicio(ejercicioIndex: Int, fueSaltado: Boolean, justificacion: String = "") {
        var sesionGuardar: SesionEntrenamiento? = null
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            nuevosEjercicios[ejercicioIndex] = nuevosEjercicios[ejercicioIndex].copy(
                fueSaltado = fueSaltado, justificacionSalto = justificacion
            )
            val sesionActualizada = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios)
            sesionGuardar = sesionActualizada
            currentState.copy(sesionEnProgreso = sesionActualizada)
        }
        sesionGuardar?.let { BorradorLocalManager.guardarBorradorLocal(it) }
    }

    fun actualizarRpe(ejercicioIndex: Int, serieIndex: Int, rpe: Int) {
        var sesionGuardar: SesionEntrenamiento? = null
        _state.update { currentState ->
            val sesionActual = currentState.sesionEnProgreso
            val nuevosEjercicios = sesionActual.ejerciciosRealizados.toMutableList()
            val ejercicioAModificar = nuevosEjercicios[ejercicioIndex]

            val nuevasSeries = ejercicioAModificar.seriesRealizadas.toMutableList()
            nuevasSeries[serieIndex] = nuevasSeries[serieIndex].copy(rpe = rpe)

            nuevosEjercicios[ejercicioIndex] = ejercicioAModificar.copy(seriesRealizadas = nuevasSeries)
            val sesionActualizada = sesionActual.copy(ejerciciosRealizados = nuevosEjercicios)
            sesionGuardar = sesionActualizada
            currentState.copy(sesionEnProgreso = sesionActualizada)
        }
        sesionGuardar?.let { BorradorLocalManager.guardarBorradorLocal(it) }
    }

    fun terminarEntrenamiento(authUid: String) {
        detenerCronometro()
        if (_state.value.isLoading) return

        val currentState = _state.value
        val rutinaActual = currentState.rutina
        val diaActual = currentState.diaActual

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

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val sesionIdFinal = currentState.sesionEnProgreso.id.ifBlank { Uuid.random().toString() }
                val ahora = getCurrentTimeMillis()

                val sesionFinal = currentState.sesionEnProgreso
                    .copy(
                        id = sesionIdFinal,
                        fechaEjecucion = ahora,
                        estado = EstadoSesion.COMPLETADA
                    )
                    .calcularMetricas()

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

    // ⏱️ CRONÓMETRO DE DESCANSO (Sincronización basada en reloj real)
    fun iniciarCronometro(segundos: Int) {
        if (segundos <= 0) return
        ReproductorAudio.detenerSonido()
        cancelarNotificacionTimer()
        programarNotificacionTimer(segundos)

        val ahoraMs = getCurrentTimeMillis()
        targetEndTimeMs = ahoraMs + (segundos * 1000L)

        timerJob?.cancel()
        _state.update {
            it.copy(
                tiempoRestanteSegundos = segundos,
                tiempoTotalSegundos = segundos,
                cronometroActivo = true,
                cronometroEnPausa = false,
                estaSonandoAlarma = false
            )
        }

        timerJob = viewModelScope.launch {
            while (_state.value.cronometroActivo) {
                delay(200L) // Polling rápido para respuesta instantánea al reanudar la app
                if (!_state.value.cronometroEnPausa) {
                    val ahora = getCurrentTimeMillis()
                    val diferenciaMs = targetEndTimeMs - ahora
                    val segundosRestantesCalculados = maxOf(0, (diferenciaMs / 1000L).toInt())

                    if (segundosRestantesCalculados == 0) {
                        if (!_state.value.estaSonandoAlarma) {
                            ReproductorAudio.reproducirSonidoFinTiempo()
                            vibrarDispositivo()
                        }
                        _state.update {
                            it.copy(
                                tiempoRestanteSegundos = 0,
                                cronometroActivo = true,
                                estaSonandoAlarma = true
                            )
                        }
                        break // Finaliza el bucle una vez llegada la meta
                    } else {
                        _state.update {
                            it.copy(
                                tiempoRestanteSegundos = segundosRestantesCalculados,
                                cronometroActivo = true,
                                estaSonandoAlarma = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun pausarReanudarCronometro() {
        val estaEnPausa = !_state.value.cronometroEnPausa

        if (estaEnPausa) {
            cancelarNotificacionTimer()
        } else {
            val restantes = _state.value.tiempoRestanteSegundos
            if (restantes > 0) {
                targetEndTimeMs = getCurrentTimeMillis() + (restantes * 1000L)
                programarNotificacionTimer(restantes)
            }
        }

        _state.update { it.copy(cronometroEnPausa = estaEnPausa) }
    }

    fun ajustarTiempoCronometro(segundosAdicionales: Int) {
        val nuevoTiempo = maxOf(0, _state.value.tiempoRestanteSegundos + segundosAdicionales)
        if (nuevoTiempo > 0) {
            val totalAnterior = _state.value.tiempoTotalSegundos
            iniciarCronometro(nuevoTiempo)
            _state.update { it.copy(tiempoTotalSegundos = maxOf(totalAnterior + segundosAdicionales, nuevoTiempo)) }
        } else {
            detenerCronometro()
        }
    }

    fun detenerCronometro() {
        ReproductorAudio.detenerSonido()
        cancelarNotificacionTimer()
        timerJob?.cancel()
        timerJob = null
        targetEndTimeMs = 0L
        _state.update {
            it.copy(
                tiempoRestanteSegundos = 0,
                tiempoTotalSegundos = 0,
                cronometroActivo = false,
                cronometroEnPausa = false,
                estaSonandoAlarma = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        ReproductorAudio.detenerSonido()
        cancelarNotificacionTimer()
        timerJob?.cancel()
    }

    private fun sincronizarBorradorConRutina(
        borrador: SesionEntrenamiento,
        dia: DiaEntrenamientoAsignado,
        historialPrevio: Map<String, AtletaProgresoRepository.RegistroEjercicioPrevio>
    ): SesionEntrenamiento {
        val ejerciciosBorradorMap = borrador.ejerciciosRealizados.associateBy {
            if (it.ejercicioGlobalId.isNotBlank()) it.ejercicioGlobalId else it.nombreEjercicio.trim().lowercase()
        }

        val nuevosEjerciciosRealizados = dia.ejercicios.sortedBy { it.ordenSecuencia }.map { ejAsignado ->
            val claveId = ejAsignado.ejercicioGlobalId
            val claveNombre = ejAsignado.nombre.trim().lowercase()

            val existente = ejerciciosBorradorMap[claveId] ?: ejerciciosBorradorMap[claveNombre]

            if (existente != null) {
                existente.copy(
                    ordenSecuencia = ejAsignado.ordenSecuencia,
                    nombreEjercicio = ejAsignado.nombre,
                    ejercicioGlobalId = ejAsignado.ejercicioGlobalId
                )
            } else {
                val registroPrevioObj = historialPrevio[ejAsignado.ejercicioGlobalId] ?: historialPrevio[ejAsignado.nombre]
                val registroPrevio = registroPrevioObj?.ejercicioLog

                val listaSeries = ejAsignado.seriesPrescritas.mapIndexed { index, prescrita ->
                    val seriePrevia = registroPrevio?.seriesRealizadas?.getOrNull(index)
                    val pesoReferencia = seriePrevia?.pesoKg ?: 0.0

                    SerieRealizada(
                        numeroSerie = index + 1,
                        tipoSerie = prescrita.tipo,
                        pesoKg = 0.0,
                        repeticionesLogradas = 0,
                        pesoTarget = pesoReferencia,
                        repsMinTarget = prescrita.minReps,
                        repsMaxTarget = prescrita.maxReps
                    )
                }

                EjercicioRealizado(
                    ejercicioGlobalId = ejAsignado.ejercicioGlobalId,
                    nombreEjercicio = ejAsignado.nombre,
                    ordenSecuencia = ejAsignado.ordenSecuencia,
                    seriesRealizadas = listaSeries
                )
            }
        }

        return borrador.copy(ejerciciosRealizados = nuevosEjerciciosRealizados)
    }
}