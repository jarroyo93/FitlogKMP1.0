package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable


@Serializable
data class CicloEntrenamiento(
    val id: String = "", // 👈 Recomendación: Inicializar vacío y usar el ID autogenerado de Firebase
    val atletaId: String = "",
    val rutinaAsignadaId: String = "",

    @Serializable(with = TimestampLongSerializer::class)
    val fechaInicio: Long = 0L,

    // 🟢 CORRECCIÓN CRÍTICA: Hacemos lo mismo con la fecha de finalización
    @Serializable(with = TimestampLongSerializer::class)
    val fechaCierre: Long = 0L,
    val estaActivo: Boolean = true,

    // --- MÉTRICA 1: ASISTENCIA ---
    val metaSesionesAsignadas: Int = 0,
    val sesionesCompletadas: Int = 0,
    val porcentajeAsistencia: Double = 0.0,

    // --- MÉTRICA 2: VOLUMEN ---
    val repeticionesMetaTotal: Int = 0,
    val repeticionesLogradasTotal: Int = 0,
    val porcentajeVolumenGlobal: Double = 0.0
)