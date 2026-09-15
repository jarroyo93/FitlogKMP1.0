package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.ModoCiclo
import dev.josearroyo.fitlog.data.model.PlantillaRutina
import dev.josearroyo.fitlog.ui.components.EditorNotasLista
import dev.josearroyo.fitlog.viewmodel.entrenador.AsignarRutinaViewModel
import androidx.navigation.NavController

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarPlantillaScreen(
    atletaId: String,
    entrenadorId: String,
    onBack: () -> Unit,
    navController: NavController? = null,
    viewModel: AsignarRutinaViewModel = viewModel { AsignarRutinaViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    var mostrarCatalogoSheet by remember { mutableStateOf(false) }
    var indiceDiaEdicionNotas by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(entrenadorId) { viewModel.cargarBiblioteca(entrenadorId) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController?.previousBackStackEntry
                ?.savedStateHandle
                ?.set("hubo_cambios_atleta", true)
            onBack()
        }
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Planificar Bloque", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NaranjaAcento)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = FondoOscuro, tonalElevation = 0.dp) {
                Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    val esValido = state.nombreRutina.isNotBlank() &&
                            state.plantillasSeleccionadas.isNotEmpty() &&
                            state.duracionTexto.isNotBlank() &&
                            (state.duracionTexto.toIntOrNull() ?: 0) > 0

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.construirYAsignarRutina(atletaId)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                        shape = RoundedCornerShape(12.dp),
                        enabled = esValido
                    ) {
                        Text("Asignar Programa al Atleta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscuro)
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.error != null) {
                    item {
                        Text(
                            text = state.error!!,
                            color = Color(0xFFE57373),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. Nombre del Bloque
                item {
                    OutlinedTextField(
                        value = state.nombreRutina,
                        onValueChange = { viewModel.actualizarNombreRutina(it) },
                        label = { Text("Nombre del Bloque o Macrociclo", color = TextoSecundario) },
                        placeholder = { Text("Ej: Hipertrofia Bloque 1", color = TextoSecundario.copy(alpha = 0.4f)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento,
                            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                            focusedContainerColor = FondoTarjeta,
                            unfocusedContainerColor = FondoTarjeta
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // 2. Indicaciones Generales de la Rutina
                item {
                    EditorNotasLista(
                        titulo = "Indicaciones Generales de la Rutina (Opcional)",
                        placeholder = "Ej: RPE 8 general",
                        notasTexto = state.notasEntrenador,
                        onNotasChanged = { nuevasNotas ->
                            viewModel.actualizarNotasEntrenador(nuevasNotas)
                        }
                    )
                }

                // 3. Configuración Modo y Duración
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Modo de Entrenamiento:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = state.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL,
                                    onClick = { viewModel.actualizarModoCiclo(ModoCiclo.CALENDARIO_SEMANAL) },
                                    label = { Text("Semanal", fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NaranjaAcento,
                                        selectedLabelColor = FondoOscuro,
                                        containerColor = FondoOscuro,
                                        labelColor = TextoSecundario
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = state.modoCiclo == ModoCiclo.SECUENCIAL_RODANTE,
                                    onClick = { viewModel.actualizarModoCiclo(ModoCiclo.SECUENCIAL_RODANTE) },
                                    label = { Text("Secuencial / Rodante", fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NaranjaAcento,
                                        selectedLabelColor = FondoOscuro,
                                        containerColor = FondoOscuro,
                                        labelColor = TextoSecundario
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val esSemanal = state.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL
                            val duracionInt = state.duracionTexto.toIntOrNull() ?: 0
                            val labelTexto = if (esSemanal) "Duración (Semanas)" else "Duración (Días exactos)"
                            val helperTexto = if (esSemanal) "Equivale a ${duracionInt * 7} días continuos" else "Ciclo continuo de $duracionInt días"

                            OutlinedTextField(
                                value = state.duracionTexto,
                                onValueChange = { viewModel.actualizarDuracion(it) },
                                label = { Text(labelTexto, color = TextoSecundario) },
                                supportingText = { Text(helperTexto, color = TextoSecundario.copy(alpha = 0.7f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NaranjaAcento,
                                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                                    focusedContainerColor = FondoOscuro,
                                    unfocusedContainerColor = FondoOscuro
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                // 4. Encabezado y Botón para agregar Plantillas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estructura del Programa:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        FilledTonalButton(
                            onClick = { mostrarCatalogoSheet = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NaranjaAcento.copy(alpha = 0.15f), contentColor = NaranjaAcento),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Añadir Día", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // 5. Secuencia de Plantillas Agregadas
                if (state.plantillasSeleccionadas.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FondoTarjeta.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Presiona '+ Añadir Día' para seleccionar plantillas del catálogo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoSecundario
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = state.plantillasSeleccionadas,
                        key = { index, plantilla -> "${plantilla.id}_$index" }
                    ) { index, plantilla ->
                        TarjetaDiaEstructurado(
                            index = index,
                            totalCount = state.plantillasSeleccionadas.size,
                            plantilla = plantilla,
                            onMover = { direccion -> viewModel.moverPlantillaSeleccionada(index, direccion) },
                            onEditarEjercicios = { indiceDiaEdicionNotas = index },
                            onEliminar = { viewModel.removerPlantillaSeleccionada(index) }
                        )
                    }
                }
            }

            // 🟢 MODAL 1: CATÁLOGO DE PLANTILLAS DISPONIBLES
            if (mostrarCatalogoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { mostrarCatalogoSheet = false },
                    containerColor = FondoTarjeta
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = "Seleccionar Plantilla de la Biblioteca",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(state.plantillas, key = { it.id }) { plantilla ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.agregarPlantilla(plantilla)
                                            mostrarCatalogoSheet = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(plantilla.nombre, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("${plantilla.ejercicios.size} ejercicios prescritos", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                                        }
                                        Icon(Icons.Default.AddCircle, contentDescription = "Anexar", tint = NaranjaAcento)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🟢 MODAL 2: EDICIÓN RÁPIDA DE NOTAS TÉCNICAS POR EJERCICIO ANTES DE ASIGNAR
            // 🟢 MODAL 2: EDICIÓN INTERACTIVA DE RECOMENDACIONES TÉCNICAS (VIÑETA POR VIÑETA)
            indiceDiaEdicionNotas?.let { index ->
                val plantilla = state.plantillasSeleccionadas.getOrNull(index)
                if (plantilla != null) {
                    ModalBottomSheet(
                        onDismissRequest = { indiceDiaEdicionNotas = null },
                        containerColor = FondoTarjeta
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding()
                        ) {
                            Text(
                                text = "Recomendaciones Técnicas - Día ${index + 1}: ${plantilla.nombre}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NaranjaAcento,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                itemsIndexed(plantilla.ejercicios) { ejIndex, ejPrescrito ->
                                    EditorNotasLista(
                                        titulo = ejPrescrito.nombreEjercicio,
                                        placeholder = "Ej: Pausa de 2s abajo",
                                        notasTexto = ejPrescrito.notas,
                                        onNotasChanged = { nuevasNotas ->
                                            viewModel.actualizarNotaEspecificaEjercicio(index, ejIndex, nuevasNotas)
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { indiceDiaEdicionNotas = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NaranjaAcento,
                                    contentColor = FondoOscuro
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Guardar Cambios del Día", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaDiaEstructurado(
    index: Int,
    totalCount: Int,
    plantilla: PlantillaRutina,
    onMover: (Int) -> Unit,
    onEditarEjercicios: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Día ${index + 1}: ${plantilla.nombre}",
                    fontWeight = FontWeight.Bold,
                    color = NaranjaAcento
                )
                Text(
                    text = "${plantilla.ejercicios.size} movimientos configurados",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditarEjercicios) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Notas por ejercicio",
                        tint = NaranjaAcento
                    )
                }

                IconButton(
                    onClick = { onMover(-1) },
                    enabled = index > 0
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Subir",
                        tint = if (index > 0) Color.White else TextoSecundario.copy(alpha = 0.3f)
                    )
                }

                IconButton(
                    onClick = { onMover(1) },
                    enabled = index < totalCount - 1
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bajar",
                        tint = if (index < totalCount - 1) Color.White else TextoSecundario.copy(alpha = 0.3f)
                    )
                }

                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Clear, contentDescription = "Quitar", tint = Color(0xFFE57373))
                }
            }
        }
    }
}