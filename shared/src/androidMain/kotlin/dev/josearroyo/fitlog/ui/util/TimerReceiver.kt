package dev.josearroyo.fitlog.ui.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

class TimerReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        // 🟢 Usar FLAG_ACTIVITY_SINGLE_TOP para traer la app al frente sin volver a recrear la UI
        val intentApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = intentApp?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val appIcon = context.applicationInfo.icon.takeIf { it != 0 }
            ?: android.R.drawable.ic_dialog_info

        val notificationBuilder = NotificationCompat.Builder(context, "descanso_timer_channel_v2")
            .setSmallIcon(appIcon)
            .setContentTitle("FitLog")
            .setContentText("¡Tiempo de descanso terminado!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, notificationBuilder.build())
    }
}