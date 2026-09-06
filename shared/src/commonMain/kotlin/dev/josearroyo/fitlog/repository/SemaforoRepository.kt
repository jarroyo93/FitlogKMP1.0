package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.ui.util.SemaforoCalculador

class SemaforoRepository(
    private val userRepository: UserRepository = UserRepository(),
    private val atletaProgresoRepository: AtletaProgresoRepository = AtletaProgresoRepository()
) {
    private val db = Firebase.firestore

    /**
     * Evalúa a todos los atletas asignados al entrenador y genera sus ítems consolidados.
     */
    suspend fun obtenerEvaluacionAtletas(entrenadorId: String): List<AtletaSemaforoItem> {
        val atletas = userRepository.obtenerAtletasPorEntrenador(entrenadorId)
        val ahora = getCurrentTimeMillis()

        return atletas.map { atleta ->
            evaluarAtletaIndividual(atleta, ahora)
        }
    }

    private suspend fun evaluarAtletaIndividual(atleta: Usuario, ahora: Long): AtletaSemaforoItem {
        val cicloActivo = atletaProgresoRepository.obtenerCicloActivo(atleta.id)

        // 1. Verificación de reglas administrativas
        val suscripcionInactiva = atleta.estadoSuscripcion != EstadoSuscripcion.ACTIVO
        val sinCiclo = cicloActivo == null
        val porVencer = cicloActivo?.estaPorVencer(ahora) ?: false

        val requiereGestionAdmin = suscripcionInactiva || sinCiclo || porVencer
        val mensajeGestion = when {
            suscripcionInactiva -> "Suscripción ${atleta.estadoSuscripcion.name.lowercase()}"
            sinCiclo -> "Sin rutina o ciclo asignado"
            porVencer -> "Ciclo por vencer"
            else -> null
        }

        if (cicloActivo == null) {
            return AtletaSemaforoItem(
                atletaId = atleta.id,
                nombreCompleto = "${atleta.nombres} ${atleta.apellidos}".trim(),
                fotoUrl = atleta.fotoPerfilUrl,
                estado = if (requiereGestionAdmin) EstadoSemaforo.REQUIERE_GESTION else EstadoSemaforo.SIN_DATOS,
                adherenciaPorcentaje = 0,
                sesionesEjecutadas = 0,
                sesionesEsperadasHoy = 0,
                rpePromedio = null,
                mensajeGestion = mensajeGestion
            )
        }

        // 2. Cálculo de días transcurridos
        val milisPorDia = 86_400_000L
        val diasTranscurridos = (((ahora - cicloActivo.fechaInicio) / milisPorDia) + 1)
            .toInt()
            .coerceIn(1, cicloActivo.duracionDias)

        // 3. Evaluación de métricas
        val metricaAdherencia = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = cicloActivo.metaSesionesAsignadas,
            duracionDiasCiclo = cicloActivo.duracionDias,
            sesionesEjecutadas = cicloActivo.sesionesCompletadas,
            diasTranscurridos = diasTranscurridos
        )

        val metricaVolumen = SemaforoCalculador.evaluarVolumenEfectivo(
            repsMetaTotal = cicloActivo.repeticionesMetaTotal,
            repsLogradasTotal = cicloActivo.repeticionesLogradasTotal,
            metaSesionesCiclo = cicloActivo.metaSesionesAsignadas,
            sesionesEjecutadas = cicloActivo.sesionesCompletadas
        )

        val entrenamientos = atletaProgresoRepository.obtenerHistorialEntrenamientos(atleta.id)
            .filter { it.fechaEjecucion >= cicloActivo.fechaInicio }

        val todasLasSeries = entrenamientos.flatMap { sesion ->
            sesion.ejerciciosRealizados.flatMap { it.seriesRealizadas }
        }

        val metricaFatiga = SemaforoCalculador.evaluarFatigaRpe(todasLasSeries)

        // 4. Resolución de estado global
        val estadoGlobal = SemaforoCalculador.resolverEstadoGlobal(
            requiereGestionAdmin = requiereGestionAdmin,
            adherencia = metricaAdherencia,
            volumen = metricaVolumen,
            fatiga = metricaFatiga
        )

        return AtletaSemaforoItem(
            atletaId = atleta.id,
            nombreCompleto = "${atleta.nombres} ${atleta.apellidos}".trim(),
            fotoUrl = atleta.fotoPerfilUrl,
            estado = estadoGlobal,
            adherenciaPorcentaje = metricaAdherencia.valor.toInt(),
            sesionesEjecutadas = cicloActivo.sesionesCompletadas,
            sesionesEsperadasHoy = cicloActivo.metaSesionesAsignadas,
            rpePromedio = if (metricaFatiga.valor > 0) metricaFatiga.valor.toFloat() else null,
            mensajeGestion = mensajeGestion
        )
    }

    fun calcularResumenGerencial(atletas: List<AtletaSemaforoItem>): ResumenGerencial {
        if (atletas.isEmpty()) return ResumenGerencial(100, 0, 0, 0)

        val sanos = atletas.count { it.estado == EstadoSemaforo.VERDE }
        val criticos = atletas.count { it.estado == EstadoSemaforo.ROJO }
        val enRiesgo = atletas.count { it.estado == EstadoSemaforo.AMARILLO }
        val enGestion = atletas.count { it.estado == EstadoSemaforo.REQUIERE_GESTION }

        val porcentajeSalud = ((sanos.toDouble() / atletas.size.toDouble()) * 100).toInt()

        return ResumenGerencial(
            porcentajeSaludEquipo = porcentajeSalud,
            totalAtletasCriticos = criticos,
            totalAtletasEnRiesgo = enRiesgo,
            totalRequierenGestion = enGestion
        )
    }
}