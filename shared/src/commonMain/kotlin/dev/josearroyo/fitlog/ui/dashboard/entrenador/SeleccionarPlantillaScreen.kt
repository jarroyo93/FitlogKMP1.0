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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
    navController: NavController? = null, // 🟢 AGREGADO: Para enviar la bandera al expediente
    viewModel: AsignarRutinaViewModel = viewModel { AsignarRutinaViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(entrenadorId) { viewModel.cargarBiblioteca(entrenadorId) }

    // 🟢 CORREGIDO: Notifica el cambio al expediente del atleta antes de cerrar la pantalla
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscuro)
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(horizontal = 16.dp)
            ) {
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        color = Color(0xFFE57373),
                        modifier = Modifier.padding(bottom = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                // 1. Nombre del Bloque
                OutlinedTextField(
                    value = state.nombreRutina,
                    onValueChange = { viewModel.actualizarNombreRutina(it) },
                    label = { Text("Nombre del Bloque o Macrociclo", color = TextoSecundario) },
                    placeholder = { Text("Ej: Hipertrofia Bloque 1", color = TextoSecundario.copy(alpha = 0.4f)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
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

                // 2. Configuración del Modo de Ciclo y Duración
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = TextoSecundario.copy(alpha = 0.3f),
                                    selectedBorderColor = NaranjaAcento,
                                    enabled = true,
                                    selected = state.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL
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
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = TextoSecundario.copy(alpha = 0.3f),
                                    selectedBorderColor = NaranjaAcento,
                                    enabled = true,
                                    selected = state.modoCiclo == ModoCiclo.SECUENCIAL_RODANTE
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val esSemanal = state.modoCiclo == ModoCiclo.CALENDARIO_SEMANAL
                        val duracionInt = state.duracionTexto.toIntOrNull() ?: 0
                        val labelTexto = if (esSemanal) "Duración (Semanas)" else "Duración (Días exactos)"
                        val helperTexto = if (esSemanal) {
                            "Equivale a ${duracionInt * 7} días continuos de ciclo"
                        } else {
                            "Ciclo continuo de $duracionInt días"
                        }

                        OutlinedTextField(
                            value = state.duracionTexto,
                            onValueChange = { viewModel.actualizarDuracion(it) },
                            label = { Text(labelTexto, color = TextoSecundario) },
                            supportingText = { Text(helperTexto, color = TextoSecundario.copy(alpha = 0.7f)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
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

                // 3. Secuencia de Plantillas Seleccionadas
                Text(
                    text = "Secuencia del Programa:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (state.plantillasSeleccionadas.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selecciona días del catálogo inferior para estructurar la rutina.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundario,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(
                            items = state.plantillasSeleccionadas,
                            key = { index, plantilla -> "${plantilla.id}_$index" }
                        ) { index, plantilla ->
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
                                        IconButton(
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.moverPlantillaSeleccionada(index, -1)
                                            },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Subir",
                                                tint = if (index > 0) Color.White else TextoSecundario.copy(alpha = 0.3f)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.moverPlantillaSeleccionada(index, 1)
                                            },
                                            enabled = index < state.plantillasSeleccionadas.size - 1
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Bajar",
                                                tint = if (index < state.plantillasSeleccionadas.size - 1) Color.White else TextoSecundario.copy(alpha = 0.3f)
                                            )
                                        }
                                        IconButton(onClick = {
                                            focusManager.clearFocus()
                                            viewModel.removerPlantillaSeleccionada(index)
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Quitar", tint = Color(0xFFE57373))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = FondoTarjeta, modifier = Modifier.padding(vertical = 12.dp))

                // 4. Catálogo de Plantillas
                Text(
                    text = "Catálogo de Plantillas Disponibles:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.plantillas, key = { it.id }) { plantilla ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    viewModel.agregarPlantilla(plantilla)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = FondoTarjeta.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FondoTarjeta)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = plantilla.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "+ Tocar para anexar como nuevo día",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NaranjaAcento,
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