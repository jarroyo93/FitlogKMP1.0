package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable
@Serializable
enum class GrupoMuscular {
    ABDOMEN, BRAZO, DOMINANTE_CADERA, DOMINANTE_RODILLA, ESPALDA, HOMBROS, PANTORRILLAS, PECHO, CARDIO
}
@Serializable
data class Ejercicio(
    val id: String = "", // Removido @DocumentId
    val nombre: String = "",
    val grupoMuscular: GrupoMuscular = GrupoMuscular.PECHO,
    val videoUrl: String = "",
    val instrucciones: String = "",
    val esPersonalizado: Boolean = false,
    val creadorId: String? = null,
    val activo: Boolean = true
)