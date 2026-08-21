package dev.josearroyo.fitlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import dev.josearroyo.fitlog.ui.util.BorradorLocalManager
import dev.josearroyo.fitlog.ui.util.ReproductorAudio
import dev.josearroyo.fitlog.ui.util.UserPreferencesManager
import dev.josearroyo.fitlog.ui.util.inicializarNotificador

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ReproductorAudio.inicializar(this)
        inicializarNotificador(this)
        FirebaseApp.initializeApp(this)

        // 🟢 Solicitar permiso de notificaciones dinámico en Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Inicializamos el contexto en Android antes de renderizar la UI
        BorradorLocalManager.initialize(this)
        UserPreferencesManager.initialize(this)
        setContent {
            App()
        }
    }
}