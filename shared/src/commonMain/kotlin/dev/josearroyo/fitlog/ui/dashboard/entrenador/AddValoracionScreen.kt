package dev.josearroyo.fitlog.ui.entrenador

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.MetodoComposicionCorporal
import dev.josearroyo.fitlog.data.model.NivelExperiencia
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.viewmodel.entrenador.AddValoracionViewModel

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddValoracionScreen(
    atletaId: String,
    onBack: () -> Unit
) {
    val viewModel: AddValoracionViewModel = viewModel { AddValoracionViewModel() }
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isGuardado) {
        if (state.isGuardado) {
            onBack()
        }
    }

    val esValido = state.valoracion.pesoKg > 0.0 &&
            state.valoracion.alturaCm > 0.0 &&
            state.valoracion.objetivoInicial.isNotBlank()

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Nueva Valoración", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = NaranjaAcento)
                    }
                },
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = NaranjaAcento)
                    } else {
                        IconButton(
                            onClick = { viewModel.guardar(atletaId) },
                            enabled = esValido
                        ) {
                            Icon(
                                Icons.Default.Check,
                                "Guardar",
                                tint = if (esValido) NaranjaAcento else TextoSecundario.copy(alpha = 0.38f)
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
                        CampoMedidaBase("Peso (kg)", state.valoracion.pesoKg, { viewModel.actualizarValoracion(state.valoracion.copy(pesoKg = it)) }, Modifier.weight(1f))
                        CampoMedidaBase("Altura (cm)", state.valoracion.alturaCm, { viewModel.actualizarValoracion(state.valoracion.copy(alturaCm = it)) }, Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = state.valoracion.objetivoInicial,
                        onValueChange = { viewModel.actualizarValoracion(state.valoracion.copy(objetivoInicial = it)) },
                        label = { Text("Objetivo Inicial (Ej: Hipertrofia, Definición)", color = TextoSecundario) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
                    )
                }
            }

            // BLOQUE 2: HISTORIAL
            Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("2. Historial de Actividad Reciente", color = NaranjaAcento, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Analiza la recencia del estímulo antes de clasificar al atleta.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoEnteroOpcional("Último Periodo (Meses)", state.valoracion.ultimoPeriodoConsistenciaMeses, { viewModel.actualizarValoracion(state.valoracion.copy(ultimoPeriodoConsistenciaMeses = it)) }, Modifier.weight(1f))
                        CampoEnteroOpcional("Inactividad (Meses)", state.valoracion.periodoInactividadActualMeses, { viewModel.actualizarValoracion(state.valoracion.copy(periodoInactividadActualMeses = it)) }, Modifier.weight(1f))
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
                                        onClick = { viewModel.actualizarValoracion(state.valoracion.copy(metodoComposicion = metodo)) },
                                        text = { Text(metodo.name, color = Color.White) }
                                    )
                                }
                            }

                            // Envolturas de clave explícitas para asegurar que la reconfiguración de subformularios mantenga los árboles limpios
                            key(state.valoracion.metodoComposicion) {
                                when (state.valoracion.metodoComposicion) {
                                    MetodoComposicionCorporal.ANTROPOMETRIA -> SubFormularioAntropometria(state.valoracion, viewModel)
                                    MetodoComposicionCorporal.BIOIMPEDANCIA -> SubFormularioBioimpedancia(state.valoracion, viewModel)
                                    MetodoComposicionCorporal.AMBOS -> {
                                        Column {
                                            SubFormularioAntropometria(state.valoracion, viewModel)
                                            HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 16.dp))
                                            SubFormularioBioimpedancia(state.valoracion, viewModel)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 4.dp))
                            Text("Registro Fotográfico", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                OutlinedButton(onClick = {}, colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento)) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Foto Frente")
                                }
                                OutlinedButton(onClick = {}, colors = ButtonDefaults.outlinedButtonColors(contentColor = NaranjaAcento)) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Foto Perfil")
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.guardar(atletaId) },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading && esValido
            ) {
                val textoBoton = if (state.isLoading) "Guardando..."
                else if (state.valoracion.mostrarComposicionAvanzada) "Finalizar Valoración Completa"
                else "Finalizar Valoración Básica"
                Text(textoBoton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SubFormularioAntropometria(valFisica: ValoracionFisica, viewModel: AddValoracionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Medidas de Perímetros Manuales (cm)", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Abdomen +2", valFisica.abdomen1, { viewModel.actualizarValoracion(valFisica.copy(abdomen1 = it)) }, Modifier.weight(1f))
            CampoMedidaOpcional("Abdomen -2", valFisica.abdomen2, { viewModel.actualizarValoracion(valFisica.copy(abdomen2 = it)) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Brazo Flex.", valFisica.brazoFlexionado, { viewModel.actualizarValoracion(valFisica.copy(brazoFlexionado = it)) }, Modifier.weight(1f))
            CampoMedidaOpcional("Brazo Relaj.", valFisica.brazoRelajado, { viewModel.actualizarValoracion(valFisica.copy(brazoRelajado = it)) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Glúteo", valFisica.gluteo, { viewModel.actualizarValoracion(valFisica.copy(gluteo = it)) }, Modifier.weight(1f))
            CampoMedidaOpcional("Muslo Prom.", valFisica.musloProminente, { viewModel.actualizarValoracion(valFisica.copy(musloProminente = it)) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("Pierna Medial", valFisica.piernaMedial, { viewModel.actualizarValoracion(valFisica.copy(piernaMedial = it)) }, Modifier.weight(1f))
            CampoMedidaOpcional("Pantorrilla", valFisica.pantorrilla, { viewModel.actualizarValoracion(valFisica.copy(pantorrilla = it)) }, Modifier.weight(1f))
        }
        OutlinedTextField(
            value = valFisica.observacionesLadoIzquierdo,
            onValueChange = { viewModel.actualizarValoracion(valFisica.copy(observacionesLadoIzquierdo = it)) },
            label = { Text("Observaciones / Afectaciones Lado Izquierdo", color = TextoSecundario) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
        )
    }
}

@Composable
fun SubFormularioBioimpedancia(valFisica: ValoracionFisica, viewModel: AddValoracionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Métricas Analíticas de Báscula Inteligente", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoMedidaOpcional("% Grasa Corp.", valFisica.porcentajeGrasaCorporal, { viewModel.actualizarValoracion(valFisica.copy(porcentajeGrasaCorporal = it)) }, Modifier.weight(1f))
            CampoMedidaOpcional("Masa Musc. (kg)", valFisica.masaMuscularKg, { viewModel.actualizarValoracion(valFisica.copy(masaMuscularKg = it)) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoEnteroOpcional("Grasa Visceral", valFisica.grasaVisceral, { viewModel.actualizarValoracion(valFisica.copy(grasaVisceral = it)) }, Modifier.weight(1f))
            CampoMedidaOpcional("% Agua Corp.", valFisica.aguaCorporalPorcentaje, { viewModel.actualizarValoracion(valFisica.copy(aguaCorporalPorcentaje = it)) }, Modifier.weight(1f))
        }
        CampoEnteroOpcional("Edad Metabolica", valFisica.edadMetabolica, { viewModel.actualizarValoracion(valFisica.copy(edadMetabolica = it)) }, Modifier.fillMaxWidth(0.5f))
    }
}

// 🟢 CORREGIDO: Los inputs de abajo se modificaron para que lean sincrónicamente el valor mutado y no causen pérdida de foco del teclado en cada pulsación
@Composable
fun CampoMedidaBase(label: String, value: Double, onValueChange: (Double) -> Unit, modifier: Modifier = Modifier) {
    val textValue = if (value == 0.0) "" else value.toString()
    OutlinedTextField(
        value = textValue,
        onValueChange = { onValueChange(it.replace(",", ".").toDoubleOrNull() ?: 0.0) },
        label = { Text(label, color = TextoSecundario) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
    )
}

@Composable
fun CampoMedidaOpcional(label: String, value: Double?, onValueChange: (Double?) -> Unit, modifier: Modifier = Modifier) {
    val textValue = value?.toString() ?: ""
    OutlinedTextField(
        value = textValue,
        onValueChange = { onValueChange(it.replace(",", ".").toDoubleOrNull()) },
        label = { Text(label, color = TextoSecundario) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
    )
}

@Composable
fun CampoEnteroOpcional(label: String, value: Int?, onValueChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val textValue = value?.toString() ?: ""
    OutlinedTextField(
        value = textValue,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.toIntOrNull()) },
        label = { Text(label, color = TextoSecundario) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f), focusedContainerColor = FondoOscuro, unfocusedContainerColor = FondoOscuro)
    )
}