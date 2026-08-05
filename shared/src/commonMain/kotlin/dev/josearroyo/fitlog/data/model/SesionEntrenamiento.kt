package dev.josearroyo.fitlog.data.model

import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
enum class EstadoSesion {
    EN_PROGRESO,
    COMPLETADA
}

@Serializable
data class SesionEntrenamiento(
    var id: String = "",
    val rutinaAsignadaId: String = "",
    val diaEntrenamientoId: String = "",
    val nombreRutina: String = "",

    @Serializable(with = TimestampLongSerializer::class)
    val fechaInicio: Long = getCurrentTimeMillis(),

    @Serializable(with = TimestampLongSerializer::class)
    val fechaEjecucion: Long = 0L,

    val estado: EstadoSesion = EstadoSesion.EN_PROGRESO,

    val ejerciciosRealizados: List<EjercicioRealizado> = emptyList(),
    val totalRepsEfectivasMeta: Int = 0,
    val totalRepsEfectivasLogradas: Int = 0,
    val porcentajeVolumenSesion: Double = 0.0
)

@Serializable
data class EjercicioRealizado(
    val ejercicioGlobalId: String = "",
    val nombreEjercicio: String = "",
    val ordenSecuencia: Int = 0,
    val seriesRealizadas: List<SerieRealizada> = listOf(),
    val notasAtleta: String = "",
    val fueSaltado: Boolean = false,
    val justificacionSalto: String = ""
)

@Serializable
data class SerieRealizada(
    val numeroSerie: Int = 1,
    val tipoSerie: TipoSerie = TipoSerie.EFECTIVA,
    val pesoKg: Double = 0.0,
    val repeticionesLogradas: Int = 0,
    val rpe: Int? = null,
    val pesoTarget: Double = 0.0,
    val repsTarget: Int = 0
)

fun SesionEntrenamiento.calcularMetricas(): SesionEntrenamiento {
    var metaTotal = 0
    var logradasTotal = 0

    this.ejerciciosRealizados.forEach { ejercicio ->
        ejercicio.seriesRealizadas.forEach { serie ->
            if (serie.tipoSerie != TipoSerie.APROXIMACION) {
                metaTotal += serie.repsTarget
                if (!ejercicio.fueSaltado) {
                    logradasTotal += serie.repeticionesLogradas
                }
            }
        }
    }

    val porcentaje = if (metaTotal > 0) {
        (logradasTotal.toDouble() / metaTotal.toDouble()) * 100.0
    } else 0.0

    return this.copy(
        totalRepsEfectivasMeta = metaTotal,
        totalRepsEfectivasLogradas = logradasTotal,
        porcentajeVolumenSesion = porcentaje
    )
}