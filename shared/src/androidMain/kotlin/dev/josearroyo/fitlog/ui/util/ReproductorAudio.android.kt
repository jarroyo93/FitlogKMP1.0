package dev.josearroyo.fitlog.ui.util

import android.content.Context
import android.media.MediaPlayer

actual object ReproductorAudio {
    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null

    fun inicializar(context: Context) {
        appContext = context.applicationContext
    }

    actual fun reproducirSonidoFinTiempo() {
        val context = appContext ?: return
        detenerSonido() // Detiene cualquier reproducción previa
        try {
            val resourceId = context.resources.getIdentifier("alarma_descanso", "raw", context.packageName)
            if (resourceId != 0) {
                mediaPlayer = MediaPlayer.create(context, resourceId).apply {
                    setOnCompletionListener {
                        release()
                        mediaPlayer = null
                    }
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun detenerSonido() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
}