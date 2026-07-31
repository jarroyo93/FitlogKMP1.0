package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.EstadoSuscripcion
import dev.josearroyo.fitlog.viewmodel.atleta.AtletaDetailViewModel
import dev.josearroyo.fitlog.formatearFechaHistorial
import dev.josearroyo.fitlog.viewmodel.atleta.InformeCoach

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtletaDetailScreen(
    atletaId: String,
    onBack: () -> Unit,
    onNavigateToHistorialValoraciones: (String) -> Unit,
    onNavigateToHistorialHabitos: (String) -> Unit,
    onNavigateToPerfil: (String) -> Unit,
    onNavigateToRendimiento: (String) -> Unit,
    onNavigateToSeleccionarPlantilla: (String, String) -> Unit,
    onNavigateToEditRutina: (String, String) -> Unit
) {
    val detailViewModel: AtletaDetailViewModel = viewModel { AtletaDetailViewModel() }
    val state by detailViewModel.state.collectAsState()

    LaunchedEffect(atletaId) {
        detailViewModel.cargarExpedienteAtleta(atletaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.atleta?.nombres ?: "Detalle del Atleta",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = NaranjaAcento
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoOscuro)
                .padding(innerPadding)
        ) {
            if (state.isLoading || (state.atleta == null && state.error == null)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NaranjaAcento)
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "Error desconocido",
                        color = Color(0xFFE57373),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val atleta = state.atleta!!
                val rutina = state.rutinaActiva
                val info = state.informeCoach

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Ficha del Atleta
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp)
                                    .background(FondoOscuro, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = NaranjaAcento,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${atleta.nombres} ${atleta.apellidos}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    "Doc: ${atleta.numeroDocumento}",
                                    color = TextoSecundario,
                                    fontSize = 13.sp
                                )
                            }

                            val colorEstado =
                                if (atleta.estadoSuscripcion == EstadoSuscripcion.ACTIVO) Color(
                                    0xFF81C784
                                ) else Color(0xFFE57373)
                            Surface(
                                color = colorEstado.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    atleta.estadoSuscripcion.name,
                                    color = colorEstado,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 2. 🚀 PANEL DE RENDIMIENTO Y CONTROL (CICLO ACTIVO MULTIPLATAFORMA)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text(
                                    "Panel de Control (Ciclo Activo)",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val textoFechas =
                                    if (info.fechaInicio != null && info.fechaFin != null) {
                                        "Del ${formatearFechaHistorial(info.fechaInicio)} al ${
                                            formatearFechaHistorial(
                                                info.fechaFin
                                            )
                                        }"
                                    } else {
                                        "Rango de ciclo operacional activo"
                                    }
                                Text(
                                    text = textoFechas,
                                    color = NaranjaAcento,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                KpiCircular(
                                    valor = "${info.asistenciaPorcentaje.toInt()}%",
                                    titulo = "Asistencia",
                                    porcentaje = (info.asistenciaPorcentaje / 100).toFloat()
                                        .coerceIn(0f, 1f),
                                    color = Color(0xFF4FC3F7)
                                )
                                KpiCircular(
                                    valor = "${info.cumplimientoVolumen.toInt()}%",
                                    titulo = "Vol. Meta",
                                    porcentaje = (info.cumplimientoVolumen / 100).toFloat()
                                        .coerceIn(0f, 1f),
                                    color = Color(0xFF81C784)
                                )

                                // 🚀 KMP SAFE: Redondeo decimal matemático puro sin String.format()
                                val rpeRedondeado = ((info.rpeAverageGlobal() * 10).toInt() / 10.0)
                                KpiCircular(
                                    valor = if (rpeRedondeado == 0.0) "0.0" else "$rpeRedondeado",
                                    titulo = "RPE Medio",
                                    porcentaje = (info.rpePromedioGlobal / 10.0).toFloat()
                                        .coerceIn(0f, 1f),
                                    color = Color(0xFFFFB74D)
                                )
                            }

                            if (info.rpePromedioPorEjercicio.isNotEmpty()) {
                                HorizontalDivider(color = FondoOscuro.copy(alpha = 0.5f))
                                Text(
                                    "Top Exigencia Neuromuscular (Mayor RPE)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                info.rpePromedioPorEjercicio.forEach { (nombre, rpe) ->
                                    val rpeEjRedondeado = ((rpe * 10).toInt() / 10.0)
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• $nombre", color = TextoSecundario, fontSize = 12.sp)
                                        Text(
                                            "RPE $rpeEjRedondeado",
                                            color = Color(0xFFFF7043),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Cuadrícula de Accesos Rápidos
                    Text(
                        "Expediente General",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MenuButton(
                                Modifier.weight(1f),
                                "Valoración",
                                Icons.Default.Assessment
                            ) { onNavigateToHistorialValoraciones(atletaId) }
                            MenuButton(
                                Modifier.weight(1f),
                                "Hábitos",
                                Icons.Default.MenuBook
                            ) { onNavigateToHistorialHabitos(atletaId) }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MenuButton(
                                Modifier.weight(1f),
                                "Perfil",
                                Icons.Default.AccountCircle
                            ) { onNavigateToPerfil(atletaId) }
                            MenuButton(
                                Modifier.weight(1f),
                                "Diario Cargas",
                                Icons.Default.FitnessCenter
                            ) { onNavigateToRendimiento(atletaId) }
                        }
                    }

                    // 4. 🚀 COMENTARIOS VIVOS DEL TRABAJO DEL ALUMNO
                    if (state.notasRecientes.isNotEmpty()) {
                        HorizontalDivider(
                            color = FondoTarjeta,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            "Últimos Comentarios del Atleta",
                            fontWeight = FontWeight.Bold,
                            color = NaranjaAcento,
                            fontSize = 15.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.notasRecientes.forEach { nota ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFF421D24
                                        )
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.Red.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NewReleases,
                                            contentDescription = "Feedback",
                                            tint = Color(0xFFE57373),
                                            modifier = Modifier.padding(top = 2.dp).size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "En: ${nota.ejercicioNombre} (${nota.rutinaNombre})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "\"${nota.mensaje}\"",
                                                fontSize = 13.sp,
                                                fontStyle = FontStyle.Italic,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                            Text(
                                                text = formatearFechaHistorial(nota.fecha),
                                                fontSize = 10.sp,
                                                color = TextoSecundario,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. SECCIÓN DE PROGRAMACIÓN DE RÚTINAS
                    HorizontalDivider(
                        color = FondoTarjeta,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Programa de Entrenamiento Activo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        TextButton(onClick = {
                            onNavigateToSeleccionarPlantilla(
                                atletaId,
                                atleta.entrenadorId ?: ""
                            )
                        }, colors = ButtonDefaults.textButtonColors(contentColor = NaranjaAcento)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Asignar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (rutina != null) {
                        // 🟢 TARJETA DE MATERIAL 3 OPTIMIZADA: Usamos la sobrecarga nativa de onClick para ripples perfectos
                        Card(
                            onClick = { onNavigateToEditRutina(atletaId, rutina.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = NaranjaAcento.copy(
                                    alpha = 0.08f
                                )
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                NaranjaAcento.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rutina.nombreRutina,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "Asignado: ${formatearFechaHistorial(rutina.fechaAsignacion)}",
                                            color = TextoSecundario,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Editar",
                                        tint = NaranjaAcento,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Este atleta no registra rutinas activas en la macroetapa.",
                            color = TextoSecundario,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCircular(valor: String, titulo: String, porcentaje: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f, sweepAngle = 360f * porcentaje, useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(valor, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(titulo, color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MenuButton(modifier: Modifier, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = NaranjaAcento, modifier = Modifier.size(24.dp))
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// Extension helper para seguridad del RPE Promedio
fun InformeCoach.rpeAverageGlobal(): Double {
    return if (this.rpePromedioGlobal.isNaN()) 0.0 else this.rpePromedioGlobal
}