package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.MetodoComposicionCorporal
import dev.josearroyo.fitlog.data.model.NivelExperiencia
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.viewmodel.entrenador.AddValoracionViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

// Configuración de colores reutilizable para campos sobre FondoOscuro con soporte a estado deshabilitado
private val coloresCampoOscuro @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = Color.White.copy(alpha = 0.6f),
    focusedBorderColor = NaranjaAcento,
    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
    disabledBorderColor = TextoSecundario.copy(alpha = 0.2f),
    focusedContainerColor = FondoOscuro,
    unfocusedContainerColor = FondoOscuro,
    disabledContainerColor = FondoOscuro, // 🟢 Mantiene el fondo oscuro al desactivar
    focusedLabelColor = NaranjaAcento,
    unfocusedLabelColor = TextoSecundario,
    disabledLabelColor = TextoSecundario.copy(alpha = 0.6f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddValoracionScreen(
    atletaId: String,
    onBack: () -> Unit
) {
    val viewModel: AddValoracionViewModel = viewModel { AddValoracionViewModel() }
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isGuardado) {
        if (state.isGuardado) {
            onBack()
        }
    }

    val esValido = state.valoracion.pesoKg > 0.0 &&
            state.valoracion.alturaCm > 0.0 &&
            state.valoracion.objetivoInicial.isNotBlank()

    val isFormEnabled = !state.isLoading

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Nueva Valoración", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = isFormEnabled // 🟢 Bloquea navegación durante el guardado
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isFormEnabled) NaranjaAcento else TextoSecundario.copy(alpha = 0.3f)
                        )
                    }
                },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            color = NaranjaAcento,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.guardar(atletaId)
                            },
                            enabled = isFormEnabled && esValido // 🟢 Inhabilita ícono de guardar si está cargando
                        ) {
                            Icon(
                                Icons.Default.Check,
                                "Guardar",
                                tint = if (esValido && isFormEnabled) NaranjaAcento else TextoSecundario.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // BLOQUE 1: DATOS CRÍTICOS
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Datos Críticos Obligatorios", color = NaranjaAcento, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoMedidaBase("Peso (kg)", state.valoracion.pesoKg, { viewModel.actualizarValoracion(state.valoracion.copy(pesoKg = it)) }, Modifier.weight(1f), enabled = isFormEnabled)
                        CampoMedidaBase("Altura (cm)", state.valoracion.alturaCm, { viewModel.actualizarValoracion(state.valoracion.copy(alturaCm = it)) }, Modifier.weight(1f), enabled = isFormEnabled)
                    }

                    OutlinedTextField(
                        value = state.valoracion.objetivoInicial,
                        onValueChange = { viewModel.actualizarValoracion(state.valoracion.copy(objetivoInicial = it)) },
                        label = { Text("Objetivo Inicial (Ej: Hipertrofia, Definición)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = isFormEnabled,
                        colors = coloresCampoOscuro
                    )
                }
            }

            // BLOQUE 2: HISTORIAL
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Historial de Actividad Reciente", color = NaranjaAcento, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Analiza la recencia del estímulo antes de clasificar al atleta.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoEnteroOpcional("Último Periodo (Meses)", state.valoracion.ultimoPeriodoConsistenciaMeses, { viewModel.actualizarValoracion(state.valoracion.copy(ultimoPeriodoConsistenciaMeses = it)) }, Modifier.weight(1f), enabled = isFormEnabled)
                        CampoEnteroOpcional("Inactividad (Meses)", state.valoracion.periodoInactividadActualMeses, { viewModel.actualizarValoracion(state.valoracion.copy(periodoInactividadActualMeses = it)) }, Modifier.weight(1f), enabled = isFormEnabled)
                    }
                }
            }

            // BLOQUE 3: CLASIFICACIÓN
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("3. Clasificación del Atleta", color = NaranjaAcento, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NivelExperiencia.entries.forEach { nivel ->
                            FilterChip(
                                selected = state.valoracion.nivelExperiencia == nivel,
                                enabled = isFormEnabled,
                                onClick = { viewModel.actualizarValoracion(state.valoracion.copy(nivelExperiencia = nivel)) },
                                label = { Text(nivel.name, color = if(state.valoracion.nivelExperiencia == nivel) FondoOscuro else Color.White) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaranjaAcento, containerColor = FondoOscuro)
                            )
                        }
                    }
                }
            }

            // BLOQUE 4: COMPOSICIÓN CORPORAL AVANZADA
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("4. Composición Avanzada", color = NaranjaAcento, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = state.valoracion.mostrarComposicionAvanzada,
                            enabled = isFormEnabled,
                            onCheckedChange = { viewModel.actualizarValoracion(state.valoracion.copy(mostrarComposicionAvanzada = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = FondoOscuro)
                        )
                    }

                    AnimatedVisibility(visible = state.valoracion.mostrarComposicionAvanzada) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            TabRow(selectedTabIndex = state.valoracion.metodoComposicion.ordinal, containerColor = FondoOscuro, contentColor = NaranjaAcento) {
                                MetodoComposicionCorporal.entries.forEach { metodo ->
                                    Tab(
                                        selected = state.valoracion.metodoComposicion == metodo,
                                        enabled = isFormEnabled,
                                        onClick = { viewModel.actualizarValoracion(state.valoracion.copy(metodoComposicion = metodo)) },
                                        text = { Text(metodo.name, color = if (isFormEnabled) Color.White else TextoSecundario.copy(alpha = 0.5f)) }
                                    )
                                }
                            }

                            key(state.valoracion.metodoComposicion) {
                                when (state.valoracion.metodoComposicion) {
                                    MetodoComposicionCorporal.ANTROPOMETRIA -> SubFormularioAntropometria(state.valoracion, viewModel, enabled = isFormEnabled)
                                    MetodoComposicionCorporal.BIOIMPEDANCIA -> SubFormularioBioimpedancia(state.valoracion, viewModel, enabled = isFormEnabled)
                                    MetodoComposicionCorporal.AMBOS -> {
                                        Column {
                                            SubFormularioAntropometria(state.valoracion, viewModel, enabled = isFormEnabled)
                                            HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 16.dp))
                                            SubFormularioBioimpedancia(state.valoracion, viewModel, enabled = isFormEnabled)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Registro Fotográfico", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                OutlinedButton(onClick = {}, enabled = isFormEnabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento)) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Foto Frente")
                                }
                                OutlinedButton(onClick = {}, enabled = isFormEnabled, colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento)) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Foto Perfil")
                                }
                            }
                        }
                    }
                }
            }

            // Alerta de error
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

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.guardar(atletaId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NaranjaAcento,
                    contentColor = FondoOscuro,
                    disabledContainerColor = NaranjaAcento.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isFormEnabled && esValido // 🟢 Inhabilitado mientras guarda o formulario inválido
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = FondoOscuro,
                        strokeWidth = 2.dp
                    )
                } else {
                    val textoBoton = if (state.valoracion.mostrarComposicionAvanzada) "Finalizar Valoración Completa"
                    else "Finalizar Valoración Básica"
                    Text(textoBoton, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SubFormularioAntropometria(valFisica: ValoracionFisica, viewModel: AddValoracionViewModel, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Medidas de Perímetros Manuales (cm)", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Abdomen +2", valFisica.abdomen1, { viewModel.actualizarValoracion(valFisica.copy(abdomen1 = it)) }, Modifier.weight(1f), enabled = enabled)
            CampoMedidaOpcional("Abdomen -2", valFisica.abdomen2, { viewModel.actualizarValoracion(valFisica.copy(abdomen2 = it)) }, Modifier.weight(1f), enabled = enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Brazo Flex.", valFisica.brazoFlexionado, { viewModel.actualizarValoracion(valFisica.copy(brazoFlexionado = it)) }, Modifier.weight(1f), enabled = enabled)
            CampoMedidaOpcional("Brazo Relaj.", valFisica.brazoRelajado, { viewModel.actualizarValoracion(valFisica.copy(brazoRelajado = it)) }, Modifier.weight(1f), enabled = enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Glúteo", valFisica.gluteo, { viewModel.actualizarValoracion(valFisica.copy(gluteo = it)) }, Modifier.weight(1f), enabled = enabled)
            CampoMedidaOpcional("Muslo Prom.", valFisica.musloProminente, { viewModel.actualizarValoracion(valFisica.copy(musloProminente = it)) }, Modifier.weight(1f), enabled = enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Pierna Medial", valFisica.piernaMedial, { viewModel.actualizarValoracion(valFisica.copy(piernaMedial = it)) }, Modifier.weight(1f), enabled = enabled)
            CampoMedidaOpcional("Pantorrilla", valFisica.pantorrilla, { viewModel.actualizarValoracion(valFisica.copy(pantorrilla = it)) }, Modifier.weight(1f), enabled = enabled)
        }
        OutlinedTextField(
            value = valFisica.observacionesLadoIzquierdo,
            onValueChange = { viewModel.actualizarValoracion(valFisica.copy(observacionesLadoIzquierdo = it)) },
            label = { Text("Observaciones / Afectaciones Lado Izquierdo") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            enabled = enabled,
            colors = coloresCampoOscuro
        )
    }
}

@Composable
fun SubFormularioBioimpedancia(valFisica: ValoracionFisica, viewModel: AddValoracionViewModel, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Métricas Analíticas de Báscula Inteligente", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("% Grasa Corp.", valFisica.porcentajeGrasaCorporal, { viewModel.actualizarValoracion(valFisica.copy(porcentajeGrasaCorporal = it)) }, Modifier.weight(1f), enabled = enabled)
            CampoMedidaOpcional("Masa Musc. (kg)", valFisica.masaMuscularKg, { viewModel.actualizarValoracion(valFisica.copy(masaMuscularKg = it)) }, Modifier.weight(1f), enabled = enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoEnteroOpcional("Grasa Visceral", valFisica.grasaVisceral, { viewModel.actualizarValoracion(valFisica.copy(grasaVisceral = it)) }, Modifier.weight(1f), enabled = enabled)
            CampoMedidaOpcional("% Agua Corp.", valFisica.aguaCorporalPorcentaje, { viewModel.actualizarValoracion(valFisica.copy(aguaCorporalPorcentaje = it)) }, Modifier.weight(1f), enabled = enabled)
        }
        CampoEnteroOpcional("Edad Metabolica", valFisica.edadMetabolica, { viewModel.actualizarValoracion(valFisica.copy(edadMetabolica = it)) }, Modifier.fillMaxWidth(0.5f), enabled = enabled)
    }
}

@Composable
fun CampoMedidaBase(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val textValue = if (value == 0.0) "" else value.toString()
    OutlinedTextField(
        value = textValue,
        onValueChange = { onValueChange(it.replace(",", ".").toDoubleOrNull() ?: 0.0) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        colors = coloresCampoOscuro
    )
}

@Composable
fun CampoMedidaOpcional(
    label: String,
    value: Double?,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val textValue = value?.toString() ?: ""
    OutlinedTextField(
        value = textValue,
        onValueChange = { onValueChange(it.replace(",", ".").toDoubleOrNull()) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        colors = coloresCampoOscuro
    )
}

@Composable
fun CampoEnteroOpcional(
    label: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val textValue = value?.toString() ?: ""
    OutlinedTextField(
        value = textValue,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.toIntOrNull()) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        colors = coloresCampoOscuro
    )
}