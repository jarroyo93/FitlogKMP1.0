package dev.josearroyo.fitlog.viewmodel.atleta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.data.model.Pesaje
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.repository.AtletaProgresoRepository
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AtletaInicioState(
    val isLoading: Boolean = true,
    val usuario: Usuario? = null,
    val rutinasSugeridas: List<RutinaAsignada> = emptyList(),
    val ultimosPesajes: List<Pesaje> = emptyList(),
    val cicloActivo: CicloEntrenamiento? = null,
    val error: String? = null
)

class AtletaInicioViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val atletaRepository = AtletaRepository()
    private val atletaProgresoRepository = AtletaProgresoRepository()

    private val _state = MutableStateFlow(AtletaInicioState())
    val state: StateFlow<AtletaInicioState> = _state.asStateFlow()

    private var currentAtletaId: String? = null

    fun cargarDashboard(authUid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val usuario = userRepository.obtenerUsuario(authUid)
                if (usuario == null) {
                    _state.update { it.copy(isLoading = false, error = "Usuario no encontrado en BD.") }
                    return@launch
                }
                currentAtletaId = usuario.id


                kotlinx.coroutines.supervisorScope {
                    val rutinasDeferred = async { atletaRepository.obtenerRutinasActivas(usuario.id) }
                    val pesajesDeferred = async { atletaProgresoRepository.obtenerUltimosPesajes(usuario.id, limite = 4) }
                    val cicloDeferred = async { atletaProgresoRepository.obtenerCicloActivo(usuario.id) }

                    val rutinas = rutinasDeferred.await()
                    val pesajes = pesajesDeferred.await()
                    val ciclo = cicloDeferred.await()

                    _state.update {
                        it.copy(
                            isLoading = false,
                            usuario = usuario,
                            rutinasSugeridas = rutinas,
                            ultimosPesajes = pesajes,
                            cicloActivo = ciclo
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar datos") }
            }
        }
    }

    fun registrarPeso(pesoKg: Double, notas: String) {
        val atletaId = currentAtletaId ?: return
        viewModelScope.launch {
            try { // 🟢 Protegido contra caídas de red al guardar peso
                val nuevoPesaje = Pesaje(
                    pesoKg = pesoKg,
                    notas = notas,
                    fecha = getCurrentTimeMillis()
                )
                val exito = atletaProgresoRepository.registrarPesaje(atletaId, nuevoPesaje)
                if (exito) {
                    cargarDashboard(atletaId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo registrar el peso: ${e.message}") }
            }
        }
    }
}