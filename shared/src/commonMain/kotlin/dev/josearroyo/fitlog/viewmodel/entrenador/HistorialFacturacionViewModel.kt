package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.EstadoPeriodo
import dev.josearroyo.fitlog.data.model.PeriodoFacturable
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.calcularFechaFinSuscripcion // 🟢 Tu función del platform
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
                val infoAtleta = userRepository.obtenerUsuario(atletaId)
                val listaPeriodos = userRepository.obtenerPeriodosDeAtleta(atletaId)

                _state.update { it.copy(
                    isLoading = false,
                    atleta = infoAtleta,
                    periodos = listaPeriodos
                ) }
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
            _state.update { it.copy(isLoading = true) }

            val ahora = getCurrentTimeMillis()
            val diasDelPlan = if (plan == TipoPlanSuscripcion.PERSONALIZADO) diasPersonalizados else plan.dias


            val vencimientoMasLejanoEnLista = _state.value.periodos
                .filter { it.estado == EstadoPeriodo.ACTIVO || it.estado == EstadoPeriodo.DIFERIDO }
                .maxOfOrNull { it.fechaFin ?: 0L }

            val vencimientoBase = vencimientoMasLejanoEnLista ?: _state.value.atleta?.vencimientoSuscripcion ?: 0L


            val fechaInicioLong = if (iniciarEnseguida) {
                if (vencimientoBase > ahora) vencimientoBase + 1000L else ahora
            } else {
                fechaInicioSeleccionadaMilis
            }


            val fechaFinLong = calcularFechaFinSuscripcion(fechaInicioLong, diasDelPlan)

            val estadoPeriodoCalculado = if (fechaInicioLong > ahora) {
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
                _state.update { it.copy(isLoading = false, error = "No se pudo encolar el plan") }
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