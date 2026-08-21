package dev.josearroyo.fitlog.ui.util

import android.content.Context
import android.content.SharedPreferences

actual object UserPreferencesManager {
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("fitlog_user_prefs", Context.MODE_PRIVATE)
    }

    actual fun guardarUltimoCorreo(correo: String) {
        prefs?.edit()?.putString("ultimo_correo", correo.trim())?.apply()
    }

    actual fun obtenerUltimoCorreo(): String {
        return prefs?.getString("ultimo_correo", "") ?: ""
    }
}