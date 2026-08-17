package dev.josearroyo.fitlog.ui.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

actual fun vibrarDispositivo() {
    try {
        val generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun programarNotificacionTimer(segundos: Int) {
    if (segundos <= 0) return

    val center = UNUserNotificationCenter.currentNotificationCenter()

    center.requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound
    ) { granted, _ ->
        if (granted) {
            val content = UNMutableNotificationContent().apply {
                setTitle("FitLog")
                setBody("¡Tiempo de descanso terminado!")
                setSound(UNNotificationSound.soundNamed("alarma_descanso.wav"))
            }

            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = segundos.toDouble(),
                repeats = false
            )

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "timer_descanso_fitlog",
                content = content,
                trigger = trigger
            )

            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }
}

actual fun cancelarNotificacionTimer() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.removePendingNotificationRequestsWithIdentifiers(listOf("timer_descanso_fitlog"))
}