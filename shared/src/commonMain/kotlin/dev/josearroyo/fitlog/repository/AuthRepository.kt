package dev.josearroyo.fitlog.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

class AuthRepository {
    private val auth = Firebase.auth

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun login(email: String, clave: String): String {
        val result = auth.signInWithEmailAndPassword(email, clave)
        return result.user?.uid ?: throw Exception("Error al obtener el UID de Firebase")
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun cambiarContrasenaPrimeraVez(nuevaContrasena: String): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No hay una sesión activa."))
            user.updatePassword(nuevaContrasena)
            Result.success(true)
        } catch (e: Exception) {
            println("🔥 [AuthRepository] Error en cambiarContrasenaPrimeraVez: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}