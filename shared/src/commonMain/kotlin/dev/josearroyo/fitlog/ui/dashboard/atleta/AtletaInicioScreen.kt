package dev.josearroyo.fitlog.ui.dashboard.atleta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.data.model.ModoCiclo
import dev.josearroyo.fitlog.data.remoteconfig.RemoteConfigManager
import dev.josearroyo.fitlog.viewmodel.atleta.AtletaInicioViewModel
import dev.josearroyo.fitlog.formatearFechaHora

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaInicioScreen(
    uid: String,
    onNavigateToEntrenar: (String) -> Unit
) {
    val viewModel: AtletaInicioViewModel = viewModel { AtletaInicioViewModel() }
    val state by viewModel.state.collectAsState()

    // 🔄 Lectura dinámica desde Remote Config
    val textoAyudaDashboard by RemoteConfigManager.ayudaDashboardInicio.collectAsState()
    val textoInformativo by RemoteConfigManager.textoPerfilInformativo.collectAsState()
    val glosarioJson by RemoteConfigManager.glosarioFitnessJson.collectAsState()

    var mostrarModalAyuda by remember { mutableStateOf(false) }
    var mostrarModalPeso by remember { mutableStateOf(false) }
    var mostrarGlosarioSheet by remember { mutableStateOf(false) }

    var inputPeso by remember { mutableStateOf("") }
    var inputNotas by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        viewModel.cargarDashboard(uid)
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NaranjaAcento)
        }
        return
    }

    // 🖼️ Modal de Guía de Pantalla (HelpOutline)
    if (mostrarModalAyuda) {
        AlertDialog(
            containerColor = FondoTarjeta,
            onDismissRequest = { mostrarModalAyuda = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = NaranjaAcento
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guía del Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = textoAyudaDashboard,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { mostrarModalAyuda = false }) {
                    Text("Entendido", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ⚖️ Modal Registro de Peso
    if (mostrarModalPeso) {
        AlertDialog(
            containerColor = FondoTarjeta,
            onDismissRequest = {
                mostrarModalPeso = false
                inputPeso = ""
                inputNotas = ""
            },
            title = { Text("Registrar Nuevo Peso", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Ingresa tu peso actual y una nota opcional.",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputPeso,
                        onValueChange = { inputPeso = it },
                        label = { Text("Peso en Kg", color = NaranjaAcento) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento
                        )
                    )
                    OutlinedTextField(
                        value = inputNotas,
                        onValueChange = { inputNotas = it },
                        label = { Text("Notas (Opcional)", color = NaranjaAcento) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    onClick = {
                        val pesoNum = inputPeso.replace(",", ".").toDoubleOrNull()
                        if (pesoNum != null) {
                            viewModel.registrarPeso(pesoNum, inputNotas)
                            mostrarModalPeso = false
                            inputPeso = ""
                            inputNotas = ""
                        }
                    }
                ) {
                    Text("Guardar Peso", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarModalPeso = false
                    inputPeso = ""
                    inputNotas = ""
                }) {
                    Text("Cancelar", color = NaranjaAcento)
                }
            }
        )
    }

    // 🔄 ENVOLTURA PULL-TO-REFRESH COMPATIBLE CON ANDROID Y IOS
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.cargarDashboard(uid, esRefrescoManual = true) },
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 🟢 Encabezado con saludo e ícono de ayuda unificado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hola, ${state.usuario?.nombres?.substringBefore(" ") ?: "Atleta"}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { mostrarModalAyuda = true }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Guía del Dashboard",
                        tint = NaranjaAcento
                    )
                }
            }

            // 📢 TARJETA DESTACADA: NOTA DEL DÍA + BOTÓN DE GLOSARIO
            if (textoInformativo.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = NaranjaAcento.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = NaranjaAcento,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NOTA DEL DÍA",
                                        color = NaranjaAcento,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = textoInformativo,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )

                        if (glosarioJson.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = FondoOscuro, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { mostrarGlosarioSheet = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = NaranjaAcento,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Consultar Glosario Fitness",
                                        color = NaranjaAcento,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DashboardCicloActivo(state.cicloActivo)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tu entrenamiento de hoy",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                val rutinaActual = state.rutinasSugeridas.firstOrNull()

                if (rutinaActual == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
                    ) {
                        Text(
                            text = "Aún no tienes un programa asignado.",
                            color = TextoSecundario,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    val diaSugerido = rutinaActual.diasEntrenamiento.minByOrNull { it.ultimaVezEjecutada ?: 0L }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = rutinaActual.nombreRutina,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NaranjaAcento
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (diaSugerido != null) {
                                Text(
                                    text = "Toca entrenar:",
                                    color = TextoSecundario,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = diaSugerido.nombreDia,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val fechaTexto = if (diaSugerido.ultimaVezEjecutada != null) {
                                    "Última vez: ${formatearFechaHora(diaSugerido.ultimaVezEjecutada)}"
                                } else {
                                    "Día nuevo, ¡a darle!"
                                }
                                Text(text = fechaTexto, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                            } else {
                                Text(
                                    text = "No hay días configurados.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onNavigateToEntrenar(rutinaActual.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NaranjaAcento,
                                    contentColor = FondoOscuro
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = diaSugerido != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ir al Programa", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Control de Peso",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val ultimoPesaje = state.ultimosPesajes.firstOrNull()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Peso Actual", color = TextoSecundario, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = if (ultimoPesaje != null) "${ultimoPesaje.pesoKg} kg" else "-- kg",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (ultimoPesaje != null) {
                                    Text(
                                        text = "Registrado: ${formatearFechaHora(ultimoPesaje.fecha)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoSecundario
                                    )
                                    if (ultimoPesaje.notas.isNotBlank()) {
                                        Text(
                                            text = "Nota: ${ultimoPesaje.notas}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = NaranjaAcento
                                        )
                                    }
                                }
                            }
                            FloatingActionButton(
                                onClick = { mostrarModalPeso = true },
                                containerColor = NaranjaAcento,
                                contentColor = FondoOscuro
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Registrar Peso")
                            }
                        }

                        if (state.ultimosPesajes.size > 1) {
                            HorizontalDivider(color = FondoOscuro)
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Registros anteriores:", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)

                                state.ultimosPesajes.drop(1).forEach { pesaje ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = formatearFechaHora(pesaje.fecha),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (pesaje.notas.isNotBlank()) {
                                                Text(
                                                    text = pesaje.notas,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontStyle = FontStyle.Italic,
                                                    color = TextoSecundario
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${pesaje.pesoKg} kg",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 📖 BOTTOM SHEET GLOSARIO FITNESS
    if (mostrarGlosarioSheet) {
        GlosarioFitnessBottomSheet(
            jsonGlosario = glosarioJson,
            onDismiss = { mostrarGlosarioSheet = false }
        )
    }
}

private data class TerminoGlosario(val termino: String, val definicion: String)

private fun parsearGlosarioJson(json: String): List<TerminoGlosario> {
    val list = mutableListOf<TerminoGlosario>()
    val regex = Regex("""\{\s*"termino"\s*:\s*"([^"]+)"\s*,\s*"definicion"\s*:\s*"([^"]+)"\s*\}""")
    regex.findAll(json).forEach { match ->
        list.add(TerminoGlosario(match.groupValues[1], match.groupValues[2]))
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlosarioFitnessBottomSheet(jsonGlosario: String, onDismiss: () -> Unit) {
    val terminos = remember(jsonGlosario) { parsearGlosarioJson(jsonGlosario) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FondoTarjeta) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Glosario Fitness", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = FondoOscuro, thickness = 1.dp)

            if (terminos.isEmpty()) {
                Text("No hay términos disponibles.", color = TextoSecundario, fontSize = 14.sp)
            } else {
                terminos.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FondoOscuro, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(item.termino, color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.definicion, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DashboardCicloActivo(cicloActivo: CicloEntrenamiento?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (cicloActivo == null) {
                Text(
                    text = "¡Nuevo Ciclo!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaAcento
                )
                Text(
                    text = "Registra tu primer entrenamiento para iniciar el seguimiento de tu ciclo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            } else {
                val esSemanal = cicloActivo.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL
                val tituloCiclo = if (esSemanal) "Progreso Semanal (Lun - Dom)" else "Progreso Ciclo Rodante (${cicloActivo.duracionDias} días)"
                val etiquetaModo = if (esSemanal) "Semana Activa" else "Ciclo de Días Corridos"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tituloCiclo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NaranjaAcento,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = NaranjaAcento.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = etiquetaModo,
                            color = NaranjaAcento,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Asistencia (${cicloActivo.sesionesCompletadas}/${cicloActivo.metaSesionesAsignadas})",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${cicloActivo.porcentajeAsistencia.toInt()}%",
                            color = NaranjaAcento,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                    val progresoAsistencia = if (cicloActivo.metaSesionesAsignadas > 0) {
                        (cicloActivo.sesionesCompletadas.toFloat() / cicloActivo.metaSesionesAsignadas.toFloat()).coerceAtMost(1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progresoAsistencia },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NaranjaAcento,
                        trackColor = FondoOscuro
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Cumplimiento de Rutina",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${cicloActivo.porcentajeVolumenGlobal.toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                    val progresoVolumen = if (cicloActivo.repeticionesMetaTotal > 0) {
                        (cicloActivo.repeticionesLogradasTotal.toFloat() / cicloActivo.repeticionesMetaTotal.toFloat()).coerceAtMost(1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progresoVolumen },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color.White,
                        trackColor = FondoOscuro
                    )
                }
            }
        }
    }
}