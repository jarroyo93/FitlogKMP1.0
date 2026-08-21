package dev.josearroyo.fitlog.ui.util

expect object UserPreferencesManager {
    fun guardarUltimoCorreo(correo: String)
    fun obtenerUltimoCorreo(): String
}