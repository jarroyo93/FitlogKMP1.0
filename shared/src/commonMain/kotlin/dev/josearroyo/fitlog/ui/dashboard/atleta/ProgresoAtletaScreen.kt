package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import dev.josearroyo.fitlog.data.model.ModoCiclo
import dev.josearroyo.fitlog.data.model.SesionEntrenamiento
import dev.josearroyo.fitlog.viewmodel.atleta.ProgresoAtletaViewModel
import dev.josearroyo.fitlog.viewmodel.atleta.DetalleEjercicioUI
import dev.josearroyo.fitlog.viewmodel.atleta.ImpactoFisicoUI
import dev.josearroyo.fitlog.viewmodel.atleta.RecordPersonalUI
import dev.josearroyo.fitlog.viewmodel.atleta.ResumenCicloUI
import dev.josearroyo.fitlog.formatearFechaHistorial
import dev.josearroyo.fitlog.extraerAnoDeFecha
import dev.josearroyo.fitlog.extraerMesDeFecha
import dev.josearroyo.fitlog.getCurrentTimeMillis
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
    var tabSeleccionada by rememberSaveable { mutableStateOf(1) }
    val titulosTabs = listOf("Evolución", "Diario de Ciclos", "Récords")

    var filtroDiaRutina by rememberSaveable { mutableStateOf("TODOS") }
    var mostrarModalHistorico by rememberSaveable { mutableStateOf(false) }

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
                        if (state.cicloSeleccionado != null) {
                            item {
                                TarjetaEncabezadoCiclo(
                                    ciclo = state.cicloSeleccionado!!,
                                    resumen = state.resumenCicloSeleccionado,
                                    impacto = state.resumenCicloSeleccionado.impactoFisico,
                                    totalCiclosHistoricos = state.historialCiclos.size,
                                    onAbrirHistorico = { mostrarModalHistorico = true }
                                )
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Diario de Entrenamientos",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )

                                        Surface(
                                            color = FondoTarjeta,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "${sesionesMostrar.size} Registros",
                                                color = NaranjaAcento,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (rutinasDelCiclo.size > 2) {
                                        FiltroRutinasCicloRow(
                                            rutinas = rutinasDelCiclo,
                                            rutinaSeleccionada = filtroDiaRutina,
                                            onRutinaSeleccionada = { filtroDiaRutina = it }
                                        )
                                    }
                                }
                            }
                        }

                        if (mostrarModalHistorico) {
                            item {
                                ModalBottomSheetHistoricoCiclos(
                                    ciclos = state.historialCiclos,
                                    cicloSeleccionado = state.cicloSeleccionado,
                                    onCicloSeleccionado = { ciclo ->
                                        filtroDiaRutina = "TODOS"
                                        viewModel.seleccionarCiclo(ciclo)
                                        mostrarModalHistorico = false
                                    },
                                    onDismiss = { mostrarModalHistorico = false }
                                )
                            }
                        }

                        if (sesionesMostrar.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(FondoTarjeta)
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No hay sesiones registradas en este ciclo.", color = TextoSecundario, fontSize = 13.sp)
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
// PESTAÑA DE EVOLUCIÓN: SELECCIÓN Y ANÁLISIS DE FUERZA
// ============================================================

@Composable
fun EvolucionEjercicioSection(
    ejerciciosDisponibles: List<String>,
    historialEjercicio: List<DetalleEjercicioUI>,
    onEjercicioSeleccionado: (String) -> Unit
) {
    var mostrarModalSeleccion by rememberSaveable { mutableStateOf(false) }
    var ejercicioActual by remember(ejerciciosDisponibles) {
        mutableStateOf(ejerciciosDisponibles.firstOrNull() ?: "Seleccionar Ejercicio")
    }
    var tipoMetrica by rememberSaveable { mutableStateOf("PESO_MAXIMO") } // "PESO_MAXIMO" o "VOLUMEN"

    val registrosOrdenados = remember(historialEjercicio) {
        historialEjercicio.reversed() // Orden cronológico (antiguo a reciente)
    }

    // Cálculo de Métricas Clave de Evolución
    val pesoInicial = remember(registrosOrdenados) {
        registrosOrdenados.firstOrNull()?.detalle?.seriesRealizadas?.maxOfOrNull { it.pesoKg } ?: 0.0
    }
    val pesoActual = remember(registrosOrdenados) {
        registrosOrdenados.lastOrNull()?.detalle?.seriesRealizadas?.maxOfOrNull { it.pesoKg } ?: 0.0
    }
    val deltaPeso = pesoActual - pesoInicial
    val porcentajeMejora = if (pesoInicial > 0) (deltaPeso / pesoInicial) * 100.0 else 0.0

    val maximo1RMEstimado = remember(registrosOrdenados) {
        registrosOrdenados.flatMap { reg ->
            reg.detalle.seriesRealizadas.map { it.pesoKg * (1.0 + it.repeticionesLogradas / 30.0) }
        }.maxOrNull() ?: 0.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = "Evolución por Ejercicio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

        if (ejerciciosDisponibles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FondoTarjeta)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay ejercicios registrados en el historial.", color = TextoSecundario, fontSize = 13.sp)
            }
            return
        }

        // 1. TARJETA DE SELECCIÓN DE EJERCICIO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { mostrarModalSeleccion = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = NaranjaAcento.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = clasificarGrupoMuscular(ejercicioActual).uppercase(),
                            color = NaranjaAcento,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ejercicioActual,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = { mostrarModalSeleccion = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento),
                    border = BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buscar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. KPIS DE PROGRESO DE FUERZA
        if (registrosOrdenados.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = FondoTarjeta,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Carga Máxima", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${if (pesoActual % 1.0 == 0.0) pesoActual.toInt() else pesoActual} kg",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        if (deltaPeso != 0.0 && registrosOrdenados.size > 1) {
                            val signo = if (deltaPeso > 0) "+" else ""
                            Text(
                                text = "$signo${if (deltaPeso % 1.0 == 0.0) deltaPeso.toInt() else ((deltaPeso * 10).roundToInt() / 10.0)} kg (${((porcentajeMejora * 10).roundToInt() / 10.0)}%)",
                                color = if (deltaPeso > 0) VerdeExito else RojoIncompleto,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        } else {
                            Text("Registro base", color = TextoSecundario, fontSize = 11.sp)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = FondoTarjeta,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("1RM Estimado Pico", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${maximo1RMEstimado.roundToInt()} kg",
                            fontWeight = FontWeight.Black,
                            color = NaranjaAcento,
                            fontSize = 18.sp
                        )
                        Text("Fuerza Teórica Max", color = TextoSecundario, fontSize = 11.sp)
                    }
                }
            }

            // 3. SELECTOR DE MÉTRICA DE GRÁFICA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tipoMetrica == "PESO_MAXIMO",
                    onClick = { tipoMetrica = "PESO_MAXIMO" },
                    label = { Text("Peso Máximo (kg)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaranjaAcento.copy(alpha = 0.2f),
                        selectedLabelColor = NaranjaAcento
                    )
                )

                FilterChip(
                    selected = tipoMetrica == "VOLUMEN",
                    onClick = { tipoMetrica = "VOLUMEN" },
                    label = { Text("Volumen Total (kg)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaranjaAcento.copy(alpha = 0.2f),
                        selectedLabelColor = NaranjaAcento
                    )
                )
            }

            // 4. GRÁFICA DE EVOLUCIÓN
            GraficaProgresoEjercicio(
                historialEjercicio = historialEjercicio,
                tipoMetrica = tipoMetrica
            )
        }

        // 5. LISTA DE REGISTROS DEL EJERCICIO
        Text(
            text = "Historial de Ejecución",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        historialEjercicio.forEach { registro ->
            key(registro.fechaFormat) {
                RegistroEjercicioCard(registro)
            }
        }
    }

    // BottomSheet de Selección de Ejercicio
    if (mostrarModalSeleccion) {
        ModalBottomSheetSeleccionEjercicio(
            ejerciciosDisponibles = ejerciciosDisponibles,
            ejercicioSeleccionado = ejercicioActual,
            onEjercicioSeleccionado = { seleccionado ->
                ejercicioActual = seleccionado
                onEjercicioSeleccionado(seleccionado)
                mostrarModalSeleccion = false
            },
            onDismiss = { mostrarModalSeleccion = false }
        )
    }
}

