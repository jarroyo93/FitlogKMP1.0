package dev.josearroyo.fitlog

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.UIKit.UIDevice
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeIntervalSince1970

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun calcularFechaCierreCiclo(inicioMilis: Long): Long {
    val calendar = NSCalendar.currentCalendar
    val date = NSDate.dateWithTimeIntervalSince1970(inicioMilis / 1000.0)
    val datePlusSeven = calendar.dateByAddingUnit(
        NSCalendarUnitDay,
        value = 7,
        toDate = date,
        options = 0UL
    ) ?: date
    return calendar.dateBySettingHour(23, minute = 59, second = 59, ofDate = datePlusSeven, options = 0UL)
        ?.timeIntervalSince1970?.times(1000)?.toLong() ?: (inicioMilis + 604800000L)
}

// 🔥 Nueva implementación para iOS:
actual fun calcularFechaFinSuscripcion(inicioMilis: Long, dias: Int): Long {
    val calendar = NSCalendar.currentCalendar
    val date = NSDate.dateWithTimeIntervalSince1970(inicioMilis / 1000.0)
    val datePlusDays = calendar.dateByAddingUnit(
        NSCalendarUnitDay,
        value = dias.toLong(),
        toDate = date,
        options = 0UL
    ) ?: date
    return calendar.dateBySettingHour(23, minute = 59, second = 59, ofDate = datePlusDays, options = 0UL)
        ?.timeIntervalSince1970?.times(1000)?.toLong() ?: (inicioMilis + (dias * 86400000L))
}

actual fun esMismoDia(timestamp1: Long, timestamp2: Long): Boolean {
    val calendar = NSCalendar.currentCalendar
    val date1 = NSDate.dateWithTimeIntervalSince1970(timestamp1 / 1000.0)
    val date2 = NSDate.dateWithTimeIntervalSince1970(timestamp2 / 1000.0)

    val comp1 = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date1)
    val comp2 = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = date2)

    return comp1.year == comp2.year && comp1.month == comp2.month && comp1.day == comp2.day
}

actual fun formatearHora(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "hh:mm a"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date)
}

actual fun formatearFechaHora(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd/MM/yyyy hh:mm a"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date)
}

// 🔥 Nueva implementación para iOS:
actual fun formatearFechaCorto(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd/MM/yyyy"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date)
}

actual fun esCumpleanosHoy(fechaNacimiento: Long): Boolean {
    val calendar = NSCalendar.currentCalendar
    val hoy = NSDate()
    val nac = NSDate.dateWithTimeIntervalSince1970(fechaNacimiento / 1000.0)

    val compHoy = calendar.components(NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = hoy)
    val compNac = calendar.components(NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = nac)

    return compHoy.month == compNac.month && compHoy.day == compNac.day
}

actual fun formatearFechaHistorial(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd 'de' MMMM, yyyy"
        locale = NSLocale.localeWithLocaleIdentifier("es_ES")
    }
    return formatter.stringFromDate(date)
}

actual fun formatearFechaDiario(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "EEEE, dd MMMM yyyy - HH:mm"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date).replaceFirstChar { it.uppercase() }
}

actual fun formatearFechaMesCorto(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd MMM yyyy"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date)
}

actual fun obtenerLetraDiaSemana(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "E"
        locale = NSLocale(localeIdentifier = "es_ES")
    }
    return formatter.stringFromDate(date).uppercase().take(1)
}

actual fun esMesActual(timestamp: Long): Boolean {
    val calendar = NSCalendar.currentCalendar
    val dateSesion = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val dateHoy = NSDate()

    val compSesion = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = dateSesion)
    val compHoy = calendar.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = dateHoy)

    return compSesion.year == compHoy.year && compSesion.month == compHoy.month
}

actual fun obtenerUltimos7DiasTimestamps(): List<Long> {
    val calendar = NSCalendar.currentCalendar
    val list = mutableListOf<Long>()
    for (i in 6 downTo 0) {
        val date = calendar.dateByAddingUnit(
            NSCalendarUnitDay,
            value = -i.toLong(),
            toDate = NSDate(),
            options = 0UL
        ) ?: NSDate()
        list.add((date.timeIntervalSince1970 * 1000).toLong())
    }
    return list
}