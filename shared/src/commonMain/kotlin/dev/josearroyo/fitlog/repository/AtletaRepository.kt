package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.getCurrentTimeMillis
import kotlin.uuid.Uuid

class AtletaRepository {
    private val db = Firebase.firestore
    private val usersRef = db.collection("users")
    private val auth = Firebase.auth

    suspend fun obtenerUsuario(atletaId: String): Usuario? = try {
        val doc = usersRef.document(atletaId).get()
        if (doc.exists) doc.data<Usuario>().copy(id = doc.id) else null
    } catch (e: Exception) { null }

    suspend fun obtenerUltimaValoracion(atletaId: String): ValoracionFisica? = try {
        val snapshot = usersRef.document(atletaId).collection("valoraciones")
            .orderBy("fechaRegistro", Direction.DESCENDING)
            .limit(1).get()

        snapshot.documents.firstOrNull()?.let { doc ->
            doc.data<ValoracionFisica>().copy(id = doc.id)
        }
    } catch (e: Exception) { null }

    suspend fun obtenerHistorialValoraciones(atletaId: String): List<ValoracionFisica> = try {
        usersRef.document(atletaId)
            .collection("valoraciones")
            .orderBy("fechaRegistro", Direction.DESCENDING)
            .get()
            .documents
            .map { doc -> doc.data<ValoracionFisica>().copy(id = doc.id) }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // 🚀 CORRECCIÓN: Cambiado .add() por .set() con ID pre-generado para evitar IDs huérfanos internos
    suspend fun guardarValoracion(atletaId: String, valoracion: ValoracionFisica): Boolean = try {
        val nuevoId = Uuid.random().toString()
        val valoracionConFecha = valoracion.copy(id = nuevoId, fechaRegistro = getCurrentTimeMillis())
        usersRef.document(atletaId).collection("valoraciones").document(nuevoId).set(valoracionConFecha)
        true
    } catch (e: Exception) { false }

    suspend fun obtenerUltimosHabitos(atletaId: String): Habitos? = try {
        val snapshot = usersRef.document(atletaId).collection("habitos")
            .orderBy("fechaRegistro", Direction.DESCENDING)
            .limit(1).get()

        snapshot.documents.firstOrNull()?.let { doc ->
            doc.data<Habitos>().copy(id = doc.id)
        }
    } catch (e: Exception) { null }

    suspend fun obtenerHistorialHabitos(atletaId: String): List<Habitos> = try {
        val snapshot = usersRef.document(atletaId).collection("habitos")
            .orderBy("fechaRegistro", Direction.DESCENDING).get()

        snapshot.documents.map { doc -> doc.data<Habitos>().copy(id = doc.id) }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // 🚀 CORRECCIÓN: Cambiado .add() por .set() con ID pre-generado
    suspend fun guardarHabitos(atletaId: String, habitos: Habitos): Boolean = try {
        val nuevoId = Uuid.random().toString()
        val habitosConFecha = habitos.copy(id = nuevoId, fechaRegistro = getCurrentTimeMillis())
        usersRef.document(atletaId).collection("habitos").document(nuevoId).set(habitosConFecha)
        true
    } catch (e: Exception) { false }

    suspend fun obtenerRutinasActivas(atletaId: String): List<RutinaAsignada> = try {
        val snapshot = usersRef.document(atletaId).collection("rutinas_asignadas")
            .where("estaActiva", equalTo = true)
            .get()

        snapshot.documents.map { doc -> doc.data<RutinaAsignada>().copy(id = doc.id) }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    suspend fun obtenerRutinaAsignada(atletaId: String, rutinaId: String): RutinaAsignada? = try {
        val doc = usersRef.document(atletaId).collection("rutinas_asignadas").document(rutinaId).get()
        if (doc.exists) doc.data<RutinaAsignada>().copy(id = doc.id) else null
    } catch (e: Exception) { null }

    suspend fun actualizarRutinaAsignada(atletaId: String, rutina: RutinaAsignada): Boolean = try {
        usersRef.document(atletaId).collection("rutinas_asignadas").document(rutina.id).set(rutina)
        true
    } catch (e: Exception) { false }

    // 🚀 CORRECCIÓN CRÍTICA: Reemplazado .add() por .set() con Uuid nativo.
    // Evita que la app se rompa silenciosamente al invocar rutinas con IDs vacíos.
    suspend fun asignarRutina(atletaId: String, rutina: RutinaAsignada): Boolean = try {
        val nuevoId = Uuid.random().toString()
        val rutinaConId = rutina.copy(id = nuevoId)
        usersRef.document(atletaId).collection("rutinas_asignadas").document(nuevoId).set(rutinaConId)
        true
    } catch (e: Exception) {
        println("🔥 Error en asignarRutina: ${e.message}")
        false
    }

    suspend fun eliminarRutinaAsignada(atletaId: String, rutinaId: String): Boolean = try {
        usersRef.document(atletaId).collection("rutinas_asignadas").document(rutinaId).delete()
        true
    } catch (e: Exception) { false }

    fun obtenerIdEntrenadorActual(): String? {
        return auth.currentUser?.uid
    }

    // AtletaRepository.kt
    suspend fun crearAtletaCompleto(
        usuario: Usuario, valoracion: ValoracionFisica, habitos: Habitos, contrasenaTemporal: String, primerPeriodo: PeriodoFacturable
    ): Boolean {
        val snapshotCorreo = db.collection("users").where("correo", equalTo = usuario.correo).get()
        val snapshotDoc = db.collection("users").where("numeroDocumento", equalTo = usuario.numeroDocumento).where("rol", equalTo = "ATLETA").get()

        if (snapshotCorreo.documents.isNotEmpty()) throw Exception("El correo ya se encuentra registrado en Firestore.")
        if (snapshotDoc.documents.isNotEmpty()) throw Exception("El documento ya se encuentra registrado en Firestore.")

        val authResult = auth.createUserWithEmailAndPassword(usuario.correo, contrasenaTemporal)
        val authUid = authResult.user?.uid ?: throw Exception("Fallo al obtener credenciales de autenticación")

        val nuevoRef = usersRef.document(authUid)
        val ahoraMilis = getCurrentTimeMillis()

        val idUnicoCompartido = Uuid.random().toString()
        val valId = Uuid.random().toString()
        val habId = Uuid.random().toString()

        val periodoRef = nuevoRef.collection("periodos_facturables").document(idUnicoCompartido)
        val registroContableRef = db.collection("historial_facturacion_general").document(idUnicoCompartido)

        val batch = db.batch()

        batch.set(nuevoRef, usuario.copy(id = authUid, authId = authUid, rol = RolUsuario.ATLETA))

        val valRef = nuevoRef.collection("valoraciones").document(valId)
        batch.set(valRef, valoracion.copy(id = valId, fechaRegistro = ahoraMilis))

        val habRef = nuevoRef.collection("habitos").document(habId)
        batch.set(habRef, habitos.copy(id = habId, fechaRegistro = ahoraMilis))

        batch.set(periodoRef, primerPeriodo.copy(id = idUnicoCompartido, atletaId = authUid, entrenadorId = usuario.entrenadorId ?: ""))

        val reciboContableInicial = mapOf(
            "id" to idUnicoCompartido, "entrenadorId" to (usuario.entrenadorId ?: ""), "atletaId" to authUid,
            "atletaNombreSnapshot" to "${usuario.nombres} ${usuario.apellidos}".trim(), "tipoPlan" to primerPeriodo.tipoPlan,
            "fechaInicio" to primerPeriodo.fechaInicio, "fechaFin" to primerPeriodo.fechaFin, "fechaRegistroTransaccion" to ahoraMilis, "estado" to primerPeriodo.estado.name
        )
        batch.set(registroContableRef, reciboContableInicial)

        batch.commit()
        return true
    }
}