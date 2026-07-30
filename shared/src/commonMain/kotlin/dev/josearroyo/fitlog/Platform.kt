package dev.josearroyo.fitlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getCurrentTimeMillis(): Long
expect fun calcularFechaCierreCiclo(inicioMilis: Long): Long
expect fun calcularFechaFinSuscripcion(inicioMilis: Long, dias: Int): Long
expect fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean
expect fun formatearHora(timestamp: Long): String
expect fun formatearFechaHora(timestamp: Long): String
expect fun formatearFechaCorto(timestamp: Long): String
expect fun esCumpleanosHoy(fechaNacimiento: Long): Boolean
expect fun formatearFechaHistorial(timestamp: Long): String

// 🟢 NUEVAS FUNCIONES PARA LA PESTAÑA DE PROGRESO
expect fun formatearFechaDiario(timestamp: Long): String
expect fun formatearFechaMesCorto(timestamp: Long): String
expect fun obtenerLetraDiaSemana(timestamp: Long): String
expect fun esMesActual(timestamp: Long): Boolean
expect fun obtenerUltimos7DiasTimestamps(): List<Long>