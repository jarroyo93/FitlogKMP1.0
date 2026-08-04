package dev.josearroyo.fitlog.ui.util

import dev.josearroyo.fitlog.data.model.SesionEntrenamiento

expect object BorradorLocalManager {
    fun guardarBorradorLocal(sesion: SesionEntrenamiento)
    fun obtenerBorradorLocal(): SesionEntrenamiento?
    fun eliminarBorradorLocal()
}