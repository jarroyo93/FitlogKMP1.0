package dev.josearroyo.fitlog.ui.atleta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.viewmodel.atleta.HistorialValoracionViewModel
import dev.josearroyo.fitlog.formatearFechaHistorial

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

private fun formatearMedida(valor: Double?, unidad: String): String {
    return valor?.let { if (it <= 0.0) "No se registró" else "$it $unidad" } ?: "No se registró"
}

private fun formatearEntero(valor: Int?, unidad: String = ""): String {
    return valor?.let { if (it <= 0) "No se registró" else "$it $unidad".trim() } ?: "No se registró"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialValoracionScreen(
    atletaId: String,
    onBack: () -> Unit,
    onNavigateToNuevaValoracion: (String) -> Unit
) {
    val viewModel: HistorialValoracionViewModel = viewModel { HistorialValoracionViewModel() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(atletaId) {
        viewModel.cargarHistorial(atletaId)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Historial de Valoraciones", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = NaranjaAcento)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNuevaValoracion(atletaId) },
                containerColor = NaranjaAcento,
                contentColor = FondoOscuro
            ) {
                Icon(Icons.Default.Add, "Nueva Valoración")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else if (state.lista.isEmpty()) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                Text("No hay registros antropométricos. Añade uno con el +.", color = TextoSecundario)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(FondoOscuro).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🟢 CORREGIDO: Se añade key basado en marca de tiempo única para optimizar recomposiciones
                items(state.lista, key = { it.fechaRegistro }) { valoracion ->
                    ValoracionCard(valoracion)
                }
            }
        }
    }
}

@Composable
fun ValoracionCard(v: ValoracionFisica) {
    // 🟢 CORREGIDO: rememberSaveable retiene el estado de expansión al hacer scroll fuera de pantalla
    var expandido by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expandido = !expandido },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = formatearFechaHistorial(v.fechaRegistro), fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Evolución: ${formatearMedida(v.pesoKg, "kg")} | Estatura: ${formatearMedida(v.alturaCm, "cm")}", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                }
                Text(
                    text = if (expandido) "Ver menos" else "Ver detalles",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaranjaAcento,
                    fontWeight = FontWeight.Black
                )
            }

            if (expandido) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = FondoOscuro)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text("Condición actual: ", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = v.nivelExperiencia.toString(), color = Color.White, fontSize = 13.sp)
                    }
                    Row {
                        Text("Objetivo fijado: ", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = v.objetivoInicial.toString(), color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Perímetros Corporales:", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)

                    Surface(
                        color = FondoOscuro.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Abdomen (Alto/Bajo): ${formatearMedida(v.abdomen1, "cm")} / ${formatearMedida(v.abdomen2, "cm")}", color = TextoSecundario, fontSize = 13.sp)
                            Text("• Brazos (Contraído/Relajado): ${formatearMedida(v.brazoFlexionado, "cm")} / ${formatearMedida(v.brazoRelajado, "cm")}", color = TextoSecundario, fontSize = 13.sp)
                            Text("• Glúteo: ${formatearMedida(v.gluteo, "cm")} | Pantorrilla: ${formatearMedida(v.pantorrilla, "cm")}", color = TextoSecundario, fontSize = 13.sp)
                            Text("• Muslo (Medial/Prominente): ${formatearMedida(v.piernaMedial, "cm")} / ${formatearMedida(v.musloProminente, "cm")}", color = TextoSecundario, fontSize = 13.sp)
                        }
                    }

                    if (v.mostrarComposicionAvanzada) {
                        Spacer(Modifier.height(8.dp))
                        Text("Métricas de Bioimpedancia (Báscula):", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)

                        Surface(
                            color = FondoOscuro.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• Porcentaje de Grasa: ${formatearMedida(v.porcentajeGrasaCorporal, "%")}", color = TextoSecundario, fontSize = 13.sp)
                                Text("• Masa Muscular: ${formatearMedida(v.masaMuscularKg, "kg")}", color = TextoSecundario, fontSize = 13.sp)
                                Text("• Grasa Visceral: ${formatearEntero(v.grasaVisceral)}", color = TextoSecundario, fontSize = 13.sp)
                                Text("• Porcentaje de Agua: ${formatearMedida(v.aguaCorporalPorcentaje, "%")}", color = TextoSecundario, fontSize = 13.sp)
                                Text("• Edad Metabólica: ${formatearEntero(v.edadMetabolica, "años")}", color = TextoSecundario, fontSize = 13.sp)
                            }
                        }
                    }

                    if (v.observacionesLadoIzquierdo.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Desbalances o Notas de Rebalanceo:", fontWeight = FontWeight.Bold, color = Color(0xFFE57373), fontSize = 13.sp)
                        Text(v.observacionesLadoIzquierdo, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}