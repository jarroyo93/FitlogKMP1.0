package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlin.uuid.Uuid

class UserRepository {
    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    suspend fun obtenerUsuario(uid: String): Usuario? {
        return try {
            val doc = usersCollection.document(uid).get()
            if (doc.exists) {
                try {
                    return doc.data<Usuario>().copy(id = doc.id)
                } catch (e: Exception) {
                    println("❌ [UserRepository] Error de conversión Firestore por ID de documento ($uid): ${e.message}")
                    e.printStackTrace()
                }
            }

            val query = usersCollection.where("authId", equalTo = uid).limit(1).get()
            if (query.documents.isNotEmpty()) {
                val docAtleta = query.documents[0]
                try {
                    return docAtleta.data<Usuario>().copy(id = docAtleta.id)
                } catch (e: Exception) {
                    println("❌ [UserRepository] Error de conversión Firestore por authId ($uid): ${e.message}")
                    e.printStackTrace()
                }
            }
            println("⚠️ [UserRepository] No se encontró usuario para el UID/authId: $uid")
            null
        } catch (e: Exception) {
            println("🔥 [UserRepository] Error crítico en obtenerUsuario ($uid): ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun existeCorreo(correo: String): Boolean = try {
        val result = usersCollection.where("correo", equalTo = correo.trim()).limit(1).get()
        result.documents.isNotEmpty()
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en existeCorreo ($correo): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun existeDocumento(documento: String): Boolean = try {
        val result = usersCollection.where("numeroDocumento", equalTo = documento.trim()).limit(1).get()
        result.documents.isNotEmpty()
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en existeDocumento ($documento): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun generarCodigoVinculacion(entrenadorId: String): String {
        return try {
            val codigo = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
            val expiracion = getCurrentTimeMillis() + 900000 // 15 minutos

            var docRef = usersCollection.document(entrenadorId)
            val docSnapshot = docRef.get()

            if (!docSnapshot.exists) {
                val query = usersCollection.where("authId", equalTo = entrenadorId).limit(1).get()
                if (query.documents.isNotEmpty()) {
                    docRef = usersCollection.document(query.documents[0].id)
                } else {
                    println("⚠️ [UserRepository] Entrenador no encontrado al generar código para ID: $entrenadorId")
                    throw Exception("Entrenador no encontrado en la base de datos")
                }
            }

            docRef.update("codigoVinculacion" to codigo, "expiracionCodigo" to expiracion)
            codigo
        } catch (e: Exception) {
            println("🔥 [UserRepository] Error en generarCodigoVinculacion ($entrenadorId): ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun obtenerAtletasPorEntrenador(entrenadorId: String): List<Usuario> = try {
        usersCollection
            .where("entrenadorId", equalTo = entrenadorId)
            .where("rol", equalTo = RolUsuario.ATLETA.name)
            .get()
            .documents.map { doc -> doc.data<Usuario>().copy(id = doc.id) }
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en obtenerAtletasPorEntrenador ($entrenadorId): ${e.message}")
        e.printStackTrace()
        emptyList()
    }

    suspend fun vincularConEntrenador(atletaId: String, correoEntrenador: String, codigoIngresado: String): Boolean = try {
        val query = db.collection("users")
            .where("correo", equalTo = correoEntrenador.trim())
            .where("codigoVinculacion", equalTo = codigoIngresado.trim().uppercase())
            .where("rol", equalTo = RolUsuario.ENTRENADOR.name)
            .limit(1)
            .get()

        if (query.documents.isEmpty()) {
            println("⚠️ [UserRepository] vincularConEntrenador: Código o correo de entrenador inválido. ($correoEntrenador / $codigoIngresado)")
            false
        } else {
            val doc = query.documents[0]
            val entrenadorDocId = doc.id
            val expiracion = doc.get<Long>("expiracionCodigo") ?: 0L

            if (getCurrentTimeMillis() > expiracion) {
                println("⚠️ [UserRepository] vincularConEntrenador: El código de vinculación ha expirado.")
                false
            } else {
                val userRef = db.collection("users").document(atletaId)

                // Consultar periodos vigentes o diferidos para cancelarlos
                val periodosVigentesSnapshot = userRef.collection("periodos_facturables")
                    .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
                    .get()

                val batch = db.batch()

                // RESETEAMOS TODOS LOS CAMPOS DE LA RAÍZ
                batch.update(
                    userRef,
                    "entrenadorId" to entrenadorDocId,
                    "estadoSuscripcion" to EstadoSuscripcion.VENCIDO.name,
                    "planActivo" to "Ninguno",
                    "fechaInicioSuscripcion" to 0L,
                    "vencimientoSuscripcion" to 0L,
                    "saldoMilisegundosRestantes" to null,
                    "motivoPausa" to null
                )

                // Cancelamos los periodos facturables de la subcolección
                for (pDoc in periodosVigentesSnapshot.documents) {
                    val refP = userRef.collection("periodos_facturables").document(pDoc.id)
                    batch.update(refP, "estado" to EstadoPeriodo.CANCELADO.name)
                }

                batch.commit()
                println("✅ [UserRepository] Atleta $atletaId vinculado correctamente al Entrenador $entrenadorDocId")
                true
            }
        }
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en vincularConEntrenador (atletaId: $atletaId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun desvincularAtleta(atletaId: String): Boolean = try {
        val userRef = usersCollection.document(atletaId)
        val periodosVigentesSnapshot = userRef.collection("periodos_facturables")
            .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
            .get()

        val batch = db.batch()
        batch.update(
            userRef,
            "entrenadorId" to null,
            "estadoSuscripcion" to EstadoSuscripcion.HUERFANO.name,
            "planActivo" to "Ninguno",
            "fechaInicioSuscripcion" to 0L,
            "vencimientoSuscripcion" to 0L,
            "saldoMilisegundosRestantes" to null,
            "motivoPausa" to null
        )

        for (doc in periodosVigentesSnapshot.documents) {
            val refPeriodo = userRef.collection("periodos_facturables").document(doc.id)
            batch.update(refPeriodo, "estado" to EstadoPeriodo.CANCELADO.name)
        }

        batch.commit()
        true
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en desvincularAtleta ($atletaId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun actualizarPerfilUsuario(uid: String, campos: Map<String, Any?>): Boolean = try {
        val pairs = campos.map { it.key to it.value }.toTypedArray()
        usersCollection.document(uid).update(*pairs)
        true
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en actualizarPerfilUsuario ($uid): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun actualizarDatosPersonales(
        uid: String, nombres: String, apellidos: String, tipoDocumento: String, documento: String, telefono: String
    ): Boolean = try {
        val campos = mapOf(
            "nombres" to nombres,
            "apellidos" to apellidos,
            "tipoDocumento" to tipoDocumento,
            "numeroDocumento" to documento,
            "telefono" to telefono
        )
        db.collection("users").document(uid).update(campos)
        true
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en actualizarDatosPersonales ($uid): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun obtenerPeriodosDeAtleta(atletaId: String): List<PeriodoFacturable> = try {
        val query = usersCollection.document(atletaId)
            .collection("periodos_facturables")
            .orderBy("fechaInicio", Direction.ASCENDING)
            .get()
        query.documents.map { doc -> doc.data<PeriodoFacturable>().copy(id = doc.id) }
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en obtenerPeriodosDeAtleta ($atletaId): ${e.message}")
        e.printStackTrace()
        emptyList()
    }

    suspend fun obtenerInformeFacturacionEntrenador(entrenadorId: String): List<RegistroContable> = try {
        db.collection("historial_facturacion_general")
            .where("entrenadorId", equalTo = entrenadorId)
            .orderBy("fechaRegistroTransaccion", Direction.DESCENDING)
            .get()
            .documents.map { doc -> doc.data<RegistroContable>().copy(id = doc.id) }
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en obtenerInformeFacturacionEntrenador ($entrenadorId): ${e.message}")
        e.printStackTrace()
        emptyList()
    }

    suspend fun suspenderAtleta(atletaId: String) {
        try {
            usersCollection.document(atletaId).update("estadoSuscripcion" to EstadoSuscripcion.SUSPENDIDO.name)
        } catch(e: Exception) {
            println("🔥 [UserRepository] Error en suspenderAtleta ($atletaId): ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun obtenerUltimaFechaFinCadena(atletaId: String): Long = try {
        val userRef = usersCollection.document(atletaId)
        val userSnap = userRef.get()

        if (!userSnap.exists) return getCurrentTimeMillis()

        val estadoSuscripcion = userSnap.get<String>("estadoSuscripcion")
        val ahora = getCurrentTimeMillis()

        // 🛡️ REGLA DE ORO: Si el atleta NO está ACTIVO ni DIFERIDO, la cadena siempre termina HOY
        if (estadoSuscripcion != EstadoSuscripcion.ACTIVO.name &&
            estadoSuscripcion != EstadoSuscripcion.DIFERIDO.name) {
            return ahora
        }

        val vencimientoRaiz = userSnap.get<Long>("vencimientoSuscripcion") ?: 0L
        val vencimientoValido = if (vencimientoRaiz > ahora) vencimientoRaiz else 0L

        val periodosSnapshot = userRef.collection("periodos_facturables")
            .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
            .get()

        val maxFechaFinDiferidos = periodosSnapshot.documents
            .mapNotNull { doc ->
                val pFin = doc.get<Long>("fechaFin") ?: 0L
                if (pFin > ahora) pFin else null
            }
            .maxOrNull() ?: 0L

        maxOf(vencimientoValido, maxFechaFinDiferidos, ahora)
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en obtenerUltimaFechaFinCadena ($atletaId): ${e.message}")
        e.printStackTrace()
        getCurrentTimeMillis()
    }

    suspend fun existeSolapamientoPeriodo(atletaId: String, fechaInicio: Long, fechaFin: Long): Boolean = try {
        val periodosSnapshot = usersCollection.document(atletaId)
            .collection("periodos_facturables")
            .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
            .get()

        periodosSnapshot.documents.any { doc ->
            val pInicio = doc.get<Long>("fechaInicio") ?: 0L
            val pFin = doc.get<Long>("fechaFin") ?: 0L
            maxOf(fechaInicio, pInicio) < minOf(fechaFin, pFin)
        }
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en existeSolapamientoPeriodo ($atletaId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun renovarSuscripcion(
        atletaId: String,
        entrenadorId: String,
        planActivo: String,
        fechaInicio: Long,
        fechaFin: Long,
        estadoPeriodo: EstadoPeriodo
    ): Boolean = try {
        if (fechaFin <= fechaInicio) {
            println("🔥 [UserRepository] renovarSuscripcion: La fecha fin ($fechaFin) debe ser mayor a la fecha inicio ($fechaInicio)")
            return false
        }

        if (existeSolapamientoPeriodo(atletaId, fechaInicio, fechaFin)) {
            println("🔥 [UserRepository] renovarSuscripcion: Solapamiento detectado para el atleta $atletaId")
            return false
        }

        val userRef = usersCollection.document(atletaId)
        val userSnapshot = userRef.get()

        if (!userSnapshot.exists) {
            println("🔥 [UserRepository] renovarSuscripcion: No existe el usuario $atletaId")
            return false
        }

        val estadoActual = userSnapshot.get<String>("estadoSuscripcion")
        val ahora = getCurrentTimeMillis()
        val nombres = userSnapshot.get<String>("nombres") ?: "Atleta"
        val apellidos = userSnapshot.get<String>("apellidos") ?: ""
        val nombreAtletaCompleto = "$nombres $apellidos".trim()

        if (estadoActual == EstadoSuscripcion.SUSPENDIDO.name) {
            println("🔥 [UserRepository] renovarSuscripcion: El atleta está SUSPENDIDO.")
            return false
        }

        val idUnicoCompartido = Uuid.random().toString()
        val periodoRef = userRef.collection("periodos_facturables").document(idUnicoCompartido)
        val registroContableRef = db.collection("historial_facturacion_general").document(idUnicoCompartido)

        val batch = db.batch()

        val esActivoAhora = estadoPeriodo == EstadoPeriodo.ACTIVO || (fechaInicio <= ahora && fechaFin > ahora)
        val nuevoEstadoPeriodo = if (esActivoAhora) EstadoPeriodo.ACTIVO else EstadoPeriodo.DIFERIDO

        if (esActivoAhora || estadoActual != EstadoSuscripcion.ACTIVO.name) {
            batch.update(
                userRef,
                "planActivo" to planActivo,
                "vencimientoSuscripcion" to if (esActivoAhora) fechaFin else fechaInicio,
                "fechaInicioSuscripcion" to fechaInicio,
                "estadoSuscripcion" to if (esActivoAhora) EstadoSuscripcion.ACTIVO.name else EstadoSuscripcion.DIFERIDO.name,
                "saldoMilisegundosRestantes" to null,
                "motivoPausa" to null
            )
        }

        val periodo = PeriodoFacturable(
            id = idUnicoCompartido,
            entrenadorId = entrenadorId,
            atletaId = atletaId,
            tipoPlan = planActivo,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            fechaCreacion = ahora,
            estado = nuevoEstadoPeriodo,
            diasRestantesAlCongelar = 0L
        )
        batch.set(periodoRef, periodo)

        val reciboContable = mapOf(
            "id" to idUnicoCompartido,
            "entrenadorId" to entrenadorId,
            "atletaId" to atletaId,
            "atletaNombreSnapshot" to nombreAtletaCompleto,
            "tipoPlan" to planActivo,
            "fechaInicio" to fechaInicio,
            "fechaFin" to fechaFin,
            "fechaRegistroTransaccion" to ahora,
            "estado" to nuevoEstadoPeriodo.name
        )
        batch.set(registroContableRef, reciboContable)

        batch.commit()
        println("✅ [UserRepository] renovarSuscripcion completada exitosamente para atleta $atletaId")
        true
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en renovarSuscripcion ($atletaId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun pausarAtleta(atletaId: String, motivo: String, saldoMilis: Long): Boolean = try {
        val userRef = usersCollection.document(atletaId)
        val saldoDias = saldoMilis / (1000 * 60 * 60 * 24)

        val periodoActivoSnapshot = userRef.collection("periodos_facturables")
            .where("estado", equalTo = EstadoPeriodo.ACTIVO.name).limit(1).get()

        val batch = db.batch()
        batch.update(
            userRef,
            "estadoSuscripcion" to EstadoSuscripcion.SUSPENDIDO.name,
            "motivoPausa" to motivo,
            "saldoMilisegundosRestantes" to saldoMilis,
            "vencimientoSuscripcion" to 0L
        )

        if (periodoActivoSnapshot.documents.isNotEmpty()) {
            val docId = periodoActivoSnapshot.documents[0].id
            val refPeriodo = userRef.collection("periodos_facturables").document(docId)
            batch.update(refPeriodo, "estado" to EstadoPeriodo.CONGELADO.name, "diasRestantesAlCongelar" to saldoDias)
        }

        batch.commit()
        true
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en pausarAtleta ($atletaId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun reactivarAtleta(atletaId: String, nuevaFechaFin: Long): Boolean = try {
        val ahora = getCurrentTimeMillis()
        val userRef = usersCollection.document(atletaId)

        val periodoCongeladoSnapshot = userRef.collection("periodos_facturables")
            .where("estado", equalTo = EstadoPeriodo.CONGELADO.name).limit(1).get()

        val batch = db.batch()
        batch.update(
            userRef,
            "estadoSuscripcion" to EstadoSuscripcion.ACTIVO.name,
            "motivoPausa" to null,
            "saldoMilisegundosRestantes" to null,
            "fechaInicioSuscripcion" to ahora,
            "vencimientoSuscripcion" to nuevaFechaFin
        )

        if (periodoCongeladoSnapshot.documents.isNotEmpty()) {
            val docId = periodoCongeladoSnapshot.documents[0].id
            val refPeriodo = userRef.collection("periodos_facturables").document(docId)
            batch.update(refPeriodo, "estado" to EstadoPeriodo.ACTIVO.name, "fechaInicio" to ahora, "fechaFin" to nuevaFechaFin, "diasRestantesAlCongelar" to 0L)
        }

        batch.commit()

        val periodosDiferidos = userRef.collection("periodos_facturables")
            .where("estado", equalTo = EstadoPeriodo.DIFERIDO.name)
            .get().documents
            .map { doc -> doc.data<PeriodoFacturable>().copy(id = doc.id) }
            .sortedBy { it.fechaInicio }

        if (periodosDiferidos.isNotEmpty()) {
            val batchDiferidos = db.batch()
            // 🟢 REGLA: +1 ms para pasar de las 23:59:59.999 a las 00:00:00.000 del día siguiente
            var proximoInicio = nuevaFechaFin + 1L

            for (p in periodosDiferidos) {
                val duracion = (p.fechaFin ?: 0L) - p.fechaInicio
                val nuevoFin = proximoInicio + duracion
                val refP = userRef.collection("periodos_facturables").document(p.id)
                batchDiferidos.update(refP, "fechaInicio" to proximoInicio, "fechaFin" to nuevoFin)

                // 🟢 El siguiente plan en la cadena también inicia 1 ms después de que vence este
                proximoInicio = nuevoFin + 1L
            }
            batchDiferidos.commit()
        }

        true
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en reactivarAtleta ($atletaId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun cancelarPeriodo(atletaId: String, periodoId: String): Boolean = try {
        val userRef = usersCollection.document(atletaId)
        val periodoAtletaRef = userRef.collection("periodos_facturables").document(periodoId)
        val registroGlobalRef = db.collection("historial_facturacion_general").document(periodoId)

        val snapshotPeriodo = periodoAtletaRef.get()
        val estadoActual = snapshotPeriodo.get<String>("estado")
        val fechaCreacionMilis = snapshotPeriodo.get<Long>("fechaCreacion") ?: 0L
        val ahora = getCurrentTimeMillis()

        val esCreadoHoy = esMismoDia(ahora, fechaCreacionMilis)

        if (estadoActual == EstadoPeriodo.ACTIVO.name && !esCreadoHoy) {
            println("⚠️ [UserRepository] No se permite cancelar un periodo ACTIVO que no fue creado hoy.")
            false
        } else {
            val batch = db.batch()
            batch.update(periodoAtletaRef, "estado" to EstadoPeriodo.CANCELADO.name)
            batch.update(registroGlobalRef, "estado" to EstadoPeriodo.CANCELADO.name)
            batch.commit()

            val userRaw = obtenerUsuario(atletaId)
            if (userRaw != null) {
                evaluarYActualizarEstadoSuscripcion(userRaw)
            }
            true
        }
    } catch (e: Exception) {
        println("🔥 [UserRepository] Error en cancelarPeriodo ($periodoId): ${e.message}")
        e.printStackTrace()
        false
    }

    suspend fun evaluarYActualizarEstadoSuscripcion(usuario: Usuario): Usuario {
        val ahora = getCurrentTimeMillis()
        val userRef = usersCollection.document(usuario.id)

        if (usuario.estadoSuscripcion == EstadoSuscripcion.HUERFANO ||
            usuario.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO) {
            return usuario
        }

        return try {
            val periodosSnapshot = userRef.collection("periodos_facturables")
                .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
                .get()
                .documents.map { it.data<PeriodoFacturable>().copy(id = it.id) }

            // 1. Marca como COMPLETADO cualquier plan cuya fecha de fin ya pasó
            periodosSnapshot.filter { it.estado == EstadoPeriodo.ACTIVO && (it.fechaFin ?: 0L) < ahora }.forEach { p ->
                userRef.collection("periodos_facturables").document(p.id).update("estado" to EstadoPeriodo.COMPLETADO.name)
            }

            val periodosVivosRestantes = periodosSnapshot.filter {
                !(it.estado == EstadoPeriodo.ACTIVO && (it.fechaFin ?: 0L) < ahora)
            }

            // 2. AUTORREPARACIÓN: Si no hay planes vivos en la subcolección, limpia la raíz a VENCIDO
            if (periodosVivosRestantes.isEmpty()) {
                if (usuario.estadoSuscripcion != EstadoSuscripcion.VENCIDO || usuario.vencimientoSuscripcion != 0L) {
                    val updates = mapOf(
                        "estadoSuscripcion" to EstadoSuscripcion.VENCIDO.name,
                        "planActivo" to "Ninguno",
                        "vencimientoSuscripcion" to 0L,
                        "fechaInicioSuscripcion" to 0L
                    )
                    userRef.update(updates)
                    return usuario.copy(
                        estadoSuscripcion = EstadoSuscripcion.VENCIDO,
                        planActivo = "Ninguno",
                        vencimientoSuscripcion = 0L,
                        fechaInicioSuscripcion = 0L
                    )
                }
                return usuario
            }

            // 3. Activa planes diferidos programados para hoy
            val proximoDiferido = periodosVivosRestantes
                .filter { it.estado == EstadoPeriodo.DIFERIDO && it.fechaInicio <= ahora }
                .minByOrNull { it.fechaInicio }

            if (proximoDiferido != null) {
                userRef.collection("periodos_facturables").document(proximoDiferido.id)
                    .update("estado" to EstadoPeriodo.ACTIVO.name)

                val updates = mapOf(
                    "estadoSuscripcion" to EstadoSuscripcion.ACTIVO.name,
                    "planActivo" to proximoDiferido.tipoPlan,
                    "fechaInicioSuscripcion" to proximoDiferido.fechaInicio,
                    "vencimientoSuscripcion" to (proximoDiferido.fechaFin ?: 0L)
                )
                userRef.update(updates)
                return usuario.copy(
                    estadoSuscripcion = EstadoSuscripcion.ACTIVO,
                    planActivo = proximoDiferido.tipoPlan,
                    fechaInicioSuscripcion = proximoDiferido.fechaInicio,
                    vencimientoSuscripcion = proximoDiferido.fechaFin
                )
            }

            usuario
        } catch (e: Exception) {
            println("🔥 [UserRepository] Error en evaluarYActualizarEstadoSuscripcion (${usuario.id}): ${e.message}")
            e.printStackTrace()
            usuario
        }
    }
}