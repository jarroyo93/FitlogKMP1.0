package dev.josearroyo.fitlog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.calcularFechaFinSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.EstadoPeriodo
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days

enum class FiltroFacturacion(val etiqueta: String) {
    TODOS("Todos"),
    ACTIVOS("Activos"),
    PROXIMOS_A_VENCER("Próximos (3 días)"),
    DIFERIDOS("Programados"),
    PAUSADOS("Pausados"),
    VENCIDOS("Vencidos"),
    SIN_PLAN("Sin Plan")
}

data class FacturacionState(
    val isLoading: Boolean = true,
    val atletas: List<Usuario> = emptyList(),
    val atletasFiltrados: List<Usuario> = emptyList(),
    val searchQuery: String = "",
    val filtroActual: FiltroFacturacion = FiltroFacturacion.TODOS,
    val error: String? = null
)

class FacturacionViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val _state = MutableStateFlow(FacturacionState())
    val state = _state.asStateFlow()

    fun cargarAtletas(entrenadorId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val lista = userRepository.obtenerAtletasPorEntrenador(entrenadorId)
                _state.update { it.copy(atletas = lista, isLoading = false) }
                aplicarFiltros()
            } catch (e: Exception) {
                println("🔥 [FacturacionViewModel] Error al cargar atletas: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión al obtener la lista") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        aplicarFiltros()
    }

    fun onFiltroChanged(filtro: FiltroFacturacion) {
        _state.update { it.copy(filtroActual = filtro) }
        aplicarFiltros()
    }

    fun limpiarError() {
        _state.update { it.copy(error = null) }
    }

    private fun aplicarFiltros() {
        val query = _state.value.searchQuery.lowercase()
        val filtro = _state.value.filtroActual
        val ahora = getCurrentTimeMillis()
        val tresDiasEnMillis = 3.days.inWholeMilliseconds

        val filtrados = _state.value.atletas.filter { atleta ->
            val coincideBusqueda = atleta.nombres.lowercase().contains(query) ||
                    atleta.apellidos.lowercase().contains(query) ||
                    atleta.correo.lowercase().contains(query)

            val coincideFiltro = when (filtro) {
                FiltroFacturacion.TODOS -> true
                FiltroFacturacion.ACTIVOS -> atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO
                FiltroFacturacion.PROXIMOS_A_VENCER -> {
                    val vencimiento = atleta.vencimientoSuscripcion ?: 0L
                    atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO &&
                            (vencimiento - ahora) in 0..tresDiasEnMillis
                }
                FiltroFacturacion.DIFERIDOS -> atleta.estadoSuscripcion == EstadoSuscripcion.DIFERIDO
                FiltroFacturacion.PAUSADOS -> atleta.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO && atleta.saldoMilisegundosRestantes != null
                FiltroFacturacion.VENCIDOS -> atleta.estadoSuscripcion == EstadoSuscripcion.VENCIDO
                FiltroFacturacion.SIN_PLAN -> atleta.estadoSuscripcion == EstadoSuscripcion.HUERFANO || atleta.planActivo == "Ninguno"
            }

            coincideBusqueda && coincideFiltro
        }
        _state.update { it.copy(atletasFiltrados = filtrados) }
    }

    fun renovarAtleta(
        atletaId: String,
        entrenadorId: String,
        tipoPlan: TipoPlanSuscripcion,
        diasPersonalizados: Int,
        iniciarEnseguida: Boolean,
        fechaInicioSeleccionada: Long
    ) {
        viewModelScope.launch {
            val atleta = _state.value.atletas.find { it.id == atletaId } ?: return@launch
            val ahora = getCurrentTimeMillis()

            // 🛡️ REGLA 1: No permitir renovar si la membresía está pausada/congelada
            if (atleta.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO) {
                _state.update {
                    it.copy(error = "El atleta está en pausa. Debe reactivar su membresía antes de asignar un nuevo plan.")
                }
                return@launch
            }

            // 🛡️ REGLA 2: Validar cantidad de días mínima
            val diasDelPlan = if (tipoPlan == TipoPlanSuscripcion.PERSONALIZADO) {
                diasPersonalizados
            } else {
                tipoPlan.dias
            }

            if (diasDelPlan <= 0) {
                _state.update { it.copy(error = "La duración del plan debe ser de al menos 1 día.") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val ultimaFechaFinCadena = userRepository.obtenerUltimaFechaFinCadena(atletaId)

                // 🟢 REGLA 3: Evaluar si realmente tiene un plan activo corriendo en el futuro
                val tienePlanActivoCorriendo = atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO &&
                        (atleta.vencimientoSuscripcion ?: 0L) > (ahora + 60_000L)

                // 🟢 REGLA 4: Determinar fecha de inicio real (+1 ms para iniciar a las 00:00:00.000 del día siguiente)
                val fechaInicioLong = if (iniciarEnseguida && tienePlanActivoCorriendo) {
                    ultimaFechaFinCadena + 1L
                } else if (iniciarEnseguida) {
                    ahora
                } else {
                    fechaInicioSeleccionada
                }

                val fechaFinLong = calcularFechaFinSuscripcion(fechaInicioLong, diasDelPlan)

                // 🛡️ REGLA 5: Verificar solapamiento antes de persistir
                if (userRepository.existeSolapamientoPeriodo(atletaId, fechaInicioLong, fechaFinLong)) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "La fecha seleccionada genera un conflicto o solapamiento con un plan existente."
                        )
                    }
                    return@launch
                }

                // 🟢 REGLA 6: Clasificar el estado del nuevo período
                // Un plan es ACTIVO solo si el atleta no tiene plan corriendo Y la fecha es hoy o pasada.
                val esHoyOPasado = fechaInicioLong <= ahora || esMismoDia(fechaInicioLong, ahora)
                val estadoPeriodoCalculado = if (tienePlanActivoCorriendo || !esHoyOPasado) {
                    EstadoPeriodo.DIFERIDO
                } else {
                    EstadoPeriodo.ACTIVO
                }

                val exito = userRepository.renovarSuscripcion(
                    atletaId = atletaId,
                    entrenadorId = entrenadorId,
                    planActivo = tipoPlan.name,
                    fechaInicio = fechaInicioLong,
                    fechaFin = fechaFinLong,
                    estadoPeriodo = estadoPeriodoCalculado
                )

                if (exito) {
                    cargarAtletas(entrenadorId)
                } else {
                    _state.update {
                        it.copy(isLoading = false, error = "No se pudo renovar la suscripción. Intente nuevamente.")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Error inesperado al renovar la membresía.")
                }
            }
        }
    }

    fun pausarAtleta(atletaId: String, entrenadorId: String, motivo: String) {
        viewModelScope.launch {
            val atleta = _state.value.atletas.find { it.id == atletaId } ?: return@launch
            val vencimiento = atleta.vencimientoSuscripcion ?: 0L
            val ahora = getCurrentTimeMillis()

            val saldoMilis = if (vencimiento > ahora) vencimiento - ahora else 0L

            if (saldoMilis <= 0L) {
                _state.update { it.copy(error = "No se puede pausar una suscripción vencida o sin tiempo restante.") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val exito = userRepository.pausarAtleta(atletaId, motivo, saldoMilis)
                if (exito) {
                    cargarAtletas(entrenadorId)
                } else {
                    _state.update { it.copy(isLoading = false, error = "No se pudo congelar la membresía.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al pausar la membresía") }
            }
        }
    }

    fun reactivarAtleta(atletaId: String, entrenadorId: String) {
        viewModelScope.launch {
            val atleta = _state.value.atletas.find { it.id == atletaId } ?: return@launch
            val saldoMilis = atleta.saldoMilisegundosRestantes ?: 0L
            val ahora = getCurrentTimeMillis()

            if (saldoMilis <= 0L) {
                _state.update { it.copy(error = "El atleta no tiene saldo acumulado para reactivar.") }
                return@launch
            }

            val nuevaFechaFin = ahora + saldoMilis

            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val exito = userRepository.reactivarAtleta(atletaId, nuevaFechaFin)
                if (exito) {
                    cargarAtletas(entrenadorId)
                } else {
                    _state.update { it.copy(isLoading = false, error = "No se pudo reactivar la membresía.") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al reactivar la membresía") }
            }
        }
    }
}