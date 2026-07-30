package dev.josearroyo.fitlog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.ui.login.LoginScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = Modifier.fillMaxSize()
    ) {
        // ============================================================
        // 🔑 PANTALLA DE LOGIN
        // ============================================================
        composable("login") {
            LoginScreen(
                onLoginSuccess = { uid, rol, requiereCambioContrasena ->
                    if (requiereCambioContrasena) {
                        navController.navigate("cambiar_password/$uid") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        val destination = when (rol) {
                            RolUsuario.ENTRENADOR -> "dashboard_entrenador/$uid"
                            else -> "dashboard_atleta/$uid"
                        }
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ============================================================
        // 🧪 DESTINOS TEMPORALES DE PRUEBA (PLACEHOLDERS)
        // ============================================================
        composable(
            route = "dashboard_entrenador/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            // 🟢 Usamos savedStateHandle para KMP
            val uid = backStackEntry.savedStateHandle.get<String>("uid") ?: ""
            PantallaPrueba(mensaje = "¡Login Exitoso!\nRol: ENTRENADOR\nUID: $uid")
        }

        composable(
            route = "dashboard_atleta/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.savedStateHandle.get<String>("uid") ?: ""
            PantallaPrueba(mensaje = "¡Login Exitoso!\nRol: ATLETA\nUID: $uid")
        }

        composable(
            route = "cambiar_password/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.savedStateHandle.get<String>("uid") ?: ""
            PantallaPrueba(mensaje = "Primer ingreso detectado\nUID: $uid\n(Requiere cambiar contraseña)")
        }
    }
}

@Composable
private fun PantallaPrueba(mensaje: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF241B3C)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mensaje,
            color = Color(0xFFFF9F6D),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}