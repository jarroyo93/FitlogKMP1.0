package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.CicloEntrenamiento
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.viewmodel.atleta.ProgresoAtletaViewModel
import dev.josearroyo.fitlog.viewmodel.atleta.DetalleEjercicioUI
import dev.josearroyo.fitlog.viewmodel.atleta.ImpactoFisicoUI
import dev.josearroyo.fitlog.viewmodel.atleta.RecordPersonalUI
import dev.josearroyo.fitlog.viewmodel.atleta.ResumenCicloUI
import dev.josearroyo.fitlog.formatearFechaHistorial
import kotlin.math.roundToInt

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)
private val VerdeExito = Color(0xFF81C784)
private val RojoIncompleto = Color(0xFFE57373)
private val AzulCumplido = Color(0xFF4FC3F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoAtletaScreen(
    userId: String,
    onBack: (() -> Unit)? = null,
    viewModel: ProgresoAtletaViewModel = viewModel { ProgresoAtletaViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    var tabSeleccionada by rememberSaveable { mutableStateOf(1) } // Por defecto pestaña "Diario de Ciclos"
    val titulosTabs = listOf("Evolución", "Diario de Ciclos", "Récords")

    // Estado local para filtrar dentro de los días/rutinas del ciclo seleccionado
    var filtroDiaRutina by rememberSaveable { mutableStateOf("TODOS") }

    val rutinasDelCiclo = remember(state.historialSesiones) {
        listOf("TODOS") + state.historialSesiones.map { it.nombreRutina }.distinct()
    }

    val sesionesMostrar = remember(state.historialSesiones, filtroDiaRutina) {
        if (filtroDiaRutina == "TODOS") state.historialSesiones
        else state.historialSesiones.filter { it.nombreRutina == filtroDiaRutina }
    }

    LaunchedEffect(userId) {
        viewModel.cargarDatosProgreso(userId)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Rendimiento de Carga", fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = NaranjaAcento)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscuro)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onBack == null) {
                    item {
                        Text(
                            text = "Mi Rendimiento",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                item {
                    KpiSection(state.rachaSemana, state.entrenosMes, state.volumenSemanal)
                }

                item {
                    TabRow(
                        selectedTabIndex = tabSeleccionada,
                        containerColor = FondoTarjeta,
                        contentColor = NaranjaAcento,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]),
                                color = NaranjaAcento
                            )
                        }
                    ) {
                        titulosTabs.forEachIndexed { index, titulo ->
                            Tab(
                                selected = tabSeleccionada == index,
                                onClick = { tabSeleccionada = index },
                                text = { Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                selectedContentColor = NaranjaAcento,
                                unselectedContentColor = TextoSecundario
                            )
                        }
                    }
                }

                when (tabSeleccionada) {
                    0 -> {
                        item {
                            EvolucionEjercicioSection(
                                ejerciciosDisponibles = state.ejerciciosDisponibles,
                                historialEjercicio = state.historialEjercicioFiltrado,
                                onEjercicioSeleccionado = { viewModel.filtrarPorEjercicio(it) }
                            )
                        }
                    }
                    1 -> {
                        // 1. Selector Horizontal de Ciclos (Ciclo Activo vs. Histórico)
                        if (state.historialCiclos.isNotEmpty()) {
                            item {
                                SelectorCiclosRow(
                                    ciclos = state.historialCiclos,
                                    cicloSeleccionado = state.cicloSeleccionado,
                                    onCicloSeleccionado = { ciclo ->
                                        filtroDiaRutina = "TODOS" // Resetea filtro al cambiar de ciclo
                                        viewModel.seleccionarCiclo(ciclo)
                                    }
                                )
                            }
                        }

                        // 2. Tarjeta Macro de Resumen del Ciclo Seleccionado
                        if (state.cicloSeleccionado != null) {
                            item {
                                TarjetaResumenCiclo(
                                    ciclo = state.cicloSeleccionado!!,
                                    resumen = state.resumenCicloSeleccionado
                                )
                            }

                            // 3. Tarjeta de Impacto Corporal (Deltas Físicos)
                            if (state.resumenCicloSeleccionado.impactoFisico.tieneDatos) {
                                item {
                                    TarjetaImpactoFisico(impacto = state.resumenCicloSeleccionado.impactoFisico)
                                }
                            }



                            if (rutinasDelCiclo.size > 2) {
                                item {
                                    FiltroRutinasCicloRow(
                                        rutinas = rutinasDelCiclo,
                                        rutinaSeleccionada = filtroDiaRutina,
                                        onRutinaSeleccionada = { filtroDiaRutina = it }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Días de Entrenamiento del Ciclo",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }



                        if (sesionesMostrar.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay sesiones registradas en este ciclo.", color = TextoSecundario)
                                }
                            }
                        } else {
                            items(sesionesMostrar, key = { it.id.ifBlank { it.fechaEjecucion.toString() } }) { sesion ->
                                TarjetaDiarioSesion(sesion = sesion)
                            }
                        }
                    }
                    2 -> {
                        if (state.recordsPersonales.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Ningún récord personal guardado aún.", color = TextoSecundario)
                                }
                            }
                        } else {
                            items(state.recordsPersonales, key = { it.nombreEjercicio }) { record ->
                                TarjetaRecordPersonal(record = record)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// COMPONENTES DE CICLOS E INTERFAZ INTERMEDIA
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorCiclosRow(
    ciclos: List<CicloEntrenamiento>,
    cicloSeleccionado: CicloEntrenamiento?,
    onCicloSeleccionado: (CicloEntrenamiento) -> Unit
) {
    var mostrarBottomSheetHistorico by rememberSaveable { mutableStateOf(false) }

    // Acotamos a los 8 ciclos más recientes para la barra rápida
    val ciclosRecientes = remember(ciclos) { ciclos.take(8) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(ciclosRecientes, key = { it.id }) { ciclo ->
            val esSeleccionado = ciclo.id == cicloSeleccionado?.id

            val fechaFormateada = remember(ciclo.fechaInicio) {
                val fechaTexto = formatearFechaHistorial(ciclo.fechaInicio)
                val partes = fechaTexto.split(" ")
                if (partes.size >= 3) {
                    "${partes[0]} ${partes[2].take(3)}"
                } else {
                    fechaTexto
                }
            }

            FilterChip(
                selected = esSeleccionado,
                onClick = { onCicloSeleccionado(ciclo) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (ciclo.estaActivo) "Ciclo Actual" else "Ciclo ($fechaFormateada)",
                            fontWeight = FontWeight.Bold,
                            color = if (esSeleccionado) FondoOscuro else Color.White,
                            fontSize = 12.sp
                        )
                        if (ciclo.estaActivo) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (esSeleccionado) FondoOscuro else VerdeExito)
                            )
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = FondoTarjeta,
                    selectedContainerColor = NaranjaAcento
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Si hay más de 8 ciclos acumulados, renderizamos la tarjeta de acceso directo al histórico
        if (ciclos.size > 8) {
            item {
                AssistChip(
                    onClick = { mostrarBottomSheetHistorico = true },
                    label = {
                        Text(
                            text = "Ver todos (${ciclos.size}) 🔍",
                            fontSize = 12.sp,
                            color = NaranjaAcento,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = FondoTarjeta),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = NaranjaAcento.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }

    // Modal BottomSheet con la lista completa
    if (mostrarBottomSheetHistorico) {
        ModalBottomSheetHistoricoCiclos(
            ciclos = ciclos,
            cicloSeleccionado = cicloSeleccionado,
            onCicloSeleccionado = { ciclo ->
                onCicloSeleccionado(ciclo)
                mostrarBottomSheetHistorico = false
            },
            onDismiss = { mostrarBottomSheetHistorico = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetHistoricoCiclos(
    ciclos: List<CicloEntrenamiento>,
    cicloSeleccionado: CicloEntrenamiento?,
    onCicloSeleccionado: (CicloEntrenamiento) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FondoTarjeta,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextoSecundario)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Histórico de Ciclos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Selecciona un período para filtrar el diario",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = FondoOscuro)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ciclos, key = { it.id }) { ciclo ->
                    val esSeleccionado = ciclo.id == cicloSeleccionado?.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCicloSeleccionado(ciclo) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (esSeleccionado) NaranjaAcento.copy(alpha = 0.15f) else FondoOscuro
                        ),
                        border = if (esSeleccionado) {
                            BorderStroke(1.5.dp, NaranjaAcento)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (ciclo.estaActivo) "Ciclo Activo" else "Ciclo de Entrenamiento",
                                        fontWeight = FontWeight.Bold,
                                        color = if (esSeleccionado) NaranjaAcento else Color.White,
                                        fontSize = 14.sp
                                    )
                                    if (ciclo.estaActivo) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(VerdeExito)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${formatearFechaHistorial(ciclo.fechaInicio)} — ${if (ciclo.fechaCierre > 0L) formatearFechaHistorial(ciclo.fechaCierre) else "Presente"}",
                                    color = TextoSecundario,
                                    fontSize = 12.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${ciclo.sesionesCompletadas}/${ciclo.metaSesionesAsignadas} Días",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${ciclo.porcentajeAsistencia.toInt()}% Asistencia",
                                    color = if (ciclo.porcentajeAsistencia >= 80.0) VerdeExito else TextoSecundario,
                                    fontSize = 11.sp,
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

@Composable
fun FiltroRutinasCicloRow(
    rutinas: List<String>,
    rutinaSeleccionada: String,
    onRutinaSeleccionada: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(rutinas) { rutina ->
            val esSeleccionado = rutina == rutinaSeleccionada
            AssistChip(
                onClick = { onRutinaSeleccionada(rutina) },
                label = {
                    Text(
                        text = if (rutina == "TODOS") "Todas las Sesiones" else rutina,
                        fontSize = 11.sp,
                        fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                        color = if (esSeleccionado) NaranjaAcento else TextoSecundario
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (esSeleccionado) NaranjaAcento.copy(alpha = 0.15f) else FondoTarjeta
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = if (esSeleccionado) NaranjaAcento else Color.Transparent
                )
            )
        }
    }
}

@Composable
fun TarjetaResumenCiclo(ciclo: CicloEntrenamiento, resumen: ResumenCicloUI) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (ciclo.estaActivo) "🟢 Ciclo Activo en Curso" else "📋 Resumen de Ciclo Cerrado",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${formatearFechaHistorial(ciclo.fechaInicio)} — ${if (ciclo.fechaCierre > 0L) formatearFechaHistorial(ciclo.fechaCierre) else "Presente"}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = FondoOscuro)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                MetricItem(
                    valor = "${resumen.porcentajeAsistencia.toInt()}%",
                    subetiqueta = "${resumen.sesionesCompletadas}/${resumen.metaSesiones} Días",
                    etiqueta = "Asistencia"
                )
                MetricItem(
                    valor = if (resumen.rpePromedio > 0.0) "${resumen.rpePromedio}" else "N/A",
                    subetiqueta = "Esfuerzo Medio",
                    etiqueta = "RPE"
                )
                MetricItem(
                    valor = "${(resumen.tonelajeTotalKg / 1000.0).let { if (it >= 1.0) "${((it * 10).roundToInt() / 10.0)}t" else "${resumen.tonelajeTotalKg.toInt()}kg" }}",
                    subetiqueta = "Volumen Aislado",
                    etiqueta = "Tonelaje"
                )
            }
        }
    }
}

@Composable
fun TarjetaImpactoFisico(impacto: ImpactoFisicoUI) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NaranjaAcento.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "⚖️ Impacto Corporal del Ciclo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

            impacto.deltaPeso?.let { delta ->
                val signo = if (delta > 0) "+" else ""
                val colorTexto = if (delta <= 0) VerdeExito else NaranjaAcento
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Masa Corporal:", color = TextoSecundario, fontSize = 13.sp)
                    Text(
                        text = "${impacto.pesoInicial} kg ➔ ${impacto.pesoFinal} kg ($signo$delta kg)",
                        fontWeight = FontWeight.Bold,
                        color = colorTexto,
                        fontSize = 13.sp
                    )
                }
            }

            impacto.deltaAbdomen?.let { delta ->
                val signo = if (delta > 0) "+" else ""
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cintura / Abdomen:", color = TextoSecundario, fontSize = 13.sp)
                    Text(
                        text = "${impacto.abdomenInicial} cm ➔ ${impacto.abdomenFinal} cm ($signo$delta cm)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ============================================================
// TARJETA DE SESIÓN CON MARCADORES DE CUMPLIMIENTO
// ============================================================

@Composable
fun TarjetaDiarioSesion(sesion: SesionEntrenamiento) {
    var mostrarDetalleDialog by rememberSaveable { mutableStateOf(false) }

    val volumenSesion = remember(sesion.ejerciciosRealizados) {
        sesion.ejerciciosRealizados.filter { !it.fueSaltado }.sumOf { ej ->
            ej.seriesRealizadas.sumOf { serie -> serie.pesoKg * serie.repeticionesLogradas }
        }
    }

    // Cálculo del estado de cumplimiento de la sesión
    val repsLogradas = sesion.totalRepsEfectivasLogradas
    val repsMeta = sesion.totalRepsEfectivasMeta
    val (colorBadge, textoBadge) = remember(repsLogradas, repsMeta) {
        when {
            repsMeta <= 0 -> VerdeExito to "Completada"
            repsLogradas > repsMeta -> VerdeExito to "¡Superada! 🔥"
            repsLogradas == repsMeta -> AzulCumplido to "100% Cumplida ✔️"
            else -> RojoIncompleto to "Incompleta (${((repsLogradas.toDouble() / repsMeta.toDouble()) * 100).toInt()}%)"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { mostrarDetalleDialog = true },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = sesion.nombreRutina, fontWeight = FontWeight.Bold, color = NaranjaAcento, fontSize = 15.sp, modifier = Modifier.weight(1f))

                // Badge de Cumplimiento de Sesión
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorBadge.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = textoBadge, color = colorBadge, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = formatearFechaHistorial(sesion.fechaEjecucion), color = TextoSecundario, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = FondoOscuro)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reps: $repsLogradas / ${if(repsMeta > 0) repsMeta else "-"}", color = Color.White, fontSize = 13.sp)
                Text(text = "Volumen: ${volumenSesion.toInt()} kg", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(text = "Tocar para detalle ➔", color = NaranjaAcento, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (mostrarDetalleDialog) {
        DetalleSesionDialog(sesion = sesion, onDismiss = { mostrarDetalleDialog = false })
    }
}

// ============================================================
// DIÁLOGO DETALLE QUIRÚRGICO DE LA SESIÓN (PAUTA VS LOGRADO)
// ============================================================

@Composable
fun DetalleSesionDialog(sesion: SesionEntrenamiento, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sesion.nombreRutina, fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                        Text(text = "Ejecutado: ${formatearFechaHistorial(sesion.fechaEjecucion)}", color = TextoSecundario, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = FondoOscuro)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(sesion.ejerciciosRealizados, key = { it.nombreEjercicio }) { ej ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(FondoOscuro.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ej.ordenSecuencia + 1}. ${ej.nombreEjercicio}",
                                    fontWeight = FontWeight.Bold,
                                    color = NaranjaAcento,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 15.sp
                                )

                                if (ej.fueSaltado) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Saltado", fontWeight = FontWeight.Bold, color = RojoIncompleto) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = RojoIncompleto.copy(alpha = 0.15f))
                                    )
                                }
                            }

                            if (ej.fueSaltado) {
                                Text(
                                    text = "Justificación: ${ej.justificacionSalto.ifBlank { "Sin motivo especificado" }}",
                                    fontStyle = FontStyle.Italic,
                                    color = RojoIncompleto,
                                    modifier = Modifier.padding(top = 4.dp),
                                    fontSize = 13.sp
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))

                                ej.seriesRealizadas.forEach { serie ->
                                    val repTarget = serie.repsTarget
                                    val repLogradas = serie.repeticionesLogradas
                                    val pesoTarget = serie.pesoTarget
                                    val pesoLogrado = serie.pesoKg

                                    // Comparativa matemática entre Pauta y Logrado
                                    val (colorMarcador, textoComparativa) = remember(repLogradas, repTarget, pesoLogrado, pesoTarget) {
                                        when {
                                            repTarget > 0 && repLogradas < repTarget -> RojoIncompleto to "Pauta: ${if(pesoTarget > 0) "${pesoTarget}kg x " else ""}$repTarget reps (Faltaron ${repTarget - repLogradas})"
                                            repTarget > 0 && repLogradas > repTarget -> VerdeExito to "Pauta: ${if(pesoTarget > 0) "${pesoTarget}kg x " else ""}$repTarget reps (+${repLogradas - repTarget} ¡Superado!)"
                                            else -> AzulCumplido to "Pauta: ${if(pesoTarget > 0) "${pesoTarget}kg x " else ""}${if(repTarget > 0) "$repTarget reps" else "Completado"}"
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "S${serie.numeroSerie} (${serie.tipoSerie.name.take(3)}): ",
                                                    color = TextoSecundario,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${pesoLogrado} kg × $repLogradas reps",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = textoComparativa,
                                                color = colorMarcador,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Badge de RPE de la serie
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(FondoTarjeta)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "RPE: ${serie.rpe?.toString() ?: "-"}",
                                                fontWeight = FontWeight.Black,
                                                color = NaranjaAcento,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                if (ej.notasAtleta.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = TextoSecundario,
                                            modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Nota: ${ej.notasAtleta}",
                                            fontStyle = FontStyle.Italic,
                                            color = TextoSecundario,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FondoOscuro)
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cerrar Detalle", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// COMPONENTES AUXILIARES PRESERVADOS
// ============================================================

@Composable
fun MetricItem(valor: String, subetiqueta: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 18.sp)
        Text(text = subetiqueta, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        Text(text = etiqueta, color = TextoSecundario, fontSize = 10.sp)
    }
}

@Composable
fun KpiSection(rachaSemana: List<Pair<String, Boolean>>, entrenosMes: Int, volumenSemanal: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Consistencia (Últimos 7 días)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rachaSemana.forEach { (dia, entrenado) ->
                        DiaRachaItem(dia = dia, entrenado = entrenado)
                    }
                }

                HorizontalDivider(color = FondoOscuro)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    MiniKpi(valor = "$entrenosMes", etiqueta = "Sesiones del mes")
                    MiniKpi(valor = "${volumenSemanal.toInt()} kg", etiqueta = "Carga Semanal total")
                }
            }
        }
    }
}

@Composable
fun DiaRachaItem(dia: String, entrenado: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(if (entrenado) NaranjaAcento else FondoOscuro),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dia,
                color = if (entrenado) FondoOscuro else TextoSecundario,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun MiniKpi(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
        Text(text = etiqueta, color = TextoSecundario, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolucionEjercicioSection(
    ejerciciosDisponibles: List<String>,
    historialEjercicio: List<DetalleEjercicioUI>,
    onEjercicioSeleccionado: (String) -> Unit
) {
    var expandirMenu by rememberSaveable { mutableStateOf(false) }
    var ejercicioActual by remember(ejerciciosDisponibles) {
        mutableStateOf(ejerciciosDisponibles.firstOrNull() ?: "Seleccionar Ejercicio")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Historial por Ejercicio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        if (ejerciciosDisponibles.isEmpty()) {
            Text("No se registran patrones de fuerza aún.", color = TextoSecundario, fontSize = 14.sp)
            return
        }

        Card(
            onClick = { expandirMenu = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = ejercicioActual, fontWeight = FontWeight.Bold, color = Color.White)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar", tint = NaranjaAcento)
            }

            DropdownMenu(expanded = expandirMenu, onDismissRequest = { expandirMenu = false }, modifier = Modifier.background(FondoTarjeta)) {
                ejerciciosDisponibles.forEach { ejercicio ->
                    DropdownMenuItem(
                        text = { Text(ejercicio, color = Color.White, fontWeight = FontWeight.Medium) },
                        onClick = {
                            ejercicioActual = ejercicio
                            expandirMenu = false
                            onEjercicioSeleccionado(ejercicio)
                        }
                    )
                }
            }
        }

        if (historialEjercicio.isNotEmpty()) {
            GraficaProgresoEjercicio(historialEjercicio = historialEjercicio)
        }

        historialEjercicio.forEach { registro ->
            key(registro.fechaFormat) {
                RegistroEjercicioCard(registro)
            }
        }
    }
}

@Composable
fun RegistroEjercicioCard(registro: DetalleEjercicioUI) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(registro.fechaFormat, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(registro.nombreRutina, color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            registro.detalle.seriesRealizadas.forEach { serie ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Serie ${serie.numeroSerie}", color = TextoSecundario, fontSize = 14.sp)
                    Text("${serie.pesoKg} kg x ${serie.repeticionesLogradas} reps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TarjetaRecordPersonal(record: RecordPersonalUI) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NaranjaAcento.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = "Récord", tint = Color(0xFFFFD700), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.nombreEjercicio,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(text = "Marcado el: ${record.fechaFormateada}", color = TextoSecundario, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${if(record.pesoMaximo % 1.0 == 0.0) record.pesoMaximo.toInt() else record.pesoMaximo} kg", fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 20.sp)
                Text(text = "x ${record.repeticiones} reps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun GraficaProgresoEjercicio(historialEjercicio: List<DetalleEjercicioUI>) {
    val ultimosRegistros = remember(historialEjercicio) { historialEjercicio.take(6).reversed() }

    val datosVolumen = remember(ultimosRegistros) {
        ultimosRegistros.map { reg ->
            reg.detalle.seriesRealizadas.sumOf { s -> s.pesoKg * s.repeticionesLogradas }.toFloat()
        }
    }

    val etiquetasFechas = remember(ultimosRegistros) {
        ultimosRegistros.map { reg ->
            val partes = reg.fechaFormat.split(" ")
            if (partes.size >= 2) "${partes[0]} ${partes[1]}" else reg.fechaFormat
        }
    }

    if (datosVolumen.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Curva de Rendimiento", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("Volumen por sesión (kg × reps)", color = TextoSecundario, fontSize = 12.sp)
                }
                Text(text = "${datosVolumen.lastOrNull()?.toInt() ?: 0} kg", fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val maxVal = datosVolumen.maxOrNull() ?: 1f
            val minVal = datosVolumen.minOrNull() ?: 0f
            val rango = if (maxVal == minVal) 1f else maxVal - minVal

            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val ancho = size.width
                val alto = size.height
                val espacioX = ancho / (if (datosVolumen.size > 1) datosVolumen.size - 1 else 1)

                val puntos = datosVolumen.mapIndexed { i, valor ->
                    val x = i * espacioX
                    val y = alto - ((valor - minVal) / rango) * (alto * 0.8f) - (alto * 0.1f)
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                if (puntos.size > 1) {
                    val pathFondo = Path().apply {
                        moveTo(puntos.first().x, alto)
                        puntos.forEach { lineTo(it.x, it.y) }
                        lineTo(puntos.last().x, alto)
                        close()
                    }
                    drawPath(path = pathFondo, brush = Brush.verticalGradient(colors = listOf(NaranjaAcento.copy(alpha = 0.25f), Color.Transparent)))

                    val pathLinea = Path().apply {
                        moveTo(puntos.first().x, puntos.first().y)
                        for (i in 1 until puntos.size) { lineTo(puntos[i].x, puntos[i].y) }
                    }
                    drawPath(path = pathLinea, color = NaranjaAcento, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }

                puntos.forEach { punto ->
                    drawCircle(color = FondoOscuro, radius = 5.dp.toPx(), center = punto)
                    drawCircle(color = NaranjaAcento, radius = 3.5.dp.toPx(), center = punto)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                etiquetasFechas.forEach { fecha ->
                    Text(text = fecha, color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}