package dev.josearroyo.fitlog.data.model

import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.obtenerInicioSemanaLunes
import dev.josearroyo.fitlog.obtenerFinSemanaDomingo
import kotlin.math.roundToInt

/**
 * Calcula las fechas de inicio y cierre del ciclo según el modo de programación.
 */
fun calcularRangoFechasCiclo(
    fechaPrimerRegistroMs: Long,
    duracionDias: Int,
    modo: ModoCiclo
): Pair<Long, Long> {
    return if (modo == ModoCiclo.CALENDARIO_SEMANAL) {
        val semanas = maxOf(1, (duracionDias / 7))
        val inicioLunes = obtenerInicioSemanaLunes(fechaPrimerRegistroMs)
        val finDomingo = obtenerFinSemanaDomingo(fechaPrimerRegistroMs, semanas)
        Pair(inicioLunes, finDomingo)
    } else {
        val milisPorDia = 86_400_000L
        val inicioReal = fechaPrimerRegistroMs
        val cierreReal = inicioReal + (duracionDias.toLong() * milisPorDia)
        Pair(inicioReal, cierreReal)
    }
}

fun CicloEntrenamiento.estaPorVencer(ahoraMilis: Long = getCurrentTimeMillis()): Boolean {
    if (!estaActivo) return false
    val milisPorDia = 86_400_000L
    val diasRestantes = (fechaCierre - ahoraMilis) / milisPorDia
    val sesionesRestantes = metaSesionesAsignadas - sesionesCompletadas

    val porCierreCalendario = diasRestantes in 0..2
    val tiempoTranscurridoMilis = ahoraMilis - fechaInicio
    val tiempoTotalMilis = maxOf(1L, fechaCierre - fechaInicio)
    val porcentajeTiempoTranscurrido = (tiempoTranscurridoMilis.toDouble() / tiempoTotalMilis.toDouble())

    val porAgotamientoSesiones = (sesionesRestantes <= 1) && (porcentajeTiempoTranscurrido >= 0.60)
    return porCierreCalendario || porAgotamientoSesiones
}

fun CicloEntrenamiento.estaVencido(ahoraMilis: Long = getCurrentTimeMillis()): Boolean {
    return !estaActivo || (metaSesionesAsignadas > 0 && sesionesCompletadas >= metaSesionesAsignadas)
}

fun CicloEntrenamiento.sincronizarConRutina(
    rutinaActualizada: RutinaAsignada
): CicloEntrenamiento {
    val cantidadDiasRutina = rutinaActualizada.diasEntrenamiento.size
    if (cantidadDiasRutina == 0) {
        return this.copy(
            metaSesionesAsignadas = 0,
            repeticionesMetaTotal = 0,
            porcentajeAsistencia = 0.0,
            porcentajeVolumenGlobal = 0.0
        )
    }

    val modo = rutinaActualizada.modoCiclo
    val nuevaDuracionDias = if (modo == ModoCiclo.CALENDARIO_SEMANAL) {
        // En modo semanal forzamos múltiplos de 7
        val semanas = maxOf(1, (this.duracionDias / 7))
        semanas * 7
    } else {
        maxOf(this.duracionDias, cantidadDiasRutina)
    }

    val (nuevaFechaInicio, nuevaFechaCierre) = calcularRangoFechasCiclo(
        fechaPrimerRegistroMs = if (this.fechaInicio > 0L) this.fechaInicio else getCurrentTimeMillis(),
        duracionDias = nuevaDuracionDias,
        modo = modo
    )

    val semanas = nuevaDuracionDias.toDouble() / 7.0
    val metaSesionesFinal = if (modo == ModoCiclo.CALENDARIO_SEMANAL) {
        (cantidadDiasRutina.toDouble() * semanas).roundToInt()
    } else {
        cantidadDiasRutina
    }

    var repsMetaUnPasada = 0
    rutinaActualizada.diasEntrenamiento.forEach { dia ->
        dia.ejercicios.forEach { ej ->
            ej.seriesPrescritas.forEach { serie ->
                if (serie.tipo != TipoSerie.APROXIMACION) {
                    repsMetaUnPasada += serie.maxReps
                }
            }
        }
    }

    val multiplicadorVolumen = if (modo == ModoCiclo.CALENDARIO_SEMANAL) semanas else 1.0
    val nuevasRepsMetaTotal = (repsMetaUnPasada.toDouble() * multiplicadorVolumen).roundToInt()

    val nuevoPorcentajeAsist = if (metaSesionesFinal > 0) {
        (this.sesionesCompletadas.toDouble() / metaSesionesFinal.toDouble()) * 100.0
    } else 0.0

    val nuevoPorcentajeVol = if (nuevasRepsMetaTotal > 0) {
        (this.repeticionesLogradasTotal.toDouble() / nuevasRepsMetaTotal.toDouble()) * 100.0
    } else 0.0

    return this.copy(
        modoCiclo = modo,
        duracionDias = nuevaDuracionDias,
        fechaInicio = nuevaFechaInicio,
        fechaCierre = nuevaFechaCierre,
        metaSesionesAsignadas = metaSesionesFinal,
        repeticionesMetaTotal = nuevasRepsMetaTotal,
        porcentajeAsistencia = nuevoPorcentajeAsist,
        porcentajeVolumenGlobal = nuevoPorcentajeVol
    )
}