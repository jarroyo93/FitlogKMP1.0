package dev.josearroyo.fitlog.ui.util

import platform.Foundation.NSUserDefaults

actual object UserPreferencesManager {
    private const val KEY_ULTIMO_CORREO = "ultimo_correo"

    actual fun guardarUltimoCorreo(correo: String) {
        NSUserDefaults.standardUserDefaults.setObject(correo.trim(), forKey = KEY_ULTIMO_CORREO)
    }

    actual fun obtenerUltimoCorreo(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_ULTIMO_CORREO) ?: ""
    }
}