/**
 * Modal BottomSheet para Buscar y Filtrar Ejercicios por Grupo Muscular.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetSeleccionEjercicio(
    ejerciciosDisponibles: List<String>,
    ejercicioSeleccionado: String,
    onEjercicioSeleccionado: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var grupoSeleccionado by rememberSaveable { mutableStateOf("TODOS") }
    var textoBusqueda by rememberSaveable { mutableStateOf("") }

    val gruposMusculares = remember(ejerciciosDisponibles) {
        listOf("TODOS", "Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core", "Otros")
    }

    val ejerciciosFiltrados = remember(ejerciciosDisponibles, grupoSeleccionado, textoBusqueda) {
        ejerciciosDisponibles.filter { ej ->
            val coincideGrupo = grupoSeleccionado == "TODOS" || clasificarGrupoMuscular(ej) == grupoSeleccionado
            val coincideTexto = textoBusqueda.isBlank() || ej.contains(textoBusqueda, ignoreCase = true)
            coincideGrupo && coincideTexto
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FondoTarjeta,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextoSecundario) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Seleccionar Ejercicio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Filtra por músculo o busca por nombre", fontSize = 12.sp, color = TextoSecundario)
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                }
            }

            // BUSCADOR DE TEXTO
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                placeholder = { Text("Buscar ejercicio...", color = TextoSecundario, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaranjaAcento) },
                trailingIcon = {
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(onClick = { textoBusqueda = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextoSecundario)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.3f),
                    focusedContainerColor = FondoOscuro,
                    unfocusedContainerColor = FondoOscuro
                ),
                singleLine = true
            )

            // FILTRO DE GRUPOS MUSCULARES
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(gruposMusculares) { grupo ->
                    val esSeleccionado = grupo == grupoSeleccionado
                    FilterChip(
                        selected = esSeleccionado,
                        onClick = { grupoSeleccionado = grupo },
                        label = { Text(grupo, fontSize = 11.sp, fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NaranjaAcento,
                            selectedLabelColor = FondoOscuro,
                            containerColor = FondoOscuro,
                            labelColor = TextoSecundario
                        )
                    )
                }
            }

            HorizontalDivider(color = FondoOscuro)

            if (ejerciciosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron ejercicios.", color = TextoSecundario, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ejerciciosFiltrados) { ej ->
                        val esActivo = ej == ejercicioSeleccionado
                        val grupo = clasificarGrupoMuscular(ej)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEjercicioSeleccionado(ej) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (esActivo) NaranjaAcento.copy(alpha = 0.15f) else FondoOscuro
                            ),
                            border = if (esActivo) BorderStroke(1.5.dp, NaranjaAcento) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ej,
                                        fontWeight = FontWeight.Bold,
                                        color = if (esActivo) NaranjaAcento else Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = grupo,
                                        color = TextoSecundario,
                                        fontSize = 11.sp
                                    )
                                }

                                if (esActivo) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = NaranjaAcento)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clasificador rápido de grupos musculares según palabras clave.
 */
