package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModoCiclo {
    CALENDARIO_SEMANAL,  // Anclado de Lunes 00:00 a Domingo 23:59
    SECUENCIAL_RODANTE   // Contado en días exactos desde el primer entrenamiento
}

@Serializable
data class CicloEntrenamiento(
    val id: String = "",
    val atletaId: String = "",
    val rutinaAsignadaId: String = "",
    @Serializable(with = TimestampLongSerializer::class)
    val fechaInicio: Long = 0L,
    @Serializable(with = TimestampLongSerializer::class)
    val fechaCierre: Long = 0L,
    val duracionDias: Int = 7,
    val modoCiclo: ModoCiclo = ModoCiclo.CALENDARIO_SEMANAL,
    val estaActivo: Boolean = true,
    val metaSesionesAsignadas: Int = 0,
    val sesionesCompletadas: Int = 0,
    val porcentajeAsistencia: Double = 0.0,
    val repeticionesMetaTotal: Int = 0,
    val repeticionesLogradasTotal: Int = 0,
    val porcentajeVolumenGlobal: Double = 0.0
)