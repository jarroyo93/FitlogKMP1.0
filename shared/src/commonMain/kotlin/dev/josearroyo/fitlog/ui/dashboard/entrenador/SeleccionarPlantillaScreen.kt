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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.viewmodel.entrenador.AsignarRutinaViewModel

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
    viewModel: AsignarRutinaViewModel = viewModel { AsignarRutinaViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(entrenadorId) { viewModel.cargarBiblioteca(entrenadorId) }
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) onBack() }

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
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.construirYAsignarRutina(atletaId)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                        shape = RoundedCornerShape(12.dp),
                        enabled = state.nombreRutina.isNotBlank() && state.plantillasSeleccionadas.isNotEmpty()
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

                OutlinedTextField(
                    value = state.nombreRutina,
                    onValueChange = { viewModel.actualizarNombreRutina(it) },
                    label = { Text("Nombre del Bloque o Macrociclo", color = TextoSecundario) },
                    placeholder = { Text("Ej: Hipertrofia Bloque 1", color = TextoSecundario.copy(alpha = 0.4f)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
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