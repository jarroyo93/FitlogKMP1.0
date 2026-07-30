package dev.josearroyo.fitlog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// Modelos y Repositorios
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.repository.AtletaRepository
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.repository.UserRepository

// 🏛️ IMPORTS UNIFICADOS DE ENTRENADOR
import dev.josearroyo.fitlog.ui.dashboard.entrenador.AddAtletaScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.AddEjercicioScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.AddPlantillaScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.BibliotecaScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.HistorialFacturacionScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.InformeFacturacionGlobalScreen
import dev.josearroyo.fitlog.ui.dashboard.entrenador.SeleccionarPlantillaScreen
import dev.josearroyo.fitlog.ui.entrenador.AddValoracionScreen
import dev.josearroyo.fitlog.ui.entrenador.EntrenadorMainScreen

// 🔑 IMPORTS DE AUTH, SPLASH Y PERFIL
import dev.josearroyo.fitlog.ui.login.CambiarContrasenaScreen
import dev.josearroyo.fitlog.ui.login.LoginScreen
import dev.josearroyo.fitlog.ui.profile.EditarDatosPersonalesScreen
import dev.josearroyo.fitlog.ui.splash.SplashScreen
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaViewModel
import dev.josearroyo.fitlog.viewmodel.entrenador.PerfilEntrenadorViewModel

/**
 * Extensión de seguridad para evitar que toques múltiples rápidos
 * vacíen la pila de navegación y provoquen pantalla en blanco.
 */
fun NavController.safePopBackStack() {
    if (previousBackStackEntry != null &&
        currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED
    ) {
        popBackStack()
    }
}

/**
 * Componente temporal visual para rutas que aún no han sido migradas.
 */
