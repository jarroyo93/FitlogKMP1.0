package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TipoSerie {
    APROXIMACION, EFECTIVA, DROP_SET, FALLO, REST_PAUSE
}
@Serializable
data class PrescripcionSerie(
    val idInterno: String = "", // Cambiado UUID -> String vacío por defecto
    val numeroSerie: Int = 1,
    val repeticiones: Int = 0,
    val tipo: TipoSerie = TipoSerie.EFECTIVA
)