package dev.josearroyo.fitlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import dev.josearroyo.fitlog.ui.util.BorradorLocalManager // 🟢 Importación agregada

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        // 🟢 Inicializamos el contexto en Android antes de renderizar la UI
        BorradorLocalManager.initialize(this)

        setContent {
            App()
        }
    }
}