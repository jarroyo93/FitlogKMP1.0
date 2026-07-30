package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Pesaje(
    val id: String = "",
    val pesoKg: Double = 0.0,
    val notas: String = "",
    @Serializable(with = TimestampLongSerializer::class)
    val fecha: Long = 0L
)