package dev.josearroyo.fitlog.data.remoteconfig

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.get
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.seconds

object RemoteConfigManager {
    private val remoteConfig get() = Firebase.remoteConfig

    // --- Perfil e Informativos Generales ---
    private val _textoPerfilInformativo = MutableStateFlow("")
    val textoPerfilInformativo: StateFlow<String> = _textoPerfilInformativo.asStateFlow()

    private val _glosarioFitnessJson = MutableStateFlow("")
    val glosarioFitnessJson: StateFlow<String> = _glosarioFitnessJson.asStateFlow()

    // --- Guías de Pantallas ---
    private val _ayudaDashboardInicio = MutableStateFlow("")
    val ayudaDashboardInicio: StateFlow<String> = _ayudaDashboardInicio.asStateFlow()

    private val _ayudaExpedienteAtleta = MutableStateFlow("")
    val ayudaExpedienteAtleta: StateFlow<String> = _ayudaExpedienteAtleta.asStateFlow()

    private val _ayudaProgresoAtleta = MutableStateFlow("")
    val ayudaProgresoAtleta: StateFlow<String> = _ayudaProgresoAtleta.asStateFlow()

    private val _ayudaPantallaEntrenar = MutableStateFlow("")
    val ayudaPantallaEntrenar: StateFlow<String> = _ayudaPantallaEntrenar.asStateFlow()

    private val _ayudaSemaforoEntrenador = MutableStateFlow("")
    val ayudaSemaforoEntrenador: StateFlow<String> = _ayudaSemaforoEntrenador.asStateFlow()

    // --- Estados de Suscripción y Vinculación ---
    private val _msgPlanVencido = MutableStateFlow("")
    val msgPlanVencido: StateFlow<String> = _msgPlanVencido.asStateFlow()

    private val _msgCuentaPausada = MutableStateFlow("")
    val msgCuentaPausada: StateFlow<String> = _msgCuentaPausada.asStateFlow()

    private val _msgPlanDiferido = MutableStateFlow("")
    val msgPlanDiferido: StateFlow<String> = _msgPlanDiferido.asStateFlow()

    private val _msgInstruccionesVinculacion = MutableStateFlow("")
    val msgInstruccionesVinculacion: StateFlow<String> = _msgInstruccionesVinculacion.asStateFlow()

