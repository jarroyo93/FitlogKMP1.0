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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.EstadoPeriodo
import dev.josearroyo.fitlog.data.model.PeriodoFacturable
import dev.josearroyo.fitlog.esMismoDia
import dev.josearroyo.fitlog.formatearFechaCorto
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.ui.dashboard.entrenador.util.AsignarPlanDialog
import dev.josearroyo.fitlog.viewmodel.entrenador.HistorialFacturacionViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)
private val RojoEliminar = Color(0xFFEF5350)

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
    var periodoAEliminar by remember { mutableStateOf<PeriodoFacturable?>(null) }

    LaunchedEffect(atletaId) {
        viewModel.cargarHistorial(atletaId)
    }

    // 🟢 DIÁLOGO UNIFICADO DE ASIGNACIÓN DE PLAN
    if (mostrarDialogAnadir) {
        val ultimaFechaCadena = remember(state.periodos) {
            state.periodos
                .filter { it.estado == EstadoPeriodo.ACTIVO || it.estado == EstadoPeriodo.DIFERIDO }
                .mapNotNull { it.fechaFin }
                .maxOrNull() ?: getCurrentTimeMillis()
        }

        AsignarPlanDialog(
            atletaNombre = state.atleta?.let { "${it.nombres} ${it.apellidos}".trim() } ?: "Atleta",
            ultimaFechaFinCadena = ultimaFechaCadena,
            onDismiss = { mostrarDialogAnadir = false },
            onConfirm = { planEnum, dias, iniciarInmediato, fechaSeleccionadaMilis ->
                viewModel.anadirPlanAHistorial(
                    atletaId = atletaId,
                    entrenadorId = entrenadorId,
                    plan = planEnum,
                    diasPersonalizados = dias,
                    iniciarEnseguida = iniciarInmediato,
                    fechaInicioSeleccionadaMilis = fechaSeleccionadaMilis
                )
                mostrarDialogAnadir = false
            }
        )
    }

    // 🟢 DIÁLOGO DE CONFIRMACIÓN PARA ELIMINAR / CANCELAR PLAN
    periodoAEliminar?.let { periodo ->
        AlertDialog(
            onDismissRequest = { periodoAEliminar = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RojoEliminar,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Cancelar Periodo de Plan",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar este plan ${periodo.tipoPlan.lowercase()} (Ref: ${periodo.id.take(8).uppercase()})? Esta acción cambiará el estado del periodo a cancelado.",
                    color = TextoSecundario,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPeriodoDiferido(atletaId, periodo.id)
                        periodoAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RojoEliminar,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sí, Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { periodoAEliminar = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextoSecundario)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = FondoTarjeta,
            shape = RoundedCornerShape(16.dp)
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
                // 🟢 ORDEN DE PRIORIDAD PERSONALIZADO POR ESTADO
                val periodosOrdenados = remember(state.periodos) {
                    state.periodos.sortedWith(
                        compareBy<PeriodoFacturable> { periodo ->
                            when (periodo.estado) {
                                EstadoPeriodo.ACTIVO -> 1      // 1° En Ejecución
                                EstadoPeriodo.DIFERIDO -> 2    // 2° En Cola (Futuro)
                                EstadoPeriodo.CONGELADO -> 3   // 3° Pausado
                                EstadoPeriodo.COMPLETADO -> 4  // 4° Histórico
                                EstadoPeriodo.CANCELADO -> 5   // 5° Cancelado
                            }
                        }.thenByDescending { it.fechaInicio }   // Criterio secundario: más reciente primero
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(items = periodosOrdenados, key = { it.id }) { periodo ->
                        ItemPeriodoHistorial(
                            periodo = periodo,
                            onEliminarClick = {
                                periodoAEliminar = periodo
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
                                tint = RojoEliminar
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
                    Text(formatearFechaCorto(periodo.fechaInicio), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Fecha de Cierre", style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
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