fun clasificarGrupoMuscular(nombreEjercicio: String): String {
    val nombre = nombreEjercicio.lowercase()
    return when {
        nombre.contains("pecho") || nombre.contains("press banca") || nombre.contains("aperturas") || nombre.contains("crossover") || nombre.contains("fondos") || nombre.contains("chest") || nombre.contains("inclinado") || nombre.contains("declinado") -> "Pecho"
        nombre.contains("espalda") || nombre.contains("remo") || nombre.contains("jalón") || nombre.contains("jalon") || nombre.contains("dominadas") || nombre.contains("pulldown") || nombre.contains("dorsal") || nombre.contains("lumbares") -> "Espalda"
        nombre.contains("sentadilla") || nombre.contains("prensa") || nombre.contains("cuadriceps") || nombre.contains("zancada") || nombre.contains("búlgar") || nombre.contains("bulgar") || nombre.contains("peso muerto") || nombre.contains("isquios") || nombre.contains("femoral") || nombre.contains("pantorrilla") || nombre.contains("gemelos") -> "Piernas"
        nombre.contains("hombro") || nombre.contains("press militar") || nombre.contains("elevaciones") || nombre.contains("pájaro") || nombre.contains("pajaro") || nombre.contains("deltoides") || nombre.contains("face pull") -> "Hombros"
        nombre.contains("bíceps") || nombre.contains("biceps") || nombre.contains("curl") || nombre.contains("tríceps") || nombre.contains("triceps") || nombre.contains("copa") || nombre.contains("patada") || nombre.contains("antebrazo") -> "Brazos"
        nombre.contains("abs") || nombre.contains("abdominal") || nombre.contains("plancha") || nombre.contains("core") || nombre.contains("rueda") -> "Core"
        else -> "Otros"
    }
}

