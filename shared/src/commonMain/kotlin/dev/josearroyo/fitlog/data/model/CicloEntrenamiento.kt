package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CicloEntrenamiento(
    val id: String = "",
    val atletaId: String = "",
    val rutinaAsignadaId: String = "",

    @Serializable(with = TimestampLongSerializer::class)
    val fechaInicio: Long = 0L,

    @Serializable(with = TimestampLongSerializer::class)
    val fechaCierre: Long = 0L,
    val estaActivo: Boolean = true,

    val metaSesionesAsignadas: Int = 0,
    val sesionesCompletadas: Int = 0,
    val porcentajeAsistencia: Double = 0.0,

    val repeticionesMetaTotal: Int = 0,
    val repeticionesLogradasTotal: Int = 0,
    val porcentajeVolumenGlobal: Double = 0.0
)