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

        // 1. Prioridad: Borrador local no finalizado para esta rutina
        if (borradorLocal != null &&
            borradorLocal.rutinaAsignadaId == rutina.id &&
            borradorLocal.estado == EstadoSesion.EN_PROGRESO
        ) {
            val diaCorrespondiente = rutina.diasEntrenamiento.find { it.idDia == borradorLocal.diaEntrenamientoId }
                ?: rutina.diasEntrenamiento.first()

            _state.update { it.copy(
                isLoading = false,
                rutina = rutina,
                diaActual = diaCorrespondiente,
                sesionEnProgreso = borradorLocal
            ) }
            return
        }

        // 2. Si no hay borrador local, determinar qué día le corresponde por rotación
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

    private suspend fun prepararOCargarSesionDiaEspecifico(
        atletaId: String,
        rutina: RutinaAsignada,
        dia: DiaEntrenamientoAsignado
    ) {
        val borradorLocal = BorradorLocalManager.obtenerBorradorLocal()
        val ahoraMs = getCurrentTimeMillis()

        // 1. Verificar si hay un borrador en progreso guardado localmente para este día específico
        if (borradorLocal != null &&
            borradorLocal.rutinaAsignadaId == rutina.id &&
            borradorLocal.diaEntrenamientoId == dia.idDia &&
            borradorLocal.estado == EstadoSesion.EN_PROGRESO
        ) {
            _state.update { it.copy(
                isLoading = false,
                rutina = rutina,
                diaActual = dia,
                sesionEnProgreso = borradorLocal
            ) }
            return
        }

        // 2. Buscar la última sesión en Firestore para este día
        val ultimaSesion = atletaProgresoRepository.obtenerUltimaSesion(atletaId, rutina.id, dia.idDia)

        // 🟢 3. Si existe una sesión COMPLETADA hoy (según zona horaria local), se intercepta para confirmar edición
        if (ultimaSesion != null &&
            ultimaSesion.estado == EstadoSesion.COMPLETADA &&
            esMismoDiaLocal(ahoraMs, ultimaSesion.fechaEjecucion)
        ) {
            _state.update { it.copy(
                isLoading = false,
                rutina = rutina,
                diaActual = dia,
                mostrarDialogoEdicionHoy = true,
                sesionGuardadaHoy = ultimaSesion
            ) }
        } else {
            // 4. Si es de otro día o no existe, genera una nueva plantilla con un UUID fresco
            generarCuadernoParaElDia(rutina, dia)
        }
    }

    // 🟢 El usuario acepta editar la sesión guardada hoy: Reutiliza el UUID original
    fun confirmarEdicionSesionHoy() {
        val sesionHoy = _state.value.sesionGuardadaHoy ?: return
        _state.update { it.copy(
            mostrarDialogoEdicionHoy = false,
            sesionEnProgreso = sesionHoy.copy(estado = EstadoSesion.EN_PROGRESO),
            sesionGuardadaHoy = null
        ) }
        BorradorLocalManager.guardarBorradorLocal(sesionHoy)
    }

    // 🔴 El usuario rechaza la edición: Crea una plantilla nueva con un nuevo UUID
    fun rechazarEdicionSesionHoy() {
        val rutina = _state.value.rutina ?: return
        val dia = _state.value.diaActual ?: return
        _state.update { it.copy(mostrarDialogoEdicionHoy = false, sesionGuardadaHoy = null) }
        generarCuadernoParaElDia(rutina, dia)
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
            sesionEnProgreso = nuevaSesion
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

    // ⏱️ INICIAR O REINICIAR CRONÓMETRO
    fun iniciarCronometro(segundos: Int) {
        if (segundos <= 0) return
        ReproductorAudio.detenerSonido()
        cancelarNotificacionTimer()
        programarNotificacionTimer(segundos)

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
            while (_state.value.tiempoRestanteSegundos > 0) {
                delay(1000L)
                if (!_state.value.cronometroEnPausa) {
                    _state.update { currentState ->
                        val nuevoTiempo = currentState.tiempoRestanteSegundos - 1

                        if (nuevoTiempo == 0) {
                            ReproductorAudio.reproducirSonidoFinTiempo()
                            vibrarDispositivo()
                            cancelarNotificacionTimer() // 👈 Limpiar notificación pendiente
                        }

                        currentState.copy(
                            tiempoRestanteSegundos = nuevoTiempo,
                            cronometroActivo = true,
                            estaSonandoAlarma = (nuevoTiempo == 0)
                        )
                    }
                }
            }
        }
    }

    // ⏱️ PAUSAR O REANUDAR
    fun pausarReanudarCronometro() {
        val estaEnPausa = !_state.value.cronometroEnPausa

        if (estaEnPausa) {
            cancelarNotificacionTimer() // 👈 Cancela notificación al pausar
        } else if (_state.value.tiempoRestanteSegundos > 0) {
            programarNotificacionTimer(_state.value.tiempoRestanteSegundos) // 👈 Reprograma al reanudar
        }

        _state.update { it.copy(cronometroEnPausa = estaEnPausa) }
    }

    // ⏱️ SUMAR O RESTAR TIEMPO (+30s / -10s)
    fun ajustarTiempoCronometro(segundosAdicionales: Int) {
        val nuevoTiempo = maxOf(0, _state.value.tiempoRestanteSegundos + segundosAdicionales)
        if (nuevoTiempo > 0) {

            iniciarCronometro(nuevoTiempo)
        } else {
            detenerCronometro()
        }
    }

    // ⏱️ CANCELAR CRONÓMETRO
    fun detenerCronometro() {
        ReproductorAudio.detenerSonido()
        cancelarNotificacionTimer() // 👈 Cancelar notificación activa
        timerJob?.cancel()
        timerJob = null
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

    // 3. Al salir o destruir el ViewModel
    override fun onCleared() {
        super.onCleared()
        ReproductorAudio.detenerSonido()
        cancelarNotificacionTimer() // 👈 Limpieza completa al destruir
        timerJob?.cancel()
    }
}