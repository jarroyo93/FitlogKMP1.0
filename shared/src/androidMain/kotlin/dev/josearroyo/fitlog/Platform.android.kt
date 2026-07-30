package dev.josearroyo.fitlog

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun calcularFechaCierreCiclo(inicioMilis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = inicioMilis
        add(Calendar.DAY_OF_YEAR, 7)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calendar.timeInMillis
}

// 🔥 Nueva implementación para Android:
actual fun calcularFechaFinSuscripcion(inicioMilis: Long, dias: Int): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = inicioMilis
        add(Calendar.DAY_OF_YEAR, dias)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calendar.timeInMillis
}

actual fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

actual fun formatearHora(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return SimpleDateFormat("hh:mm a", Locale("es", "ES")).format(date)
}

actual fun formatearFechaHora(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "ES")).format(date)
}

// 🔥 Nueva implementación para Android:
actual fun formatearFechaCorto(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(date)
}

actual fun esCumpleanosHoy(fechaNacimiento: Long): Boolean {
    val calHoy = Calendar.getInstance()
    val calNac = Calendar.getInstance().apply { timeInMillis = fechaNacimiento }
    return calHoy.get(Calendar.MONTH) == calNac.get(Calendar.MONTH) &&
            calHoy.get(Calendar.DAY_OF_MONTH) == calNac.get(Calendar.DAY_OF_MONTH)
}

actual fun formatearFechaHistorial(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
    return sdf.format(date)
}

actual fun formatearFechaDiario(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm", Locale("es", "ES"))
    return sdf.format(date).replaceFirstChar { it.uppercase() }
}

actual fun formatearFechaMesCorto(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    return sdf.format(date)
}

actual fun obtenerLetraDiaSemana(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = SimpleDateFormat("E", Locale("es", "ES"))
    return sdf.format(date).uppercase().take(1)
}

actual fun esMesActual(timestamp: Long): Boolean {
    val calSesion = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calHoy = Calendar.getInstance()
    return calSesion.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
            calSesion.get(Calendar.MONTH) == calHoy.get(Calendar.MONTH)
}

actual fun obtenerUltimos7DiasTimestamps(): List<Long> {
    val list = mutableListOf<Long>()
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 12) // Mediodía para evitar problemas de huso horario
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        list.add(cal.timeInMillis)
    }
    return list
}