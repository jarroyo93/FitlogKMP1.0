package dev.josearroyo.fitlog.ui.dashboard.atleta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.*
import dev.josearroyo.fitlog.viewmodel.atleta.EntrenarViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenarScreen(
    atletaId: String,
    rutinaId: String,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    // 🟢 CORREGIDO: Inicialización explícita compatible con la arquitectura DI KMP
    val viewModel: EntrenarViewModel = viewModel { EntrenarViewModel() }
    val state by viewModel.state.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    LaunchedEffect(rutinaId) {
        viewModel.cargarRutina(atletaId, rutinaId)
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinish()
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text(state.rutina?.nombreRutina ?: "Entrenamiento", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = NaranjaAcento) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            if (state.rutina != null) {
                Surface(color = FondoOscuro, tonalElevation = 0.dp) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp)
                    ) {
                        Button(
                            onClick = { mostrarConfirmacion = true },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Terminar Entrenamiento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else {
            if (mostrarConfirmacion) {
                AlertDialog(
                    containerColor = FondoTarjeta,
                    onDismissRequest = { mostrarConfirmacion = false },
                    title = { Text("Finalizar Entrenamiento", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = { Text("¿Estás seguro de que deseas terminar y guardar este entrenamiento? Revisa que todos los pesos y repeticiones estén correctos.", color = TextoSecundario) },
                    confirmButton = {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                            onClick = {
                                mostrarConfirmacion = false
                                viewModel.terminarEntrenamiento(atletaId)
                            }
                        ) { Text("Sí, terminar", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarConfirmacion = false }) {
                            Text("Revisar de nuevo", color = NaranjaAcento)
                        }
                    }
                )
            }

            if (state.error != null) {
                AlertDialog(
                    containerColor = FondoTarjeta,
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Atención", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = { Text(state.error!!, color = TextoSecundario) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("Entendido", color = NaranjaAcento) }
                    }
                )
            }

            val rutina = state.rutina
            val diaActual = state.diaActual
            val sesion = state.sesionEnProgreso

            if (rutina != null && diaActual != null && sesion.ejerciciosRealizados.isNotEmpty()) {

                // 🟢 CORREGIDO: Aislamos el ordenamiento visual para no romper el mapeo de datos con el ViewModel
                val ejerciciosOrdenados = remember(diaActual.ejercicios) {
                    diaActual.ejercicios.sortedBy { it.ordenSecuencia }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FondoOscuro),
                    contentPadding = PaddingValues(
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        if (rutina.notasEntrenador.isNotBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.3f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = NaranjaAcento)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Indicación del Coach: ${rutina.notasEntrenador}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        var expandedDiaSelector by remember { mutableStateOf(false) }

                        Card(
                            onClick = { expandedDiaSelector = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Bloque de trabajo activo:", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                                    Text("Día ${diaActual.ordenSecuencia}: ${diaActual.nombreDia}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar Día", tint = NaranjaAcento)
                            }

                            DropdownMenu(
                                expanded = expandedDiaSelector,
                                onDismissRequest = { expandedDiaSelector = false },
                                modifier = Modifier.background(FondoTarjeta)
                            ) {
                                rutina.diasEntrenamiento.sortedBy { it.ordenSecuencia }.forEach { diaOpcion ->
                                    DropdownMenuItem(
                                        text = { Text("Día ${diaOpcion.ordenSecuencia}: ${diaOpcion.nombreDia}", color = Color.White) },
                                        onClick = {
                                            expandedDiaSelector = false
                                            viewModel.cambiarDiaSeleccionado(diaOpcion.idDia)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Iteración segura basada en el árbol de dependencias estable de claves hash
                    itemsIndexed(ejerciciosOrdenados, key = { _, ej -> ej.nombre }) { _, asignado ->
                        // 🟢 CORREGIDO: Buscamos el índice transaccional real para mantener la integridad de los datos
                        val realIndex = remember(diaActual.ejercicios, asignado) {
                            diaActual.ejercicios.indexOf(asignado)
                        }

                        val realizado = sesion.ejerciciosRealizados.getOrNull(realIndex)
                        if (realizado != null) {
                            EjercicioInteractivoCard(
                                ejercicioAsignado = asignado,
                                ejercicioRealizado = realizado,
                                onActualizarSerie = { serieIndex, peso, reps -> viewModel.actualizarSerie(realIndex, serieIndex, peso, reps) },
                                onActualizarRpe = { serieIndex, rpe -> viewModel.actualizarRpe(realIndex, serieIndex, rpe) },
                                onActualizarNota = { nota -> viewModel.actualizarNotaAtleta(realIndex, nota) },
                                onToggleSaltar = { fue, just -> viewModel.toggleSaltarEjercicio(realIndex, fue, just) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioInteractivoCard(
    ejercicioAsignado: EjercicioAsignado,
    ejercicioRealizado: EjercicioRealizado,
    onActualizarSerie: (Int, Double, Int) -> Unit,
    onActualizarRpe: (Int, Int) -> Unit,
    onActualizarNota: (String) -> Unit,
    onToggleSaltar: (Boolean, String) -> Unit
) {
    var mostrarInfoEntrenador by remember { mutableStateOf(false) }
    var mostrarRpeSheet by remember { mutableStateOf(false) }
    var serieSeleccionadaParaRpe by remember { mutableStateOf(-1) }
    var rpeActualSeleccionado by remember { mutableStateOf(8) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(ejercicioAsignado.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NaranjaAcento, modifier = Modifier.weight(1f))
                IconButton(onClick = { mostrarInfoEntrenador = true }) { Icon(Icons.Default.Info, null, tint = NaranjaAcento) }
            }

            Text("Objetivo: ${ejercicioAsignado.seriesPrescritas.size} series prescritas", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FondoOscuro)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("¿Omitir ejercicio en esta sesión?", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Switch(
                    checked = ejercicioRealizado.fueSaltado,
                    onCheckedChange = { isChecked ->
                        onToggleSaltar(isChecked, if (isChecked) ejercicioRealizado.justificacionSalto else "")
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = NaranjaAcento.copy(alpha = 0.4f))
                )
            }

            if (ejercicioRealizado.fueSaltado) {
                OutlinedTextField(
                    value = ejercicioRealizado.justificacionSalto,
                    onValueChange = { onToggleSaltar(true, it) },
                    label = { Text("Justificación del cambio", color = TextoSecundario) },
                    placeholder = { Text("Ej. Molestia muscular, máquina ocupada...", color = TextoSecundario.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedContainerColor = FondoOscuro, focusedContainerColor = FondoOscuro)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SERIE", modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, fontWeight = FontWeight.Bold)
                    Text("PESO (KG)", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, fontWeight = FontWeight.Bold)
                    Text("REPS", modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, fontWeight = FontWeight.Bold)
                    Text("RPE", modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, fontWeight = FontWeight.Bold)
                }

                ejercicioRealizado.seriesRealizadas.forEachIndexed { sIndex, serie ->
                    val colorTextoSerie = when (serie.tipoSerie) {
                        TipoSerie.APROXIMACION -> Color(0xFF4FC3F7)
                        TipoSerie.DROP_SET -> Color(0xFFE57373)
                        TipoSerie.FALLO -> Color(0xFFBA68C8)
                        TipoSerie.REST_PAUSE -> Color(0xFFAED581)
                        TipoSerie.EFECTIVA -> Color.White
                    }

                    val textoSerie = when (serie.tipoSerie) {
                        TipoSerie.APROXIMACION -> "Aprox"
                        TipoSerie.DROP_SET -> "Drop"
                        TipoSerie.FALLO -> "Fallo"
                        TipoSerie.REST_PAUSE -> "R-P"
                        TipoSerie.EFECTIVA -> "${serie.numeroSerie}"
                    }

                    // 🟢 CORREGIDO: Envoltura bajo clave de ejecución única para blindar la retención de foco táctil del sistema
                    key(sIndex) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(FondoOscuro.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(textoSerie, modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Black, color = colorTextoSerie, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

                            OutlinedTextField(
                                value = if (serie.pesoKg <= 0.0) "" else if (serie.pesoKg % 1.0 == 0.0) serie.pesoKg.toInt().toString() else serie.pesoKg.toString(),
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*[.,]?\\d*\$"))) {
                                        onActualizarSerie(sIndex, newValue.replace(",", ".").toDoubleOrNull() ?: 0.0, serie.repeticionesLogradas)
                                    }
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp).heightIn(min = 56.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.2f), focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta)
                            )

                            OutlinedTextField(
                                value = if (serie.repeticionesLogradas <= 0) "" else serie.repeticionesLogradas.toString(),
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        onActualizarSerie(sIndex, serie.pesoKg, newValue.toIntOrNull() ?: 0)
                                    }
                                },
                                modifier = Modifier.weight(0.8f).padding(horizontal = 2.dp).heightIn(min = 56.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.2f), focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta)
                            )

                            val rpeColor = obtenerColorRpe(serie.rpe ?: 0)
                            ElevatedFilterChip(
                                selected = serie.rpe != null,
                                onClick = { serieSeleccionadaParaRpe = sIndex; rpeActualSeleccionado = serie.rpe ?: 8; mostrarRpeSheet = true },
                                label = { Text(if (serie.rpe != null) "${serie.rpe}" else "-", fontWeight = FontWeight.Black) },
                                modifier = Modifier.weight(0.8f).padding(start = 4.dp),
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = rpeColor.copy(alpha = 0.2f),
                                    selectedLabelColor = rpeColor,
                                    containerColor = FondoTarjeta,
                                    labelColor = TextoSecundario
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = ejercicioRealizado.notasAtleta,
                    onValueChange = { onActualizarNota(it) },
                    label = { Text("Sensaciones o notas de carga", color = TextoSecundario) },
                    placeholder = { Text("Ej. RPE alto, se sintió sólido...", color = TextoSecundario.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedContainerColor = FondoOscuro, focusedContainerColor = FondoOscuro)
                )
            }
        }
    }

    if (mostrarInfoEntrenador) {
        AlertDialog(
            containerColor = FondoTarjeta,
            onDismissRequest = { mostrarInfoEntrenador = false },
            title = { Text("Prescripción del Ciclo", color = NaranjaAcento, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ejercicioAsignado.seriesPrescritas.forEach { serie ->
                        val etiquetaTipo = when (serie.tipo) {
                            TipoSerie.EFECTIVA -> "Efectiva"
                            TipoSerie.APROXIMACION -> "Aproximación"
                            TipoSerie.DROP_SET -> "Drop Set"
                            TipoSerie.FALLO -> "Al Fallo"
                            TipoSerie.REST_PAUSE -> "Rest-Pause"
                        }
                        Text("• Serie ${serie.numeroSerie}: $etiquetaTipo - ${serie.repeticiones} reps meta", color = Color.White)
                    }
                    if(ejercicioAsignado.notasEspecificas.isNotBlank()){
                        HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Nota técnica: ${ejercicioAsignado.notasEspecificas}", fontStyle = FontStyle.Italic, color = TextoSecundario)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarInfoEntrenador = false }) { Text("Cerrar", color = NaranjaAcento, fontWeight = FontWeight.Bold) } }
        )
    }

    if (mostrarRpeSheet) {
        SelectorRpeBottomSheet(
            rpeInicial = rpeActualSeleccionado,
            onDismiss = { mostrarRpeSheet = false },
            onRpeSeleccionado = { nuevoRpe -> onActualizarRpe(serieSeleccionadaParaRpe, nuevoRpe); mostrarRpeSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorRpeBottomSheet(rpeInicial: Int, onDismiss: () -> Unit, onRpeSeleccionado: (Int) -> Unit) {
    var sliderValue by remember { mutableStateOf(rpeInicial.toFloat()) }
    val rpeEntero = sliderValue.toInt()
    val colorDinamico = obtenerColorRpe(rpeEntero)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FondoTarjeta) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Esfuerzo Percibido (RPE)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            val descripcion = when(rpeEntero) {
                10 -> "Máximo esfuerzo. RIR 0 (Fallo Absoluto)."
                9 -> "Muy pesado. RIR 1 (Quedaba 1 repetición)."
                8 -> "Pesado. RIR 2 (Quedaban 2 repeticiones)."
                7 -> "Exigente. RIR 3 (Quedaban 3 repeticiones)."
                5, 6 -> "Velocidad media. Carga de calentamiento."
                else -> "Muy ligero. Activación neuromuscular."
            }

            Text(descripcion, style = MaterialTheme.typography.bodyMedium, color = colorDinamico, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "$rpeEntero", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = colorDinamico)

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(thumbColor = colorDinamico, activeTrackColor = colorDinamico, inactiveTrackColor = FondoOscuro),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )

            Button(
                onClick = { onRpeSeleccionado(rpeEntero) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorDinamico, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Fijar Intensidad RPE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun obtenerColorRpe(rpe: Int): Color {
    return when (rpe) {
        10 -> Color(0xFFEF5350)
        9 -> Color(0xFFFF7043)
        8 -> Color(0xFFFFB74D)
        7 -> Color(0xFFFFF176)
        5, 6 -> Color(0xFF81C784)
        else -> Color(0xFF90A4AE)
    }
}