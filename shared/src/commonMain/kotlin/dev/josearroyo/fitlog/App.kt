package dev.josearroyo.fitlog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.josearroyo.fitlog.data.remoteconfig.RemoteConfigManager
import dev.josearroyo.fitlog.ui.navigation.AppNavigation

@Composable
fun App() {
    // 🔄 Descarga e inicializa los parámetros en la memoria caché en segundo plano
    LaunchedEffect(Unit) {
        RemoteConfigManager.inicializar()
    }

    MaterialTheme {
        // 🚀 Invocamos el módulo de rutas aislado
        AppNavigation()
    }
}