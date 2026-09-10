package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AtletaProgresoRepository {
    private val db = Firebase.firestore

    // ============================================================
    // CONSULTA HISTÓRICA POR EJERCICIO
    // ============================================================
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

                            if (globalId.isNotBlank() && !mapaResultado.containsKey(globalId)) {
                                mapaResultado[globalId] = registroObj
                            }

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

    suspend fun obtenerEntrenamientosCicloActivo(
        atletaId: String,
        fechaInicioCicloMs: Long
    ): List<SesionEntrenamiento> {
        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("historial_entrenamientos")
                .where { "fechaEjecucion" greaterThanOrEqualTo fechaInicioCicloMs }
                .orderBy("fechaEjecucion", Direction.DESCENDING)
                .get()

            snapshot.documents.map { doc -> doc.data<SesionEntrenamiento>().copy(id = doc.id) }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al obtener entrenamientos del ciclo activo: ${e.message}")
            emptyList()
        }
    }

    // 🟢 CORREGIDO: Cierra automáticamente los ciclos expirados por fecha al consultar
    suspend fun obtenerCicloActivo(atletaId: String): CicloEntrenamiento? {
        return try {
            val snapshot = db.collection("users").document(atletaId)
                .collection("ciclos_entrenamiento")
                .where("estaActivo", equalTo = true)
                .limit(1)
                .get()

            val doc = snapshot.documents.firstOrNull() ?: return null
            val ciclo = doc.data<CicloEntrenamiento>().copy(id = doc.id)
            val ahoraMilis = getCurrentTimeMillis()

            if (ahoraMilis > ciclo.fechaCierre) {
                db.collection("users").document(atletaId)
                    .collection("ciclos_entrenamiento")
                    .document(ciclo.id)
                    .update("estaActivo" to false)
                return null
            }

            ciclo
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al obtener ciclo activo: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // 🟢 CORREGIDO: Expiración universal por fecha para cualquier tipo de ciclo
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

        val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true)
            .orderBy("fechaInicio", Direction.DESCENDING)
            .get()

        var cicloActivo = activeCyclesSnapshot.documents.firstOrNull()?.let { doc ->
            doc.data<CicloEntrenamiento>().copy(id = doc.id)
        }

        // 🟢 Aplica para cualquier modo si la fecha actual es mayor a la fecha de cierre programada
        val cicloExpiradoPorFecha = cicloActivo != null && ahoraMilis > cicloActivo.fechaCierre

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

            if (cicloActivo == null || cicloExpiradoPorFecha) {
                if (cicloExpiradoPorFecha && cicloActivo != null) {
                    val cicloAnteriorCerrado = cicloActivo.copy(estaActivo = false)
                    set(ciclosRef.document(cicloActivo.id), cicloAnteriorCerrado)
                }

                val modo = rutinaActual.modoCiclo
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

                val fechaPrimerRegistro = minOf(ahoraMilis, sesionFinal.fechaInicio)
                val duracionDiasDefecto = if (modo == ModoCiclo.CALENDARIO_SEMANAL) 7 else maxOf(7, rutinaActual.diasEntrenamiento.size)

                val (inicioCalculado, cierreCalculado) = calcularRangoFechasCiclo(
                    fechaPrimerRegistroMs = fechaPrimerRegistro,
                    duracionDias = duracionDiasDefecto,
                    modo = modo
                )

                val nuevoCicloId = Uuid.random().toString()
                val nuevoCiclo = CicloEntrenamiento(
                    id = nuevoCicloId,
                    atletaId = atletaId,
                    rutinaAsignadaId = sesionFinal.rutinaAsignadaId,
                    fechaInicio = inicioCalculado,
                    fechaCierre = cierreCalculado,
                    duracionDias = duracionDiasDefecto,
                    modoCiclo = modo,
                    estaActivo = true,
                    metaSesionesAsignadas = metaSesiones,
                    sesionesCompletadas = 1,
                    repeticionesMetaTotal = totalRepsGlobales,
                    repeticionesLogradasTotal = sesionFinal.totalRepsEfectivasLogradas
                )

                val porcentajeAsist = if (nuevoCiclo.metaSesionesAsignadas > 0) {
                    ((nuevoCiclo.sesionesCompletadas.toDouble() / nuevoCiclo.metaSesionesAsignadas.toDouble()) * 100.0).coerceAtMost(100.0)
                } else 0.0

                val porcentajeVol = if (nuevoCiclo.repeticionesMetaTotal > 0) {
                    ((nuevoCiclo.repeticionesLogradasTotal.toDouble() / nuevoCiclo.repeticionesMetaTotal.toDouble()) * 100.0).coerceAtMost(100.0)
                } else 0.0

                cicloActualizado = nuevoCiclo.copy(
                    porcentajeAsistencia = porcentajeAsist,
                    porcentajeVolumenGlobal = porcentajeVol
                )
                set(ciclosRef.document(nuevoCicloId), cicloActualizado)
            } else {
                val nuevasSesiones = if (esEdicion) cicloActivo.sesionesCompletadas else cicloActivo.sesionesCompletadas + 1
                val nuevaMetaReps = cicloActivo.repeticionesMetaTotal
                val nuevasRepsLogradas = maxOf(0, cicloActivo.repeticionesLogradasTotal + deltaRepsLogradas)

                val porcentajeAsist = if (cicloActivo.metaSesionesAsignadas > 0) {
                    ((nuevasSesiones.toDouble() / cicloActivo.metaSesionesAsignadas.toDouble()) * 100.0).coerceAtMost(100.0)
                } else 0.0

                val porcentajeVol = if (nuevaMetaReps > 0) {
                    ((nuevasRepsLogradas.toDouble() / nuevaMetaReps.toDouble()) * 100.0).coerceAtMost(100.0)
                } else 0.0

                val cicloCompleto = nuevasSesiones >= cicloActivo.metaSesionesAsignadas

                cicloActualizado = cicloActivo.copy(
                    sesionesCompletadas = nuevasSesiones,
                    repeticionesMetaTotal = nuevaMetaReps,
                    repeticionesLogradasTotal = nuevasRepsLogradas,
                    porcentajeAsistencia = porcentajeAsist,
                    porcentajeVolumenGlobal = porcentajeVol,
                    estaActivo = !cicloCompleto,
                    fechaCierre = if (cicloCompleto) ahoraMilis else cicloActivo.fechaCierre
                )
                set(ciclosRef.document(cicloActivo.id), cicloActualizado)
            }

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

    suspend fun sincronizarCicloActivoConRutina(atletaId: String, rutinaActualizada: RutinaAsignada) {
        try {
            val ciclosRef = db.collection("users").document(atletaId).collection("ciclos_entrenamiento")
            val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true)
                .orderBy("fechaInicio", Direction.DESCENDING)
                .get()

            val cicloActivo = activeCyclesSnapshot.documents.firstOrNull()?.let { doc ->
                doc.data<CicloEntrenamiento>().copy(id = doc.id)
            }

            if (cicloActivo != null) {
                val nuevaMetaSesiones = rutinaActualizada.diasEntrenamiento.size
                var nuevasRepsMetaTotal = 0
                rutinaActualizada.diasEntrenamiento.forEach { dia ->
                    dia.ejercicios.forEach { ejercicio ->
                        ejercicio.seriesPrescritas.forEach { serie ->
                            if (serie.tipo != TipoSerie.APROXIMACION) {
                                nuevasRepsMetaTotal += serie.maxReps
                            }
                        }
                    }
                }

                val nuevoPctAsistencia = if (nuevaMetaSesiones > 0) {
                    ((cicloActivo.sesionesCompletadas.toDouble() / nuevaMetaSesiones.toDouble()) * 100.0).coerceAtMost(100.0)
                } else 0.0

                val nuevoPctVolumen = if (nuevasRepsMetaTotal > 0) {
                    ((cicloActivo.repeticionesLogradasTotal.toDouble() / nuevasRepsMetaTotal.toDouble()) * 100.0).coerceAtMost(100.0)
                } else 0.0

                val cicloActualizado = cicloActivo.copy(
                    modoCiclo = rutinaActualizada.modoCiclo,
                    metaSesionesAsignadas = nuevaMetaSesiones,
                    repeticionesMetaTotal = nuevasRepsMetaTotal,
                    porcentajeAsistencia = nuevoPctAsistencia,
                    porcentajeVolumenGlobal = nuevoPctVolumen
                )

                ciclosRef.document(cicloActivo.id).set(cicloActualizado)
            }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al sincronizar ciclo activo con rutina: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun actualizarMetaCicloActivo(atletaId: String, nuevaMetaSesiones: Int, nuevasRepsMetaTotal: Int) {
        try {
            val ciclosRef = db.collection("users").document(atletaId).collection("ciclos_entrenamiento")
            val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true)
                .orderBy("fechaInicio", Direction.DESCENDING)
                .get()

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

    // 🟢 CORREGIDO: Cierra TODOS los ciclos activos en caso de duplicados o reasignación
    suspend fun forzarCierreCicloActivo(atletaId: String) {
        try {
            val ciclosRef = db.collection("users").document(atletaId).collection("ciclos_entrenamiento")
            val activeCyclesSnapshot = ciclosRef.where("estaActivo", equalTo = true).get()

            for (doc in activeCyclesSnapshot.documents) {
                ciclosRef.document(doc.id).update("estaActivo" to false)
            }
        } catch (e: Exception) {
            println("🔥 [AtletaProgresoRepository] Error al forzar cierre de ciclos activos: ${e.message}")
            e.printStackTrace()
        }
    }
}