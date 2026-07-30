package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

class AuthRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // 🟢 OPTIMIZADO: Eliminado bloque try-catch redundante. GitLive ya propaga la excepción directamente.
    suspend fun login(email: String, clave: String): String {
        val result = auth.signInWithEmailAndPassword(email, clave)
        return result.user?.uid ?: throw Exception("Error al obtener el UID de Firebase")
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun cambiarContrasenaPrimeraVez(uid: String, nuevaContrasena: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No hay una sesión activa."))

            user.updatePassword(nuevaContrasena)

            db.collection("users").document(uid)
                .update("requiereCambioContrasena" to false)

            Result.success(true)
        } catch (e: Exception) {
            // 🚀 Reporte visible si las reglas de Firestore o las políticas de Auth bloquean el cambio
            println("🔥 [AuthRepository] Error en cambiarContrasenaPrimeraVez: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}