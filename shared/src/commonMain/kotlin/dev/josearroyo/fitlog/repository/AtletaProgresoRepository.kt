package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.data.model.DiaEntrenamientoAsignado
import dev.josearroyo.fitlog.data.model.Pesaje
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.data.model.TipoSerie
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.calcularFechaCierreCiclo
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AtletaProgresoRepository {
    private val db = Firebase.firestore

    // ============================================================
    // PESAJE Y MÉTRICAS
    // ============================================================
    suspend fun registrarPesaje(atletaId: String, pesaje: Pesaje): Boolean {
        return try {
            val idUnico = Uuid.random().toString()
            val ref = db.collection("users").document(atletaId).collection("pesajes").document(idUnico)
            ref.set(pesaje.copy(id = idUnico))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerUltimosPesajes(atletaId: String, limite: Long = 20): List<Pesaje> {
        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("pesajes")
                .orderBy("fecha", Direction.DESCENDING)
                .limit(limite)
                .get()

            snapshot.documents.map { doc -> doc.data<Pesaje>().copy(id = doc.id) }
        } catch (e: Exception) {
            println("🔥 ERROR EN REPOSITORIO AL TRAER PESAJES: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // ============================================================
    // HISTORIAL Y CICLOS DE ENTRENAMIENTO
    // ============================================================
    suspend fun obtenerHistorialCiclos(atletaId: String): List<CicloEntrenamiento> {
        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("ciclos_entrenamiento")
                .orderBy("fechaInicio", Direction.DESCENDING)
                .get()

            snapshot.documents.map { doc -> doc.data<CicloEntrenamiento>().copy(id = doc.id) }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al obtener historial de ciclos: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun obtenerHistorialEntrenamientos(atletaId: String): List<SesionEntrenamiento> {
        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("historial_entrenamientos")
                .orderBy("fechaEjecucion", Direction.DESCENDING)
                .get()

            snapshot.documents.map { doc -> doc.data<SesionEntrenamiento>().copy(id = doc.id) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun registrarSesionYActualizarCiclo(
        atletaId: String,
        sesionProcesada: SesionEntrenamiento,
        rutinaActual: RutinaAsignada,
        diaActual: DiaEntrenamientoAsignado,
        metaSesiones: Int
    ): Boolean = try {
        val ahoraMilis = getCurrentTimeMillis()

        val rutinaIdReal = rutinaActual.id.ifBlank {
            println("⚠️ ALERTA DE CONFIGURACIÓN: 'rutinaActual.id' vino VACÍO. Revisa cómo mapeas las rutinas en AtletaRepository.")
            "ID_RUTINA_DESCONOCIDO"
        }

        val nuevaSesionId = Uuid.random().toString()
        val nuevaSesionRef = db.collection("users").document(atletaId).collection("historial_entrenamientos").document(nuevaSesionId)
        val sesionFinal = sesionProcesada.copy(id = nuevaSesionId)

        val ciclosRef = db.collection("users").document(atletaId).collection("ciclos_entrenamiento")
        val rutinaRef = db.collection("users").document(atletaId).collection("rutinas_asignadas").document(rutinaIdReal)

        val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true).get()
        var cicloActivo = activeCyclesSnapshot.documents.firstOrNull()?.let { doc ->
            doc.data<CicloEntrenamiento>().copy(id = doc.id)
        }

        if (cicloActivo != null && ahoraMilis > cicloActivo.fechaCierre) {
            ciclosRef.document(cicloActivo.id).update("estaActivo" to false)
            cicloActivo = null
        }

        db.runTransaction {
            set(nuevaSesionRef, sesionFinal)

            val cicloActualizado: CicloEntrenamiento
            val cicloIdToUse = cicloActivo?.id ?: Uuid.random().toString()
            val cicloRefToUse = ciclosRef.document(cicloIdToUse)

            if (cicloActivo == null) {
                var totalRepsGlobales = 0
                rutinaActual.diasEntrenamiento.forEach { dia ->
                    dia.ejercicios.forEach { ejercicio ->
                        ejercicio.seriesPrescritas.forEach { serie ->
                            if (serie.tipo != TipoSerie.APROXIMACION) {
                                totalRepsGlobales += serie.repeticiones
                            }
                        }
                    }
                }

                val fechaInicioReal = minOf(ahoraMilis, sesionFinal.fechaEjecucion)
                val fechaCierreCalculada = calcularFechaCierreCiclo(fechaInicioReal)

                val nuevoCiclo = CicloEntrenamiento(
                    id = cicloIdToUse,
                    atletaId = atletaId,
                    rutinaAsignadaId = sesionFinal.rutinaAsignadaId,
                    fechaInicio = fechaInicioReal,
                    fechaCierre = fechaCierreCalculada,
                    estaActivo = true,
                    metaSesionesAsignadas = metaSesiones,
                    sesionesCompletadas = 1,
                    repeticionesMetaTotal = totalRepsGlobales,
                    repeticionesLogradasTotal = sesionFinal.totalRepsEfectivasLogradas
                )

                val porcentajeAsist = if (nuevoCiclo.metaSesionesAsignadas > 0) {
                    (nuevoCiclo.sesionesCompletadas.toDouble() / nuevoCiclo.metaSesionesAsignadas.toDouble()) * 100.0
                } else 0.0

                val porcentajeVol = if (nuevoCiclo.repeticionesMetaTotal > 0) {
                    (nuevoCiclo.repeticionesLogradasTotal.toDouble() / nuevoCiclo.repeticionesMetaTotal.toDouble()) * 100.0
                } else 0.0

                cicloActualizado = nuevoCiclo.copy(
                    porcentajeAsistencia = porcentajeAsist,
                    porcentajeVolumenGlobal = porcentajeVol
                )
            } else {
                val nuevasSesiones = cicloActivo.sesionesCompletadas + 1
                val nuevaMetaReps = cicloActivo.repeticionesMetaTotal
                val nuevasRepsLogradas = cicloActivo.repeticionesLogradasTotal + sesionFinal.totalRepsEfectivasLogradas

                val porcentajeAsist = if (cicloActivo.metaSesionesAsignadas > 0) {
                    (nuevasSesiones.toDouble() / cicloActivo.metaSesionesAsignadas.toDouble()) * 100.0
                } else 0.0

                val porcentajeVol = if (nuevaMetaReps > 0) {
                    (nuevasRepsLogradas.toDouble() / nuevaMetaReps.toDouble()) * 100.0
                } else 0.0

                cicloActualizado = cicloActivo.copy(
                    sesionesCompletadas = nuevasSesiones,
                    repeticionesMetaTotal = nuevaMetaReps,
                    repeticionesLogradasTotal = nuevasRepsLogradas,
                    porcentajeAsistencia = porcentajeAsist,
                    porcentajeVolumenGlobal = porcentajeVol
                )
            }
            set(cicloRefToUse, cicloActualizado)

            val diasActualizados = rutinaActual.diasEntrenamiento.map { dia ->
                if (dia.idDia == diaActual.idDia) dia.copy(ultimaVezEjecutada = ahoraMilis) else dia
            }
            val rutinaActualizada = rutinaActual.copy(
                ultimaVezEjecutada = ahoraMilis,
                diasEntrenamiento = diasActualizados
            )
            set(rutinaRef, rutinaActualizada)
        }
        true
    } catch (e: Exception) {
        println("🔥 [AtletaProgresoRepository] ERROR CRÍTICO AL GUARDAR ENTRENAMIENTO: ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun obtenerCicloActivo(atletaId: String): CicloEntrenamiento? {
        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("ciclos_entrenamiento")
                .where("estaActivo", equalTo = true)
                .limit(1)
                .get()

            val ciclo = snapshot.documents.firstOrNull()?.let { doc ->
                doc.data<CicloEntrenamiento>().copy(id = doc.id)
            }

            if (ciclo != null && getCurrentTimeMillis() > ciclo.fechaCierre) {
                null
            } else {
                ciclo
            }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al obtener ciclo activo: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun actualizarMetaCicloActivo(atletaId: String, nuevaMetaSesiones: Int, nuevasRepsMetaTotal: Int) {
        try {
            val ciclosRef = db.collection("users").document(atletaId).collection("ciclos_entrenamiento")
            val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true).limit(1).get()
            val cicloActivo = activeCyclesSnapshot.documents.firstOrNull()?.let { doc ->
                doc.data<CicloEntrenamiento>().copy(id = doc.id)
            }

            if (cicloActivo != null) {
                val nuevoPorcentajeAsist = if (nuevaMetaSesiones > 0) {
                    (cicloActivo.sesionesCompletadas.toDouble() / nuevaMetaSesiones.toDouble()) * 100.0
                } else 0.0

                val nuevoPorcentajeVol = if (nuevasRepsMetaTotal > 0) {
                    (cicloActivo.repeticionesLogradasTotal.toDouble() / nuevasRepsMetaTotal.toDouble()) * 100.0
                } else 0.0

                ciclosRef.document(cicloActivo.id).update(
                    "metaSesionesAsignadas" to nuevaMetaSesiones,
                    "porcentajeAsistencia" to nuevoPorcentajeAsist,
                    "repeticionesMetaTotal" to nuevasRepsMetaTotal,
                    "porcentajeVolumenGlobal" to nuevoPorcentajeVol
                )
            }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al actualizar meta del ciclo activo: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun forzarCierreCicloActivo(atletaId: String) {
        try {
            val ciclosRef = db.collection("users").document(atletaId).collection("ciclos_entrenamiento")
            val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true).limit(1).get()
            val cicloActivo = activeCyclesSnapshot.documents.firstOrNull()?.id

            if (cicloActivo != null) {
                ciclosRef.document(cicloActivo).update("estaActivo" to false)
            }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al forzar cierre del ciclo activo: ${e.message}")
            e.printStackTrace()
        }
    }
}