@Composable
private fun AtletaPlaceholderScreen(
    titulo: String,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1B2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "🚧 $titulo",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta pantalla del módulo de Atleta se migrará en la siguiente etapa.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            if (onBack != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Regresar")
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = Modifier.fillMaxSize()
    ) {
        // ============================================================
        // 🔑 AUTENTICACIÓN Y SPLASH
        // ============================================================
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

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

        composable(
            route = "cambiar_password/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid: String = backStackEntry.savedStateHandle["uid"] ?: ""
            CambiarContrasenaScreen(
                uid = uid,
                onPasswordChangedSuccess = {
                    navController.navigate("dashboard_atleta/$uid") {
                        popUpTo("cambiar_password/$uid") { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ============================================================
        // 🏛️ PANEL DEL ENTRENADOR (DASHBOARD CO-CENTRAL)
        // ============================================================
        composable(
            route = "dashboard_entrenador/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid: String = backStackEntry.savedStateHandle["uid"] ?: ""
            EntrenadorMainScreen(
                uid = uid,
                onNavigateToAtletaDetail = { atletaId ->
                    navController.navigate("atleta_detail/$atletaId")
                },
                onNavigateToAddExercise = { id ->
                    navController.navigate("add_ejercicio/$id")
                },
                onNavigateToAddPlantilla = { id ->
                    navController.navigate("add_plantilla/$id")
                },
                onNavigateToEditExercise = { idEnt, idEj ->
                    navController.navigate("edit_ejercicio/$idEnt/$idEj")
                },
                onNavigateToEditPlantilla = { idEnt, idPlan ->
                    navController.navigate("edit_plantilla/$idEnt/$idPlan")
                },
                onNavigateToAddAtleta = {
                    navController.navigate("agregar_atleta")
                },
                onNavigateToEditarDatosPersonales = { entrenadorId ->
                    navController.navigate("editar_datos_personales/$entrenadorId")
                },
                onNavigateToHistorialFacturacion = { atletaId, entrenadorId ->
                    navController.navigate("historial_facturacion/$atletaId/$entrenadorId")
                },
                onNavigateToInformeGlobalFacturacion = { entrenadorId ->
                    navController.navigate("informe_global_facturacion/$entrenadorId")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ============================================================
        // 📊 MÓDULO DE FACTURACIÓN Y CONTABILIDAD KMP
        // ============================================================
        composable(
            route = "historial_facturacion/{atletaId}/{entrenadorId}",
            arguments = listOf(
                navArgument("atletaId") { type = NavType.StringType },
                navArgument("entrenadorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val atletaId: String = backStackEntry.savedStateHandle["atletaId"] ?: ""
            val entrenadorId: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            HistorialFacturacionScreen(
                atletaId = atletaId,
                entrenadorId = entrenadorId,
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "informe_global_facturacion/{entrenadorId}",
            arguments = listOf(navArgument("entrenadorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entrenadorId: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            InformeFacturacionGlobalScreen(
                entrenadorId = entrenadorId,
                onBack = { navController.safePopBackStack() }
            )
        }

        // ============================================================
        // ➕ CREACIÓN MANUAL DE ATLETA
        // ============================================================
        composable(route = "agregar_atleta") {
            val addAtletaVM: AddAtletaViewModel = viewModel {
                AddAtletaViewModel(
                    atletaRepository = AtletaRepository(),
                    userRepository = UserRepository(),
                    authRepository = AuthRepository()
                )
            }

            AddAtletaScreen(
                viewModel = addAtletaVM,
                onNavigateBack = { navController.safePopBackStack() }
            )
        }

        // ============================================================
        // 📋 EXPEDIENTE Y GESTIÓN DEL ALUMNO
        // ============================================================
        composable(
            route = "atleta_detail/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Expediente / Detalle del Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }

        // ============================================================
        // 🏋️ PLANIFICACIÓN Y ASIGNACIÓN
        // ============================================================
        composable(
            route = "seleccionar_plantilla/{atletaId}/{entrenadorId}",
            arguments = listOf(
                navArgument("atletaId") { type = NavType.StringType },
                navArgument("entrenadorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val atletaId: String = backStackEntry.savedStateHandle["atletaId"] ?: ""
            val entrenadorId: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            SeleccionarPlantillaScreen(
                atletaId = atletaId,
                entrenadorId = entrenadorId,
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "agregar_valoracion/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val atletaId: String = backStackEntry.savedStateHandle["atletaId"] ?: ""
            AddValoracionScreen(
                atletaId = atletaId,
                onBack = { navController.safePopBackStack() }
            )
        }

        // ============================================================
        // 📚 MÓDULO DE LA BIBLIOTECA (EJERCICIOS Y PLANTILLAS)
        // ============================================================
        composable(
            route = "biblioteca/{entrenadorId}",
            arguments = listOf(navArgument("entrenadorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            BibliotecaScreen(
                entrenadorId = id,
                onNavigateToAddEjercicio = { entId -> navController.navigate("add_ejercicio/$entId") },
                onNavigateToEditEjercicio = { entId, ejId -> navController.navigate("edit_ejercicio/$entId/$ejId") },
                onNavigateToAddPlantilla = { entId -> navController.navigate("add_plantilla/$entId") },
                onNavigateToEditPlantilla = { entId, planId -> navController.navigate("edit_plantilla/$entId/$planId") }
            )
        }

        composable(
            route = "add_ejercicio/{entrenadorId}",
            arguments = listOf(navArgument("entrenadorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            AddEjercicioScreen(
                entrenadorId = id,
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "edit_ejercicio/{entrenadorId}/{ejercicioId}",
            arguments = listOf(
                navArgument("entrenadorId") { type = NavType.StringType },
                navArgument("ejercicioId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entId: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            val ejId: String = backStackEntry.savedStateHandle["ejercicioId"] ?: ""
            AddEjercicioScreen(
                entrenadorId = entId,
                ejercicioId = ejId,
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "add_plantilla/{entrenadorId}",
            arguments = listOf(navArgument("entrenadorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            AddPlantillaScreen(
                entrenadorId = id,
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "edit_plantilla/{entrenadorId}/{plantillaId}",
            arguments = listOf(
                navArgument("entrenadorId") { type = NavType.StringType },
                navArgument("plantillaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entId: String = backStackEntry.savedStateHandle["entrenadorId"] ?: ""
            val planId: String = backStackEntry.savedStateHandle["plantillaId"] ?: ""
            AddPlantillaScreen(
                entrenadorId = entId,
                plantillaId = planId,
                onBack = { navController.safePopBackStack() }
            )
        }

        // ============================================================
        // 👥 RUTA DE DATOS PERSONALES
        // ============================================================
        composable(
            route = "editar_datos_personales/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid: String = backStackEntry.savedStateHandle["uid"] ?: ""
            val entrenadorVM: PerfilEntrenadorViewModel = viewModel()
            val stateEntrenador by entrenadorVM.uiState.collectAsState()

            LaunchedEffect(uid) {
                entrenadorVM.cargarPerfil(uid)
            }

            LaunchedEffect(stateEntrenador.exitoGuardado) {
                if (stateEntrenador.exitoGuardado) {
                    entrenadorVM.resetExito()
                    navController.safePopBackStack()
                }
            }

            when {
                stateEntrenador.usuarioLogueado != null -> {
                    val usuario = stateEntrenador.usuarioLogueado!!
                    EditarDatosPersonalesScreen(
                        usuarioActual = usuario,
                        isSaving = stateEntrenador.isSaving,
                        error = stateEntrenador.error,
                        onBack = { navController.safePopBackStack() },
                        onGuardarCambios = { nom, ape, tDoc, nDoc, tel, _, _, _ ->
                            entrenadorVM.guardarDatosPersonales(
                                uid = uid,
                                nombres = nom,
                                apellidos = ape,
                                tipoDocumento = tDoc,
                                documento = nDoc,
                                telefono = tel
                            )
                        }
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF241B3C)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF9F6D))
                    }
                }
            }
        }

        // ============================================================
        // ⏳ RUTAS DE ATLETA (MENSAJES TEMPORALES / STUBS)
        // ============================================================
        composable(
            route = "dashboard_atleta/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Panel Principal de Atleta",
                onBack = { navController.navigate("login") { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(
            route = "historial_valoracion/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Historial de Valoraciones del Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "historial_habitos/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Historial de Hábitos del Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "agregar_habitos/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Agregar Hábitos al Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "perfil_atleta/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Perfil del Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "edit_rutina_asignada/{atletaId}/{rutinaId}",
            arguments = listOf(
                navArgument("atletaId") { type = NavType.StringType },
                navArgument("rutinaId") { type = NavType.StringType }
            )
        ) {
            AtletaPlaceholderScreen(
                titulo = "Editar Rutina Asignada al Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }

        composable(
            route = "progreso_atleta/{atletaId}",
            arguments = listOf(navArgument("atletaId") { type = NavType.StringType })
        ) {
            AtletaPlaceholderScreen(
                titulo = "Progreso del Atleta",
                onBack = { navController.safePopBackStack() }
            )
        }
    }
}