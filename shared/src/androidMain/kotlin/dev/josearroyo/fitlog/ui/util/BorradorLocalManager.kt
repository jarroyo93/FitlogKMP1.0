package dev.josearroyo.fitlog.ui.util

import android.content.Context
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

actual object BorradorLocalManager {
    private const val PREFS_NAME = "fitlog_borradores_cache"
    private const val KEY_BORRADOR = "borrador_sesion_activa"

    private var appContext: Context? = null

    // Se llama una sola vez al iniciar la App en Android
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun guardarBorradorLocal(sesion: SesionEntrenamiento) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Json.encodeToString(sesion)
        prefs.edit().putString(KEY_BORRADOR, json).apply()
    }

    actual fun obtenerBorradorLocal(): SesionEntrenamiento? {
        val context = appContext ?: return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BORRADOR, null) ?: return null
        return try {
            Json.decodeFromString<SesionEntrenamiento>(json)
        } catch (e: Exception) {
            null
        }
    }

    actual fun eliminarBorradorLocal() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BORRADOR).apply()
    }
}