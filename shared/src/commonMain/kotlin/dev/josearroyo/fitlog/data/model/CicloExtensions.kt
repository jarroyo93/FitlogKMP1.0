package dev.josearroyo.fitlog.data.model

import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlin.math.roundToInt

/**
 * Calcula la fecha final del ciclo sumando la duración en días definida por el coach.
 */
fun calcularFechaCierreCiclo(inicioMilis: Long, duracionDias: Int): Long {
    val milisPorDia = 86_400_000L
    return inicioMilis + (duracionDias.toLong() * milisPorDia)
}

/**
 * Evalúa si un ciclo requiere gestión por vencimiento o agotamiento.
 * Mantiene la regla: <= 5 días para cerrar o <= 1 sesión restante.
 */
fun CicloEntrenamiento.estaPorVencer(ahoraMilis: Long = getCurrentTimeMillis()): Boolean {
    if (!estaActivo) return false
    val milisPorDia = 86_400_000L
    val diasRestantes = (fechaCierre - ahoraMilis) / milisPorDia
    val sesionesRestantes = metaSesionesAsignadas - sesionesCompletadas

    // Condición 1: Quedan 2 días calendario o menos para la fecha de cierre
    val porCierreCalendario = diasRestantes in 0..2

    // Condición 2: Queda 1 o 0 sesiones RESTANTES, PERO solo si ya transcurrió más del 60% del tiempo del ciclo
    val tiempoTranscurridoMilis = ahoraMilis - fechaInicio
    val tiempoTotalMilis = duracionDias.toLong() * milisPorDia
    val porcentajeTiempoTranscurrido = if (tiempoTotalMilis > 0) {
        (tiempoTranscurridoMilis.toDouble() / tiempoTotalMilis.toDouble())
    } else 0.0

    val porAgotamientoSesiones = (sesionesRestantes <= 1) && (porcentajeTiempoTranscurrido >= 0.60)

    return porCierreCalendario || porAgotamientoSesiones
}

// ✅ CORRECCIÓN EN CicloExtensions.kt

fun CicloEntrenamiento.estaVencido(ahoraMilis: Long = getCurrentTimeMillis()): Boolean {
    // Un ciclo se considera vencido si ya no está activo O si ya cumplió la meta de sesiones.
    // La fechaCierre es solo una referencia estimada, no un detonador automático de expiración.
    return !estaActivo || (metaSesionesAsignadas > 0 && sesionesCompletadas >= metaSesionesAsignadas)
}

/**
 * Sincroniza y recalcula dinámicamente un ciclo activo cuando se modifica
 * o asigna la estructura de una rutina, discriminando bloques fijos de
 * microciclos semanales repetibles.
 */
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

    // 1. Duración mínima: no puede durar menos días que las sesiones asignadas
    val nuevaDuracionDias = maxOf(this.duracionDias, cantidadDiasRutina)
    val milisPorDia = 86_400_000L
    val nuevaFechaCierre = this.fechaInicio + (nuevaDuracionDias.toLong() * milisPorDia)

    // 2. Discriminación de tipo de programa
    val esBloqueFijo = cantidadDiasRutina > 7 || (nuevaDuracionDias <= cantidadDiasRutina + 2)

    val metaSesionesFinal: Int
    val multiplicadorVolumen: Double

    if (esBloqueFijo) {
        metaSesionesFinal = cantidadDiasRutina
        multiplicadorVolumen = 1.0
    } else {
        val semanas = nuevaDuracionDias.toDouble() / 7.0
        metaSesionesFinal = (cantidadDiasRutina.toDouble() * semanas).roundToInt()
        multiplicadorVolumen = semanas
    }

    // 3. Recálculo de repeticiones meta
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
    val nuevasRepsMetaTotal = (repsMetaUnPasada.toDouble() * multiplicadorVolumen).roundToInt()

    // 4. Recálculo de porcentajes acumulados
    val nuevoPorcentajeAsist = if (metaSesionesFinal > 0) {
        (this.sesionesCompletadas.toDouble() / metaSesionesFinal.toDouble()) * 100.0
    } else 0.0

    val nuevoPorcentajeVol = if (nuevasRepsMetaTotal > 0) {
        (this.repeticionesLogradasTotal.toDouble() / nuevasRepsMetaTotal.toDouble()) * 100.0
    } else 0.0

    return this.copy(
        duracionDias = nuevaDuracionDias,
        fechaCierre = nuevaFechaCierre,
        metaSesionesAsignadas = metaSesionesFinal,
        repeticionesMetaTotal = nuevasRepsMetaTotal,
        porcentajeAsistencia = nuevoPorcentajeAsist,
        porcentajeVolumenGlobal = nuevoPorcentajeVol
    )
}