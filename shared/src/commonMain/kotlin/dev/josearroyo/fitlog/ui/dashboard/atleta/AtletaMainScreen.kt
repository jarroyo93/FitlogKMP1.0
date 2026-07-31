package dev.josearroyo.fitlog.ui.dashboard.atleta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// 🟢 MODELOS Y REPOSITORIOS KMP DEL PROYECTO
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.repository.UserRepository
import dev.josearroyo.fitlog.ui.navigation.BottomNavItem
import dev.josearroyo.fitlog.viewmodel.atleta.PerfilAtletaViewModel

// 🟢 IMPORTACIONES DE LAS NUEVAS FUNCIONES PLATAFORMA KMP
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.formatearFechaHistorial
import dev.josearroyo.fitlog.ui.dashboard.ProgresoAtletaScreen

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaMainScreen(
    uid: String,
    onLogout: () -> Unit,
    onNavigateToCambiarContrasena: (String) -> Unit,
    onNavigateToEditarDatosPersonales: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavController = rememberNavController()
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    val authRepository = remember { AuthRepository() }

    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isActionLoading by remember { mutableStateOf(false) }
    var showDesvincularDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val esPantallaEntrenar = currentRoute?.startsWith("entrenar") == true

    val recargarEstado = {
        scope.launch {
            isLoading = true
            usuario = userRepository.obtenerUsuario(uid)
            isLoading = false
        }
    }

    LaunchedEffect(usuario) {
        if (usuario?.requiereCambioContrasena == true) {
            onNavigateToCambiarContrasena(usuario!!.id)
        }
    }

    LaunchedEffect(uid) {
        recargarEstado()
    }

    val estadoReal = remember(usuario, isLoading) {
        if (isLoading || usuario == null) {
            EstadoSuscripcion.ACTIVO
        } else {
            val u = usuario!!
            val ahora = getCurrentTimeMillis()
            val vencimiento = u.vencimientoSuscripcion ?: 0L
            val fechaInicio = u.fechaInicioSuscripcion ?: 0L

            when {
                u.estadoSuscripcion == EstadoSuscripcion.HUERFANO -> EstadoSuscripcion.HUERFANO
                u.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO -> EstadoSuscripcion.SUSPENDIDO
                u.estadoSuscripcion == EstadoSuscripcion.VENCIDO || (vencimiento in 1..<ahora) -> EstadoSuscripcion.VENCIDO
                fechaInicio > ahora -> EstadoSuscripcion.VENCIDO
                else -> EstadoSuscripcion.ACTIVO
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(FondoOscuro)) {

        // 1. Cabecera Estática Superior
        if (!esPantallaEntrenar && estadoReal == EstadoSuscripcion.ACTIVO) {
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
        }

        // 2. Contenedor del Cuerpo Central
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = NaranjaAcento
                )
            } else if (usuario != null) {
                when (estadoReal) {
                    EstadoSuscripcion.HUERFANO -> {
                        PantallaHuerfano(
                            isActionLoading = isActionLoading,
                            onIngresarCodigo = { corr, cod ->
                                scope.launch {
                                    isActionLoading = true
                                    if (userRepository.vincularConEntrenador(usuario!!.id, corr, cod)) recargarEstado()
                                    isActionLoading = false
                                }
                            },
                            onLogout = {
                                scope.launch {
                                    authRepository.logout()
                                    onLogout()
                                }
                            }
                        )
                    }
                    EstadoSuscripcion.SUSPENDIDO -> {
                        PantallaRestringida(
                            titulo = "Cuenta Congelada",
                            mensaje = "Tu plan de entrenamiento está pausado. Comunícate con tu coach.",
                            textoBotonPrincipal = "Actualizar Estado",
                            isActionLoading = isActionLoading,
                            onAccionPrincipal = { recargarEstado() },
                            onDesvincularClick = { showDesvincularDialog = true },
                            onLogout = {
                                scope.launch {
                                    authRepository.logout()
                                    onLogout()
                                }
                            }
                        )
                    }
                    EstadoSuscripcion.VENCIDO -> {
                        PantallaRestringida(
                            titulo = "Plan Vencido",
                            mensaje = "Tu ciclo ha terminado. Solicita la renovación a tu entrenador.",
                            textoBotonPrincipal = "Actualizar Estado",
                            isActionLoading = isActionLoading,
                            onAccionPrincipal = { recargarEstado() },
                            onDesvincularClick = { showDesvincularDialog = true },
                            onLogout = {
                                scope.launch {
                                    authRepository.logout()
                                    onLogout()
                                }
                            }
                        )
                    }
                    EstadoSuscripcion.ACTIVO -> {
                        NavHost(
                            navController = bottomNavController,
                            startDestination = BottomNavItem.AtletaInicio.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(BottomNavItem.AtletaInicio.route) {
                                AtletaInicioScreen(
                                    uid = uid,
                                    onNavigateToEntrenar = { rutinaId ->
                                        bottomNavController.navigate("entrenar/$rutinaId")
                                    }
                                )
                            }
                            composable(BottomNavItem.AtletaRutinas.route) {
                                AtletaRutinasScreen(
                                    uid = uid,
                                    onNavigateToEntrenar = { rutinaId ->
                                        bottomNavController.navigate("entrenar/$rutinaId")
                                    }
                                )
                            }
                            composable(BottomNavItem.AtletaProgreso.route) {
                                ProgresoAtletaScreen(userId = uid)
                            }
                            composable(BottomNavItem.AtletaPerfil.route) {
                                PerfilAtletaTab(
                                    uid = uid,
                                    atletaFallback = usuario!!,
                                    onLogout = {
                                        scope.launch {
                                            authRepository.logout()
                                            onLogout()
                                        }
                                    },
                                    onDesvincularClick = { showDesvincularDialog = true },
                                    onEditDatosPersonalesClick = { onNavigateToEditarDatosPersonales(uid) }
                                )
                            }
                            composable("entrenar/{rutinaId}") { backStackEntry ->
                                val rutinaId: String = backStackEntry.savedStateHandle["rutinaId"] ?: ""
                                EntrenarScreen(
                                    atletaId = uid,
                                    rutinaId = rutinaId,
                                    onBack = { bottomNavController.popBackStack() },
                                    onFinish = { bottomNavController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Base Rígida Inferior
        if (!esPantallaEntrenar && estadoReal == EstadoSuscripcion.ACTIVO) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FondoTarjeta)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AtletaBottomNavigationBar(bottomNavController)
            }
        }
    }

    if (showDesvincularDialog && usuario != null) {
        val diasRestantes = when (estadoReal) {
            EstadoSuscripcion.SUSPENDIDO -> ((usuario!!.saldoMilisegundosRestantes ?: 0L) / (1000 * 60 * 60 * 24))
            EstadoSuscripcion.ACTIVO -> {
                val diff = (usuario!!.vencimientoSuscripcion ?: 0L) - getCurrentTimeMillis()
                if (diff > 0) diff / (1000 * 60 * 60 * 24) else 0L
            }
            else -> 0L
        }

        AlertDialog(
            containerColor = FondoTarjeta,
            onDismissRequest = { showDesvincularDialog = false },
            title = { Text("¿Deseas desvincularte?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Si te desvinculas ahora, perderás el acceso a tu entrenador actual.", color = TextoSecundario)
                    if (diasRestantes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("¡Atención! Perderás los $diasRestantes días restantes de tu plan.", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDesvincularDialog = false
                        scope.launch {
                            isActionLoading = true
                            if (userRepository.desvincularAtleta(usuario!!.id)) recargarEstado()
                            isActionLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = FondoOscuro)
                ) { Text("Sí, desvincularme", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDesvincularDialog = false }) { Text("Cancelar", color = NaranjaAcento) } }
        )
    }
}

@Composable
fun AtletaBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.AtletaInicio,
        BottomNavItem.AtletaRutinas,
        BottomNavItem.AtletaProgreso,
        BottomNavItem.AtletaPerfil
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
                    navController.navigate(item.route) {
                        val startRoute = navController.graph.findStartDestination().route
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
fun PantallaRestringida(
    titulo: String,
    mensaje: String,
    textoBotonPrincipal: String,
    isActionLoading: Boolean,
    onAccionPrincipal: () -> Unit,
    onDesvincularClick: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFE57373)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isActionLoading) {
                CircularProgressIndicator(color = NaranjaAcento)
            } else {
                Button(
                    onClick = onAccionPrincipal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = textoBotonPrincipal, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDesvincularClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
                    border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = Color(0xFFE57373).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Desvincularme de este entrenador", fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TextoSecundario)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar Sesión", color = TextoSecundario, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun PantallaHuerfano(
    isActionLoading: Boolean,
    onIngresarCodigo: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var correoEntrenador by remember { mutableStateOf("") }
    var codigoEntrenador by remember { mutableStateOf("") }
    var errorVinculacion by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¡Cuenta Sin Entrenador!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Para utilizar la plataforma, pide a tu entrenador que genere un código de vinculación en su perfil.",
                textAlign = TextAlign.Center,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = correoEntrenador,
                onValueChange = { correoEntrenador = it; errorVinculacion = false },
                label = { Text("Correo del Entrenador", color = TextoSecundario) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ), // 🟢
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedContainerColor = FondoTarjeta,
                    unfocusedContainerColor = FondoTarjeta
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = codigoEntrenador,
                onValueChange = { codigoEntrenador = it; errorVinculacion = false },
                label = { Text("Código de Vinculación", color = TextoSecundario) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done), // 🟢
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (correoEntrenador.isNotBlank() && codigoEntrenador.isNotBlank()) {
                        onIngresarCodigo(correoEntrenador, codigoEntrenador)
                    }
                }), // 🟢
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorVinculacion,
                supportingText = {
                    if (errorVinculacion) {
                        Text("Correo o Código incorrecto / expirado", color = Color(0xFFE57373))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedContainerColor = FondoTarjeta,
                    unfocusedContainerColor = FondoTarjeta,
                    errorContainerColor = FondoTarjeta
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isActionLoading) {
                CircularProgressIndicator(color = NaranjaAcento)
            } else {
                Button(
                    onClick = {
                        onIngresarCodigo(correoEntrenador, codigoEntrenador)
                        errorVinculacion = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = correoEntrenador.isNotBlank() && codigoEntrenador.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Vincular Cuenta", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TextoSecundario)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar Sesión", color = TextoSecundario, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun PerfilAtletaTab(
    uid: String,
    atletaFallback: Usuario,
    onLogout: () -> Unit,
    onDesvincularClick: () -> Unit,
    onEditDatosPersonalesClick: () -> Unit
) {
    val perfilViewModel: PerfilAtletaViewModel = viewModel { PerfilAtletaViewModel() }
    val uiState by perfilViewModel.uiState.collectAsState()
    var mostrarEntrenadorSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        perfilViewModel.cargarPerfil(uid)
    }

    val atleta: Usuario = uiState.usuarioLogueado ?: atletaFallback

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(2.dp, NaranjaAcento, CircleShape)
                .background(FondoTarjeta, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = NaranjaAcento)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "${atleta.nombres} ${atleta.apellidos}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = "Atleta FitLog", color = NaranjaAcento, fontSize = 14.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardMembership, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Plan Activo: ${atleta.planActivo}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    atleta.vencimientoSuscripcion?.let { fechaLong ->
                        val fechaStr = formatearFechaHistorial(fechaLong)
                        Text("Vence: $fechaStr", color = TextoSecundario, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Datos Personales", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    TextButton(onClick = onEditDatosPersonalesClick) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = FondoOscuro, thickness = 1.dp)
                ItemDatoPerfil("Nombres", atleta.nombres)
                ItemDatoPerfil("Apellidos", atleta.apellidos)
                ItemDatoPerfil("Correo Electrónico", atleta.correo)
                ItemDatoPerfil("Identificación", "${atleta.tipoDocumento} ${atleta.numeroDocumento}")
                ItemDatoPerfil("Nacionalidad", atleta.nacionalidad)
                ItemDatoPerfil("Tipo de Sangre", atleta.tipoSangre)
                if (atleta.telefono.isNotBlank()) {
                    ItemDatoPerfil("Teléfono Móvil", atleta.telefono)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (atleta.entrenadorId != null) {
            Button(
                onClick = { mostrarEntrenadorSheet = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.entrenadorAsignado != null
            ) {
                Icon(Icons.Default.Sports, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Perfil de mi Coach", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onDesvincularClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE57373)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.LinkOff, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cambiar de Entrenador", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onLogout) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TextoSecundario)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cerrar Sesión Activa", color = TextoSecundario, fontWeight = FontWeight.Medium)
        }
    }

    if (mostrarEntrenadorSheet && uiState.entrenadorAsignado != null) {
        VerEntrenadorBottomSheet(entrenador = uiState.entrenadorAsignado!!, onDismiss = { mostrarEntrenadorSheet = false })
    }
}

@Composable
fun ItemDatoPerfil(titulo: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, color = TextoSecundario, fontSize = 12.sp)
        Text(valor, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AtletaPlaceholderScreen(titulo: String) {
    Box(modifier = Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerEntrenadorBottomSheet(entrenador: Usuario, onDismiss: () -> Unit) {
    val fondoOscuro = Color(0xFF241B3C)
    val naranjaAcento = Color(0xFFFF9F6D)
    val fondoTarjeta = Color(0xFF2F254E)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = fondoTarjeta) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(72.dp).background(fondoOscuro, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(36.dp), tint = naranjaAcento)
            }
            Text("Coach ${entrenador.nombres} ${entrenador.apellidos}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            entrenador.especialidad?.let { Text(it, color = naranjaAcento, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            HorizontalDivider(color = fondoOscuro, thickness = 1.dp)
            entrenador.biografia?.let {
                Text("Sobre mí:", color = naranjaAcento, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Text(it, color = Color.White, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
            }
            if (entrenador.certificaciones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Credenciales Académicas:", color = naranjaAcento, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                entrenador.certificaciones.forEach { cert ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = naranjaAcento, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cert, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}