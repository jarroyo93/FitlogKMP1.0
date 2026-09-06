package dev.josearroyo.fitlog

import dev.josearroyo.fitlog.data.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CicloExtensionsTest {

    private val milisPorDia = 86_400_000L

    @Test
    fun testCalcularFechaCierreCiclo_Personalizado() {
        val inicio = 1_000_000L
        val duracionDias = 14
        val esperado = inicio + (14 * milisPorDia)

        val obtenido = calcularFechaCierreCiclo(inicio, duracionDias)

        assertEquals(esperado, obtenido)
    }

    @Test
    fun testEstaPorVencer_CincoDiasOMenos() {
        val ahora = 10_000_000L
        val ciclo = CicloEntrenamiento(
            estaActivo = true,
            fechaCierre = ahora + (3 * milisPorDia),
            metaSesionesAsignadas = 5,
            sesionesCompletadas = 2
        )

        assertTrue(ciclo.estaPorVencer(ahora))
    }

    @Test
    fun testEstaPorVencer_UnaSesionRestante() {
        val ahora = 10_000_000L
        val ciclo = CicloEntrenamiento(
            estaActivo = true,
            fechaCierre = ahora + (10 * milisPorDia),
            metaSesionesAsignadas = 5,
            sesionesCompletadas = 4
        )

        assertTrue(ciclo.estaPorVencer(ahora))
    }

    @Test
    fun testEstaPorVencer_CicloNormalEnCurso() {
        val ahora = 10_000_000L
        val ciclo = CicloEntrenamiento(
            estaActivo = true,
            fechaCierre = ahora + (12 * milisPorDia),
            metaSesionesAsignadas = 6,
            sesionesCompletadas = 2
        )

        assertFalse(ciclo.estaPorVencer(ahora))
    }

    @Test
    fun testEstaVencido() {
        val ahora = 10_000_000L
        val cicloVencido = CicloEntrenamiento(
            estaActivo = true,
            fechaCierre = ahora - 1000L
        )

        assertTrue(cicloVencido.estaVencido(ahora))
    }

    @Test
    fun testSincronizarConRutina_ExpandeDiasCicloSiRutinaEsMayor() {
        val cicloOriginal = CicloEntrenamiento(
            id = "c1",
            duracionDias = 5,
            fechaInicio = 1_000_000L,
            fechaCierre = 1_000_000L + (5 * 86_400_000L)
        )

        val rutinaOchoDias = RutinaAsignada(
            diasEntrenamiento = List(8) { DiaEntrenamientoAsignado(nombreDia = "Día ${it + 1}") }
        )

        val cicloSincronizado = cicloOriginal.sincronizarConRutina(rutinaOchoDias)

        assertEquals(8, cicloSincronizado.duracionDias)
        assertEquals(8, cicloSincronizado.metaSesionesAsignadas)
        assertEquals(1_000_000L + (8 * 86_400_000L), cicloSincronizado.fechaCierre)
    }

    @Test
    fun testSincronizarConRutina_CicloMultisemanaEscalaMetasCorrectamente() {
        val cicloCatorceDias = CicloEntrenamiento(
            id = "c1",
            duracionDias = 14,
            fechaInicio = 1_000_000L,
            fechaCierre = 1_000_000L + (14 * 86_400_000L)
        )

        val rutinaCuatroDias = RutinaAsignada(
            diasEntrenamiento = List(4) { DiaEntrenamientoAsignado(nombreDia = "Día ${it + 1}") }
        )

        val cicloSincronizado = cicloCatorceDias.sincronizarConRutina(rutinaCuatroDias)

        assertEquals(14, cicloSincronizado.duracionDias)
        assertEquals(8, cicloSincronizado.metaSesionesAsignadas)
    }

    @Test
    fun testSincronizarConRutina_CicloOchoDias_RutinaSieteDias() {
        val cicloOchoDias = CicloEntrenamiento(
            id = "c1",
            duracionDias = 8,
            fechaInicio = 1_000_000L,
            fechaCierre = 1_000_000L + (8 * 86_400_000L)
        )

        val rutinaSieteDias = RutinaAsignada(
            diasEntrenamiento = List(7) { DiaEntrenamientoAsignado(nombreDia = "Día ${it + 1}") }
        )

        val cicloSincronizado = cicloOchoDias.sincronizarConRutina(rutinaSieteDias)

        assertEquals(8, cicloSincronizado.duracionDias)
        assertEquals(7, cicloSincronizado.metaSesionesAsignadas)
    }

    @Test
    fun testSincronizarConRutina_CicloDieciseisDias_RutinaQuinceDias() {
        val cicloDieciseisDias = CicloEntrenamiento(
            id = "c2",
            duracionDias = 16,
            fechaInicio = 1_000_000L,
            fechaCierre = 1_000_000L + (16 * 86_400_000L)
        )

        val rutinaQuinceDias = RutinaAsignada(
            diasEntrenamiento = List(15) { DiaEntrenamientoAsignado(nombreDia = "Día ${it + 1}") }
        )

        val cicloSincronizado = cicloDieciseisDias.sincronizarConRutina(rutinaQuinceDias)

        assertEquals(16, cicloSincronizado.duracionDias)
        assertEquals(15, cicloSincronizado.metaSesionesAsignadas)
    }
}