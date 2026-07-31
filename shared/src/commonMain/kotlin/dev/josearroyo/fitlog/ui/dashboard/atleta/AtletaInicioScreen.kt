package dev.josearroyo.fitlog.ui.dashboard.atleta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.viewmodel.atleta.AtletaInicioViewModel

// 🟢 IMPORTACIONES DE TU MÓDULO PLATAFORMA KMP (CERO IMPORTS DE JAVA)
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

    var mostrarModalPeso by remember { mutableStateOf(false) }
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

    if (mostrarModalPeso) {
        AlertDialog(
            containerColor = FondoTarjeta,
            onDismissRequest = {
                mostrarModalPeso = false
                inputPeso = ""  // 🟢 Limpieza preventiva al cerrar por fuera
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
                    inputPeso = ""  // 🟢 CORREGIDO: Resetea el buffer temporal al cancelar
                    inputNotas = ""
                }) {
                    Text("Cancelar", color = NaranjaAcento)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Hola, ${state.usuario?.nombres?.substringBefore(" ") ?: "Atleta"}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

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
                // 🟢 Corregido: .ultimaVezEjecutada es Long? en KMP, por lo que removemos el antiguo operador .time de Java
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

                            // 🟢 Reemplazamos SimpleDateFormat por tu función expect/actual KMP
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
                                // 🟢 Fecha formateada con función multiplataforma
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
                                        // 🟢 Fecha formateada con función multiplataforma
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
                    text = "¡Semana Nueva!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaAcento
                )
                Text(
                    text = "Registra tu primer entrenamiento para iniciar tu ciclo de asistencia semanal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Progreso de la Semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaAcento
                )

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