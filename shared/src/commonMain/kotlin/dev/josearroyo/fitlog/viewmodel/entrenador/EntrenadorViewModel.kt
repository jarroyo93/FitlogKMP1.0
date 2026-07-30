package dev.josearroyo.fitlog.viewmodel.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.formatearHora
import dev.josearroyo.fitlog.formatearFechaHora
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AsistenciaAtletaUI(
    val atleta: Usuario,
    val asistio: Boolean,
    val horaEntrenamiento: String? = null
)

// 🟢 Estado unificado con bandera explícita para la visibilidad del modal
data class EntrenadorUiState(
    val atletas: List<Usuario> = emptyList(),
    val asistenciaDia: List<AsistenciaAtletaUI> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingAsistencia: Boolean = false,
    val isGeneratingCode: Boolean = false,
    val codigoGenerado: String? = null,
    val expiracionCodigoTexto: String? = null,
    val mostrarModalCodigo: Boolean = false, // 🟢 Controla si el modal se muestra o no
    val textoBusqueda: String = "",
    val tabSeleccionado: Int = 0,
    val error: String? = null
)

class EntrenadorViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val progresoRepository = AtletaProgresoRepository()

    private var listaAtletasCompleta: List<Usuario> = emptyList()
    private var listaAsistenciaCompleta: List<AsistenciaAtletaUI> = emptyList()

    private val _uiState = MutableStateFlow(EntrenadorUiState())
    val uiState: StateFlow<EntrenadorUiState> = _uiState.asStateFlow()

    fun cargarDashboard(entrenadorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                listaAtletasCompleta = userRepository.obtenerAtletasPorEntrenador(entrenadorId)
                val perfilCoach = userRepository.obtenerUsuario(entrenadorId)
                val ahora = getCurrentTimeMillis()

                val codigo = if (perfilCoach?.codigoVinculacion != null && (perfilCoach.expiracionCodigo ?: 0L) > ahora) {
                    perfilCoach.codigoVinculacion
                } else null

                val expiracionTexto = perfilCoach?.expiracionCodigo?.let { formatearFechaHora(it) }

                _uiState.update { state ->
                    state.copy(
                        codigoGenerado = codigo,
                        expiracionCodigoTexto = expiracionTexto
                        // 🟢 NOTA: No activamos 'mostrarModalCodigo' aquí para evitar pop-ups al cargar/refrescar
                    )
                }

                aplicarBusqueda(_uiState.value.textoBusqueda)
                cargarAsistenciaDelDia()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al cargar el dashboard") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun cargarAsistenciaDelDia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAsistencia = true) }
            try {
                val hoy = getCurrentTimeMillis()

                supervisorScope {
                    listaAsistenciaCompleta = listaAtletasCompleta.map { atleta ->
                        async {
                            val historial = progresoRepository.obtenerHistorialEntrenamientos(atleta.id)
                            val sesionDeHoy = historial.find { esMismoDia(it.fechaEjecucion, hoy) }

                            AsistenciaAtletaUI(
                                atleta = atleta,
                                asistio = sesionDeHoy != null,
                                horaEntrenamiento = sesionDeHoy?.let { formatearHora(it.fechaEjecucion) }
                            )
                        }
                    }.awaitAll()
                }

                aplicarBusqueda(_uiState.value.textoBusqueda)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al procesar asistencia.") }
            } finally {
                _uiState.update { it.copy(isLoadingAsistencia = false) }
            }
        }
    }

    fun aplicarBusqueda(query: String) {
        val filteredAtletas = listaAtletasCompleta.filter {
            "${it.nombres} ${it.apellidos}".contains(query, ignoreCase = true)
        }

        val filteredAsistencia = listaAsistenciaCompleta.filter {
            "${it.atleta.nombres} ${it.atleta.apellidos}".contains(query, ignoreCase = true)
        }

        _uiState.update { it.copy(
            textoBusqueda = query,
            atletas = filteredAtletas,
            asistenciaDia = filteredAsistencia
        ) }
    }

    fun cambiarTab(index: Int) {
        _uiState.update { it.copy(tabSeleccionado = index) }
    }

    fun generarCodigoVinculacion(entrenadorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingCode = true, error = null) }
            try {
                val codigo = userRepository.generarCodigoVinculacion(entrenadorId)
                val tiempoExpiracion = getCurrentTimeMillis() + 900000

                _uiState.update { it.copy(
                    codigoGenerado = codigo,
                    expiracionCodigoTexto = formatearFechaHora(tiempoExpiracion),
                    mostrarModalCodigo = true // 🟢 Activamos la visibilidad solo al generar deliberadamente
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo generar el código.") }
            } finally {
                _uiState.update { it.copy(isGeneratingCode = false) }
            }
        }
    }

    fun ocultarModalCodigo() {
        _uiState.update { it.copy(mostrarModalCodigo = false) }
    }

    fun limpiarCodigo() {
        _uiState.update { it.copy(codigoGenerado = null, expiracionCodigoTexto = null, mostrarModalCodigo = false) }
    }
}