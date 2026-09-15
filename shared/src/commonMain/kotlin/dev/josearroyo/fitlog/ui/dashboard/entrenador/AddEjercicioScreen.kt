package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
    val focusManager = LocalFocusManager.current

    // Configuración de colores reutilizable para los campos de texto
    val coloresCampoTexto = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color.White.copy(alpha = 0.6f),
        focusedBorderColor = NaranjaAcento,
        unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
        disabledBorderColor = TextoSecundario.copy(alpha = 0.2f),
        focusedContainerColor = FondoTarjeta,
        unfocusedContainerColor = FondoTarjeta,
        disabledContainerColor = FondoTarjeta, // 🟢 Mantiene el fondo oscuro de la tarjeta al desactivar
        focusedLabelColor = NaranjaAcento,
        unfocusedLabelColor = TextoSecundario,
        disabledLabelColor = TextoSecundario.copy(alpha = 0.6f),
        disabledTrailingIconColor = NaranjaAcento.copy(alpha = 0.4f)
    )

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
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isLoading // 🟢 Bloquea el botón atrás mientras se guarda
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (!state.isLoading) NaranjaAcento else TextoSecundario.copy(alpha = 0.3f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Si se está cargando la información inicial al editar, mostramos el indicador de carga principal
        if (state.isLoading && state.nombre.isBlank() && ejercicioId != null) {
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
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Campo de Nombre
                OutlinedTextField(
                    value = state.nombre,
                    onValueChange = viewModel::actualizarNombre,
                    label = { Text("Nombre del Ejercicio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading,
                    colors = coloresCampoTexto
                )

                // DESPLEGABLE KMP SEGURO
                val grupoFormateado = remember(state.grupoMuscular) {
                    state.grupoMuscular.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = grupoFormateado,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isLoading,
                        label = { Text("Grupo Muscular") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Desplegar",
                                tint = if (!state.isLoading) NaranjaAcento else TextoSecundario.copy(alpha = 0.3f)
                            )
                        },
                        colors = coloresCampoTexto
                    )

                    // Capa transparente que captura el clic para abrir el menú
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = !state.isLoading) { expanded = !expanded }
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
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.guardarEjercicio(entrenadorId)
                    },
                    enabled = !state.isLoading && state.nombre.isNotBlank(), // 🟢 Inhabilitado mientras guarda
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NaranjaAcento,
                        contentColor = FondoOscuro,
                        disabledContainerColor = NaranjaAcento.copy(alpha = 0.5f)
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FondoOscuro,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}