package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.data.model.DiaEntrenamientoAsignado
import dev.josearroyo.fitlog.data.model.EjercicioRealizado
import dev.josearroyo.fitlog.data.model.EstadoSesion
import dev.josearroyo.fitlog.data.model.Pesaje
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.data.model.TipoSerie
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.calcularFechaCierreCiclo
import dev.josearroyo.fitlog.data.model.EjercicioAsignado
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AtletaProgresoRepository {
    private val db = Firebase.firestore

    // ============================================================
    // CONSULTA HISTÓRICA POR EJERCICIO (NUEVA MEJORA)
    // ============================================================
    // 📦 Objeto contenedor con el registro y su fecha de ejecución
    data class RegistroEjercicioPrevio(
        val ejercicioLog: EjercicioRealizado = EjercicioRealizado(),
        val fechaEjecucion: Long = 0L
    )

    suspend fun obtenerUltimosRegistrosPorEjercicio(
        atletaId: String,
        ejercicios: List<EjercicioAsignado>,
        sesionActualIdExcluir: String? = null
    ): Map<String, RegistroEjercicioPrevio> {
        if (ejercicios.isEmpty()) return emptyMap()

        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("historial_entrenamientos")
                .orderBy("fechaEjecucion", Direction.DESCENDING)
                .limit(30)
                .get()

            val mapaResultado = mutableMapOf<String, RegistroEjercicioPrevio>()
            val idsBuscar = ejercicios.map { it.ejercicioGlobalId }.filter { it.isNotBlank() }.toSet()
            val nombresBuscar = ejercicios.associateBy { it.nombre.trim().lowercase() }

            for (doc in snapshot.documents) {
                if (sesionActualIdExcluir != null && doc.id == sesionActualIdExcluir) continue

                val sesion = doc.data<SesionEntrenamiento>()
                if (sesion.estado == EstadoSesion.COMPLETADA) {
                    for (ej in sesion.ejerciciosRealizados) {
                        val globalId = ej.ejercicioGlobalId
                        val nombreNorm = ej.nombreEjercicio.trim().lowercase()

                        val coincideId = globalId.isNotBlank() && idsBuscar.contains(globalId)
                        val coincideNombre = nombresBuscar.containsKey(nombreNorm)

                        if ((coincideId || coincideNombre) &&
                            !ej.fueSaltado &&
                            ej.seriesRealizadas.any { it.pesoKg > 0 || it.repeticionesLogradas > 0 }
                        ) {
                            val registroObj = RegistroEjercicioPrevio(
                                ejercicioLog = ej,
                                fechaEjecucion = sesion.fechaEjecucion
                            )

                            // Guardar por ID
                            if (globalId.isNotBlank() && !mapaResultado.containsKey(globalId)) {
                                mapaResultado[globalId] = registroObj
                            }

                            // Guardar por Nombre (resguardo)
                            val ejCoincidente = nombresBuscar[nombreNorm]
                            if (ejCoincidente != null && !mapaResultado.containsKey(ejCoincidente.nombre)) {
                                mapaResultado[ejCoincidente.nombre] = registroObj
                            }
                        }
                    }
                }
            }
            mapaResultado
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al obtener historial previo: ${e.message}")
            emptyMap()
        }
    }

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

    suspend fun obtenerUltimaSesion(
        atletaId: String,
        rutinaId: String,
        diaId: String
    ): SesionEntrenamiento? = try {
        val snapshot = db.collection("users").document(atletaId)
            .collection("historial_entrenamientos")
            .where("rutinaAsignadaId", equalTo = rutinaId)
            .where("diaEntrenamientoId", equalTo = diaId)
            .orderBy("fechaEjecucion", Direction.DESCENDING)
            .limit(1)
            .get()

        snapshot.documents.firstOrNull()?.let { doc ->
            doc.data<SesionEntrenamiento>().copy(id = doc.id)
        }
    } catch (e: Exception) {
        println("🔥 [AtletaProgresoRepository] Error al consultar última sesión: ${e.message}")
        null
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

        val rutinaIdReal = rutinaActual.id.ifBlank { "ID_RUTINA_DESCONOCIDO" }

        val sesionIdFinal = sesionProcesada.id.ifBlank { Uuid.random().toString() }
        val sesionRef = db.collection("users").document(atletaId).collection("historial_entrenamientos").document(sesionIdFinal)

        val sesionFinal = sesionProcesada.copy(
            id = sesionIdFinal,
            estado = EstadoSesion.COMPLETADA,
            fechaEjecucion = if (sesionProcesada.fechaEjecucion <= 0L) ahoraMilis else sesionProcesada.fechaEjecucion
        )

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
            val sesionExistenteDoc = get(sesionRef)
            val esEdicion = sesionExistenteDoc.exists
            val sesionPrevia = if (esEdicion) sesionExistenteDoc.data<SesionEntrenamiento>() else null

            val deltaRepsLogradas = if (esEdicion && sesionPrevia != null) {
                sesionFinal.totalRepsEfectivasLogradas - sesionPrevia.totalRepsEfectivasLogradas
            } else {
                sesionFinal.totalRepsEfectivasLogradas
            }

            set(sesionRef, sesionFinal)

            val cicloActualizado: CicloEntrenamiento
            val cicloIdToUse = cicloActivo?.id ?: Uuid.random().toString()
            val cicloRefToUse = ciclosRef.document(cicloIdToUse)

            if (cicloActivo == null) {
                var totalRepsGlobales = 0
                rutinaActual.diasEntrenamiento.forEach { dia ->
                    dia.ejercicios.forEach { ejercicio ->
                        ejercicio.seriesPrescritas.forEach { serie ->
                            if (serie.tipo != TipoSerie.APROXIMACION) {
                                totalRepsGlobales += serie.maxReps
                            }
                        }
                    }
                }

                val fechaInicioReal = minOf(ahoraMilis, sesionFinal.fechaInicio)
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
                val nuevasSesiones = if (esEdicion) cicloActivo.sesionesCompletadas else cicloActivo.sesionesCompletadas + 1
                val nuevaMetaReps = cicloActivo.repeticionesMetaTotal
                val nuevasRepsLogradas = maxOf(0, cicloActivo.repeticionesLogradasTotal + deltaRepsLogradas)

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