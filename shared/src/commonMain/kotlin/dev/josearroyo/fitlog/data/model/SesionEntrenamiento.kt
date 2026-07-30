package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SesionEntrenamiento(
    var id: String = "",
    val rutinaAsignadaId: String = "",
    val diaEntrenamientoId: String = "",
    val nombreRutina: String = "",

    // 🚀 SOLUCIÓN AL ERROR DE LOGCAT: Mapea el objeto Timestamp de la base de datos de producción a Long
    @Serializable(with = TimestampLongSerializer::class)
    val fechaEjecucion: Long = 0L,

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

// 🔥 Tu función de extensión de métricas intacta y funcional en KMP
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