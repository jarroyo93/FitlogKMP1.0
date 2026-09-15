package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ElementoRutina(
    val ejercicioId: String = "",
    val nombreEjercicio: String = "",
    val seriesPrescritas: List<PrescripcionSerie> = emptyList(),
    val descansoSegundos: Int = 60,
    val notas: String = "",
    val ordenSecuencia: Int = 0,
    val bloqueId: String? = null,
    val bloqueNombre: String? = null
)

@Serializable
data class PlantillaRutina(
    val id: String = "",
    val nombre: String = "",
    val entrenadorId: String = "",
    val ejercicios: List<ElementoRutina> = emptyList(),
    val etiquetas: List<String> = emptyList(),
    val activo: Boolean = true
)