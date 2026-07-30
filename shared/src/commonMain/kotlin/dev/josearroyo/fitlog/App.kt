package dev.josearroyo.fitlog

import androidx.compose.runtime.Composable
//import dev.josearroyo.fitlog.ui.navigation.AppNavigation
import androidx.compose.material3.MaterialTheme
import dev.josearroyo.fitlog.ui.navigation.AppNavigation

@Composable
fun App() {
    MaterialTheme {
        // 🚀 Invocamos el módulo de rutas aislado
        AppNavigation()
    }
}