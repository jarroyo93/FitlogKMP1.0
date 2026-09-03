package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.formatearFechaHistorial
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.ui.dashboard.entrenador.util.AsignarPlanDialog
import dev.josearroyo.fitlog.viewmodel.FacturacionViewModel
import dev.josearroyo.fitlog.viewmodel.FiltroFacturacion

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturacionScreen(
    entrenadorId: String,
    onNavigateToHistorial: (atletaId: String) -> Unit,
    onNavigateToInformeGlobal: () -> Unit,
    viewModel: FacturacionViewModel = viewModel { FacturacionViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var atletaSeleccionadoParaRenovar by rememberSaveable { mutableStateOf<Usuario?>(null) }
    var atletaSeleccionadoParaPausar by rememberSaveable { mutableStateOf<Usuario?>(null) }

    LaunchedEffect(entrenadorId) {
        viewModel.cargarAtletas(entrenadorId)
    }

    LaunchedEffect(state.error) {
        state.error?.let { mensaje ->
            snackbarHostState.showSnackbar(
                message = mensaje,
                duration = SnackbarDuration.Short
            )
            viewModel.limpiarError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Facturación & Suscripciones", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = onNavigateToInformeGlobal) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Reporte general", tint = NaranjaAcento)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(FondoOscuro).padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

                EstadisticasRapidas(atletas = state.atletas)

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Buscar atleta...", color = TextoSecundario) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextoSecundario) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, null, tint = TextoSecundario)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NaranjaAcento,
                        unfocusedBorderColor = FondoTarjeta,
                        focusedContainerColor = FondoTarjeta,
                        unfocusedContainerColor = FondoTarjeta,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FiltroFacturacion.entries) { filtro ->
                        val esSeleccionado = state.filtroActual == filtro
                        FilterChip(
                            selected = esSeleccionado,
                            onClick = { viewModel.onFiltroChanged(filtro) },
                            label = { Text(filtro.etiqueta) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaranjaAcento,
                                selectedLabelColor = FondoOscuro,
                                containerColor = FondoTarjeta,
                                labelColor = TextoSecundario
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NaranjaAcento)
                    }
                } else if (state.atletasFiltrados.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron atletas.", color = TextoSecundario, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = state.atletasFiltrados, key = { it.id }) { atleta ->
                            AtletaFacturacionItem(
                                atleta = atleta,
                                onHistory = { onNavigateToHistorial(atleta.id) },
                                onPause = { atletaSeleccionadoParaPausar = atleta },
                                onResume = { viewModel.reactivarAtleta(atleta.id, entrenadorId) },
                                onRenew = { atletaSeleccionadoParaRenovar = atleta }
                            )
                        }
                    }
                }
            }

            // 🟢 DIÁLOGO UNIFICADO PARA RENOVAR / VENDER PLAN
            // 🟢 CORRECCIÓN DEFINITIVA: Validar estado antes de pasar la fecha al diálogo
            atletaSeleccionadoParaRenovar?.let { atleta ->
                val ahora = getCurrentTimeMillis()
                val vencimientoActual = remember(atleta) {
                    val venc = atleta.vencimientoSuscripcion ?: 0L
                    if ((atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO || atleta.estadoSuscripcion == EstadoSuscripcion.DIFERIDO) && venc > ahora) {
                        venc
                    } else {
                        ahora // Si está VENCIDO, HUERFANO o con fecha pasada, la fecha base es HOY
                    }
                }

                AsignarPlanDialog(
                    atletaNombre = "${atleta.nombres} ${atleta.apellidos}".trim(),
                    ultimaFechaFinCadena = vencimientoActual,
                    onDismiss = { atletaSeleccionadoParaRenovar = null },
                    onConfirm = { plan, dias, enseguida, inicioMilis ->
                        viewModel.renovarAtleta(
                            atletaId = atleta.id,
                            entrenadorId = entrenadorId,
                            tipoPlan = plan,
                            diasPersonalizados = dias,
                            iniciarEnseguida = enseguida,
                            fechaInicioSeleccionada = inicioMilis
                        )
                        atletaSeleccionadoParaRenovar = null
                    }
                )
            }

            atletaSeleccionadoParaPausar?.let { atleta ->
                DialogoPausar(
                    atletaNombre = "${atleta.nombres} ${atleta.apellidos}".trim(),
                    onDismiss = { atletaSeleccionadoParaPausar = null },
                    onConfirm = { motivo ->
                        viewModel.pausarAtleta(atleta.id, entrenadorId, motivo)
                        atletaSeleccionadoParaPausar = null
                    }
                )
            }
        }
    }
}

