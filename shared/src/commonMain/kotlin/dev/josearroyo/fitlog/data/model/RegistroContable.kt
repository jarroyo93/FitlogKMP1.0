package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistroContable(
    val id: String = "",
    val entrenadorId: String = "",
    val atletaId: String = "",
    val atletaNombreSnapshot: String = "",
    val tipoPlan: String = "",
    val fechaInicio: Long = 0L,
    val fechaFin: Long = 0L,
    val fechaRegistroTransaccion: Long = 0L,
    val estado: String = ""
)