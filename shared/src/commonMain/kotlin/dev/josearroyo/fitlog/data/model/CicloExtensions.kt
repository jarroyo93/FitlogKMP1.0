package dev.josearroyo.fitlog.data.model

import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.obtenerInicioSemanaLunes
import dev.josearroyo.fitlog.obtenerFinSemanaDomingo
import kotlin.math.roundToInt

/**
 * Helper para obtener únicamente la fecha de cierre del ciclo.
 */
fun calcularFechaCierreCiclo(
    inicioMilis: Long,
    duracionDias: Int,
    modo: ModoCiclo = ModoCiclo.SECUENCIAL_RODANTE
): Long {
    return calcularRangoFechasCiclo(
        fechaPrimerRegistroMs = inicioMilis,
        duracionDias = duracionDias,
        modo = modo
    ).second
}

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

    val porCierreCalendario = diasRestantes in 0..5
    val porAgotamientoSesiones = (metaSesionesAsignadas > 0 && sesionesRestantes <= 1)
    return porCierreCalendario || porAgotamientoSesiones
}

fun CicloEntrenamiento.estaVencido(ahoraMilis: Long = getCurrentTimeMillis()): Boolean {
    if (!estaActivo) return true
    val porFecha = fechaCierre > 0L && ahoraMilis >= fechaCierre
    val porSesiones = metaSesionesAsignadas > 0 && sesionesCompletadas >= metaSesionesAsignadas
    return porFecha || porSesiones
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
        val semanas = maxOf(1, (maxOf(this.duracionDias, cantidadDiasRutina) + 6) / 7)
        semanas * 7
    } else {
        maxOf(this.duracionDias, cantidadDiasRutina)
    }

    val (nuevaFechaInicio, nuevaFechaCierre) = if (modo == ModoCiclo.CALENDARIO_SEMANAL) {
        calcularRangoFechasCiclo(
            fechaPrimerRegistroMs = if (this.fechaInicio > 0L) this.fechaInicio else getCurrentTimeMillis(),
            duracionDias = nuevaDuracionDias,
            modo = modo
        )
    } else {
        val inicio = if (this.fechaInicio > 0L) this.fechaInicio else getCurrentTimeMillis()
        val cierre = inicio + (nuevaDuracionDias.toLong() * 86_400_000L)
        Pair(inicio, cierre)
    }

    val metaSesionesFinal = if (modo == ModoCiclo.CALENDARIO_SEMANAL) {
        val semanas = nuevaDuracionDias.toDouble() / 7.0
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

    val multiplicadorVolumen = if (modo == ModoCiclo.CALENDARIO_SEMANAL) (nuevaDuracionDias.toDouble() / 7.0) else 1.0
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