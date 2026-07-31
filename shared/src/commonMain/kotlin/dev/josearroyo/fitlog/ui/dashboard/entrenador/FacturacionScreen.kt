package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.formatearFechaHistorial
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.viewmodel.FacturacionViewModel
import dev.josearroyo.fitlog.viewmodel.FiltroFacturacion
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction


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

    // 🟢 CORREGIDO: rememberSaveable para evitar cierre de diálogos al rotar/recomponer
    var atletaSeleccionadoParaRenovar by rememberSaveable { mutableStateOf<Usuario?>(null) }
    var atletaSeleccionadoParaPausar by rememberSaveable { mutableStateOf<Usuario?>(null) }

    LaunchedEffect(entrenadorId) {
        viewModel.cargarAtletas(entrenadorId)
    }

    Scaffold(
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

                LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 🟢 CORREGIDO: Uso de .entries en lugar de .values()
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
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 🟢 CORREGIDO: Key estable para optimizar la lista
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

            atletaSeleccionadoParaRenovar?.let { atleta ->
                DialogoRenovacion(
                    atletaNombre = "${atleta.nombres} ${atleta.apellidos}",
                    onDismiss = { atletaSeleccionadoParaRenovar = null },
                    onConfirm = { plan, dias, enseguida, inicioMilis ->
                        viewModel.renovarAtleta(atleta.id, entrenadorId, plan, dias, enseguida, inicioMilis)
                        atletaSeleccionadoParaRenovar = null
                    }
                )
            }

            atletaSeleccionadoParaPausar?.let { atleta ->
                DialogoPausar(
                    atletaNombre = "${atleta.nombres} ${atleta.apellidos}",
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

// ============================================================
// MODAL DE CONFIGURACIÓN DE RENOVACIÓN DE PLAN (REFACTORIZADO)
// ============================================================
// 1. Definimos la lista fuera del Composable para no recrearla en cada frame
private val Offsets = listOf(
    0 to "Hoy",
    1 to "Mañana",
    2 to "+2 días",
    7 to "+1 semana",
    15 to "+15 días",
    30 to "+1 mes"
)

@Composable
fun DialogoRenovacion(
    atletaNombre: String,
    onDismiss: () -> Unit,
    onConfirm: (TipoPlanSuscripcion, Int, Boolean, Long) -> Unit
) {
    var planSeleccionado by remember { mutableStateOf(TipoPlanSuscripcion.MENSUAL) }
    var diasPersonalizadosInput by remember { mutableStateOf("30") }
    var iniciarEnseguida by remember { mutableStateOf(true) }
    var diasOffsetSeleccionado by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current

    val ahora = getCurrentTimeMillis()
    val fechaInicioCalculada = ahora + (diasOffsetSeleccionado * 86400000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = {
            Text(
                text = "Renovar Plan a:",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 🟢 Captura toques fuera del campo de texto dentro del modal
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = atletaNombre,
                    color = NaranjaAcento,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text("Selecciona el tipo de plan:", color = TextoSecundario, fontSize = 12.sp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoPlanSuscripcion.entries.forEach { plan ->
                        val esPlanActual = planSeleccionado == plan
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (esPlanActual) NaranjaAcento.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (esPlanActual) NaranjaAcento else FondoOscuro,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    focusManager.clearFocus()
                                    planSeleccionado = plan
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = esPlanActual,
                                onClick = {
                                    focusManager.clearFocus()
                                    planSeleccionado = plan
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NaranjaAcento, unselectedColor = TextoSecundario)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = plan.etiqueta,
                                color = if (esPlanActual) Color.White else TextoSecundario,
                                fontSize = 14.sp,
                                fontWeight = if (esPlanActual) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
                    OutlinedTextField(
                        value = diasPersonalizadosInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) diasPersonalizadosInput = input
                        },
                        label = { Text("Duración del plan (Días)", color = TextoSecundario) },
                        // 🟢 Se añade ImeAction.Done y la acción para ocultar teclado al presionar Aceptar
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaranjaAcento,
                            unfocusedBorderColor = FondoOscuro,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Iniciar inmediatamente", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Cola inteligente o vigencia inmediata", color = TextoSecundario, fontSize = 11.sp)
                    }
                    Switch(
                        checked = iniciarEnseguida,
                        onCheckedChange = {
                            focusManager.clearFocus()
                            iniciarEnseguida = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = NaranjaAcento.copy(alpha = 0.3f))
                    )
                }

                if (!iniciarEnseguida) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Configurar fecha diferida de activación:", color = TextoSecundario, fontSize = 12.sp)

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(Offsets) { (dias, label) ->
                                val esOffsetActual = diasOffsetSeleccionado == dias
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (esOffsetActual) NaranjaAcento else FondoOscuro,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            focusManager.clearFocus()
                                            diasOffsetSeleccionado = dias
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (esOffsetActual) FondoOscuro else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Fecha calculada: ${formatearFechaHistorial(fechaInicioCalculada)}",
                            color = NaranjaAcento,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val diasPersonalizados = diasPersonalizadosInput.toIntOrNull() ?: 30
                    onConfirm(planSeleccionado, diasPersonalizados, iniciarEnseguida, fechaInicioCalculada)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar Venta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = TextoSecundario)
            ) {
                Text("Cancelar")
            }
        }
    )
}

// ============================================================
// MODAL DE PAUSA MANUAL DE SUSCRIPCIÓN
// ============================================================

@Composable
fun DialogoPausar(
    atletaNombre: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivoInput by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = {
            Text(
                text = "Pausar Membresía",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 🟢 Captura toques fuera del campo de texto dentro del modal
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    // 🟢 Se añade ImeAction.Done y la acción para ocultar teclado al presionar Aceptar
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFB74D),
                        unfocusedBorderColor = FondoOscuro,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onConfirm(motivoInput.trim().ifEmpty { "Pausa solicitada por el entrenador" })
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = FondoOscuro),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Congelar Membresía", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = TextoSecundario)
            ) {
                Text("Cancelar")
            }
        }
    )
}