    suspend fun inicializar(esDesarrollo: Boolean = true) {
        try {
            remoteConfig.settings {
                minimumFetchInterval = if (esDesarrollo) 0.seconds else 3600.seconds
            }

            remoteConfig.setDefaults(
                "perfil_texto_informativo" to "Mantén tu consistencia de entrenamiento activa.",
                "glosario_fitness_json" to """[
                {"termino": "RPE", "definicion": "Escala de esfuerzo percibido del 1 al 10."},
                {"termino": "RIR", "definicion": "Repeticiones en recámara antes de llegar al fallo."}
            ]""",
                "ayuda_dashboard_inicio" to """
- Progreso del Ciclo: La Asistencia mide tus días entrenados. El Cumplimiento de Rutina evalúa el volumen real (repeticiones y series) ejecutado frente a lo programado.


- Tu Entrenamiento de Hoy: Te indica automáticamente cuál es el día sugerido para entrenar según tu historial reciente.


- Control de Peso: Registra tu peso corporal periódicamente para monitorear tu tendencia de composición física.

            """.trimIndent(),
                "ayuda_expediente_atleta" to """
- Panel de Control:

- Asistencia: Mide los días entrenados frente a los programados en el ciclo activo.

- Vol. Meta: Porcentaje de cumplimiento en volumen (series y repeticiones ejecutadas vs. prescritas).

- RPE Medio: Promedio de fatiga percibida (1 a 10). Un RPE constantemente superior a 8.5 sugiere evaluar sobrecarga.

- Top Exigencia: Ejercicios con mayor fatiga neuromuscular reportada.


- Expediente General: Accesos directos a Valoración antropométrica, Registro de hábitos, Datos del atleta y Diario de cargas completo.


- Comentarios del Atleta: Alertas y sensaciones reportadas por el alumno en vivo durante sus entrenamientos.


- Programa Activo: Gestión y edición de la rutina activa asignada (máximo 1 programa por ciclo).

            """.trimIndent(),
                "ayuda_progreso_atleta" to """
- Evolución: Selecciona cualquier ejercicio para analizar tu progresión de carga máxima (kg), volumen total y fuerza teórica estimada (1RM) a lo largo del tiempo.


- Diario de Ciclos: Revisa el historial de sesiones de tu ciclo activo o ciclos anteriores, filtrando por ejercicio, tonelaje y desglose serie por serie.


- Récords: Consulta tus mejores marcas históricas alcanzadas (máximo peso levantado y repeticiones) por cada ejercicio.

            """.trimIndent(),
                "ayuda_pantalla_entrenar" to """
- Registro de Series: Ingresa el peso (kg) y las repeticiones ejecutadas en cada serie. La app evaluará en tiempo real tu volumen frente a la pauta.


- Intensidad (RPE): Toca el indicador de cada serie al finalizarla para calificar del 1 al 10 tu esfuerzo percibido o repeticiones en recámara (RIR).


- Indicaciones del Coach: Consulta el ícono de nota en la barra superior para indicaciones generales de la rutina, o los íconos (i) de cada ejercicio para ver la pauta técnica.


- Cronómetro de Descanso: Toca el tiempo de descanso recomendado para activar la cuenta regresiva flotante entre series.

            """.trimIndent(),
                "ayuda_semaforo_entrenador" to """
- Semáforo de Adherencia: Monitorea la actividad de tus atletas en tiempo real.


- Estado Verde (Activo): El atleta ha entrenado en los últimos días y mantiene un volumen adecuado.


- Estado Amarillo (Atención): El atleta lleva entre 4 y 7 días sin registrar entrenamientos o presenta caídas de volumen.


- Estado Rojo (Riesgo / Inactivo): Más de 7 días sin entrenar o reporte sostenido de fatiga neuromuscular alta (RPE > 8.5).

            """.trimIndent(),
                "msg_plan_vencido" to "Tu ciclo ha terminado. Solicita la renovación a tu entrenador para continuar tu proceso.",
                "msg_cuenta_pausada" to "Tu plan de entrenamiento está pausado. Comunícate con tu coach para reactivarlo.",
                "msg_plan_diferido" to "Tu plan está agendado para iniciar próximamente. Vuelve a consultar cuando llegue la fecha de inicio.",
                "msg_instrucciones_vinculacion" to "Para utilizar la plataforma, pide a tu entrenador que genere un código de vinculación en su perfil e ingrésalo a continuación."
            )

            // 🔄 Descarga e inicializa el primer estado
            fetchAndActivate()
        } catch (e: Exception) {
            println("  [RemoteConfigManager] Error al sincronizar Remote Config: ${e.message}")
        }
    }

    /**
     * 🔄 Función pública expuesta para invocarse en el Pull-to-Refresh.
     * Descarga de Firebase, activa las claves y actualiza los StateFlows de la UI.
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            val activado = remoteConfig.fetchAndActivate()
            actualizarValores()
            activado
        } catch (e: Exception) {
            println("  [RemoteConfigManager] Error al refrescar datos en demanda: ${e.message}")
            false
        }
    }

    /**
     * Re-pobla de manera centralizada todos los StateFlows con los valores vigentes de Firebase.
     */
    private fun actualizarValores() {
        _textoPerfilInformativo.value = remoteConfig.get("perfil_texto_informativo")
        _glosarioFitnessJson.value = remoteConfig.get("glosario_fitness_json")
        _ayudaDashboardInicio.value = remoteConfig.get<String>("ayuda_dashboard_inicio")
            .replace(Regex("<br\\s*/?>"), "\n")
        _ayudaExpedienteAtleta.value = remoteConfig.get<String>("ayuda_expediente_atleta")
            .replace(Regex("<br\\s*/?>"), "\n")
        _ayudaProgresoAtleta.value = remoteConfig.get<String>("ayuda_progreso_atleta")
            .replace(Regex("<br\\s*/?>"), "\n")
        _ayudaPantallaEntrenar.value = remoteConfig.get<String>("ayuda_pantalla_entrenar")
            .replace(Regex("<br\\s*/?>"), "\n")
        _ayudaSemaforoEntrenador.value = remoteConfig.get<String>("ayuda_semaforo_entrenador")
            .replace(Regex("<br\\s*/?>"), "\n")
        _msgPlanVencido.value = remoteConfig.get("msg_plan_vencido")
        _msgCuentaPausada.value = remoteConfig.get("msg_cuenta_pausada")
        _msgPlanDiferido.value = remoteConfig.get("msg_plan_diferido")
        _msgInstruccionesVinculacion.value = remoteConfig.get("msg_instrucciones_vinculacion")
    }
}