package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

/**
 * Representa los estados unificados del semáforo en toda la aplicación.
 */
@Serializable
enum class EstadoSemaforo {
    VERDE,           // Óptimo / Al día
    AMARILLO,        // En observación / Desvío leve
    ROJO,            // Crítico / Inactividad / Alerta alta
    REQUIERE_GESTION,// Vencido, sin rutina, requiere acción del coach
    SIN_DATOS        // Sin registros en el período
}

/**
 * Detalle individual de una métrica dentro del expediente del atleta.
 */
@Serializable
data class MetricaSemaforo(
    val valor: Double,
    val estado: EstadoSemaforo,
    val detalle: String
)

/**
 * Evaluación consolidada para el expediente del atleta (AtletaDetailScreen).
 */
@Serializable
data class EvaluacionSemanaAtleta(
    val adherencia: MetricaSemaforo,
    val volumenEfectivo: MetricaSemaforo,
    val fatigaRpe: MetricaSemaforo,
    val estadoGlobal: EstadoSemaforo
)

/**
 * Item simplificado de un atleta preparado para renderizarse en las tarjetas del Dashboard.
 */
@Serializable
data class AtletaSemaforoItem(
    val atletaId: String,
    val nombreCompleto: String,
    val fotoUrl: String?,
    val estado: EstadoSemaforo,
    val adherenciaPorcentaje: Int,
    val sesionesEjecutadas: Int,
    val sesionesEsperadasHoy: Int,
    val rpePromedio: Float?,
    val mensajeGestion: String? = null,
    val motivosAlerta: List<String> = emptyList() // 🟢 NUEVO: Lista de alertas para la Card
)

/**
 * Cabecera gerencial con totales y salud global del equipo para el Entrenador.
 */
@Serializable
data class ResumenGerencial(
    val porcentajeSaludEquipo: Int,
    val totalAtletasCriticos: Int,
    val totalAtletasEnRiesgo: Int,
    val totalRequierenGestion: Int
)