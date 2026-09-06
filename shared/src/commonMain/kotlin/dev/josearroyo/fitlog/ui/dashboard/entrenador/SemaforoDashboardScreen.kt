package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.viewmodel.entrenador.EntrenadorDashboardViewModel
import kotlin.math.roundToInt

@Composable
fun SemaforoDashboardContent(
    entrenadorId: String,
    viewModel: EntrenadorDashboardViewModel,
    onAtletaClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(entrenadorId) {
        viewModel.cargarDashboard(entrenadorId)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = Color(0xFFFF9F6D),
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null) {
            Text(
                text = uiState.error ?: "Error desconocido",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ResumenGerencialCard(
                    resumen = uiState.resumen,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                FiltrosSemaforoRow(
                    filtroSeleccionado = uiState.filtroEstado,
                    onFiltroSelected = { viewModel.filtrarPorEstado(it) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (uiState.atletasFiltrados.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay atletas para el filtro seleccionado",
                            color = Color(0xFFB3AEC6)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.atletasFiltrados, key = { it.atletaId }) { atleta ->
                            AtletaSemaforoCard(
                                item = atleta,
                                onClick = { onAtletaClick(atleta.atletaId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResumenGerencialCard(
    resumen: ResumenGerencial,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2F254E)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Salud del Equipo",
                        fontSize = 13.sp,
                        color = Color(0xFFB3AEC6)
                    )
                    Text(
                        text = "${resumen.porcentajeSaludEquipo}% Óptimo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(obtenerColorEstado(EstadoSemaforo.VERDE).copy(alpha = 0.2f))
                ) {
                    Text(text = "🏋️", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                BadgeConteo("Críticos", resumen.totalAtletasCriticos, EstadoSemaforo.ROJO)
                BadgeConteo("En Riesgo", resumen.totalAtletasEnRiesgo, EstadoSemaforo.AMARILLO)
                BadgeConteo("Gestión", resumen.totalRequierenGestion, EstadoSemaforo.REQUIERE_GESTION)
            }
        }
    }
}

@Composable
fun BadgeConteo(etiqueta: String, cantidad: Int, estado: EstadoSemaforo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(obtenerColorEstado(estado))
        )
        Text(
            text = "$etiqueta: $cantidad",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun FiltrosSemaforoRow(
    filtroSeleccionado: EstadoSemaforo?,
    onFiltroSelected: (EstadoSemaforo?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        item {
            FilterChip(
                selected = filtroSeleccionado == null,
                onClick = { onFiltroSelected(null) },
                label = { Text("Todos", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF9F6D),
                    selectedLabelColor = Color(0xFF241B3C),
                    containerColor = Color(0xFF2F254E),
                    labelColor = Color.White
                )
            )
        }
        items(EstadoSemaforo.entries.toTypedArray()) { estado ->
            val seleccionado = filtroSeleccionado == estado
            FilterChip(
                selected = seleccionado,
                onClick = { onFiltroSelected(estado) },
                label = { Text(obtenerEtiquetaEstado(estado), fontSize = 12.sp) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(obtenerColorEstado(estado))
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF9F6D),
                    selectedLabelColor = Color(0xFF241B3C),
                    containerColor = Color(0xFF2F254E),
                    labelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun AtletaSemaforoCard(
    item: AtletaSemaforoItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2F254E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = obtenerColorEstado(item.estado).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF241B3C))
                    ) {
                        Text(
                            text = item.nombreCompleto.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9F6D)
                        )
                    }

                    Column {
                        Text(
                            text = item.nombreCompleto,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Asistencia: ${item.adherenciaPorcentaje}%",
                            fontSize = 12.sp,
                            color = Color(0xFFB3AEC6)
                        )
                    }
                }

                EstadoBadge(estado = item.estado)
            }

            AnimatedVisibility(visible = item.mensajeGestion != null) {
                item.mensajeGestion?.let { mensaje ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠️ $mensaje",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE57373)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sesiones: ${item.sesionesEjecutadas}/${item.sesionesEsperadasHoy}",
                    fontSize = 12.sp,
                    color = Color(0xFFB3AEC6)
                )
                Text(
                    text = "RPE Medio: ${item.rpePromedio?.let { ((it * 10).roundToInt() / 10.0).toString() } ?: "N/A"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun EstadoBadge(estado: EstadoSemaforo) {
    Surface(
        color = obtenerColorEstado(estado).copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(obtenerColorEstado(estado))
            )
            Text(
                text = obtenerEtiquetaEstado(estado),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = obtenerColorEstado(estado)
            )
        }
    }
}

fun obtenerColorEstado(estado: EstadoSemaforo): Color {
    return when (estado) {
        EstadoSemaforo.VERDE -> Color(0xFF81C784)
        EstadoSemaforo.AMARILLO -> Color(0xFFFFB74D)
        EstadoSemaforo.ROJO -> Color(0xFFE57373)
        EstadoSemaforo.REQUIERE_GESTION -> Color(0xFFBA68C8)
        EstadoSemaforo.SIN_DATOS -> Color(0xFFB3AEC6)
    }
}

fun obtenerEtiquetaEstado(estado: EstadoSemaforo): String {
    return when (estado) {
        EstadoSemaforo.VERDE -> "Óptimo"
        EstadoSemaforo.AMARILLO -> "Riesgo"
        EstadoSemaforo.ROJO -> "Crítico"
        EstadoSemaforo.REQUIERE_GESTION -> "Gestión"
        EstadoSemaforo.SIN_DATOS -> "Sin Datos"
    }
}