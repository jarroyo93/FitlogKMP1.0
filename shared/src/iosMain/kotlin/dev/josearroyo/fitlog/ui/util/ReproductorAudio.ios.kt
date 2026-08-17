package dev.josearroyo.fitlog.ui.util

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

actual object ReproductorAudio {
    private var audioPlayer: AVAudioPlayer? = null

    actual fun reproducirSonidoFinTiempo() {
        detenerSonido()
        try {
            val path = NSBundle.mainBundle.pathForResource("alarma_descanso", "wav")
            if (path != null) {
                val url = NSURL.fileURLWithPath(path)
                audioPlayer = AVAudioPlayer(contentsOfURL = url, error = null).apply {
                    prepareToPlay()
                    play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun detenerSonido() {
        try {
            audioPlayer?.let {
                if (it.isPlaying()) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioPlayer = null
        }
    }
}