/**
 * Gráfica de Progreso de Fuerza y Volumen con Valores Claros.
 */
@Composable
fun GraficaProgresoEjercicio(
    historialEjercicio: List<DetalleEjercicioUI>,
    tipoMetrica: String
) {
    val ultimosRegistros = remember(historialEjercicio) { historialEjercicio.take(6).reversed() }

    val datosMétricas = remember(ultimosRegistros, tipoMetrica) {
        ultimosRegistros.map { reg ->
            if (tipoMetrica == "PESO_MAXIMO") {
                reg.detalle.seriesRealizadas.maxOfOrNull { it.pesoKg }?.toFloat() ?: 0f
            } else {
                reg.detalle.seriesRealizadas.sumOf { s -> s.pesoKg * s.repeticionesLogradas }.toFloat()
            }
        }
    }

    val etiquetasFechas = remember(ultimosRegistros) {
        ultimosRegistros.map { reg ->
            val partes = reg.fechaFormat.split(" ")
            if (partes.size >= 2) "${partes[0]} ${partes[1]}" else reg.fechaFormat
        }
    }

    if (datosMétricas.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                        text = if (tipoMetrica == "PESO_MAXIMO") "Evolución de Carga Máxima" else "Evolución de Volumen",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (tipoMetrica == "PESO_MAXIMO") "Kilos levantados en la serie top" else "Volumen total (kg × reps)",
                        color = TextoSecundario,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "${datosMétricas.lastOrNull()?.toInt() ?: 0} ${if (tipoMetrica == "PESO_MAXIMO") "kg" else "kg total"}",
                    fontWeight = FontWeight.Black,
                    color = NaranjaAcento,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // VALORES ENCIMA DE CADA PUNTO DE LA GRÁFICA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                datosMétricas.forEachIndexed { idx, valor ->
                    val esMaximo = valor == datosMétricas.maxOrNull()
                    Surface(
                        color = if (esMaximo) NaranjaAcento.copy(alpha = 0.2f) else FondoOscuro,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${valor.toInt()}kg",
                            color = if (esMaximo) NaranjaAcento else Color.White,
                            fontWeight = if (esMaximo) FontWeight.Black else FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CANVAS DE LA CURVA
            val maxVal = datosMétricas.maxOrNull() ?: 1f
            val minVal = datosMétricas.minOrNull() ?: 0f
            val rango = if (maxVal == minVal) 1f else maxVal - minVal

            Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                val ancho = size.width
                val alto = size.height
                val espacioX = ancho / (if (datosMétricas.size > 1) datosMétricas.size - 1 else 1)

                val puntos = datosMétricas.mapIndexed { i, valor ->
                    val x = i * espacioX
                    val y = alto - ((valor - minVal) / rango) * (alto * 0.75f) - (alto * 0.12f)
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

            Spacer(modifier = Modifier.height(8.dp))

            // FECHAS EN EL EJE X
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                etiquetasFechas.forEach { fecha ->
                    Text(text = fecha, color = TextoSecundario, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tarjeta de Registro con Resalte de Serie Máxima (Serie Top).
 */
@Composable
fun RegistroEjercicioCard(registro: DetalleEjercicioUI) {
    val serieTop = remember(registro.detalle.seriesRealizadas) {
        registro.detalle.seriesRealizadas.maxByOrNull { it.pesoKg * it.repeticionesLogradas }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(registro.fechaFormat, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(registro.nombreRutina, color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            serieTop?.let { top ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = NaranjaAcento.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "🔥 Serie Top: ${top.pesoKg} kg × ${top.repeticionesLogradas} reps",
                        color = NaranjaAcento,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            registro.detalle.seriesRealizadas.forEach { serie ->
                val esTop = serie.numeroSerie == serieTop?.numeroSerie
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Serie ${serie.numeroSerie} (${serie.tipoSerie.name.take(3)})",
                        color = if (esTop) Color.White else TextoSecundario,
                        fontSize = 13.sp,
                        fontWeight = if (esTop) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = "${serie.pesoKg} kg × ${serie.repeticionesLogradas} reps",
                        fontWeight = FontWeight.Bold,
                        color = if (esTop) NaranjaAcento else Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ============================================================
// COMPONENTES DE DIARIO DE CICLOS (HISTÓRICO)
// ============================================================

@Composable
fun TarjetaEncabezadoCiclo(
    ciclo: CicloEntrenamiento,
    resumen: ResumenCicloUI,
    impacto: ImpactoFisicoUI,
    totalCiclosHistoricos: Int,
    onAbrirHistorico: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Título y punto indicador de estado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (ciclo.estaActivo) "Ciclo Activo" else "Ciclo Histórico",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (ciclo.estaActivo) VerdeExito else TextoSecundario)
                        )
                    }

                    // 2. Badge de la modalidad (Ubicado en su propia línea para evitar compresión)
                    val modoTexto = if (ciclo.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL) "Calendario Semanal" else "Secuencial Rodante"
                    Surface(
                        color = NaranjaAcento.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = modoTexto,
                            color = NaranjaAcento,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 3. Rango de fechas del ciclo
                    Text(
                        text = "${formatearFechaHistorial(ciclo.fechaInicio)} — ${if (ciclo.fechaCierre > 0L) formatearFechaHistorial(ciclo.fechaCierre) else "Presente"}",
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onAbrirHistorico,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento),
                    border = BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (totalCiclosHistoricos > 1) "Histórico ($totalCiclosHistoricos)" else "Cambiar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiPill(
                    modifier = Modifier.weight(1f),
                    valor = "${resumen.porcentajeAsistencia.toInt()}%",
                    etiqueta = "Asistencia",
                    subetiqueta = "${resumen.sesionesCompletadas}/${resumen.metaSesiones} Días",
                    colorVal = VerdeExito
                )
                KpiPill(
                    modifier = Modifier.weight(1f),
                    valor = if (resumen.rpePromedio > 0.0) "${resumen.rpePromedio}" else "N/A",
                    etiqueta = "RPE Medio",
                    subetiqueta = "Esfuerzo",
                    colorVal = NaranjaAcento
                )
                KpiPill(
                    modifier = Modifier.weight(1f),
                    valor = (resumen.tonelajeTotalKg / 1000.0).let {
                        if (it >= 1.0) "${((it * 10).roundToInt() / 10.0)}t" else "${resumen.tonelajeTotalKg.toInt()}kg"
                    },
                    etiqueta = "Tonelaje",
                    subetiqueta = "Volumen",
                    colorVal = AzulCumplido
                )
            }

            if (impacto.tieneDatos) {
                HorizontalDivider(color = FondoOscuro)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "⚖️ Cambios Físicos del Ciclo",
                        fontWeight = FontWeight.Bold,
                        color = NaranjaAcento,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        impacto.deltaPeso?.let { delta ->
                            val signo = if (delta > 0) "+" else ""
                            Text(
                                text = "Peso: ${impacto.pesoInicial}kg ➔ ${impacto.pesoFinal}kg ($signo$delta kg)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        impacto.deltaAbdomen?.let { delta ->
                            val signo = if (delta > 0) "+" else ""
                            Text(
                                text = "Cintura: ${impacto.abdomenInicial}cm ➔ ${impacto.abdomenFinal}cm ($signo$delta cm)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * BottomSheet con Buscador/Filtro por AÑO y MES considerando rangos compartidos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetHistoricoCiclos(
    ciclos: List<CicloEntrenamiento>,
    cicloSeleccionado: CicloEntrenamiento?,
    onCicloSeleccionado: (CicloEntrenamiento) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var anoSeleccionado by rememberSaveable { mutableStateOf("TODOS") }
    var mesSeleccionado by rememberSaveable { mutableStateOf("TODOS") }

    val anosDisponibles = remember(ciclos) {
        val anos = ciclos.flatMap { ciclo ->
            val fin = if (ciclo.fechaCierre > 0L) ciclo.fechaCierre else getCurrentTimeMillis()
            listOf(extraerAnoDeFecha(ciclo.fechaInicio), extraerAnoDeFecha(fin))
        }.distinct().sortedDescending()
        listOf("TODOS") + anos
    }

    val mesesDisponibles = remember(ciclos, anoSeleccionado) {
        val ciclosDelAno = if (anoSeleccionado == "TODOS") ciclos
        else ciclos.filter { ciclo ->
            val fin = if (ciclo.fechaCierre > 0L) ciclo.fechaCierre else getCurrentTimeMillis()
            extraerAnoDeFecha(ciclo.fechaInicio) == anoSeleccionado || extraerAnoDeFecha(fin) == anoSeleccionado
        }

        val meses = ciclosDelAno.flatMap { ciclo ->
            val fin = if (ciclo.fechaCierre > 0L) ciclo.fechaCierre else getCurrentTimeMillis()
            listOf(extraerMesDeFecha(ciclo.fechaInicio), extraerMesDeFecha(fin))
        }.distinct()

        listOf("TODOS") + meses
    }

    val ciclosFiltrados = remember(ciclos, anoSeleccionado, mesSeleccionado) {
        ciclos.filter { ciclo ->
            val fin = if (ciclo.fechaCierre > 0L) ciclo.fechaCierre else getCurrentTimeMillis()

            val anoInicio = extraerAnoDeFecha(ciclo.fechaInicio)
            val anoFin = extraerAnoDeFecha(fin)
            val mesInicio = extraerMesDeFecha(ciclo.fechaInicio)
            val mesFin = extraerMesDeFecha(fin)

            val cumpleAno = anoSeleccionado == "TODOS" || anoInicio == anoSeleccionado || anoFin == anoSeleccionado
            val cumpleMes = mesSeleccionado == "TODOS" || mesInicio == mesSeleccionado || mesFin == mesSeleccionado

            cumpleAno && cumpleMes
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FondoTarjeta,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextoSecundario) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Buscar Ciclo Histórico",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Filtra por periodo de tiempo",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectorFiltroDropdown(
                    titulo = "Año",
                    opcionSeleccionada = anoSeleccionado,
                    opciones = anosDisponibles,
                    onOpcionSeleccionada = {
                        anoSeleccionado = it
                        mesSeleccionado = "TODOS"
                    },
                    modifier = Modifier.weight(1f)
                )

                SelectorFiltroDropdown(
                    titulo = "Mes",
                    opcionSeleccionada = mesSeleccionado,
                    opciones = mesesDisponibles,
                    onOpcionSeleccionada = { mesSeleccionado = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FondoOscuro)
            Spacer(modifier = Modifier.height(12.dp))

            if (ciclosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron ciclos para este periodo.",
                        color = TextoSecundario,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.60f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ciclosFiltrados, key = { it.id }) { ciclo ->
                        val esSeleccionado = ciclo.id == cicloSeleccionado?.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCicloSeleccionado(ciclo) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (esSeleccionado) NaranjaAcento.copy(alpha = 0.15f) else FondoOscuro
                            ),
                            border = if (esSeleccionado) BorderStroke(1.5.dp, NaranjaAcento) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Columna izquierda: Información del ciclo y fechas
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Ciclo de entrenamiento",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )

                                    Surface(
                                        color = Color(0xFFFF9F6D).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (ciclo.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL) "Calendario Semanal" else "Secuencial Rodante",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFF9F6D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${formatearFechaHistorial(ciclo.fechaInicio)} — ${if (ciclo.fechaCierre > 0L) formatearFechaHistorial(ciclo.fechaCierre) else "Presente"}",
                                        color = TextoSecundario,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Columna derecha: Estadísticas de días y asistencia
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFiltroDropdown(
    titulo: String,
    opcionSeleccionada: String,
    opciones: List<String>,
    onOpcionSeleccionada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (opcionSeleccionada == "TODOS") "Todos ($titulo)" else opcionSeleccionada,
            onValueChange = {},
            readOnly = true,
            label = { Text(titulo, color = TextoSecundario, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NaranjaAcento,
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
            modifier = Modifier.background(FondoTarjeta)
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (opcion == "TODOS") "Todos ($titulo)" else opcion,
                            color = Color.White,
                            fontWeight = if (opcion == opcionSeleccionada) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOpcionSeleccionada(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
fun KpiPill(
    modifier: Modifier = Modifier,
    valor: String,
    etiqueta: String,
    subetiqueta: String,
    colorVal: Color
) {
    Surface(
        modifier = modifier,
        color = FondoOscuro,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = valor, fontWeight = FontWeight.Black, color = colorVal, fontSize = 16.sp)
            Text(text = etiqueta, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(text = subetiqueta, color = TextoSecundario, fontSize = 10.sp)
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

// ============================================================
// TARJETA DE SESIÓN DEL DIARIO
// ============================================================

@Composable
fun TarjetaDiarioSesion(sesion: SesionEntrenamiento) {
    var mostrarDetalleDialog by rememberSaveable { mutableStateOf(false) }

    val volumenSesion = remember(sesion.ejerciciosRealizados) {
        sesion.ejerciciosRealizados.filter { !it.fueSaltado }.sumOf { ej ->
            ej.seriesRealizadas.sumOf { serie -> serie.pesoKg * serie.repeticionesLogradas }
        }
    }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatearFechaHistorial(sesion.fechaEjecucion),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = colorBadge.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = textoBadge,
                            color = colorBadge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = sesion.nombreRutina,
                    fontWeight = FontWeight.Black,
                    color = NaranjaAcento,
                    fontSize = 15.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Reps: $repsLogradas / ${if (repsMeta > 0) repsMeta else "-"}",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Volumen: ${volumenSesion.toInt()} kg",
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver Detalle",
                tint = NaranjaAcento,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (mostrarDetalleDialog) {
        DetalleSesionDialog(sesion = sesion, onDismiss = { mostrarDetalleDialog = false })
    }
}

// ============================================================
// DIÁLOGO DETALLE DE LA SESIÓN
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = sesion.ejerciciosRealizados,
                        key = { index, ej -> "${ej.nombreEjercicio}_$index" }
                    ) { index, ej ->
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
                                    val minT = serie.minTarget
                                    val maxT = serie.maxTarget
                                    val repLogradas = serie.repeticionesLogradas
                                    val pesoTarget = serie.pesoTarget
                                    val pesoLogrado = serie.pesoKg

                                    val (colorMarcador, textoComparativa) = remember(repLogradas, minT, maxT, pesoLogrado, pesoTarget) {
                                        val textoPautaReps = when {
                                            minT > 0 && maxT > 0 && minT != maxT -> "$minT-$maxT reps"
                                            maxT > 0 -> "$maxT reps"
                                            else -> "Reps libres"
                                        }
                                        val textoPeso = if (pesoTarget > 0) "${pesoTarget}kg x " else ""

                                        when {
                                            minT > 0 && repLogradas < minT -> RojoIncompleto to "Pauta: $textoPeso$textoPautaReps (Faltaron ${minT - repLogradas})"
                                            maxT > 0 && repLogradas > maxT -> VerdeExito to "Pauta: $textoPeso$textoPautaReps (+${repLogradas - maxT} sobre rango)"
                                            minT > 0 && repLogradas in minT..maxT -> VerdeExito to "Pauta: $textoPeso$textoPautaReps (¡En rango! ✔️)"
                                            else -> AzulCumplido to "Pauta: $textoPeso$textoPautaReps"
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
// COMPONENTES AUXILIARES DE RACHA Y RÉCORDS
// ============================================================

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

@Composable
fun TarjetaRecordPersonal(record: RecordPersonalUI) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NaranjaAcento.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.3f))
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