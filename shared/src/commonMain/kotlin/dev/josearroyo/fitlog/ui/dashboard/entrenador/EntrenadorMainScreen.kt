package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.ui.navigation.BottomNavItem
import dev.josearroyo.fitlog.viewmodel.FacturacionViewModel
import dev.josearroyo.fitlog.viewmodel.entrenador.PerfilEntrenadorViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import dev.josearroyo.fitlog.ui.dashboard.entrenador.BibliotecaScreen
import dev.josearroyo.fitlog.ui.dashboard.FacturacionScreen

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@Composable
fun EntrenadorMainScreen(
    uid: String,
    onNavigateToAddAtleta: (String) -> Unit,
    onNavigateToAtletaDetail: (String) -> Unit,
    onNavigateToAddExercise: (String) -> Unit,
    onNavigateToAddPlantilla: (String) -> Unit,
    onNavigateToEditExercise: (String, String) -> Unit,
    onNavigateToEditPlantilla: (String, String) -> Unit,
    onNavigateToEditarDatosPersonales: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToHistorialFacturacion: (String, String) -> Unit,
    onNavigateToInformeGlobalFacturacion: (String) -> Unit
) {
    val bottomNavController = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A132B))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FitLog",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Atletas.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Atletas.route) {
                    EntrenadorDashboardScreen(
                        entrenadorId = uid,
                        onAtletaClick = { atletaId ->
                            onNavigateToAtletaDetail(atletaId)
                        },
                        onAddAtletaClick = {
                            onNavigateToAddAtleta(uid)
                        }
                    )
                }

                composable(BottomNavItem.Biblioteca.route) {
                    BibliotecaScreen(
                        entrenadorId = uid,
                        onNavigateToAddEjercicio = { entId -> onNavigateToAddExercise(entId) },
                        onNavigateToEditEjercicio = { entId, ejId -> onNavigateToEditExercise(entId, ejId) },
                        onNavigateToAddPlantilla = { entId -> onNavigateToAddPlantilla(entId) },
                        onNavigateToEditPlantilla = { entId, planId -> onNavigateToEditPlantilla(entId, planId) }
                    )
                }

                // 🚀 PESTAÑA 3: Control General de Facturación y Cobros (CORREGIDO PARA KMP)
                composable(BottomNavItem.Facturacion.route) {
                    val facturacionVM: FacturacionViewModel = viewModel { FacturacionViewModel() }

                    FacturacionScreen(
                        entrenadorId = uid,
                        viewModel = facturacionVM,
                        onNavigateToHistorial = { atletaId ->
                            onNavigateToHistorialFacturacion(atletaId, uid)
                        },
                        onNavigateToInformeGlobal = {
                            onNavigateToInformeGlobalFacturacion(uid)
                        }
                    )
                }

                composable(BottomNavItem.Perfil.route) {
                    PerfilEntrenadorTab(
                        uid = uid,
                        onLogout = onLogout,
                        onEditDatosPersonalesClick = { onNavigateToEditarDatosPersonales(uid) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FondoTarjeta)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            EntrenadorBottomNavigationBar(bottomNavController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerfilEntrenadorTab(
    uid: String,
    onLogout: () -> Unit,
    onEditDatosPersonalesClick: () -> Unit
) {
    val authRepository = remember { AuthRepository() }
    val perfilViewModel: PerfilEntrenadorViewModel = viewModel { PerfilEntrenadorViewModel() }
    val uiState by perfilViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var especialidad by remember { mutableStateOf("") }
    var biografia by remember { mutableStateOf("") }
    var nuevaCertificacion by remember { mutableStateOf("") }
    val certificaciones = remember { mutableStateListOf<String>() }

    LaunchedEffect(uid) { perfilViewModel.cargarPerfil(uid) }

    LaunchedEffect(uiState.usuarioLogueado) {
        uiState.usuarioLogueado?.let { prof ->
            especialidad = prof.especialidad ?: ""
            biografia = prof.biografia ?: ""
            certificaciones.clear()
            certificaciones.addAll(prof.certificaciones)
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NaranjaAcento)
        }
        return
    }

    val entrenador = uiState.usuarioLogueado ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(modifier = Modifier.size(90.dp).background(FondoTarjeta, CircleShape).border(2.dp, NaranjaAcento, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ContactPage, contentDescription = null, modifier = Modifier.size(45.dp), tint = NaranjaAcento)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${entrenador.nombres} ${entrenador.apellidos}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Entrenador Principal", color = NaranjaAcento, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onEditDatosPersonalesClick) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Editar Datos Personales", color = NaranjaAcento, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = FondoTarjeta, thickness = 1.dp)

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Perfil Comercial", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                CampoEditableEntrenador("Enfoque o Especialidad", especialidad) { especialidad = it }
                OutlinedTextField(
                    value = biografia,
                    onValueChange = { biografia = it },
                    label = { Text("Biografía o Descripción", color = NaranjaAcento) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                        focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro
                    )
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Certificaciones y Títulos", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    certificaciones.forEach { cert ->
                        InputChip(
                            selected = true,
                            onClick = { },
                            label = { Text(cert, color = Color.White, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(onClick = { certificaciones.remove(cert) }, modifier = Modifier.size(16.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = NaranjaAcento)
                                }
                            },
                            colors = InputChipDefaults.inputChipColors(containerColor = FondoOscuro, selectedContainerColor = FondoOscuro)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nuevaCertificacion, onValueChange = { nuevaCertificacion = it },
                        label = { Text("Añadir logro académico", color = TextoSecundario) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                            focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nuevaCertificacion.isNotBlank()) {
                                certificaciones.add(nuevaCertificacion.trim())
                                nuevaCertificacion = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                }
            }
        }

        if (uiState.exitoGuardado) {
            Text("✔ ¡Tus datos se actualizaron correctamente!", color = Color(0xFF81C784), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = { perfilViewModel.guardarPerfilEntrenador(uid, especialidad, biografia, certificaciones.toList()) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isSaving
        ) {
            if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FondoOscuro)
            else Text("Guardar Perfil Profesional", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            coroutineScope.launch {
                authRepository.logout()
                onLogout()
            }
        }) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TextoSecundario)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cerrar Sesión", color = TextoSecundario, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun EntrenadorBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Atletas,
        BottomNavItem.Biblioteca,
        BottomNavItem.Facturacion,
        BottomNavItem.Perfil
    )

    NavigationBar(
        containerColor = Color.Transparent,
        windowInsets = WindowInsets(0.dp)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(text = item.title, fontWeight = FontWeight.Bold) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FondoOscuro,
                    selectedTextColor = NaranjaAcento,
                    indicatorColor = NaranjaAcento,
                    unselectedIconColor = TextoSecundario,
                    unselectedTextColor = TextoSecundario
                ),
                onClick = {
                    val startRoute = navController.graph.findStartDestination().route

                    navController.navigate(item.route) {
                        if (startRoute != null) {
                            popUpTo(startRoute) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun CampoEditableEntrenador(label: String, valor: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(label, color = NaranjaAcento) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
            focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro
        )
    )
}