package dev.josearroyo.fitlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    // --- RUTAS DEL ENTRENADOR ---
    data object Atletas : BottomNavItem("tab_atletas", Icons.Default.Person, "Atletas")
    data object Biblioteca : BottomNavItem("tab_biblioteca", Icons.Default.List, "Biblioteca")
    data object Facturacion : BottomNavItem("tab_facturacion", Icons.Default.ShoppingCart, "Facturación")
    data object Perfil : BottomNavItem("tab_perfil", Icons.Default.Build, "Perfil")

    // --- RUTAS DEL ATLETA ---
    data object AtletaInicio : BottomNavItem("tab_atleta_inicio", Icons.Default.Home, "Inicio")
    data object AtletaRutinas : BottomNavItem("tab_atleta_rutinas", Icons.Default.CheckCircle, "Rutinas")
    data object AtletaProgreso : BottomNavItem("tab_atleta_progreso", Icons.Default.Star, "Progreso")
    data object AtletaPerfil : BottomNavItem("tab_atleta_perfil", Icons.Default.Person, "Perfil")
}