package dev.josearroyo.fitlog.ui.atleta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.viewmodel.atleta.AddHabitosViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitosScreen(atletaId: String, onBack: () -> Unit) {
    val viewModel: AddHabitosViewModel = viewModel { AddHabitosViewModel() }
    val state by viewModel.state.collectAsState()

    var mostrarPickerDormir by remember { mutableStateOf(false) }
    var mostrarPickerDespertar by remember { mutableStateOf(false) }
    var mostrarPickerEntrenamiento by remember { mutableStateOf(false) }

    LaunchedEffect(state.isGuardado) {
        if (state.isGuardado) onBack()
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Registrar Hábitos", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = NaranjaAcento)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.guardar(atletaId) }) {
                        Icon(Icons.Default.Check, "Guardar", tint = NaranjaAcento)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(FondoOscuro)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            // BLOQUE 1: PLANIFICACIÓN SEMANAL
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Planificación Semanal", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = state.habitos.diasDisponibles,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Días seleccionados", color = TextoSecundario) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento,
                            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                            focusedContainerColor = FondoOscuro,
                            unfocusedContainerColor = FondoOscuro
                        )
                    )

                    SelectorDiasSemanaEstricto(
                        diasSeleccionados = state.habitos.diasDisponibles,
                        onDiasCambiados = { nuevosDias ->
                            viewModel.actualizarHabitos(state.habitos.copy(diasDisponibles = nuevosDias))
                        }
                    )
                }
            }

            // BLOQUE 2: TIEMPOS Y RELOJES MULTIPLATAFORMA
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Horarios y Descanso", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 🟢 CAPA INTERACTIVA TRANSPARENTE: Asegura captura de toques en iOS/Android sin bloquearse
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.habitos.horaDormir,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hora Dormir", color = TextoSecundario) },
                                trailingIcon = { Icon(Icons.Default.AccessTime, null, tint = NaranjaAcento) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = TextoSecundario.copy(alpha = 0.4f), unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f))
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { mostrarPickerDormir = true })
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = state.habitos.horaDespertar,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hora Despertar", color = TextoSecundario) },
                                trailingIcon = { Icon(Icons.Default.AccessTime, null, tint = NaranjaAcento) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = TextoSecundario.copy(alpha = 0.4f), unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f))
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { mostrarPickerDespertar = true })
                        }
                    }

                    Text("Total calculado de sueño: ${state.habitos.horasSueno} horas", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = state.habitos.horarioEntrenamiento,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bloque de entrenamiento", color = TextoSecundario) },
                            trailingIcon = { Icon(Icons.Default.AccessTime, null, tint = NaranjaAcento) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = TextoSecundario.copy(alpha = 0.4f), unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f))
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { mostrarPickerEntrenamiento = true })
                    }

                    OutlinedTextField(
                        value = if (state.habitos.tiempoDisponibleMinutos <= 0) "" else state.habitos.tiempoDisponibleMinutos.toString(),
                        onValueChange = { valor ->
                            val minutos = valor.toIntOrNull() ?: 0
                            viewModel.actualizarHabitos(state.habitos.copy(tiempoDisponibleMinutos = minutos))
                        },
                        label = { Text("Tiempo disponible por sesión (minutos)", color = TextoSecundario) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f))
                    )
                }
            }

            // BLOQUE 3: CONTEXTO COTIDIANO
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Actividades cotidianas", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = state.habitos.actividadesPrincipales,
                        onValueChange = { valor ->
                            viewModel.actualizarHabitos(state.habitos.copy(actividadesPrincipales = valor))
                        },
                        label = { Text("¿A qué te dedicas en el día? (Trabajo, estudio, etc.)", color = TextoSecundario) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }

    if (mostrarPickerDormir) {
        KmpTimePickerDialog(
            onDismiss = { mostrarPickerDormir = false },
            onConfirm = { h, m ->
                val nuevaHoraDormir = formatTime(h, m)
                val calculo = calcularHorasSuenoMatematico(nuevaHoraDormir, state.habitos.horaDespertar)
                viewModel.actualizarHabitos(state.habitos.copy(horaDormir = nuevaHoraDormir, horasSueno = calculo))
                mostrarPickerDormir = false
            }
        )
    }

    if (mostrarPickerDespertar) {
        KmpTimePickerDialog(
            onDismiss = { mostrarPickerDespertar = false },
            onConfirm = { h, m ->
                val nuevaHoraDespertar = formatTime(h, m)
                val calculo = calcularHorasSuenoMatematico(state.habitos.horaDormir, nuevaHoraDespertar)
                viewModel.actualizarHabitos(state.habitos.copy(horaDespertar = nuevaHoraDespertar, horasSueno = calculo))
                mostrarPickerDespertar = false
            }
        )
    }

    if (mostrarPickerEntrenamiento) {
        KmpTimePickerDialog(
            onDismiss = { mostrarPickerEntrenamiento = false },
            onConfirm = { h, m ->
                viewModel.actualizarHabitos(state.habitos.copy(horarioEntrenamiento = formatTime(h, m)))
                mostrarPickerEntrenamiento = false
            }
        )
    }
}

@Composable
fun SelectorDiasSemanaEstricto(diasSeleccionados: String, onDiasCambiados: (String) -> Unit) {
    val todosLosDias = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val listaActual = remember(diasSeleccionados) {
        if (diasSeleccionados.isEmpty()) emptyList<String>()
        else diasSeleccionados.split(", ").map { it.trim() }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        todosLosDias.forEach { dia ->
            val estaSeleccionado = listaActual.contains(dia)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(color = if (estaSeleccionado) NaranjaAcento else FondoOscuro, shape = RoundedCornerShape(8.dp))
                    .clickable {
                        // 🟢 MUTACIÓN PURA: Evitamos mutar objetos directos dentro de remember
                        val nuevaLista = if (estaSeleccionado) listaActual - dia else listaActual + dia
                        val ordenados = todosLosDias.filter { nuevaLista.contains(it) }
                        onDiasCambiados(ordenados.joinToString(", "))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = dia, color = if (estaSeleccionado) FondoOscuro else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KmpTimePickerDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Confirmar", color = NaranjaAcento) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextoSecundario) }
        },
        containerColor = FondoTarjeta,
        title = { Text("Seleccionar hora", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state, colors = TimePickerDefaults.colors(clockDialColor = FondoOscuro, selectorColor = NaranjaAcento))
            }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun calcularHorasSuenoMatematico(horaDormir: String, horaDespertar: String): Double {
    return try {
        val partesDormir = horaDormir.split(":")
        val partesDespertar = horaDespertar.split(":")
        if (partesDormir.size < 2 || partesDespertar.size < 2) return 0.0

        val minDormir = partesDormir[0].toInt() * 60 + partesDormir[1].toInt()
        var minDespertar = partesDespertar[0].toInt() * 60 + partesDespertar[1].toInt()

        if (minDespertar <= minDormir) {
            minDespertar += 24 * 60
        }
        val diff = minDespertar - minDormir
        ((diff / 60.0) * 10).toInt() / 10.0
    } catch (e: Exception) {
        0.0
    }
}