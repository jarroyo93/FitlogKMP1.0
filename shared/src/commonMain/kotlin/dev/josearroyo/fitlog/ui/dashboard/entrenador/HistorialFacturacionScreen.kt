package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.EstadoPeriodo
import dev.josearroyo.fitlog.data.model.PeriodoFacturable
import dev.josearroyo.fitlog.viewmodel.entrenador.HistorialFacturacionViewModel
import dev.josearroyo.fitlog.formatearFechaCorto // 🟢 Tu función del platform
import dev.josearroyo.fitlog.esMismoDia            // 🟢 Tu función del platform
import dev.josearroyo.fitlog.getCurrentTimeMillis   // 🟢 Tu función del platform

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialFacturacionScreen(
    atletaId: String,
    entrenadorId: String,
    onBack: () -> Unit,
    viewModel: HistorialFacturacionViewModel = viewModel { HistorialFacturacionViewModel() }
) {
    val state by viewModel.state.collectAsState()
    var mostrarDialogAnadir by remember { mutableStateOf(false) }

    LaunchedEffect(atletaId) {
        viewModel.cargarHistorial(atletaId)
    }

    if (mostrarDialogAnadir) {
        RenovarSuscripcionDialog(
            atletaNombre = state.atleta?.nombres ?: "Atleta",
            onDismiss = { mostrarDialogAnadir = false },
            onRenovar = { plan, dias, iniciarInmediato, fechaSeleccionadaMilis ->
                // 🟢 CORRECCIÓN: Quitamos el '.time' porque 'fechaSeleccionadaMilis' ya es un Long puro
                viewModel.anadirPlanAHistorial(
                    atletaId = atletaId,
                    entrenadorId = entrenadorId,
                    plan = plan,
                    diasPersonalizados = dias,
                    iniciarEnseguida = iniciarInmediato,
                    fechaInicioSeleccionadaMilis = fechaSeleccionadaMilis
                )
                mostrarDialogAnadir = false
            }
        )
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.atleta?.let { "${it.nombres} ${it.apellidos}" } ?: "Cargando...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Historial y Cola de Planes",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        },
        floatingActionButton = {
            if (!state.isLoading) {
                FloatingActionButton(
                    onClick = { mostrarDialogAnadir = true },
                    containerColor = NaranjaAcento,
                    contentColor = FondoOscuro,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Plan")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoOscuro)
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = NaranjaAcento,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.periodos.isEmpty()) {
                Text(
                    text = "Este atleta no registra periodos de facturación.",
                    color = TextoSecundario,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(state.periodos.sortedByDescending { it.fechaInicio }) { periodo ->
                        ItemPeriodoHistorial(
                            periodo = periodo,
                            onEliminarClick = {
                                viewModel.eliminarPeriodoDiferido(atletaId, periodo.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemPeriodoHistorial(
    periodo: PeriodoFacturable,
    onEliminarClick: () -> Unit
) {
    val (colorEstado, textoEstado) = when (periodo.estado) {
        EstadoPeriodo.ACTIVO -> Color(0xFF81C784) to "En Ejecución"
        EstadoPeriodo.DIFERIDO -> Color(0xFFFFB74D) to "En Cola (Futuro)"
        EstadoPeriodo.CONGELADO -> Color(0xFF4FC3F7) to "Congelado"
        EstadoPeriodo.CANCELADO -> Color(0xFFEF5350) to "Cancelado"
        EstadoPeriodo.COMPLETADO -> TextoSecundario to "Histórico"
    }

    // 🟢 CORRECCIÓN KMP: Validamos si fue creado hoy usando tu función 'esMismoDia' del platform
    val esCreadoHoy = esMismoDia(getCurrentTimeMillis(), periodo.fechaCreacion)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Plan ${periodo.tipoPlan.lowercase().replaceFirstChar { it.uppercase() }}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "ID Ref: ${periodo.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = colorEstado.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = textoEstado,
                            color = colorEstado,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (periodo.estado == EstadoPeriodo.DIFERIDO || (periodo.estado == EstadoPeriodo.ACTIVO && esCreadoHoy)) {
                        IconButton(
                            onClick = onEliminarClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar periodo",
                                tint = Color(0xFFEF5350)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Fecha de Inicio", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                    // 🟢 Usamos tu formateador corto multiplataforma
                    Text(formatearFechaCorto(periodo.fechaInicio), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fecha de Cierre", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
                    // 🟢 Usamos tu formateador corto multiplataforma
                    Text(
                        text = periodo.fechaFin?.let { formatearFechaCorto(it) } ?: "No registra",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}