package dev.josearroyo.fitlog.ui.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private var appContext: Context? = null

fun inicializarNotificador(context: Context) {
    appContext = context.applicationContext

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        // 🟢 CANAL V2: Forzamos a Android a recrear el canal con soporte de Banner/Heads-Up
        val channel = NotificationChannel(
            "descanso_timer_channel_v2",
            "Temporizador de Descanso",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de fin de descanso en FitLog"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            setSound(soundUri, audioAttributes)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@SuppressLint("MissingPermission")
actual fun vibrarDispositivo() {
    val context = appContext ?: return
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@SuppressLint("ScheduleExactAlarm")
actual fun programarNotificacionTimer(segundos: Int) {
    val context = appContext ?: return
    if (segundos <= 0) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TimerReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val triggerAtMillis = System.currentTimeMillis() + (segundos * 1000L)

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}

actual fun cancelarNotificacionTimer() {
    val context = appContext ?: return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, TimerReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(1001)
}