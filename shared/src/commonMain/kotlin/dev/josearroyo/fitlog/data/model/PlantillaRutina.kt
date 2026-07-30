package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable


@Serializable
data class ElementoRutina(
    val ejercicioId: String = "",
    val nombreEjercicio: String = "",
    val seriesPrescritas: List<PrescripcionSerie> = emptyList(),
    val descansoSegundos: Int = 60,
    val notas: String = "",
    val ordenSecuencia: Int = 0
)
@Serializable
data class PlantillaRutina(
    val id: String = "", // Removido @DocumentId
    val nombre: String = "",
    val entrenadorId: String = "",
    val ejercicios: List<ElementoRutina> = emptyList(),
    val etiquetas: List<String> = emptyList(),
    val activo: Boolean = true
)