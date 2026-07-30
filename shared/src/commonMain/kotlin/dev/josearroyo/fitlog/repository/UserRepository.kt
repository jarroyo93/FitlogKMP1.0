package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.*
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

    // 🟢 OPTIMIZADO: Eliminada línea huérfana innecesaria de ordenamiento que no ejecutaba nada.
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

    suspend fun renovarSuscripcion(
        atletaId: String, entrenadorId: String, planActivo: String, fechaInicio: Long, fechaFin: Long, estadoPeriodo: EstadoPeriodo
    ): Boolean = try {
        val userRef = usersCollection.document(atletaId)

        // 🟢 MEJORADO: Cambiada generación manual de String por Uuid universal nativo KMP
        val idUnicoCompartido = Uuid.random().toString()

        val periodoRef = userRef.collection("periodos_facturables").document(idUnicoCompartido)
        val registroContableRef = db.collection("historial_facturacion_general").document(idUnicoCompartido)

        val userSnapshot = userRef.get()
        val estadoActual = userSnapshot.get<String>("estadoSuscripcion")
        val vencimientoActual = userSnapshot.get<Long>("vencimientoSuscripcion")
        val ahora = getCurrentTimeMillis()

        val nombres = userSnapshot.get<String>("nombres") ?: "Atleta"
        val apellidos = userSnapshot.get<String>("apellidos") ?: ""
        val nombreAtletaCompleto = "$nombres $apellidos"

        val tienePlanActivoCorriendo = estadoActual == EstadoSuscripcion.ACTIVO.name && vencimientoActual > ahora

        val batch = db.batch()

        if (tienePlanActivoCorriendo) {
            batch.update(userRef, "vencimientoSuscripcion" to fechaFin, "saldoMilisegundosRestantes" to null, "motivoPausa" to null)
        } else {
            batch.update(
                userRef,
                "planActivo" to planActivo,
                "fechaInicioSuscripcion" to fechaInicio,
                "vencimientoSuscripcion" to fechaFin,
                "estadoSuscripcion" to EstadoSuscripcion.ACTIVO.name,
                "saldoMilisegundosRestantes" to null,
                "motivoPausa" to null
            )
        }

        val periodo = PeriodoFacturable(
            id = idUnicoCompartido, entrenadorId = entrenadorId, atletaId = atletaId, tipoPlan = planActivo,
            fechaInicio = fechaInicio, fechaFin = fechaFin, fechaCreacion = ahora, estado = estadoPeriodo, diasRestantesAlCongelar = 0L
        )
        batch.set(periodoRef, periodo)

        val reciboContable = mapOf(
            "id" to idUnicoCompartido, "entrenadorId" to entrenadorId, "atletaId" to atletaId,
            "atletaNombreSnapshot" to nombreAtletaCompleto.trim(), "tipoPlan" to planActivo,
            "fechaInicio" to fechaInicio, "fechaFin" to fechaFin, "fechaRegistroTransaccion" to ahora, "estado" to estadoPeriodo.name
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
        val fechaCreacionMilis = snapshotPeriodo.get<Long>("fechaCreacion")

        val ahora = getCurrentTimeMillis()
        val esCreadoHoy = (ahora / 86400000) == (fechaCreacionMilis / 86400000)

        if (estadoActual == EstadoPeriodo.ACTIVO.name && !esCreadoHoy) {
            false
        } else {
            val batch = db.batch()
            batch.update(periodoAtletaRef, "estado" to EstadoPeriodo.CANCELADO.name)
            batch.update(registroGlobalRef, "estado" to EstadoPeriodo.CANCELADO.name)
            batch.commit()

            val periodosVivosSnapshot = userRef.collection("periodos_facturables")
                .where { "estado" inArray listOf(EstadoPeriodo.ACTIVO.name, EstadoPeriodo.DIFERIDO.name) }
                .get()
                .documents.map { it.data<PeriodoFacturable>() }

            val nuevoVencimientoRaiz = periodosVivosSnapshot.maxOfOrNull { it.fechaFin ?: 0L } ?: 0L

            if (nuevoVencimientoRaiz > 0L) {
                userRef.update(
                    "vencimientoSuscripcion" to nuevoVencimientoRaiz,
                    "estadoSuscripcion" to if (nuevoVencimientoRaiz > ahora) EstadoSuscripcion.ACTIVO.name else EstadoSuscripcion.VENCIDO.name
                )
            } else {
                userRef.update("vencimientoSuscripcion" to 0L, "planActivo" to "Ninguno", "estadoSuscripcion" to EstadoSuscripcion.VENCIDO.name)
            }
            true
        }
    } catch (e: Exception) {
        println("🔥 Error en cancelarPeriodo: ${e.message}")
        false
    }
}