package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.EstadoPeriodo
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.data.model.PeriodoFacturable
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddAtletaState(
    val currentStep: Int = 1,
    val usuario: Usuario = Usuario(),
    val confirmarCorreo: String = "",
    val valoracionFisica: ValoracionFisica = ValoracionFisica(),
    val habitos: Habitos = Habitos(),
    val planSeleccionado: TipoPlanSuscripcion = TipoPlanSuscripcion.MENSUAL,
    val diasPersonalizados: Int = 0,
    val iniciarPeriodoEnseguida: Boolean = true,
    val fechaInicioPlan: Long = 0L,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

sealed interface AddAtletaEvent {
    data class UpdateUsuario(val usuario: Usuario) : AddAtletaEvent
    data class UpdateConfirmarCorreo(val correo: String) : AddAtletaEvent
    data class UpdateValoracion(val valoracion: ValoracionFisica) : AddAtletaEvent
    data class UpdateHabitos(val habitos: Habitos) : AddAtletaEvent
    data class UpdatePlan(val plan: TipoPlanSuscripcion) : AddAtletaEvent
    data class UpdateDiasPersonalizados(val dias: Int) : AddAtletaEvent
    data class UpdateIniciarPeriodo(val iniciar: Boolean) : AddAtletaEvent
    data class UpdateFechaInicioPlan(val fecha: Long) : AddAtletaEvent
    object NextStep : AddAtletaEvent
    object PrevStep : AddAtletaEvent
    object SaveAtleta : AddAtletaEvent
    object ResetState : AddAtletaEvent
}

class AddAtletaViewModel(
    private val atletaRepository: AtletaRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddAtletaState(fechaInicioPlan = getCurrentTimeMillis()))
    val state: StateFlow<AddAtletaState> = _state.asStateFlow()

    fun onEvent(event: AddAtletaEvent) {
        when (event) {
            // 🟢 Al modificar cualquier campo del formulario, limpiamos el error acumulado
            is AddAtletaEvent.UpdateUsuario -> _state.update { it.copy(usuario = event.usuario, error = null) }
            is AddAtletaEvent.UpdateConfirmarCorreo -> _state.update { it.copy(confirmarCorreo = event.correo, error = null) }
            is AddAtletaEvent.UpdateValoracion -> _state.update { it.copy(valoracionFisica = event.valoracion, error = null) }
            is AddAtletaEvent.UpdateHabitos -> _state.update { it.copy(habitos = event.habitos, error = null) }
            is AddAtletaEvent.UpdatePlan -> _state.update { it.copy(planSeleccionado = event.plan, error = null) }
            is AddAtletaEvent.UpdateDiasPersonalizados -> _state.update { it.copy(diasPersonalizados = event.dias, error = null) }
            is AddAtletaEvent.UpdateIniciarPeriodo -> _state.update { it.copy(iniciarPeriodoEnseguida = event.iniciar, error = null) }
            is AddAtletaEvent.UpdateFechaInicioPlan -> _state.update { it.copy(fechaInicioPlan = event.fecha, error = null) }

            AddAtletaEvent.NextStep -> {
                if (_state.value.currentStep == 1) {
                    validarPaso1YContinuar()
                } else {
                    _state.update { it.copy(currentStep = it.currentStep + 1, error = null) }
                }
            }
            AddAtletaEvent.PrevStep -> _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1), error = null) }
            AddAtletaEvent.SaveAtleta -> guardarAtleta()
            AddAtletaEvent.ResetState -> _state.update { AddAtletaState(fechaInicioPlan = getCurrentTimeMillis()) }
        }
    }

    private fun validarPaso1YContinuar() {
        // 🟢 1. Limpiamos cualquier error previo en la primera línea
        _state.update { it.copy(error = null) }

        val currentState = _state.value
        val usuario = currentState.usuario
        val correo = usuario.correo.trim()
        val confirmar = currentState.confirmarCorreo.trim()
        val documento = usuario.numeroDocumento.trim()

        // 2. Validaciones de campos obligatorios
        if (usuario.nombres.isBlank() || usuario.apellidos.isBlank() || documento.isBlank() || correo.isBlank()) {
            _state.update { it.copy(error = "Por favor, completa los campos obligatorios.") }
            return
        }

        // 3. Documento mínimo de 6 dígitos
        if (documento.length < 6) {
            _state.update { it.copy(error = "El número de documento debe tener al menos 6 dígitos para la clave inicial.") }
            return
        }

        // 4. Coincidencia de correos
        if (correo.lowercase() != confirmar.lowercase()) {
            _state.update { it.copy(error = "Los correos electrónicos ingresados no coinciden.") }
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val existeCorreo = userRepository.existeCorreo(correo)
                if (existeCorreo) {
                    _state.update { it.copy(isSaving = false, error = "El correo ya está registrado.") }
                    return@launch
                }

                val existeDoc = userRepository.existeDocumento(documento, RolUsuario.ATLETA)
                if (existeDoc) {
                    _state.update { it.copy(isSaving = false, error = "El número de documento ya está registrado para un atleta.") }
                    return@launch
                }

                _state.update { it.copy(isSaving = false, currentStep = 2, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "Ocurrió un error al validar la información.") }
            }
        }
    }

    private fun guardarAtleta() {
        val currentState = _state.value
        val correo = currentState.usuario.correo.trim().lowercase()
        val confirmar = currentState.confirmarCorreo.trim().lowercase()

        // 1. Verificación de coincidencia de correo
        if (correo != confirmar) {
            _state.update { it.copy(error = "Los correos electrónicos no coinciden.") }
            return
        }

        // 2. 🟢 PUNTO 3: Validar que los días sean al menos 1 si se eligió un plan personalizado
        if (currentState.planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO && currentState.diasPersonalizados < 1) {
            _state.update { it.copy(error = "Para un plan personalizado, debes asignar al menos 1 día de duración.") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val entrenadorId = atletaRepository.obtenerIdEntrenadorActual()
                    ?: throw Exception("No se pudo obtener el ID del entrenador actual.")

                val ahoraMilis = getCurrentTimeMillis()
                val fechaInicioLong = if (currentState.iniciarPeriodoEnseguida) ahoraMilis else currentState.fechaInicioPlan

                val diasPlan = if (currentState.planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
                    currentState.diasPersonalizados
                } else {
                    currentState.planSeleccionado.dias
                }

                val fechaFinLong = dev.josearroyo.fitlog.calcularFechaFinSuscripcion(fechaInicioLong, diasPlan)

                val esHoyOPasado = fechaInicioLong <= ahoraMilis || esMismoDia(fechaInicioLong, ahoraMilis)
                val esActivoDesdeInicio = currentState.iniciarPeriodoEnseguida || esHoyOPasado

                val estadoPeriodoInicial = if (esActivoDesdeInicio) EstadoPeriodo.ACTIVO else EstadoPeriodo.DIFERIDO
                val estadoSuscripcionInicial = if (esActivoDesdeInicio) EstadoSuscripcion.ACTIVO else EstadoSuscripcion.DIFERIDO

                val primerPeriodo = PeriodoFacturable(
                    tipoPlan = currentState.planSeleccionado.name,
                    fechaInicio = fechaInicioLong,
                    fechaFin = fechaFinLong,
                    fechaCreacion = ahoraMilis,
                    estado = estadoPeriodoInicial
                )

                // 3. 🟢 PUNTO 4: Limpieza estricta (.trim()) en todos los campos de texto del usuario
                val usuarioModificado = currentState.usuario.copy(
                    nombres = currentState.usuario.nombres.trim(),
                    apellidos = currentState.usuario.apellidos.trim(),
                    numeroDocumento = currentState.usuario.numeroDocumento.trim(),
                    telefono = currentState.usuario.telefono.trim(),
                    correo = correo,
                    entrenadorId = entrenadorId,
                    planActivo = currentState.planSeleccionado.name,
                    fechaInicioSuscripcion = fechaInicioLong,
                    estadoSuscripcion = estadoSuscripcionInicial,
                    vencimientoSuscripcion = fechaFinLong,
                    requiereCambioContrasena = true,
                    fechaCreacion = ahoraMilis
                )

                // Como el Paso 1 ya garantizó que el documento tiene al menos 6 dígitos, la clave temporal es idéntica
                val contrasenaTemporalSegura = usuarioModificado.numeroDocumento

                atletaRepository.crearAtletaCompleto(
                    usuario = usuarioModificado,
                    valoracion = currentState.valoracionFisica,
                    habitos = currentState.habitos,
                    contrasenaTemporal = contrasenaTemporalSegura,
                    primerPeriodo = primerPeriodo
                )

                _state.update { it.copy(isSaving = false, isSuccess = true) }

            } catch (e: Exception) {
                val mensajeLimpio = when {
                    e.message?.contains("already in use", ignoreCase = true) == true ->
                        "El correo electrónico ya existe en la autenticación del sistema."
                    else -> e.message ?: "Ocurrió un error inesperado"
                }
                _state.update { it.copy(isSaving = false, error = mensajeLimpio) }
            }
        }
    }
}