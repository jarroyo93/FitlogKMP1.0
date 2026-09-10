package dev.josearroyo.fitlog

import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.ui.util.SemaforoCalculador
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemaforoCalculadorTest {

    // ============================================================
    // 1. PRUEBAS DE ADHERENCIA PRO-RATA (DURACIÓN FLEXIBLE)
    // ============================================================

    @Test
    fun testAdherencia_CicloSieteDias_Verde() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 4,
            duracionDiasCiclo = 7,
            sesionesEjecutadas = 2,
            diasTranscurridos = 3
        )

        assertEquals(EstadoSemaforo.VERDE, resultado.estado)
        assertTrue(resultado.valor >= 80.0)
    }

    @Test
    fun testAdherencia_CicloPersonalizadoDiezDias_Amarillo() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 6,
            duracionDiasCiclo = 10,
            sesionesEjecutadas = 2,
            diasTranscurridos = 5
        )

        assertEquals(EstadoSemaforo.AMARILLO, resultado.estado)
    }

    @Test
    fun testAdherencia_CicloPersonalizadoCatorceDias_Rojo() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 8,
            duracionDiasCiclo = 14,
            sesionesEjecutadas = 1,
            diasTranscurridos = 7
        )

        assertEquals(EstadoSemaforo.ROJO, resultado.estado)
    }

    @Test
    fun testAdherencia_SinDatosValidos() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 0,
            duracionDiasCiclo = 7,
            sesionesEjecutadas = 0,
            diasTranscurridos = 0
        )

        assertEquals(EstadoSemaforo.SIN_DATOS, resultado.estado)
        assertEquals(0.0, resultado.valor)
    }

    // ============================================================
    // 2. PRUEBAS DE VOLUMEN EFECTIVO
    // ============================================================

    @Test
    fun testVolumenEfectivo_RangoVerde() {
        val resultado = SemaforoCalculador.evaluarVolumenEfectivo(
            repsMetaTotal = 100,
            repsLogradasTotal = 90
        )

        assertEquals(EstadoSemaforo.VERDE, resultado.estado)
        assertEquals(90.0, resultado.valor)
    }

    @Test
    fun testVolumenEfectivo_Subentrenamiento_Amarillo() {
        val resultado = SemaforoCalculador.evaluarVolumenEfectivo(
            repsMetaTotal = 100,
            repsLogradasTotal = 75
        )

        assertEquals(EstadoSemaforo.AMARILLO, resultado.estado)
    }

    @Test
    fun testVolumenEfectivo_ExcesoVolumen_Amarillo() {
        val resultado = SemaforoCalculador.evaluarVolumenEfectivo(
            repsMetaTotal = 100,
            repsLogradasTotal = 120
        )

        assertEquals(EstadoSemaforo.AMARILLO, resultado.estado)
    }

    @Test
    fun testVolumenEfectivo_Critico_Rojo() {
        val resultado = SemaforoCalculador.evaluarVolumenEfectivo(
            repsMetaTotal = 100,
            repsLogradasTotal = 50
        )

        assertEquals(EstadoSemaforo.ROJO, resultado.estado)
    }

    // ============================================================
    // 3. PRUEBAS DE FATIGA Y RPE
    // ============================================================

    @Test
    fun testFatigaRpe_OmiteAproximacion_ZonaOptimaVerde() {
        val series = listOf(
            SerieRealizada(numeroSerie = 1, tipoSerie = TipoSerie.APROXIMACION, rpe = 5),
            SerieRealizada(numeroSerie = 2, tipoSerie = TipoSerie.EFECTIVA, rpe = 8),
            SerieRealizada(numeroSerie = 3, tipoSerie = TipoSerie.EFECTIVA, rpe = 8)
        )

        val resultado = SemaforoCalculador.evaluarFatigaRpe(series)

        assertEquals(EstadoSemaforo.VERDE, resultado.estado)
        assertEquals(8.0, resultado.valor)
    }

    @Test
    fun testFatigaRpe_RiesgoAltoFallo_Rojo() {
        val series = listOf(
            SerieRealizada(numeroSerie = 1, tipoSerie = TipoSerie.EFECTIVA, rpe = 9),
            SerieRealizada(numeroSerie = 2, tipoSerie = TipoSerie.FALLO, rpe = 10)
        )

        val resultado = SemaforoCalculador.evaluarFatigaRpe(series)

        assertEquals(EstadoSemaforo.ROJO, resultado.estado)
        assertEquals(9.5, resultado.valor)
    }

    @Test
    fun testFatigaRpe_SinRegistros_SinDatos() {
        val series = listOf(
            SerieRealizada(numeroSerie = 1, tipoSerie = TipoSerie.APROXIMACION, rpe = 6)
        )

        val resultado = SemaforoCalculador.evaluarFatigaRpe(series)

        assertEquals(EstadoSemaforo.SIN_DATOS, resultado.estado)
    }

    // ============================================================
    // 4. PRUEBAS DE JERARQUÍA UNIFICADA DE ESTADOS
    // ============================================================

    @Test
    fun testJerarquia_PrioridadRequiereGestion() {
        val adherencia = MetricaSemaforo(100.0, EstadoSemaforo.VERDE, "")
        val volumen = MetricaSemaforo(90.0, EstadoSemaforo.VERDE, "")
        val fatiga = MetricaSemaforo(8.0, EstadoSemaforo.VERDE, "")

        val global = SemaforoCalculador.resolverEstadoGlobal(
            requiereGestionAdmin = true,
            adherencia = adherencia,
            volumen = volumen,
            fatiga = fatiga
        )

        assertEquals(EstadoSemaforo.REQUIERE_GESTION, global)
    }

    @Test
    fun testJerarquia_PrioridadRojoSobreAmarilloYVerde() {
        val adherencia = MetricaSemaforo(100.0, EstadoSemaforo.VERDE, "")
        val volumen = MetricaSemaforo(75.0, EstadoSemaforo.AMARILLO, "")
        val fatiga = MetricaSemaforo(9.5, EstadoSemaforo.ROJO, "")

        val global = SemaforoCalculador.resolverEstadoGlobal(
            requiereGestionAdmin = false,
            adherencia = adherencia,
            volumen = volumen,
            fatiga = fatiga
        )

        assertEquals(EstadoSemaforo.ROJO, global)
    }

    @Test
    fun testJerarquia_EstadoVerdeConsolidado() {
        val adherencia = MetricaSemaforo(90.0, EstadoSemaforo.VERDE, "")
        val volumen = MetricaSemaforo(95.0, EstadoSemaforo.VERDE, "")
        val fatiga = MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "")

        val global = SemaforoCalculador.resolverEstadoGlobal(
            requiereGestionAdmin = false,
            adherencia = adherencia,
            volumen = volumen,
            fatiga = fatiga
        )

        assertEquals(EstadoSemaforo.VERDE, global)
    }

    // ============================================================
    // 5. PRUEBAS DE CASOS FRONTERA (BOUNDARY TESTING)
    // ============================================================

    @Test
    fun testAdherencia_LimitesExactos() {
        val exacto80 = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 10, duracionDiasCiclo = 10, sesionesEjecutadas = 8, diasTranscurridos = 10
        )
        assertEquals(EstadoSemaforo.VERDE, exacto80.estado)

        val limite79 = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 1000, duracionDiasCiclo = 10, sesionesEjecutadas = 799, diasTranscurridos = 10
        )
        assertEquals(EstadoSemaforo.AMARILLO, limite79.estado)

        val limite49 = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 1000, duracionDiasCiclo = 10, sesionesEjecutadas = 499, diasTranscurridos = 10
        )
        assertEquals(EstadoSemaforo.ROJO, limite49.estado)
    }

    @Test
    fun testAdherencia_DiasTranscurridosExcedenDuracionCiclo() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 4, duracionDiasCiclo = 7, sesionesEjecutadas = 4, diasTranscurridos = 10
        )
        assertEquals(EstadoSemaforo.VERDE, resultado.estado)
        assertEquals(100.0, resultado.valor)
    }

    @Test
    fun testFatigaRpe_LimitesFrontera() {
        val seriesAlto = listOf(
            SerieRealizada(tipoSerie = TipoSerie.EFECTIVA, rpe = 8),
            SerieRealizada(tipoSerie = TipoSerie.EFECTIVA, rpe = 9)
        )
        assertEquals(EstadoSemaforo.AMARILLO, SemaforoCalculador.evaluarFatigaRpe(seriesAlto).estado)

        val seriesRojoExacto = listOf(
            SerieRealizada(tipoSerie = TipoSerie.EFECTIVA, rpe = 9),
            SerieRealizada(tipoSerie = TipoSerie.EFECTIVA, rpe = 10)
        )
        assertEquals(EstadoSemaforo.ROJO, SemaforoCalculador.evaluarFatigaRpe(seriesRojoExacto).estado)
    }

    @Test
    fun testJerarquia_CombinacionTodosAmarillos() {
        val adherencia = MetricaSemaforo(60.0, EstadoSemaforo.AMARILLO, "")
        val volumen = MetricaSemaforo(75.0, EstadoSemaforo.AMARILLO, "")
        val fatiga = MetricaSemaforo(8.8, EstadoSemaforo.AMARILLO, "")

        val global = SemaforoCalculador.resolverEstadoGlobal(false, adherencia, volumen, fatiga)
        assertEquals(EstadoSemaforo.AMARILLO, global)
    }

    @Test
    fun testJerarquia_CombinacionTodosSinDatos() {
        val sinDatos = MetricaSemaforo(0.0, EstadoSemaforo.SIN_DATOS, "")
        val global = SemaforoCalculador.resolverEstadoGlobal(false, sinDatos, sinDatos, sinDatos)
        assertEquals(EstadoSemaforo.SIN_DATOS, global)
    }

    @Test
    fun testAdherencia_MetaSesionesMayorADiasCiclo_MantieneVerdeSiCumple() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 8,
            duracionDiasCiclo = 7,
            sesionesEjecutadas = 8,
            diasTranscurridos = 7
        )

        assertEquals(EstadoSemaforo.VERDE, resultado.estado)
        assertEquals(100.0, resultado.valor)
    }

    @Test
    fun testAdherencia_FormatoTextoConDecimalesSinInconsistencias() {
        val resultado = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = 4,
            duracionDiasCiclo = 7,
            sesionesEjecutadas = 2,
            diasTranscurridos = 4,
            modoCiclo = ModoCiclo.CALENDARIO_SEMANAL
        )

        // 1. Verifica que el cálculo contenga las sesiones esperadas redondeadas con el texto explicativo
        assertTrue(resultado.detalle.contains("2.3 sesiones esperadas acumuladas"))

        // 2. Verifica que el porcentaje calculado sea coherente (2 / 2.2857... = 87.5%)
        assertEquals(87.5, resultado.valor, 0.1) // 👈 Se cambió 'porcentaje' por 'valor'
    }
}