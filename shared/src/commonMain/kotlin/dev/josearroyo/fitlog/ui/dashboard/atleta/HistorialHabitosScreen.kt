package dev.josearroyo.fitlog.ui.atleta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.viewmodel.atleta.HistorialHabitosViewModel
import dev.josearroyo.fitlog.formatearFechaHistorial

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialHabitosScreen(
    atletaId: String,
    onBack: () -> Unit,
    onNavigateToNuevo: (String) -> Unit
) {
    val viewModel: HistorialHabitosViewModel = viewModel { HistorialHabitosViewModel() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(atletaId) {
        viewModel.cargarHistorial(atletaId)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Historial de Hábitos", color = Color.White, fontWeight = FontWeight.Bold) },
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
                onClick = { onNavigateToNuevo(atletaId) },
                containerColor = NaranjaAcento,
                contentColor = FondoOscuro
            ) {
                Icon(Icons.Default.Add, "Registrar Hábitos")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else if (state.lista.isEmpty()) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                Text("No hay registros de estilo de vida. Añade uno con el +.", color = TextoSecundario)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(FondoOscuro).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🟢 CORREGIDO: Se añade una firma key basándose en la marca de tiempo de registro para optimizar la recomposición del LazyColumn
                items(state.lista, key = { it.fechaRegistro }) { habitos ->
                    HabitosCard(habitos)
                }
            }
        }
    }
}

@Composable
fun HabitosCard(h: Habitos) {
    // 🟢 CORREGIDO: Usamos rememberSaveable para que la tarjeta no se cierre sola al reciclarse con el scroll del LazyColumn
    var expandido by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expandido = !expandido },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = formatearFechaHistorial(h.fechaRegistro), fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Sueño: ${h.horasSueno} hrs | Entrena: ${h.horarioEntrenamiento}", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
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
                        Text("Horario de Descanso: ", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Se duerme a las ${h.horaDormir} y despierta a las ${h.horaDespertar}", color = Color.White, fontSize = 13.sp)
                    }
                    Row {
                        Text("Días Disponibles: ", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = h.diasDisponibles.ifEmpty { "No especificado" }, color = Color.White, fontSize = 13.sp)
                    }
                    Row {
                        Text("Tiempo por Sesión: ", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${h.tiempoDisponibleMinutos} minutos", color = Color.White, fontSize = 13.sp)
                    }
                    if (h.actividadesPrincipales.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Actividades Laborales/Diarias:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(text = h.actividadesPrincipales, color = TextoSecundario, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}