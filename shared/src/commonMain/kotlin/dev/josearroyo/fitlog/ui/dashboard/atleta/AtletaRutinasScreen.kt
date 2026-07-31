package dev.josearroyo.fitlog.ui.dashboard.atleta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.RutinaAsignada
import dev.josearroyo.fitlog.viewmodel.atleta.AtletaRutinasViewModel

// 🟢 IMPORTACIONES DE PLATAFORMA KMP
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.formatearFechaHora

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@Composable
fun AtletaRutinasScreen(
    uid: String,
    onNavigateToEntrenar: (String) -> Unit
) {
    // 🟢 CORREGIDO: Inicialización explícita compatible con la arquitectura DI KMP
    val viewModel: AtletaRutinasViewModel = viewModel { AtletaRutinasViewModel() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uid) {
        viewModel.cargarRutinas(uid)
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NaranjaAcento)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(FondoOscuro).padding(24.dp)) {
        Text("Mis Rutinas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Tu plan de entrenamiento actual", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Spacer(modifier = Modifier.height(16.dp))

        if (state.rutinas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes rutinas asignadas en este momento.", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.rutinas) { rutina ->
                    RutinaExpandableCard(
                        rutina = rutina,
                        onComenzar = { onNavigateToEntrenar(rutina.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RutinaExpandableCard(rutina: RutinaAsignada, onComenzar: () -> Unit) {
    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expandido = !expandido },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = rutina.nombreRutina, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "${rutina.diasEntrenamiento.size} Días", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                        IndicadorFrecuencia(rutina.ultimaVezEjecutada)
                    }
                }
                Icon(
                    imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir", tint = NaranjaAcento
                )
            }

            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(bottom = 8.dp))

                    if (rutina.diasEntrenamiento.isEmpty()) {
                        Text("No hay días configurados.", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                    } else {
                        rutina.diasEntrenamiento.sortedBy { it.ordenSecuencia }.forEach { dia ->
                            Text(text = dia.nombreDia, fontWeight = FontWeight.Bold, color = NaranjaAcento, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))

                            dia.ejercicios.sortedBy { it.ordenSecuencia }.forEachIndexed { index, ej ->

                                val totalSeries = ej.seriesPrescritas.size
                                val listaRepeticiones = ej.seriesPrescritas.map { it.repeticiones }
                                val sonRepeticionesUniforme = listaRepeticiones.distinct().size == 1

                                val textoDosificacion = when {
                                    totalSeries == 0 -> "Sin series"
                                    sonRepeticionesUniforme -> "${totalSeries}s x ${listaRepeticiones.first()} reps"
                                    else -> "${totalSeries}s x ${listaRepeticiones.joinToString("/")}"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}.", color = TextoSecundario, modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(ej.nombre, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)

                                    Text(
                                        text = textoDosificacion,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaranjaAcento,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    val fechaFormateada = rutina.ultimaVezEjecutada?.let {
                        formatearFechaHora(it)
                    } ?: "Nunca"

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Última ejecución: $fechaFormateada", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onComenzar, modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                        shape = RoundedCornerShape(12.dp), enabled = rutina.diasEntrenamiento.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ir al Programa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IndicadorFrecuencia(ultimaVez: Long?) {
    if (ultimaVez == null) {
        Surface(shape = MaterialTheme.shapes.small, color = NaranjaAcento.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 2.dp)) {
            Text(text = "¡Rutina Nueva!", color = NaranjaAcento, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        return
    }

    // 🟢 CORREGIDO: El cálculo matemático ahora se recuerda para evitar ejecuciones repetitivas en cada frame
    val esActivo = remember(ultimaVez) {
        val diffMilis = getCurrentTimeMillis() - ultimaVez
        val diffDias = diffMilis / (1000 * 60 * 60 * 24)
        diffDias < 2
    }

    val diffDiasTexto = remember(ultimaVez) {
        val diffMilis = getCurrentTimeMillis() - ultimaVez
        diffMilis / (1000 * 60 * 60 * 24)
    }

    val containerColor = if (esActivo) NaranjaAcento else FondoOscuro
    val contentColor = if (esActivo) FondoOscuro else TextoSecundario
    val texto = if (esActivo) "Activo" else "Hace $diffDiasTexto días"

    Surface(shape = MaterialTheme.shapes.small, color = containerColor, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = texto, color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}