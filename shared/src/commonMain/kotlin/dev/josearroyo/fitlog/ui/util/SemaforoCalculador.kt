package dev.josearroyo.fitlog.ui.util

import dev.josearroyo.fitlog.data.model.*
import kotlin.math.roundToInt

object SemaforoCalculador {

    /**
     * Evalúa la adherencia de asistencia pro-rata según la secuencia de sesiones completadas.
     * Fórmula: $$ \text{Porcentaje} = \left( \frac{\text{Sesiones Completadas}}{\text{Sesiones Esperadas}} \right) \times 100 $$
     */
    fun evaluarAdherenciaProRata(
        metaSesionesCiclo: Int,
        duracionDiasCiclo: Int,
        sesionesEjecutadas: Int,
        diasTranscurridos: Int
    ): MetricaSemaforo {
        if (metaSesionesCiclo <= 0 || duracionDiasCiclo <= 0 || diasTranscurridos <= 0) {
            return MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "Sin meta o días válidos asignados")
        }

        // ✅ REGLA 1: Si alcanzó la meta de sesiones, la adherencia es 100% (Verde), sin importar los días transcurridos.
        if (sesionesEjecutadas >= metaSesionesCiclo) {
            val detalle = "$sesionesEjecutadas de $metaSesionesCiclo sesiones completadas (100%)"
            return MetricaSemaforo(100.0, EstadoSemaforo.VERDE, detalle)
        }

        // ✅ REGLA 2: Limita la acumulación de días transcurridos al máximo de días del ciclo
        // para evitar un denominador sobre-inflado si el atleta estuvo inactivo.
        val diasClamped = diasTranscurridos.coerceIn(1, duracionDiasCiclo)
        val esperadasAcc = (metaSesionesCiclo.toDouble() / duracionDiasCiclo.toDouble()) * diasClamped

        val porcentajeCalculado = if (esperadasAcc > 0) {
            (sesionesEjecutadas.toDouble() / esperadasAcc) * 100.0
        } else 0.0

        val porcentajeCumplimiento = porcentajeCalculado.coerceAtMost(100.0)

        val estado = when {
            porcentajeCumplimiento >= 80.0 -> EstadoSemaforo.VERDE
            porcentajeCumplimiento >= 50.0 -> EstadoSemaforo.AMARILLO
            else -> EstadoSemaforo.ROJO
        }

        val porcentajeRedondeado = (porcentajeCumplimiento * 10).roundToInt() / 10.0
        val esperadasFormateado = if (esperadasAcc % 1.0 == 0.0) {
            esperadasAcc.toInt().toString()
        } else {
            ((esperadasAcc * 10).roundToInt() / 10.0).toString()
        }

        val detalle = "$sesionesEjecutadas de $esperadasFormateado sesiones esperadas acumuladas ($porcentajeRedondeado%)"

        return MetricaSemaforo(porcentajeCumplimiento, estado, detalle)
    }

    /**
     * Evalúa el volumen efectivo ajustado a las sesiones ejecutadas hasta la fecha (pro-rata).
     */
    fun evaluarVolumenEfectivo(
        repsMetaTotal: Int,
        repsLogradasTotal: Int,
        metaSesionesCiclo: Int = 1,
        sesionesEjecutadas: Int = 1
    ): MetricaSemaforo {
        if (repsMetaTotal <= 0 || metaSesionesCiclo <= 0 || sesionesEjecutadas <= 0) {
            return MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "Sin meta de volumen definida o sin sesiones ejecutadas")
        }

        val repsMetaAcumulada = (repsMetaTotal.toDouble() / metaSesionesCiclo.toDouble()) * sesionesEjecutadas.toDouble()

        val porcentaje = if (repsMetaAcumulada > 0) {
            (repsLogradasTotal.toDouble() / repsMetaAcumulada) * 100.0
        } else 0.0

        val estado = when {
            porcentaje in 85.0..110.0 -> EstadoSemaforo.VERDE
            porcentaje in 70.0..84.9 || porcentaje > 110.0 -> EstadoSemaforo.AMARILLO
            else -> EstadoSemaforo.ROJO
        }

        val porcentajeRedondeado = (porcentaje * 10).roundToInt() / 10.0
        val detalle = "$repsLogradasTotal de ${repsMetaAcumulada.roundToInt()} reps esperadas ($porcentajeRedondeado%)"

        return MetricaSemaforo(porcentaje, estado, detalle)
    }

    /**
     * Evalúa el RPE promedio de series efectivas completadas.
     */
    fun evaluarFatigaRpe(
        seriesEfectivas: List<SerieRealizada>
    ): MetricaSemaforo {
        val seriesConRpe = seriesEfectivas.filter {
            it.tipoSerie != TipoSerie.APROXIMACION && it.rpe != null && it.rpe > 0
        }

        if (seriesConRpe.isEmpty()) {
            return MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "Sin registros de RPE en series efectivas")
        }

        val rpePromedio = seriesConRpe.mapNotNull { it.rpe }.average()
        val estado = when {
            rpePromedio in 7.0..8.4 -> EstadoSemaforo.VERDE
            rpePromedio in 5.0..6.9 || rpePromedio in 8.5..9.0 -> EstadoSemaforo.AMARILLO
            rpePromedio >= 9.1 -> EstadoSemaforo.ROJO
            else -> EstadoSemaforo.AMARILLO
        }

        val rpeRedondeado = (rpePromedio * 10).roundToInt() / 10.0
        val detalle = "RPE medio efectivo: $rpeRedondeado"

        return MetricaSemaforo(rpePromedio, estado, detalle)
    }

    /**
     * Resuelve el estado unificado global con la jerarquía definida.
     */
    fun resolverEstadoGlobal(
        requiereGestionAdmin: Boolean,
        adherencia: MetricaSemaforo,
        volumen: MetricaSemaforo,
        fatiga: MetricaSemaforo
    ): EstadoSemaforo {
        if (requiereGestionAdmin) return EstadoSemaforo.REQUIERE_GESTION

        if (adherencia.estado == EstadoSemaforo.ROJO ||
            fatiga.estado == EstadoSemaforo.ROJO ||
            volumen.estado == EstadoSemaforo.ROJO) {
            return EstadoSemaforo.ROJO
        }

        if (adherencia.estado == EstadoSemaforo.AMARILLO ||
            fatiga.estado == EstadoSemaforo.AMARILLO ||
            volumen.estado == EstadoSemaforo.AMARILLO) {
            return EstadoSemaforo.AMARILLO
        }

        if (adherencia.estado == EstadoSemaforo.VERDE &&
            (volumen.estado == EstadoSemaforo.VERDE || volumen.estado == EstadoSemaforo.SIN_DATOS) &&
            (fatiga.estado == EstadoSemaforo.VERDE || fatiga.estado == EstadoSemaforo.SIN_DATOS)) {
            return EstadoSemaforo.VERDE
        }

        return EstadoSemaforo.SIN_DATOS
    }
}