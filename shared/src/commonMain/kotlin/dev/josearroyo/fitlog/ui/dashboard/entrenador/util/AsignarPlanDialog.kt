package dev.josearroyo.fitlog.ui.dashboard.entrenador.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.josearroyo.fitlog.calcularFechaFinSuscripcion
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.formatearFechaCorto
import dev.josearroyo.fitlog.getCurrentTimeMillis
import dev.josearroyo.fitlog.normalizarFechaDatePicker

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignarPlanDialog(
    atletaNombre: String,
    // 🟢 Recibe la lista de pares (fechaInicio, fechaFin) de los planes guardados
    periodosExistentes: List<Pair<Long, Long>> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        plan: TipoPlanSuscripcion,
        diasPersonalizados: Int,
        iniciarEnseguida: Boolean,
        fechaInicioMilis: Long
    ) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    fun ocultarTeclado() {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    var planSeleccionado by remember { mutableStateOf(TipoPlanSuscripcion.MENSUAL) }
    var menuPlanExpandido by remember { mutableStateOf(false) }
    var diasPersonalizadosTexto by remember { mutableStateOf("30") }

    // Obtenemos la última fecha de fin solo para sugerir el inicio por defecto si se desea encadenar al final
    val ultimaFechaFinCadena = remember(periodosExistentes) {
        periodosExistentes.maxOfOrNull { it.second } ?: getCurrentTimeMillis()
    }

    val fechaRecomendadaInicio = remember(ultimaFechaFinCadena) {
        val ahora = getCurrentTimeMillis()
        if (ultimaFechaFinCadena > (ahora + 60_000L)) {
            ultimaFechaFinCadena + 1L
        } else {
            ahora
        }
    }

    var fechaInicioMilis by remember { mutableStateOf(fechaRecomendadaInicio) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    val diasEfectivos = remember(planSeleccionado, diasPersonalizadosTexto) {
        if (planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
            diasPersonalizadosTexto.toIntOrNull() ?: 0
        } else {
            planSeleccionado.dias
        }
    }

    val fechaFinCalculada = remember(fechaInicioMilis, diasEfectivos) {
        if (diasEfectivos > 0) {
            calcularFechaFinSuscripcion(fechaInicioMilis, diasEfectivos)
        } else {
            fechaInicioMilis
        }
    }

    // 🟢 CÁLCULO DE COLISIÓN REAL: Verifica traslape directo de rangos [inicio, fin]
    val hayColision = remember(fechaInicioMilis, fechaFinCalculada, periodosExistentes) {
        periodosExistentes.any { (inicioExistente, finExistente) ->
            fechaInicioMilis <= finExistente && fechaFinCalculada >= inicioExistente
        }
    }

    Dialog(
        onDismissRequest = {
            ocultarTeclado()
            onDismiss()
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        ocultarTeclado()
                    })
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabecera
                Column {
                    Text(
                        text = "Asignar Plan de Suscripción",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Atleta: $atletaNombre",
                        color = NaranjaAcento,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = FondoOscuro)

                // 1. Desplegable de selección de plan
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Plan", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = planSeleccionado.etiqueta,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NaranjaAcento)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NaranjaAcento,
                                unfocusedBorderColor = TextoSecundario.copy(alpha = 0.3f),
                                focusedContainerColor = FondoOscuro,
                                unfocusedContainerColor = FondoOscuro
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    ocultarTeclado()
                                    menuPlanExpandido = true
                                }
                        )

                        DropdownMenu(
                            expanded = menuPlanExpandido,
                            onDismissRequest = { menuPlanExpandido = false },
                            modifier = Modifier.background(FondoTarjeta)
                        ) {
                            TipoPlanSuscripcion.entries.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.etiqueta, color = Color.White) },
                                    onClick = {
                                        planSeleccionado = plan
                                        if (plan != TipoPlanSuscripcion.PERSONALIZADO) {
                                            diasPersonalizadosTexto = plan.dias.toString()
                                        }
                                        menuPlanExpandido = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Campo opcional para días personalizados
                if (planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
                    OutlinedTextField(
                        value = diasPersonalizadosTexto,
                        onValueChange = { if (it.all { char -> char.isDigit() }) diasPersonalizadosTexto = it },
                        label = { Text("Número de días", color = TextoSecundario) },
                        trailingIcon = {
                            IconButton(
                                onClick = { ocultarTeclado() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Ocultar teclado",
                                    tint = NaranjaAcento
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { ocultarTeclado() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento,
                            unfocusedBorderColor = TextoSecundario.copy(alpha = 0.3f),
                            focusedContainerColor = FondoOscuro,
                            unfocusedContainerColor = FondoOscuro
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 3. Selección de fecha con DatePicker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Fecha de Inicio", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatearFechaCorto(fechaInicioMilis),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Elegir Fecha", tint = NaranjaAcento)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (hayColision) Color(0xFFEF5350) else NaranjaAcento,
                                unfocusedBorderColor = if (hayColision) Color(0xFFEF5350) else TextoSecundario.copy(alpha = 0.3f),
                                focusedContainerColor = FondoOscuro,
                                unfocusedContainerColor = FondoOscuro
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    ocultarTeclado()
                                    mostrarDatePicker = true
                                }
                        )
                    }

                    if (hayColision) {
                        Text(
                            text = "⚠ Colisión detectada con un período existente en esas fechas.",
                            color = Color(0xFFEF5350),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (fechaInicioMilis == ultimaFechaFinCadena + 1L) {
                        Text(
                            text = "Se encadenará automáticamente al finalizar el plan actual.",
                            color = NaranjaAcento,
                            fontSize = 11.sp
                        )
                    }
                }

                // 4. Resumen de vigencia
                Surface(
                    color = FondoOscuro.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Fecha de Vencimiento:", fontSize = 11.sp, color = TextoSecundario)
                            Text(
                                text = formatearFechaCorto(fechaFinCalculada),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "$diasEfectivos Días",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = NaranjaAcento
                        )
                    }
                }

                // 5. Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            ocultarTeclado()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = TextoSecundario)
                    }

                    Button(
                        onClick = {
                            ocultarTeclado()
                            val esInmediato = fechaInicioMilis <= (getCurrentTimeMillis() + 60_000L)
                            onConfirm(
                                planSeleccionado,
                                diasEfectivos,
                                esInmediato,
                                fechaInicioMilis
                            )
                        },
                        enabled = diasEfectivos > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaranjaAcento,
                            contentColor = FondoOscuro
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Asignar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaInicioMilis
        )

        val datePickerCustomColors = DatePickerDefaults.colors(
            containerColor = FondoTarjeta,
            titleContentColor = Color.White,
            headlineContentColor = NaranjaAcento,
            weekdayContentColor = TextoSecundario,
            subheadContentColor = TextoSecundario,
            yearContentColor = Color.White,
            currentYearContentColor = NaranjaAcento,
            selectedYearContentColor = FondoOscuro,
            selectedYearContainerColor = NaranjaAcento,
            dayContentColor = Color.White,
            disabledDayContentColor = TextoSecundario.copy(alpha = 0.3f),
            selectedDayContentColor = FondoOscuro,
            selectedDayContainerColor = NaranjaAcento,
            todayContentColor = NaranjaAcento,
            todayDateBorderColor = NaranjaAcento,
            navigationContentColor = Color.White,
            dividerColor = FondoOscuro
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            fechaInicioMilis = normalizarFechaDatePicker(dateMillis)
                        }
                        mostrarDatePicker = false
                    }
                ) {
                    Text("Aceptar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = FondoTarjeta
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = datePickerCustomColors
            )
        }
    }
}