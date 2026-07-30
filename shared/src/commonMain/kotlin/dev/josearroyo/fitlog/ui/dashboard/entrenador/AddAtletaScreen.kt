@file:OptIn(ExperimentalMaterial3Api::class)
package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.josearroyo.fitlog.data.model.MetodoComposicionCorporal
import dev.josearroyo.fitlog.data.model.NivelExperiencia
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.data.model.ValoracionFisica
import dev.josearroyo.fitlog.data.model.Habitos
import dev.josearroyo.fitlog.data.model.Sexo
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaViewModel
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaEvent
import dev.josearroyo.fitlog.viewmodel.entrenador.AddAtletaState
import dev.josearroyo.fitlog.formatearFechaCorto

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)
private val TextoPrincipal = Color(0xFFFFFFFF)

@Composable
fun AddAtletaScreen(
    viewModel: AddAtletaViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.onEvent(AddAtletaEvent.ResetState)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Nuevo Atleta", color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = TextoPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        },
        containerColor = FondoOscuro
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FondoOscuro)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Paso ${state.currentStep} de 4", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LinearProgressIndicator(
                    progress = { state.currentStep / 4f },
                    modifier = Modifier.width(120.dp).height(8.dp),
                    color = NaranjaAcento,
                    trackColor = TextoSecundario.copy(alpha = 0.2f)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (state.currentStep) {
                        1 -> FormularioDatosPersonales(state.usuario, state.confirmarCorreo, viewModel)
                        2 -> FormularioValoracionFisica(state.valoracionFisica, viewModel)
                        3 -> FormularioHabitos(state.habitos, viewModel)
                        4 -> FormularioSuscripcion(state, viewModel)
                    }
                }
            }

            state.error?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5).copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF2B8B5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFF2B8B5),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentStep > 1) {
                    Button(
                        onClick = { viewModel.onEvent(AddAtletaEvent.PrevStep) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FondoTarjeta)
                    ) {
                        Text("Atrás", color = TextoPrincipal)
                    }
                }

                Button(
                    onClick = {
                        if (state.currentStep < 4) {
                            viewModel.onEvent(AddAtletaEvent.NextStep)
                        } else {
                            viewModel.onEvent(AddAtletaEvent.SaveAtleta)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (state.currentStep == 4) "Guardar Atleta" else "Siguiente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FormularioDatosPersonales(u: Usuario, confirmarCorreo: String, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos Personales Básicos", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            AtletaTextField(u.nombres, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(nombres = it))) }, "Nombres *")
            AtletaTextField(u.apellidos, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(apellidos = it))) }, "Apellidos *")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtletaDropdown(
                    selectedOption = u.tipoDocumento,
                    onOptionSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(tipoDocumento = it))) },
                    options = listOf("CC", "TI", "CE", "Pasaporte", "PPT"),
                    label = "Tipo Doc.",
                    modifier = Modifier.weight(1.2f)
                )
                AtletaTextField(
                    u.numeroDocumento,
                    { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(numeroDocumento = it))) },
                    "Número Doc. *",
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            AtletaDatePickerFieldKmp(
                value = u.fechaNacimiento,
                onDateSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(fechaNacimiento = it))) },
                label = "Fecha de Nacimiento"
            )

            // 🟢 FILA 1: SEXO Y TIPO DE SANGRE
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtletaDropdown(
                    selectedOption = u.sexo.etiqueta,
                    onOptionSelected = { etiqueta ->
                        val sexoSeleccionado = Sexo.entries.find { it.etiqueta == etiqueta } ?: Sexo.MASCULINO
                        viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(sexo = sexoSeleccionado)))
                    },
                    options = Sexo.entries.map { it.etiqueta },
                    label = "Sexo *",
                    modifier = Modifier.weight(1f)
                )
                AtletaDropdown(
                    selectedOption = u.tipoSangre,
                    onOptionSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(tipoSangre = it))) },
                    options = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"),
                    label = "T. Sangre",
                    modifier = Modifier.weight(1f)
                )
            }

            // 🟢 FILA 2: NACIONALIDAD (Ocupa todo el ancho)
            SearchableNacionalidadDropdown(
                selectedNacionalidad = u.nacionalidad,
                onNacionalidadSelected = { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(nacionalidad = it))) },
                modifier = Modifier.fillMaxWidth()
            )

            AtletaTextField(u.telefono, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(telefono = it))) }, "Teléfono Celular", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            AtletaTextField(u.correo, { viewModel.onEvent(AddAtletaEvent.UpdateUsuario(u.copy(correo = it))) }, "Correo Electrónico *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))

            val noCoincide = u.correo.isNotBlank() && confirmarCorreo.isNotBlank() && u.correo.trim().lowercase() != confirmarCorreo.trim().lowercase()
            AtletaTextField(
                value = confirmarCorreo,
                onValueChange = { viewModel.onEvent(AddAtletaEvent.UpdateConfirmarCorreo(it)) },
                label = "Confirmar Correo Electrónico *",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (noCoincide) {
                Text(
                    text = "Los correos electrónicos no coinciden",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FormularioValoracionFisica(v: ValoracionFisica, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Valoración Física Inicial", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(v.pesoKg, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(pesoKg = it))) }, "Peso (kg)", Modifier.weight(1f))
                DecimalField(v.alturaCm, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(alturaCm = it))) }, "Altura (cm)", Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mediciones Corporales Avanzadas", color = TextoPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Activa antropometría, bioimpedancia o ambas", color = TextoSecundario, fontSize = 12.sp)
                }
                Switch(
                    checked = v.mostrarComposicionAvanzada,
                    onCheckedChange = { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(mostrarComposicionAvanzada = it))) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = NaranjaAcento.copy(alpha = 0.4f))
                )
            }

            AnimatedVisibility(visible = v.mostrarComposicionAvanzada) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabs = listOf(
                        MetodoComposicionCorporal.ANTROPOMETRIA to "Antropometría",
                        MetodoComposicionCorporal.BIOIMPEDANCIA to "Bioimpedancia",
                        MetodoComposicionCorporal.AMBOS to "Ambos"
                    )
                    val selectedIndex = tabs.indexOfFirst { it.first == v.metodoComposicion }.coerceAtLeast(0)

                    TabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = FondoOscuro,
                        contentColor = NaranjaAcento,
                        modifier = Modifier.fillMaxWidth().background(FondoOscuro, RoundedCornerShape(8.dp))
                    ) {
                        tabs.forEachIndexed { index, pair ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = {
                                    viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(metodoComposicion = pair.first)))
                                },
                                text = { Text(pair.second, color = if (selectedIndex == index) NaranjaAcento else TextoSecundario, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            )
                        }
                    }

                    when (v.metodoComposicion) {
                        MetodoComposicionCorporal.ANTROPOMETRIA -> {
                            SubFormularioAntropometriaAtleta(v, viewModel)
                        }
                        MetodoComposicionCorporal.BIOIMPEDANCIA -> {
                            SubFormularioBioimpedanciaAtleta(v, viewModel)
                        }
                        MetodoComposicionCorporal.AMBOS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                SubFormularioAntropometriaAtleta(v, viewModel)
                                HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                                SubFormularioBioimpedanciaAtleta(v, viewModel)
                            }
                        }
                    }
                }
            }

            AtletaDropdown(
                selectedOption = v.nivelExperiencia.name,
                onOptionSelected = { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(nivelExperiencia = NivelExperiencia.valueOf(it)))) },
                options = NivelExperiencia.entries.map { it.name },
                label = "Nivel de Experiencia"
            )

            AtletaTextField(v.observacionesLadoIzquierdo, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(observacionesLadoIzquierdo = it))) }, "Observaciones / Notas Diagnósticas")
        }
    }
}

