package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.calcularFechaFinSuscripcion
import dev.josearroyo.fitlog.data.model.EstadoPeriodo
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.PeriodoFacturable
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorialFacturacionState(
    val isLoading: Boolean = true,
    val atleta: Usuario? = null,
    val periodos: List<PeriodoFacturable> = emptyList(),
    val error: String? = null
)

class HistorialFacturacionViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _state = MutableStateFlow(HistorialFacturacionState())
    val state = _state.asStateFlow()

    fun cargarHistorial(atletaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                var infoAtleta = userRepository.obtenerUsuario(atletaId)

                // 🟢 AUTO-CORRECCIÓN: Evalúa y pasa a COMPLETADO los periodos vencidos antes de listar
                if (infoAtleta != null) {
                    infoAtleta = userRepository.evaluarYActualizarEstadoSuscripcion(infoAtleta)
                }

                val listaPeriodos = userRepository.obtenerPeriodosDeAtleta(atletaId)

                _state.update {
                    it.copy(
                        isLoading = false,
                        atleta = infoAtleta,
                        periodos = listaPeriodos
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun anadirPlanAHistorial(
        atletaId: String,
        entrenadorId: String,
        plan: TipoPlanSuscripcion,
        diasPersonalizados: Int,
        iniciarEnseguida: Boolean,
        fechaInicioSeleccionadaMilis: Long
    ) {
        viewModelScope.launch {
            val atleta = _state.value.atleta

            // 🛡️ REGLA 1: Prevenir renovaciones a atletas en pausa
            if (atleta?.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO) {
                _state.update {
                    it.copy(error = "El atleta está en pausa. Debe reactivar su membresía antes de asignar un nuevo plan.")
                }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            val ahora = getCurrentTimeMillis()
            val diasDelPlan = if (plan == TipoPlanSuscripcion.PERSONALIZADO) diasPersonalizados else plan.dias

            if (diasDelPlan <= 0) {
                _state.update { it.copy(isLoading = false, error = "La duración del plan debe ser de al menos 1 día.") }
                return@launch
            }

            try {
                val ultimaFechaFinCadena = userRepository.obtenerUltimaFechaFinCadena(atletaId)

                // 🟢 REGLA 2: Evaluar si el atleta tiene un plan activo vigente corriendo
                val tienePlanActivoCorriendo = atleta?.estadoSuscripcion == EstadoSuscripcion.ACTIVO &&
                        (atleta.vencimientoSuscripcion ?: 0L) > (ahora + 60_000L)

                // 🟢 REGLA 3: Determinar fecha de inicio real (+1 ms para iniciar a las 00:00:00.000)
                val fechaInicioLong = if (iniciarEnseguida && tienePlanActivoCorriendo) {
                    ultimaFechaFinCadena + 1L
                } else if (iniciarEnseguida) {
                    ahora
                } else {
                    fechaInicioSeleccionadaMilis
                }

                val fechaFinLong = calcularFechaFinSuscripcion(fechaInicioLong, diasDelPlan)

                // 🛡️ REGLA 4: Verificar que no haya solapamientos
                if (userRepository.existeSolapamientoPeriodo(atletaId, fechaInicioLong, fechaFinLong)) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "La fecha seleccionada genera un conflicto o solapamiento con un plan existente."
                        )
                    }
                    return@launch
                }

                // 🟢 REGLA 5: Estado del nuevo período
                val esHoyOPasado = fechaInicioLong <= ahora || esMismoDia(fechaInicioLong, ahora)
                val estadoPeriodoCalculado = if (tienePlanActivoCorriendo || !esHoyOPasado) {
                    EstadoPeriodo.DIFERIDO
                } else {
                    EstadoPeriodo.ACTIVO
                }

                val exito = userRepository.renovarSuscripcion(
                    atletaId = atletaId,
                    entrenadorId = entrenadorId,
                    planActivo = plan.name,
                    fechaInicio = fechaInicioLong,
                    fechaFin = fechaFinLong,
                    estadoPeriodo = estadoPeriodoCalculado
                )

                if (exito) {
                    cargarHistorial(atletaId)
                } else {
                    _state.update { it.copy(isLoading = false, error = "No se pudo encolar o renovar el plan.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al procesar el plan") }
            }
        }
    }

    fun eliminarPeriodoDiferido(atletaId: String, periodoId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val exito = userRepository.cancelarPeriodo(atletaId, periodoId)
            if (exito) {
                cargarHistorial(atletaId)
            } else {
                _state.update { it.copy(isLoading = false, error = "No se puede eliminar un periodo activo antiguo.") }
            }
        }
    }
}