package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RutinaAsignada(
    var id: String = "",
    val nombreRutina: String = "",

    // Conversión segura del Timestamp de asignación
    @Serializable(with = TimestampLongSerializer::class)
    val fechaAsignacion: Long = 0L,

    val estaActiva: Boolean = true,
    val notasEntrenador: String = "",
    val diasEntrenamiento: List<DiaEntrenamientoAsignado> = emptyList(),

    // 🚀 SOLUCIÓN: Cambiado a Long? para tolerar los valores null de la app nativa
    @Serializable(with = TimestampLongSerializer::class)
    val ultimaVezEjecutada: Long? = null
)

@Serializable
data class DiaEntrenamientoAsignado(
    val idDia: String = "",
    val plantillaOriginalId: String = "",
    val nombreDia: String = "",
    val ordenSecuencia: Int = 0,
    val ejercicios: List<EjercicioAsignado> = emptyList(),

    // 🚀 SOLUCIÓN: Cambiado a Long? para días que nunca han sido ejecutados
    @Serializable(with = TimestampLongSerializer::class)
    val ultimaVezEjecutada: Long? = null
)

@Serializable
data class EjercicioAsignado(
    val idInterno: String = "",
    val ejercicioGlobalId: String = "",
    val nombre: String = "",
    val seriesPrescritas: List<PrescripcionSerie> = emptyList(),
    val descansoSegundos: Int = 0,
    val notasEspecificas: String = "",
    val ordenSecuencia: Int = 0
)