@Composable
fun SubFormularioAntropometriaAtleta(v: ValoracionFisica, viewModel: AddAtletaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Perímetros Antropométricos (cm)", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.abdomen1, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(abdomen1 = it))) }, "Abdomen 1", Modifier.weight(1f))
            DecimalFieldNulable(v.abdomen2, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(abdomen2 = it))) }, "Abdomen 2", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.brazoRelajado, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(brazoRelajado = it))) }, "Brazo Relajado", Modifier.weight(1f))
            DecimalFieldNulable(v.brazoFlexionado, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(brazoFlexionado = it))) }, "Brazo Flexionado", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.gluteo, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(gluteo = it))) }, "Glúteo", Modifier.weight(1f))
            DecimalFieldNulable(v.musloProminente, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(musloProminente = it))) }, "Muslo Prom.", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.piernaMedial, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(piernaMedial = it))) }, "Pierna Medial", Modifier.weight(1f))
            DecimalFieldNulable(v.pantorrilla, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(pantorrilla = it))) }, "Pantorrilla", Modifier.weight(1f))
        }
    }
}

@Composable
fun SubFormularioBioimpedanciaAtleta(v: ValoracionFisica, viewModel: AddAtletaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Métricas Analíticas de Báscula Inteligente", style = MaterialTheme.typography.labelSmall, color = NaranjaAcento)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DecimalFieldNulable(v.porcentajeGrasaCorporal, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(porcentajeGrasaCorporal = it))) }, "% Grasa Corp.", Modifier.weight(1f))
            DecimalFieldNulable(v.masaMuscularKg, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(masaMuscularKg = it))) }, "Masa Musc. (kg)", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntFieldNulable(v.grasaVisceral, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(grasaVisceral = it))) }, "Grasa Visceral", Modifier.weight(1f))
            DecimalFieldNulable(v.aguaCorporalPorcentaje, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(aguaCorporalPorcentaje = it))) }, "% Agua Corp.", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntFieldNulable(v.edadMetabolica, { viewModel.onEvent(AddAtletaEvent.UpdateValoracion(v.copy(edadMetabolica = it))) }, "Edad Metabólica", Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FormularioHabitos(h: Habitos, viewModel: AddAtletaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Planificación de Entrenamiento",
                    style = MaterialTheme.typography.titleMedium,
                    color = NaranjaAcento,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Días disponibles para entrenar:",
                    color = TextoSecundario,
                    fontSize = 13.sp
                )

                SelectorDiasSemanaEstricto(
                    diasSeleccionados = h.diasDisponibles,
                    onDiasCambiados = { nuevosDias ->
                        viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(diasDisponibles = nuevosDias)))
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtletaTimePickerFieldKmp(
                        value = h.horarioEntrenamiento,
                        onTimeSelected = { hora ->
                            viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(horarioEntrenamiento = hora)))
                        },
                        label = "Franja Entrenamiento",
                        modifier = Modifier.weight(1f)
                    )
                    IntField(
                        value = h.tiempoDisponibleMinutos,
                        onValueChange = { mins ->
                            viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(tiempoDisponibleMinutos = mins)))
                        },
                        label = "Tiempo/Sesión (min)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Horarios y Descanso",
                    style = MaterialTheme.typography.titleMedium,
                    color = NaranjaAcento,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtletaTimePickerFieldKmp(
                        value = h.horaDormir,
                        onTimeSelected = { horaDormir ->
                            val horasCalculadas = calcularHorasSuenoMatematico(horaDormir, h.horaDespertar)
                            viewModel.onEvent(
                                AddAtletaEvent.UpdateHabitos(
                                    h.copy(
                                        horaDormir = horaDormir,
                                        horasSueno = horasCalculadas
                                    )
                                )
                            )
                        },
                        label = "Hora Dormir",
                        modifier = Modifier.weight(1f)
                    )
                    AtletaTimePickerFieldKmp(
                        value = h.horaDespertar,
                        onTimeSelected = { horaDespertar ->
                            val horasCalculadas = calcularHorasSuenoMatematico(h.horaDormir, horaDespertar)
                            viewModel.onEvent(
                                AddAtletaEvent.UpdateHabitos(
                                    h.copy(
                                        horaDespertar = horaDespertar,
                                        horasSueno = horasCalculadas
                                    )
                                )
                            )
                        },
                        label = "Hora Despertar",
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = FondoOscuro,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Promedio de descanso estimado:",
                            color = TextoSecundario,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${h.horasSueno} hrs",
                            color = NaranjaAcento,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Estilo de Vida",
                    style = MaterialTheme.typography.titleMedium,
                    color = NaranjaAcento,
                    fontWeight = FontWeight.Bold
                )

                AtletaTextField(
                    value = h.actividadesPrincipales,
                    onValueChange = { actividades ->
                        viewModel.onEvent(AddAtletaEvent.UpdateHabitos(h.copy(actividadesPrincipales = actividades)))
                    },
                    label = "Actividades Laborales / Diarias (ej. Trabajo oficina, estudiante)"
                )
            }
        }
    }
}

