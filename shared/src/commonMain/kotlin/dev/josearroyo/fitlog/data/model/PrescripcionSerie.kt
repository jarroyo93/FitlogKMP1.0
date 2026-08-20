package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TipoSerie {
    APROXIMACION, EFECTIVA, DROP_SET, FALLO, REST_PAUSE
}

@Serializable
data class PrescripcionSerie(
    val idInterno: String = "",
    val numeroSerie: Int = 1,
    val repsMin: Int = 0,
    val repsMax: Int = 0,
    val repeticiones: Int = 0,
    val tipo: TipoSerie = TipoSerie.EFECTIVA
) {
    // Getters de lectura segura
    val minReps: Int get() = if (repsMin > 0) repsMin else if (repeticiones > 0) repeticiones else 0
    val maxReps: Int get() = if (repsMax > 0) repsMax else if (repeticiones > 0) repeticiones else minReps
}