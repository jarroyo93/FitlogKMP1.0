package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.ElementoRutina
import dev.josearroyo.fitlog.ui.dashboard.EditorSeriesPrescritas
import dev.josearroyo.fitlog.viewmodel.entrenador.AddPlantillaViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantillaScreen(
    entrenadorId: String,
    plantillaId: String? = null,
    onBack: () -> Unit
) {
    val viewModel: AddPlantillaViewModel = viewModel { AddPlantillaViewModel() }
    val state by viewModel.state.collectAsState()

    // 🟢 CORREGIDO: Uso de rememberSaveable para que no se cierre el bottom sheet accidentalmente
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargarPlantillaSiExiste(plantillaId, entrenadorId) }
    LaunchedEffect(state.isGuardado) { if (state.isGuardado) onBack() }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text(if (plantillaId == null) "Nueva Plantilla" else "Editar Plantilla", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = NaranjaAcento) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }, containerColor = NaranjaAcento, contentColor = FondoOscuro) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Ejercicio")
            }
        },
        bottomBar = {
            Surface(tonalElevation = 0.dp, color = FondoOscuro) {
                Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { viewModel.guardarPlantilla(entrenadorId) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                        shape = RoundedCornerShape(12.dp),
                        enabled = state.nombrePlantilla.isNotBlank() && state.ejerciciosEnCarrito.isNotEmpty()
                    ) {
                        Text(if (plantillaId == null) "Guardar Plantilla" else "Actualizar Plantilla", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(FondoOscuro).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.nombrePlantilla,
                onValueChange = { viewModel.actualizarNombre(it) },
                label = { Text("Nombre de la Rutina", color = TextoSecundario) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Ejercicios seleccionados", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 🟢 CORREGIDO: Se inyecta un identificador key estable basado en el nombre o id para que no colapse el reordenamiento
                itemsIndexed(state.ejerciciosEnCarrito, key = { _, item -> item.nombreEjercicio }) { index, elemento ->
                    ElementoRutinaCard(
                        elemento = elemento,
                        index = index,
                        onUpdate = { elementoModificado -> viewModel.actualizarElemento(index, elementoModificado) },
                        onMove = { dir -> viewModel.moverEjercicio(index, dir) },
                        onDelete = { viewModel.eliminarEjercicio(index) },
                        canMoveUp = index > 0,
                        canMoveDown = index < state.ejerciciosEnCarrito.size - 1
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        var searchQuery by rememberSaveable { mutableStateOf("") }
        val ejerciciosFiltrados = state.bibliotecaDisponible.filter { it.nombre.contains(searchQuery, ignoreCase = true) }

        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, containerColor = FondoTarjeta) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar ejercicio...", color = TextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn {
                    items(ejerciciosFiltrados, key = { it.nombre }) { ejercicio ->
                        ListItem(
                            headlineContent = { Text(ejercicio.nombre, color = Color.White, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.clickable { viewModel.agregarEjercicioAlCarrito(ejercicio); showBottomSheet = false },
                            colors = ListItemDefaults.colors(containerColor = FondoTarjeta)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ElementoRutinaCard(
    elemento: ElementoRutina,
    index: Int,
    onUpdate: (ElementoRutina) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(elemento.nombreEjercicio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NaranjaAcento, modifier = Modifier.weight(1f))

                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Default.ArrowUpward, null, tint = if(canMoveUp) NaranjaAcento else TextoSecundario) }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Default.ArrowDownward, null, tint = if(canMoveDown) NaranjaAcento else TextoSecundario) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373)) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FondoOscuro)

            EditorSeriesPrescritas(
                seriesPrescritas = elemento.seriesPrescritas,
                onSeriesUpdate = { nuevaLista -> onUpdate(elemento.copy(seriesPrescritas = nuevaLista)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = if (elemento.descansoSegundos == 0) "" else elemento.descansoSegundos.toString(),
                onValueChange = { valor -> onUpdate(elemento.copy(descansoSegundos = valor.filter { it.isDigit() }.toIntOrNull() ?: 0)) },
                label = { Text("Descanso (s)", color = TextoSecundario) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
            )
        }
    }
}