@Composable
fun SelectorDiasSemanaEstricto(
    diasSeleccionados: String,
    onDiasCambiados: (String) -> Unit
) {
    val todosLosDias = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val listaActual = remember(diasSeleccionados) {
        if (diasSeleccionados.isBlank()) emptyList()
        else diasSeleccionados.split(", ").map { it.trim() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        todosLosDias.forEach { dia ->
            val estaSeleccionado = listaActual.contains(dia)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .background(
                        color = if (estaSeleccionado) NaranjaAcento else FondoOscuro,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val nuevaLista = if (estaSeleccionado) listaActual - dia else listaActual + dia
                        val ordenados = todosLosDias.filter { nuevaLista.contains(it) }
                        onDiasCambiados(ordenados.joinToString(", "))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dia,
                    color = if (estaSeleccionado) FondoOscuro else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
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

@Composable
fun FormularioSuscripcion(state: AddAtletaState, viewModel: AddAtletaViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Asignación de Membresía Inicial", style = MaterialTheme.typography.titleMedium, color = NaranjaAcento, fontWeight = FontWeight.Bold)

            AtletaDropdown(
                selectedOption = state.planSeleccionado.etiqueta,
                onOptionSelected = { etiqueta ->
                    val plan = TipoPlanSuscripcion.entries.find { it.etiqueta == etiqueta } ?: TipoPlanSuscripcion.MENSUAL
                    viewModel.onEvent(AddAtletaEvent.UpdatePlan(plan))
                },
                options = TipoPlanSuscripcion.entries.map { it.etiqueta },
                label = "Membresía / Suscripción"
            )

            AnimatedVisibility(visible = state.planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
                IntField(state.diasPersonalizados, { viewModel.onEvent(AddAtletaEvent.UpdateDiasPersonalizados(it)) }, "Duración en Días")
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Switch(
                    checked = state.iniciarPeriodoEnseguida,
                    onCheckedChange = { viewModel.onEvent(AddAtletaEvent.UpdateIniciarPeriodo(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NaranjaAcento, checkedTrackColor = NaranjaAcento.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Activar Periodo de Inmediato", color = TextoPrincipal, fontSize = 14.sp)
                    Text(
                        if (state.iniciarPeriodoEnseguida) "Comienza a facturar hoy mismo" else "Quedará diferido para activación futura",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                }
            }

            AnimatedVisibility(visible = !state.iniciarPeriodoEnseguida) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Establecer Fecha de Activación Diferida", color = TextoPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    AtletaDatePickerFieldKmp(
                        value = state.fechaInicioPlan,
                        onDateSelected = { viewModel.onEvent(AddAtletaEvent.UpdateFechaInicioPlan(it)) },
                        label = "Fecha de Inicio del Periodo"
                    )
                }
            }
        }
    }
}

@Composable
fun AtletaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NaranjaAcento,
            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
            focusedContainerColor = FondoOscuro,
            unfocusedContainerColor = FondoOscuro,
            focusedTextColor = TextoPrincipal,
            unfocusedTextColor = TextoPrincipal
        )
    )
}

@Composable
fun AtletaDropdown(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    options: List<String>,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
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
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextoPrincipal) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SearchableNacionalidadDropdown(
    selectedNacionalidad: String,
    onNacionalidadSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedNacionalidad,
            onValueChange = {},
            readOnly = true,
            label = { Text("Nacionalidad", color = TextoSecundario, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        SearchableNacionalidadDialog(
            onDismiss = { showDialog = false },
            onSelected = onNacionalidadSelected
        )
    }
}

@Composable
fun SearchableNacionalidadDialog(
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredList = remember(query) {
        listaCompletaNacionalidades.filter { it.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = { Text("Seleccionar Nacionalidad", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar nacionalidad...", color = TextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NaranjaAcento,
                        unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal
                    )
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (filteredList.isEmpty()) {
                        Text("No se encontraron resultados.", color = TextoSecundario, modifier = Modifier.align(Alignment.Center))
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredList.size, key = { filteredList[it] }) { index ->
                                val nacionalidad = filteredList[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelected(nacionalidad)
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(nacionalidad, color = TextoPrincipal, fontSize = 16.sp)
                                }
                                if (index < filteredList.size - 1) {
                                    HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = NaranjaAcento)
            }
        }
    )
}

@Composable
fun AtletaDatePickerFieldKmp(
    value: Long,
    onDateSelected: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = if (value == 0L) "" else formatearFechaCorto(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Seleccionar fecha",
                    tint = NaranjaAcento
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        KmpDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = onDateSelected
        )
    }
}

@Composable
fun AtletaTimePickerFieldKmp(
    value: String,
    onTimeSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextoSecundario, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                focusedContainerColor = FondoOscuro,
                unfocusedContainerColor = FondoOscuro,
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showTimePicker = true }
        )
    }

    if (showTimePicker) {
        KmpTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = onTimeSelected
        )
    }
}

