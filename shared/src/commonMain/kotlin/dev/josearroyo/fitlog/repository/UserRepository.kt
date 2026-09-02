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
                    println("❌ ERROR DE CONVERSIÓN GITLIVE Firestore (Directo): ${e.message}")
                    e.printStackTrace()
                }
            }

            val query = usersCollection.where("authId", equalTo = uid).limit(1).get()
            if (query.documents.isNotEmpty()) {
                val docAtleta = query.documents[0]
                try {
                    return docAtleta.data<Usuario>().copy(id = docAtleta.id)
                } catch (e: Exception) {
                    println("❌ ERROR DE CONVERSIÓN GITLIVE Firestore (authId): ${e.message}")
                    e.printStackTrace()
                }
            }
            null
        } catch (e: Exception) {
            println("❌ ERROR CRÍTICO FIRESTORE: ${e.message}")
            null
        }
    }

    suspend fun existeCorreo(correo: String): Boolean = try {
        val result = usersCollection.where("correo", equalTo = correo.trim()).limit(1).get()
        result.documents.isNotEmpty()
    } catch (e: Exception) { false }

    suspend fun existeDocumento(documento: String): Boolean = try {
        val result = usersCollection.where("numeroDocumento", equalTo = documento.trim()).limit(1).get()
        result.documents.isNotEmpty()
    } catch (e: Exception) { false }

    suspend fun generarCodigoVinculacion(entrenadorId: String): String {
        val codigo = (1..6).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
        val expiracion = getCurrentTimeMillis() + 900000

        var docRef = usersCollection.document(entrenadorId)
        val docSnapshot = docRef.get()

        if (!docSnapshot.exists) {
            val query = usersCollection.where("authId", equalTo = entrenadorId).limit(1).get()
            if (query.documents.isNotEmpty()) {
                docRef = usersCollection.document(query.documents[0].id)
            } else {
                throw Exception("Entrenador no encontrado en la base de datos")
            }
        }

        docRef.update("codigoVinculacion" to codigo, "expiracionCodigo" to expiracion)
        return codigo
    }

    suspend fun obtenerAtletasPorEntrenador(entrenadorId: String): List<Usuario> = try {
        usersCollection
            .where("entrenadorId", equalTo = entrenadorId)
            .where("rol", equalTo = RolUsuario.ATLETA.name)
            .get()
            .documents.map { doc -> doc.data<Usuario>().copy(id = doc.id) }
    } catch (e: Exception) { emptyList() }

    suspend fun vincularConEntrenador(atletaId: String, correoEntrenador: String, codigoIngresado: String): Boolean = try {
        val query = db.collection("users")
            .where("correo", equalTo = correoEntrenador.trim())
            .where("codigoVinculacion", equalTo = codigoIngresado.trim().uppercase())
            .where("rol", equalTo = RolUsuario.ENTRENADOR.name)
            .limit(1)
            .get()

        if (query.documents.isEmpty()) false
        else {
            val doc = query.documents[0]
            val entrenadorDocId = doc.id
            val expiracion = doc.get<Long>("expiracionCodigo")

            if (getCurrentTimeMillis() > expiracion) false
            else {
                db.collection("users").document(atletaId).update(
                    "entrenadorId" to entrenadorDocId,
                    "estadoSuscripcion" to EstadoSuscripcion.VENCIDO.name
                )
                true
            }
        }
    } catch (e: Exception) { false }

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
        println("🔥 Error en desvincularAtleta: ${e.message}")
        false
    }

    suspend fun actualizarPerfilUsuario(uid: String, campos: Map<String, Any?>): Boolean = try {
        val pairs = campos.map { it.key to it.value }.toTypedArray()
        usersCollection.document(uid).update(*pairs)
        true
    } catch (e: Exception) { false }

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
    } catch (e: Exception) { false }

    suspend fun obtenerPeriodosDeAtleta(atletaId: String): List<PeriodoFacturable> = try {
        val query = usersCollection.document(atletaId)
            .collection("periodos_facturables")
            .orderBy("fechaInicio", Direction.ASCENDING)
            .get()
        query.documents.map { doc -> doc.data<PeriodoFacturable>().copy(id = doc.id) }
    } catch (e: Exception) { emptyList() }

    suspend fun obtenerInformeFacturacionEntrenador(entrenadorId: String): List<RegistroContable> = try {
        db.collection("historial_facturacion_general")
            .where("entrenadorId", equalTo = entrenadorId)
            .orderBy("fechaRegistroTransaccion", Direction.DESCENDING)
            .get()
            .documents.map { doc -> doc.data<RegistroContable>().copy(id = doc.id) }
    } catch (e: Exception) { emptyList() }

    suspend fun suspenderAtleta(atletaId: String) {
        try {
            usersCollection.document(atletaId).update("estadoSuscripcion" to EstadoSuscripcion.SUSPENDIDO.name)
        } catch(e: Exception) {
            println("🔥 Error en suspenderAtleta: ${e.message}")
        }
    }

    // 🟢 NUEVO: Obtiene la fecha fin más lejana considerando el plan activo Y todos los diferidos en cola
    suspend fun obtenerUltimaFechaFinCadena(atletaId: String): Long = try {
        val userRef = usersCollection.document(atletaId)
        val userSnap = userRef.get()
        val vencimientoRaiz = userSnap.get<Long>("vencimientoSuscripcion") ?: 0L
        val ahora = getCurrentTimeMillis()

        val periodosSnapshot = userRef.collection("periodos_facturables")
            .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
            .get()

        val maxFechaFinDiferidos = periodosSnapshot.documents
            .mapNotNull { it.get<Long>("fechaFin") }
            .maxOrNull() ?: 0L

        maxOf(vencimientoRaiz, maxFechaFinDiferidos, ahora)
    } catch (e: Exception) {
        getCurrentTimeMillis()
    }

    // 🟢 NUEVO: Valida si un rango de fechas manual colisiona con periodos activos o diferidos
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
        false
    }

    // 🟢 CORREGIDO: Evita sobreescribir la raíz del usuario cuando se compra un plan DIFERIDO para un atleta ACTIVO
    suspend fun renovarSuscripcion(
        atletaId: String,
        entrenadorId: String,
        planActivo: String,
        fechaInicio: Long,
        fechaFin: Long,
        estadoPeriodo: EstadoPeriodo
    ): Boolean = try {
        val userRef = usersCollection.document(atletaId)
        val idUnicoCompartido = Uuid.random().toString()
        val periodoRef = userRef.collection("periodos_facturables").document(idUnicoCompartido)
        val registroContableRef = db.collection("historial_facturacion_general").document(idUnicoCompartido)

        val userSnapshot = userRef.get()
        val estadoActual = userSnapshot.get<String>("estadoSuscripcion")
        val ahora = getCurrentTimeMillis()
        val nombres = userSnapshot.get<String>("nombres") ?: "Atleta"
        val apellidos = userSnapshot.get<String>("apellidos") ?: ""
        val nombreAtletaCompleto = "$nombres $apellidos".trim()

        val batch = db.batch()

        // 🛡️ REGLA DE ORO: Solo actualizamos la raíz si el nuevo periodo se ACTIVA de una vez o si el usuario no estaba ACTIVO.
        if (estadoPeriodo == EstadoPeriodo.ACTIVO || estadoActual != EstadoSuscripcion.ACTIVO.name) {
            batch.update(
                userRef,
                "planActivo" to planActivo,
                "vencimientoSuscripcion" to fechaFin,
                "fechaInicioSuscripcion" to fechaInicio,
                "estadoSuscripcion" to if (estadoPeriodo == EstadoPeriodo.ACTIVO) EstadoSuscripcion.ACTIVO.name else EstadoSuscripcion.DIFERIDO.name,
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
            estado = estadoPeriodo,
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
            "estado" to estadoPeriodo.name
        )
        batch.set(registroContableRef, reciboContable)

        batch.commit()
        true
    } catch (e: Exception) {
        println("🔥 Error en renovarSuscripcion: ${e.message}")
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
        println("🔥 Error en pausarAtleta: ${e.message}")
        false
    }

    // 🟢 CORREGIDO: Reorganiza y desplaza en cadena (efecto dominó) todos los periodos DIFERIDOS al despausar
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

        // ⚡ EFECTO DOMINÓ: Re-encadenar periodos diferidos para evitar solapamientos tras la pausa
        val periodosDiferidos = userRef.collection("periodos_facturables")
            .where("estado", equalTo = EstadoPeriodo.DIFERIDO.name)
            .get().documents
            .map { doc -> doc.data<PeriodoFacturable>().copy(id = doc.id) }
            .sortedBy { it.fechaInicio }

        if (periodosDiferidos.isNotEmpty()) {
            val batchDiferidos = db.batch()
            var proximoInicio = nuevaFechaFin + 1000L

            for (p in periodosDiferidos) {
                val duracion = (p.fechaFin ?: 0L) - p.fechaInicio
                val nuevoFin = proximoInicio + duracion
                val refP = userRef.collection("periodos_facturables").document(p.id)
                batchDiferidos.update(refP, "fechaInicio" to proximoInicio, "fechaFin" to nuevoFin)
                proximoInicio = nuevoFin + 1000L
            }
            batchDiferidos.commit()
        }

        true
    } catch (e: Exception) {
        println("🔥 Error en reactivarAtleta: ${e.message}")
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
        println("🔥 Error en cancelarPeriodo: ${e.message}")
        false
    }

    suspend fun evaluarYActualizarEstadoSuscripcion(usuario: Usuario): Usuario {
        val ahora = getCurrentTimeMillis()
        val userRef = usersCollection.document(usuario.id)

        if (usuario.estadoSuscripcion == EstadoSuscripcion.HUERFANO ||
            usuario.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO) {
            return usuario
        }

        val vencimiento = usuario.vencimientoSuscripcion ?: 0L
        val fechaInicio = usuario.fechaInicioSuscripcion ?: 0L

        val estaVencido = usuario.estadoSuscripcion == EstadoSuscripcion.ACTIVO && vencimiento in 1..<ahora
        val debeActivarDiferido = usuario.estadoSuscripcion == EstadoSuscripcion.DIFERIDO && fechaInicio in 1..ahora

        if (!estaVencido && !debeActivarDiferido && usuario.estadoSuscripcion == EstadoSuscripcion.ACTIVO) {
            return usuario
        }

        return try {
            val periodosSnapshot = userRef.collection("periodos_facturables")
                .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
                .get()
                .documents.map { it.data<PeriodoFacturable>().copy(id = it.id) }

            periodosSnapshot.filter { it.estado == EstadoPeriodo.ACTIVO && (it.fechaFin ?: 0L) < ahora }.forEach { p ->
                userRef.collection("periodos_facturables").document(p.id).update("estado" to EstadoPeriodo.COMPLETADO.name)
            }

            val proximoDiferido = periodosSnapshot
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
                usuario.copy(
                    estadoSuscripcion = EstadoSuscripcion.ACTIVO,
                    planActivo = proximoDiferido.tipoPlan,
                    fechaInicioSuscripcion = proximoDiferido.fechaInicio,
                    vencimientoSuscripcion = proximoDiferido.fechaFin
                )
            } else if (estaVencido) {
                val updates = mapOf(
                    "estadoSuscripcion" to EstadoSuscripcion.VENCIDO.name,
                    "planActivo" to "Ninguno",
                    "vencimientoSuscripcion" to 0L
                )
                userRef.update(updates)
                usuario.copy(
                    estadoSuscripcion = EstadoSuscripcion.VENCIDO,
                    planActivo = "Ninguno",
                    vencimientoSuscripcion = 0L
                )
            } else {
                usuario
            }
        } catch (e: Exception) {
            println("🔥 Error en evaluarYActualizarEstadoSuscripcion: ${e.message}")
            usuario
        }
    }
}