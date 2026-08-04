package dev.josearroyo.fitlog.ui.util

import android.content.Context
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

actual object BorradorLocalManager {
    private const val PREFS_NAME = "fitlog_borradores_cache"
    private const val KEY_BORRADOR = "borrador_sesion_activa"

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun guardarBorradorLocal(sesion: SesionEntrenamiento) {
        val context = appContext ?: run {
            println("🔥 BorradorLocalManager: ERROR - Context no ha sido inicializado en Android.")
            return
        }
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Json.encodeToString(sesion)
            // 🟢 Usamos .commit() en lugar de .apply() para garantizar la escritura síncrona en disco
            prefs.edit().putString(KEY_BORRADOR, json).commit()
        } catch (e: Exception) {
            println("🔥 BorradorLocalManager: Error al guardar borrador: ${e.message}")
        }
    }

    actual fun obtenerBorradorLocal(): SesionEntrenamiento? {
        val context = appContext ?: return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BORRADOR, null) ?: return null
        return try {
            Json.decodeFromString<SesionEntrenamiento>(json)
        } catch (e: Exception) {
            println("🔥 BorradorLocalManager: Error al leer borrador: ${e.message}")
            null
        }
    }

    actual fun eliminarBorradorLocal() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BORRADOR).commit()
    }
}