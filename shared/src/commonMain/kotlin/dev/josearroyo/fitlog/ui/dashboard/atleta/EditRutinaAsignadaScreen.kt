package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.PrescripcionSerie
import dev.josearroyo.fitlog.data.model.TipoSerie
import dev.josearroyo.fitlog.viewmodel.atleta.EditRutinaAsignadaViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRutinaAsignadaScreen(atletaId: String, rutinaId: String, onBack: () -> Unit) {
    val viewModel: EditRutinaAsignadaViewModel = viewModel { EditRutinaAsignadaViewModel() }
    val state by viewModel.state.collectAsState()

    val currentEntrenadorId = remember { Firebase.auth.currentUser?.uid ?: "" }

    var showDialogBorrar by remember { mutableStateOf(false) }
    var showBottomSheetEjercicios by remember { mutableStateOf(false) }
    var showBottomSheetPlantillas by remember { mutableStateOf(false) }
    var diaSeleccionadoParaEjercicio by remember { mutableStateOf(-1) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(rutinaId) { viewModel.cargarRutinaYBiblioteca(atletaId, rutinaId, currentEntrenadorId) }
    LaunchedEffect(state.isSaved, state.isDeleted) { if (state.isSaved || state.isDeleted) onBack() }

    if (showDialogBorrar) {
        AlertDialog(
            containerColor = FondoTarjeta,
            onDismissRequest = { showDialogBorrar = false },
            title = { Text("Eliminar Programa", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro de eliminar este programa completamente?", color = TextoSecundario) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = FondoOscuro),
                    onClick = { showDialogBorrar = false; viewModel.eliminarRutinaCompleta(atletaId) }
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialogBorrar = false }) { Text("Cancelar", color = NaranjaAcento) }
            }
        )
    }

    if (showBottomSheetEjercicios) {
        var searchQuery by remember { mutableStateOf("") }
        val ejerciciosFiltrados = state.bibliotecaEjercicios.filter { it.nombre.contains(searchQuery, ignoreCase = true) }

        ModalBottomSheet(onDismissRequest = { showBottomSheetEjercicios = false }, sheetState = sheetState, containerColor = FondoTarjeta) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Text("Seleccionar Ejercicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("Buscar ejercicio...", color = TextoSecundario) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = NaranjaAcento) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
                )
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                    items(ejerciciosFiltrados) { ej ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoOscuro), onClick = { viewModel.agregarEjercicioDesdeBiblioteca(diaSeleccionadoParaEjercicio, ej); showBottomSheetEjercicios = false }) {
                            Row(Modifier.padding(16.dp)) { Text(ej.nombre, fontWeight = FontWeight.SemiBold, color = Color.White) }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheetPlantillas) {
        ModalBottomSheet(onDismissRequest = { showBottomSheetPlantillas = false }, sheetState = sheetState, containerColor = FondoTarjeta) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                Text("Agregar Día (Plantillas)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                    items(state.plantillasDisponibles) { plantilla ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoOscuro), onClick = { viewModel.agregarDiaDesdePlantilla(plantilla); showBottomSheetPlantillas = false }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, tint = NaranjaAcento)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(plantilla.nombre, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Editar Programa", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = NaranjaAcento) } },
                actions = {
                    if (state.rutina != null) {
                        IconButton(onClick = { showDialogBorrar = true }) { Icon(Icons.Default.Delete, "Borrar", tint = Color(0xFFE57373)) }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading || state.rutina == null) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NaranjaAcento) }
        } else {
            val rutina = state.rutina!!
            Column(
                modifier = Modifier.fillMaxSize().background(FondoOscuro).padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = rutina.nombreRutina,
                    onValueChange = { viewModel.actualizarNombreONotas(it, rutina.notasEntrenador) },
                    label = { Text("Nombre del Programa", color = TextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta)
                )

                HorizontalDivider(color = FondoTarjeta)

                val diasOrdenados = rutina.diasEntrenamiento.sortedBy { it.ordenSecuencia }

                diasOrdenados.forEachIndexed { visualDiaIndex, dia ->
                    // 🟢 CORREGIDO: Buscamos el índice real del día en la colección desordenada para no mutar el objeto equivocado
                    val realDiaIndex = remember(rutina.diasEntrenamiento, dia) {
                        rutina.diasEntrenamiento.indexOf(dia)
                    }

                    key(dia.ordenSecuencia) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("Día ${dia.ordenSecuencia}: ${dia.nombreDia}", fontWeight = FontWeight.Black, color = NaranjaAcento, fontSize = 16.sp, modifier = Modifier.weight(1f))

                                    IconButton(
                                        onClick = { viewModel.moverDia(realDiaIndex, -1) },
                                        enabled = visualDiaIndex > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Subir Día",
                                            tint = if (visualDiaIndex > 0) NaranjaAcento else TextoSecundario.copy(alpha = 0.2f)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.moverDia(realDiaIndex, 1) },
                                        enabled = visualDiaIndex < diasOrdenados.size - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Bajar Día",
                                            tint = if (visualDiaIndex < diasOrdenados.size - 1) NaranjaAcento else TextoSecundario.copy(alpha = 0.2f)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.eliminarDia(realDiaIndex) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373)) }
                                }

                                val ejerciciosOrdenados = dia.ejercicios.sortedBy { it.ordenSecuencia }

                                ejerciciosOrdenados.forEachIndexed { visualEjIndex, ejercicio ->
                                    // 🟢 CORREGIDO: Buscamos el índice real del ejercicio en el arreglo nativo para evitar desfases al borrar/mover
                                    val realEjIndex = remember(dia.ejercicios, ejercicio) {
                                        dia.ejercicios.indexOf(ejercicio)
                                    }

                                    key(ejercicio.ordenSecuencia) {
                                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoOscuro), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FondoTarjeta)) {
                                            Column(modifier = Modifier.padding(12.dp)) {

                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                    Text("${ejercicio.ordenSecuencia}. ${ejercicio.nombre}", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))

                                                    IconButton(
                                                        onClick = { viewModel.moverEjercicio(realDiaIndex, realEjIndex, -1) },
                                                        enabled = visualEjIndex > 0
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowUpward,
                                                            contentDescription = "Subir Ejercicio",
                                                            tint = if (visualEjIndex > 0) TextoSecundario else TextoSecundario.copy(alpha = 0.2f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.moverEjercicio(realDiaIndex, realEjIndex, 1) },
                                                        enabled = visualEjIndex < ejerciciosOrdenados.size - 1
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDownward,
                                                            contentDescription = "Bajar Ejercicio",
                                                            tint = if (visualEjIndex < ejerciciosOrdenados.size - 1) TextoSecundario else TextoSecundario.copy(alpha = 0.2f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    IconButton(onClick = { viewModel.eliminarEjercicio(realDiaIndex, realEjIndex) }) { Icon(Icons.Default.Clear, null, tint = TextoSecundario) }
                                                }

                                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FondoTarjeta)

                                                EditorSeriesPrescritas(
                                                    seriesPrescritas = ejercicio.seriesPrescritas,
                                                    onSeriesUpdate = { nuevaLista ->
                                                        viewModel.actualizarEjercicio(realDiaIndex, realEjIndex, ejercicio.copy(seriesPrescritas = nuevaLista))
                                                    }
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))

                                                OutlinedTextField(
                                                    value = if (ejercicio.descansoSegundos == 0) "" else ejercicio.descansoSegundos.toString(),
                                                    onValueChange = { nv -> if (nv.all { it.isDigit() }) viewModel.actualizarEjercicio(realDiaIndex, realEjIndex, ejercicio.copy(descansoSegundos = nv.toIntOrNull() ?: 0)) },
                                                    label = { Text("Descanso sugerido (segundos)", color = TextoSecundario) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { diaSeleccionadoParaEjercicio = realDiaIndex; showBottomSheetEjercicios = true },
                                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.5f)), shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Añadir Ejercicio", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showBottomSheetPlantillas = true },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextoSecundario.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = NaranjaAcento)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir Día Extra (Plantilla)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.guardarCambios(atletaId) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro), shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar Cambios de Planificación", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun EditorSeriesPrescritas(
    seriesPrescritas: List<PrescripcionSerie>,
    onSeriesUpdate: (List<PrescripcionSerie>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(FondoOscuro, RoundedCornerShape(8.dp)).padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#", color = NaranjaAcento, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center, fontSize = 12.sp)
            Text("TIPO SERIE", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f), fontSize = 12.sp)
            Text("REPS", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, fontSize = 12.sp)
            Text("ELIMINAR", color = NaranjaAcento, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, fontSize = 12.sp)
        }

        seriesPrescritas.forEachIndexed { index, serie ->
            val (etiquetaUi, colorClave) = when (serie.tipo) {
                TipoSerie.EFECTIVA -> "Efectiva" to NaranjaAcento
                TipoSerie.APROXIMACION -> "Aproximación" to Color(0xFF4FC3F7)
                TipoSerie.DROP_SET -> "Drop Set" to Color(0xFFE57373)
                TipoSerie.FALLO -> "Al Fallo" to Color(0xFFBA68C8)
                TipoSerie.REST_PAUSE -> "Rest-Pause" to Color(0xFFAED581)
            }

            // 🟢 CORREGIDO: Bloque key explícito en base al índice estable. Bloquea la pérdida de foco del teclado al modificar el TextField
            key(index) {
                Row(modifier = Modifier.fillMaxWidth().background(FondoOscuro.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).border(1.dp, TextoSecundario.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(0.4f).size(24.dp).background(FondoTarjeta, CircleShape).border(1.dp, NaranjaAcento.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Box(modifier = Modifier.weight(1.8f).padding(horizontal = 4.dp)) {
                        Surface(
                            onClick = {
                                val todosLosEnums = TipoSerie.entries // Usamos .entries en lugar de .values() por optimización moderna KMP
                                val siguienteOrdinal = (serie.tipo.ordinal + 1) % todosLosEnums.size
                                val nuevoTipoEnum = todosLosEnums[siguienteOrdinal]
                                val nuevaLista = seriesPrescritas.toMutableList()
                                nuevaLista[index] = serie.copy(tipo = nuevoTipoEnum)
                                onSeriesUpdate(nuevaLista)
                            },
                            shape = RoundedCornerShape(8.dp), color = colorClave.copy(alpha = 0.15f), border = androidx.compose.foundation.BorderStroke(1.dp, colorClave)
                        ) {
                            Text(text = etiquetaUi, color = colorClave, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), textAlign = TextAlign.Center)
                        }
                    }

                    Box(modifier = Modifier.weight(0.9f).padding(horizontal = 2.dp)) {
                        OutlinedTextField(
                            value = if (serie.repeticiones == 0) "" else serie.repeticiones.toString(),
                            onValueChange = { valor ->
                                val limpiado = valor.filter { it.isDigit() }
                                val nuevaLista = seriesPrescritas.toMutableList()
                                nuevaLista[index] = serie.copy(repeticiones = limpiado.toIntOrNull() ?: 0)
                                onSeriesUpdate(nuevaLista)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(0.9f), contentAlignment = Alignment.Center) {
                        IconButton(onClick = { val nuevaLista = seriesPrescritas.toMutableList(); nuevaLista.removeAt(index); onSeriesUpdate(nuevaLista) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373).copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val nuevaSerie = PrescripcionSerie(numeroSerie = seriesPrescritas.size + 1, repeticiones = 10, tipo = TipoSerie.EFECTIVA)
                onSeriesUpdate(seriesPrescritas + nuevaSerie)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaranjaAcento.copy(alpha = 0.6f)), shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Añadir Nueva Serie", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}