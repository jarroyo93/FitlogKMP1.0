package dev.josearroyo.fitlog.ui.util

import platform.Foundation.NSUserDefaults
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

actual object BorradorLocalManager {
    private const val KEY_BORRADOR = "borrador_sesion_activa"

    actual fun guardarBorradorLocal(sesion: SesionEntrenamiento) {
        val json = Json.encodeToString(sesion)
        NSUserDefaults.standardUserDefaults.setObject(json, forKey = KEY_BORRADOR)
    }

    actual fun obtenerBorradorLocal(): SesionEntrenamiento? {
        val json = NSUserDefaults.standardUserDefaults.stringForKey(KEY_BORRADOR) ?: return null
        return try {
            Json.decodeFromString<SesionEntrenamiento>(json)
        } catch (e: Exception) {
            null
        }
    }

    actual fun eliminarBorradorLocal() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_BORRADOR)
    }
}