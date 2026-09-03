package dev.josearroyo.fitlog.ui.dashboard.entrenador.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    ultimaFechaFinCadena: Long = getCurrentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (
        plan: TipoPlanSuscripcion,
        diasPersonalizados: Int,
        iniciarEnseguida: Boolean,
        fechaInicioMilis: Long
    ) -> Unit
) {
    var planSeleccionado by remember { mutableStateOf(TipoPlanSuscripcion.MENSUAL) }
    var menuPlanExpandido by remember { mutableStateOf(false) }
    var diasPersonalizadosTexto by remember { mutableStateOf("30") }

    // 🟢 CORRECCIÓN: Solo encadena si el vencimiento es superior a hoy con una tolerancia mínima
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

    // Días a asignar según selección
    val diasEfectivos = remember(planSeleccionado, diasPersonalizadosTexto) {
        if (planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
            diasPersonalizadosTexto.toIntOrNull() ?: 0
        } else {
            planSeleccionado.dias
        }
    }

    // Fecha final precalculada
    val fechaFinCalculada = remember(fechaInicioMilis, diasEfectivos) {
        if (diasEfectivos > 0) {
            calcularFechaFinSuscripcion(fechaInicioMilis, diasEfectivos)
        } else {
            fechaInicioMilis
        }
    }

    // Detección de colisión con planes vigentes/futuros
    val hayColision = fechaInicioMilis < ultimaFechaFinCadena && ultimaFechaFinCadena > (getCurrentTimeMillis() + 60_000L)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                                .clickable { menuPlanExpandido = true }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                .clickable { mostrarDatePicker = true }
                        )
                    }

                    if (hayColision) {
                        Text(
                            text = "⚠ Colisión detectada. Se sugiere iniciar después del ${formatearFechaCorto(ultimaFechaFinCadena)}.",
                            color = Color(0xFFEF5350),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (ultimaFechaFinCadena > (getCurrentTimeMillis() + 60_000L)) {
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
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = TextoSecundario)
                    }

                    Button(
                        onClick = {
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

    // Modal de selección de fecha Material 3 (Estilizado en Modo Oscuro)
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
                            // 🟢 Convierte las 00:00 UTC del DatePicker a la medianoche local exacta
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