@Composable
fun KmpDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                onDismiss()
            }) {
                Text("Confirmar", color = NaranjaAcento)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextoSecundario)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = FondoTarjeta)
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                titleContentColor = TextoPrincipal,
                headlineContentColor = TextoPrincipal,
                selectedDayContainerColor = NaranjaAcento,
                selectedDayContentColor = Color.White,
                todayContentColor = NaranjaAcento
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KmpTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 6,
        initialMinute = 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = {
            Text(
                text = "Seleccionar Hora",
                color = TextoPrincipal,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = FondoOscuro,
                        clockDialUnselectedContentColor = Color.White,
                        clockDialSelectedContentColor = FondoOscuro,
                        selectorColor = NaranjaAcento,
                        containerColor = FondoTarjeta,
                        periodSelectorBorderColor = NaranjaAcento,
                        periodSelectorSelectedContainerColor = NaranjaAcento,
                        periodSelectorUnselectedContainerColor = FondoOscuro,
                        periodSelectorSelectedContentColor = FondoOscuro,
                        periodSelectorUnselectedContentColor = TextoPrincipal,
                        timeSelectorSelectedContainerColor = NaranjaAcento.copy(alpha = 0.25f),
                        timeSelectorUnselectedContainerColor = FondoOscuro,
                        timeSelectorSelectedContentColor = NaranjaAcento,
                        timeSelectorUnselectedContentColor = TextoPrincipal
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
                    onTimeSelected(formattedTime)
                    onDismiss()
                }
            ) {
                Text("Confirmar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextoSecundario)
            }
        }
    )
}

