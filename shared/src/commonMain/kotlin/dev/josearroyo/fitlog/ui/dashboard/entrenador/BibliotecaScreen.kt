package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.Ejercicio
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.data.model.PlantillaRutina
import dev.josearroyo.fitlog.viewmodel.entrenador.BibliotecaViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@Composable
fun BibliotecaScreen(
    entrenadorId: String,
    onNavigateToAddEjercicio: (String) -> Unit,
    onNavigateToEditEjercicio: (String, String) -> Unit,
    onNavigateToAddPlantilla: (String) -> Unit,
    onNavigateToEditPlantilla: (String, String) -> Unit
) {
    val viewModel: BibliotecaViewModel = viewModel { BibliotecaViewModel() }
    val state by viewModel.state.collectAsState()

    // 🟢 CORREGIDO: rememberSaveable blinda el estado del filtro contra recomposiciones de teclado
    var menuExpandidoFiltro by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.tabSeleccionado) {
        if (state.tabSeleccionado == 0) {
            viewModel.cargarBiblioteca(entrenadorId)
        } else {
            viewModel.cargarPlantillas(entrenadorId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .padding(16.dp)
    ) {
        TabRow(
            selectedTabIndex = state.tabSeleccionado,
            containerColor = FondoTarjeta,
            contentColor = NaranjaAcento,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.tabSeleccionado]),
                    color = NaranjaAcento
                )
            },
            modifier = Modifier.fillMaxWidth().background(FondoTarjeta, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = state.tabSeleccionado == 0,
                onClick = { viewModel.cambiarPestana(0) },
                text = { Text("Ejercicios", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selectedContentColor = Color.White,
                unselectedContentColor = TextoSecundario
            )
            Tab(
                selected = state.tabSeleccionado == 1,
                onClick = { viewModel.cambiarPestana(1) },
                text = { Text("Plantillas", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selectedContentColor = Color.White,
                unselectedContentColor = TextoSecundario
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.tabSeleccionado == 0) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.textoBusqueda,
                    onValueChange = { viewModel.filtrarEjercicios(it, state.grupoSeleccionado) },
                    label = { Text("Buscar ejercicio...", color = TextoSecundario) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = NaranjaAcento) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = NaranjaAcento, unfocusedBorderColor = FondoTarjeta,
                        focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(onClick = { menuExpandidoFiltro = true }) {
                        Icon(Icons.Default.FilterList, "Filtrar grupo", tint = NaranjaAcento)
                    }
                    DropdownMenu(expanded = menuExpandidoFiltro, onDismissRequest = { menuExpandidoFiltro = false }, modifier = Modifier.background(FondoTarjeta)) {
                        DropdownMenuItem(text = { Text("Todos los grupos", color = Color.White) }, onClick = { viewModel.filtrarEjercicios(state.textoBusqueda, null); menuExpandidoFiltro = false })

                        // 🟢 CORREGIDO: Uso de .entries sobre .values() para proteger la memoria RAM
                        GrupoMuscular.entries.forEach { grupo ->
                            val nombreFormateado = grupo.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                            DropdownMenuItem(
                                text = { Text(nombreFormateado, color = Color.White) },
                                onClick = { viewModel.filtrarEjercicios(state.textoBusqueda, grupo); menuExpandidoFiltro = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NaranjaAcento) }
            } else {
                // 🟢 CORREGIDO: Se inyecta key única para optimizar el reciclaje de las tarjetas de fuerza
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(state.listaFiltrada, key = { it.id }) { ejercicio ->
                        CardEjercicioRow(
                            ejercicio = ejercicio,
                            onEdit = { onNavigateToEditEjercicio(entrenadorId, ejercicio.id) },
                            onDelete = { viewModel.eliminarEjercicioPersonalizado(ejercicio.id, entrenadorId) }
                        )
                    }
                }

                Button(
                    onClick = { onNavigateToAddEjercicio(entrenadorId) },
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Ejercicio Personalizado", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            if (state.isLoadingPlantillas) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NaranjaAcento) }
            } else {
                // 🟢 CORREGIDO: Llave key explícita para evitar desfase de índices en la bitácora de plantillas
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(state.listaPlantillas, key = { it.id }) { plantilla ->
                        CardPlantillaRow(
                            plantilla = plantilla,
                            onEdit = { onNavigateToEditPlantilla(entrenadorId, plantilla.id) },
                            onDelete = { viewModel.eliminarPlantilla(plantilla.id, entrenadorId) }
                        )
                    }
                }

                Button(
                    onClick = { onNavigateToAddPlantilla(entrenadorId) },
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Nueva Plantilla", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CardEjercicioRow(ejercicio: Ejercicio, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ejercicio.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val grupoFormateado = remember(ejercicio.grupoMuscular) { ejercicio.grupoMuscular.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } }
                Text(grupoFormateado, color = NaranjaAcento, fontSize = 12.sp)
            }
            if (ejercicio.esPersonalizado) {
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(FondoTarjeta)) {
                        DropdownMenuItem(text = { Text("Editar", color = Color.White) }, onClick = { menuOpen = false; onEdit() })
                        DropdownMenuItem(text = { Text("Eliminar", color = Color.Red) }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            } else {
                Text("Defecto", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun CardPlantillaRow(plantilla: PlantillaRutina, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(plantilla.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${plantilla.ejercicios.size} ejercicios en secuencia", color = TextoSecundario, fontSize = 13.sp)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.background(FondoTarjeta)) {
                    DropdownMenuItem(text = { Text("Editar", color = Color.White) }, onClick = { menuOpen = false; onEdit() })
                    DropdownMenuItem(text = { Text("Eliminar", color = Color.Red) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}