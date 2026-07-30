package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.GrupoMuscular
import dev.josearroyo.fitlog.viewmodel.entrenador.AddEjercicioViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEjercicioScreen(
    entrenadorId: String,
    ejercicioId: String? = null,
    onBack: () -> Unit,
    viewModel: AddEjercicioViewModel = viewModel { AddEjercicioViewModel() }
) {
    val state by viewModel.state.collectAsState()

    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ejercicioId) {
        viewModel.cargarEjercicioSiExiste(ejercicioId)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (ejercicioId == null) "Nuevo Ejercicio" else "Editar Ejercicio",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = NaranjaAcento)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscuro),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Campo de Nombre
                OutlinedTextField(
                    value = state.nombre,
                    onValueChange = viewModel::actualizarNombre,
                    label = { Text("Nombre del Ejercicio", color = TextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NaranjaAcento,
                        unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                        focusedContainerColor = FondoTarjeta,
                        unfocusedContainerColor = FondoTarjeta
                    )
                )

                // 🟢 DESPLEGABLE KMP SEGURO (Reemplaza ExposedDropdownMenuBox)
                val grupoFormateado = remember(state.grupoMuscular) {
                    state.grupoMuscular.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = grupoFormateado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grupo Muscular", color = TextoSecundario) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Desplegar",
                                tint = NaranjaAcento
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento,
                            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                            focusedContainerColor = FondoTarjeta,
                            unfocusedContainerColor = FondoTarjeta
                        )
                    )

                    // Capa transparente que captura el clic para abrir el menú
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = !expanded }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(FondoTarjeta)
                    ) {
                        GrupoMuscular.entries.forEach { g ->
                            val opcionFormateada = g.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                            DropdownMenuItem(
                                text = { Text(opcionFormateada, color = Color.White) },
                                onClick = {
                                    viewModel.actualizarGrupo(g)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Alerta de Errores
                state.error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5).copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF2B8B5).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = Color(0xFFF2B8B5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.guardarEjercicio(entrenadorId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro)
                ) {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}