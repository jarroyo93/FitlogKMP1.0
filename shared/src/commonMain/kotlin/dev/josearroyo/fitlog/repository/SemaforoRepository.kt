package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.ui.util.SemaforoCalculador
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SemaforoRepository(
    private val userRepository: UserRepository = UserRepository(),
    private val atletaProgresoRepository: AtletaProgresoRepository = AtletaProgresoRepository()
) {
    private val db = Firebase.firestore

    /**
     * Evalúa a todos los atletas asignados al entrenador en PARALELO.
     */
    suspend fun obtenerEvaluacionAtletas(entrenadorId: String): List<AtletaSemaforoItem> = coroutineScope {
        val atletas = userRepository.obtenerAtletasPorEntrenador(entrenadorId)
        val ahora = getCurrentTimeMillis()

        atletas.map { atleta ->
            async {
                evaluarAtletaIndividual(atleta, ahora)
            }
        }.awaitAll()
    }

    private suspend fun evaluarAtletaIndividual(atleta: Usuario, ahora: Long): AtletaSemaforoItem {
        val cicloActivo = atletaProgresoRepository.obtenerCicloActivo(atleta.id)

        // 1. Verificación de reglas administrativas
        val suscripcionInactiva = atleta.estadoSuscripcion != EstadoSuscripcion.ACTIVO
        val sinCiclo = cicloActivo == null
        val porVencer = cicloActivo?.estaPorVencer(ahora) ?: false

        val requiereGestionAdmin = suscripcionInactiva || sinCiclo
        val mensajeGestion = when {
            suscripcionInactiva -> "Suscripción ${atleta.estadoSuscripcion.name.lowercase()}"
            sinCiclo -> "Sin rutina o ciclo asignado"
            else -> null
        }

        if (cicloActivo == null) {
            val motivosInactivos = mutableListOf<String>()
            if (suscripcionInactiva) motivosInactivos.add("Suscripción Inactiva")
            if (sinCiclo) motivosInactivos.add("Sin plan asignado")

            return AtletaSemaforoItem(
                atletaId = atleta.id,
                nombreCompleto = "${atleta.nombres} ${atleta.apellidos}".trim(),
                fotoUrl = atleta.fotoPerfilUrl,
                estado = if (requiereGestionAdmin) EstadoSemaforo.REQUIERE_GESTION else EstadoSemaforo.SIN_DATOS,
                adherenciaPorcentaje = 0,
                sesionesEjecutadas = 0,
                sesionesEsperadasHoy = 0,
                rpePromedio = null,
                mensajeGestion = mensajeGestion,
                motivosAlerta = motivosInactivos
            )
        }

        // 2. Cálculo de días transcurridos segun fechaInicio (sea Lunes o inicio de bloque)
        val milisPorDia = 86_400_000L
        val diasTranscurridos = (((ahora - cicloActivo.fechaInicio) / milisPorDia) + 1)
            .toInt()
            .coerceIn(1, cicloActivo.duracionDias)

        // 3. Evaluación de métricas pasando el modo de ciclo
        val metricaAdherencia = SemaforoCalculador.evaluarAdherenciaProRata(
            metaSesionesCiclo = cicloActivo.metaSesionesAsignadas,
            duracionDiasCiclo = cicloActivo.duracionDias,
            sesionesEjecutadas = cicloActivo.sesionesCompletadas,
            diasTranscurridos = diasTranscurridos,
            modoCiclo = cicloActivo.modoCiclo
        )

        val metricaVolumen = SemaforoCalculador.evaluarVolumenEfectivo(
            repsMetaTotal = cicloActivo.repeticionesMetaTotal,
            repsLogradasTotal = cicloActivo.repeticionesLogradasTotal,
            metaSesionesCiclo = cicloActivo.metaSesionesAsignadas,
            sesionesEjecutadas = cicloActivo.sesionesCompletadas
        )

        val entrenamientos = atletaProgresoRepository.obtenerEntrenamientosCicloActivo(
            atletaId = atleta.id,
            fechaInicioCicloMs = cicloActivo.fechaInicio
        )

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

        // 5. Construcción de motivos de alerta explicativos
        val motivos = mutableListOf<String>()

        if (metricaAdherencia.estado == EstadoSemaforo.ROJO) {
            motivos.add("Asistencia crítica (${metricaAdherencia.valor.toInt()}%)")
        } else if (metricaAdherencia.estado == EstadoSemaforo.AMARILLO) {
            motivos.add("Baja asistencia (${metricaAdherencia.valor.toInt()}%)")
        }

        if (metricaVolumen.estado == EstadoSemaforo.ROJO) {
            if (metricaVolumen.valor < 70.0) {
                motivos.add("Volumen crítico (${metricaVolumen.valor.toInt()}%)")
            } else {
                motivos.add("Exceso crítico de volumen (${metricaVolumen.valor.toInt()}%)")
            }
        } else if (metricaVolumen.estado == EstadoSemaforo.AMARILLO) {
            if (metricaVolumen.valor < 85.0) {
                motivos.add("Volumen bajo (${metricaVolumen.valor.toInt()}%)")
            } else {
                motivos.add("Volumen elevado (${metricaVolumen.valor.toInt()}%)")
            }
        }

        if (metricaFatiga.estado == EstadoSemaforo.ROJO) {
            motivos.add("Sobrecarga RPE (${metricaFatiga.valor})")
        } else if (metricaFatiga.estado == EstadoSemaforo.AMARILLO) {
            motivos.add("RPE elevado (${metricaFatiga.valor})")
        }

        if (porVencer) {
            val etiquetaCierre = if (cicloActivo.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL) "Semana" else "Ciclo"
            motivos.add("$etiquetaCierre de ${cicloActivo.duracionDias} días por vencer")
        }

        return AtletaSemaforoItem(
            atletaId = atleta.id,
            nombreCompleto = "${atleta.nombres} ${atleta.apellidos}".trim(),
            fotoUrl = atleta.fotoPerfilUrl,
            estado = estadoGlobal,
            adherenciaPorcentaje = metricaAdherencia.valor.toInt(),
            sesionesEjecutadas = cicloActivo.sesionesCompletadas,
            sesionesEsperadasHoy = cicloActivo.metaSesionesAsignadas,
            rpePromedio = if (metricaFatiga.valor > 0) metricaFatiga.valor.toFloat() else null,
            mensajeGestion = mensajeGestion,
            motivosAlerta = motivos
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