@Composable
fun NumberPickerSimple(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextoSecundario, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > range.first) onValueChange(value - 1) else onValueChange(range.last) }) {
                Text("-", color = NaranjaAcento, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = value.toString().padStart(2, '0'),
                color = TextoPrincipal,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { if (value < range.last) onValueChange(value + 1) else onValueChange(range.first) }) {
                Text("+", color = NaranjaAcento, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable fun DecimalField(value: Double, onValueChange: (Double) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(if (value == 0.0) "" else value.toString(), { onValueChange(it.toDoubleOrNull() ?: 0.0) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
@Composable fun DecimalFieldNulable(value: Double?, onValueChange: (Double?) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(value?.toString() ?: "", { onValueChange(it.toDoubleOrNull()) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
@Composable fun IntField(value: Int, onValueChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(if (value == 0) "" else value.toString(), { onValueChange(it.toIntOrNull() ?: 0) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Number))
}
@Composable fun IntFieldNulable(value: Int?, onValueChange: (Int?) -> Unit, label: String, modifier: Modifier = Modifier) {
    AtletaTextField(value?.toString() ?: "", { onValueChange(it.toIntOrNull()) }, label, modifier, KeyboardOptions(keyboardType = KeyboardType.Number))
}

private val listaCompletaNacionalidades = listOf(
    "Colombiana", "Alemana", "Argentina", "Australiana", "Belga", "Boliviana", "Brasileña", "Canadiense",
    "Chilena", "China", "Costarricense", "Cubana", "Ecuatoriana", "Salvadoreña",
    "Española", "Estadounidense", "Francesa", "Guatemalteca", "Hondureña", "Inglesa", "Italiana",
    "Mexicana", "Nicaragüense", "Panameña", "Paraguaya", "Peruana", "Portorriqueña", "Dominicana",
    "Uruguaya", "Venezolana", "Otra"
)