// ============================================================
// COMPOSABLES INTERNOS Y ELEMENTOS DE DISEÑO
// ============================================================

@Composable
fun EstadisticasRapidas(atletas: List<Usuario>) {
    val total = atletas.size
    val activos = atletas.count { it.estadoSuscripcion == EstadoSuscripcion.ACTIVO }
    val diferidos = atletas.count { it.estadoSuscripcion == EstadoSuscripcion.DIFERIDO }
    val vencidos = atletas.count { it.estadoSuscripcion == EstadoSuscripcion.VENCIDO }
    val pausados = atletas.count { it.estadoSuscripcion == EstadoSuscripcion.SUSPENDIDO }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", color = TextoSecundario, fontSize = 11.sp)
                Text("$total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Activos", color = TextoSecundario, fontSize = 11.sp)
                Text("$activos", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Programados", color = TextoSecundario, fontSize = 11.sp)
                Text("$diferidos", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Vencidos", color = TextoSecundario, fontSize = 11.sp)
                Text("$vencidos", color = Color(0xFFE57373), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pausados", color = TextoSecundario, fontSize = 11.sp)
                Text("$pausados", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AtletaFacturacionItem(
    atleta: Usuario,
    onHistory: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRenew: () -> Unit
) {
    val colorSuscripcion = when (atleta.estadoSuscripcion) {
        EstadoSuscripcion.ACTIVO -> Color(0xFF81C784)
        EstadoSuscripcion.SUSPENDIDO -> Color(0xFFFFB74D)
        EstadoSuscripcion.VENCIDO -> Color(0xFFE57373)
        EstadoSuscripcion.HUERFANO -> TextoSecundario
        EstadoSuscripcion.DIFERIDO -> Color(0xFF64B5F6)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(FondoOscuro, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = atleta.nombres.take(1).uppercase(),
                        color = NaranjaAcento,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${atleta.nombres} ${atleta.apellidos}".trim().ifEmpty { "Atleta Sin Nombre" },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Plan: ${atleta.planActivo}",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                    atleta.vencimientoSuscripcion?.let { venc ->
                        if (venc > 0) {
                            Text(
                                text = "Vence: ${formatearFechaHistorial(venc)}",
                                fontSize = 11.sp,
                                color = if (venc < getCurrentTimeMillis() && atleta.estadoSuscripcion != EstadoSuscripcion.SUSPENDIDO) Color(0xFFE57373) else TextoSecundario
                            )
                        }
                    }
                }

                Surface(
                    color = colorSuscripcion.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorSuscripcion.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = atleta.estadoSuscripcion.name,
                        color = colorSuscripcion,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = FondoOscuro)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onHistory,
                    colors = ButtonDefaults.textButtonColors(contentColor = NaranjaAcento)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Historial", fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (atleta.estadoSuscripcion) {
                        EstadoSuscripcion.ACTIVO -> {
                            Button(
                                onClick = onPause,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.15f), contentColor = Color(0xFFFFB74D)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pausar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onRenew,
                                colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Renovar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EstadoSuscripcion.DIFERIDO -> {
                            Button(
                                onClick = onRenew,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6), contentColor = FondoOscuro),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agregar Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EstadoSuscripcion.SUSPENDIDO -> {
                            Button(
                                onClick = onResume,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784), contentColor = FondoOscuro),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reactivar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EstadoSuscripcion.VENCIDO, EstadoSuscripcion.HUERFANO -> {
                            Button(
                                onClick = onRenew,
                                colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vender Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoPausar(
    atletaNombre: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivoInput by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    val ocultarTeclado = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BasicAlertDialog(
        onDismissRequest = {
            ocultarTeclado()
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    ocultarTeclado()
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        ocultarTeclado()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pausar Membresía",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "¿Deseas pausar temporalmente a $atletaNombre?",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "El saldo de días vigentes se congelará y podrá ser reactivado posteriormente sin perder su tiempo comprado.",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = motivoInput,
                        onValueChange = { motivoInput = it },
                        label = { Text("Motivo de la pausa", color = TextoSecundario) },
                        placeholder = { Text("Ej: Lesión médica, vacaciones...", color = TextoSecundario.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { ocultarTeclado() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB74D),
                            unfocusedBorderColor = FondoOscuro,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                ocultarTeclado()
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextoSecundario)
                        ) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                ocultarTeclado()
                                onConfirm(motivoInput.trim().ifEmpty { "Pausa solicitada por el entrenador" })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = FondoOscuro),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Congelar Membresía", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}