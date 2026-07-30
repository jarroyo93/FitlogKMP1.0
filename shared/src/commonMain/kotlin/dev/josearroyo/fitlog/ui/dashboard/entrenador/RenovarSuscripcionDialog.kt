package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.josearroyo.fitlog.data.model.TipoPlanSuscripcion
import dev.josearroyo.fitlog.formatearFechaCorto
import dev.josearroyo.fitlog.getCurrentTimeMillis

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenovarSuscripcionDialog(
    atletaNombre: String,
    onDismiss: () -> Unit,
    onRenovar: (plan: TipoPlanSuscripcion, diasCustom: Int, iniciarInmediato: Boolean, fechaInicioMilis: Long) -> Unit
) {
    var planSeleccionado by remember { mutableStateOf(TipoPlanSuscripcion.MENSUAL) }
    var diasCustomStr by remember { mutableStateOf("30") }
    var iniciarInmediato by remember { mutableStateOf(true) }

    // Estado del DatePicker nativo de Compose Multiplatform
    var mostrarDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = getCurrentTimeMillis())
    val fechaSeleccionadaMilis = datePickerState.selectedDateMillis ?: getCurrentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FondoTarjeta,
        title = {
            Text(
                text = "Asignar Plan a $atletaNombre",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona el tipo de suscripción:",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Render de opciones usando los entries del Enum de tu modelo
                TipoPlanSuscripcion.entries.forEach { plan ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (planSeleccionado == plan),
                            onClick = { planSeleccionado = plan },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NaranjaAcento,
                                unselectedColor = TextoSecundario
                            )
                        )
                        Text(
                            text = plan.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Input numérico condicional si el plan es Personalizado
                if (planSeleccionado == TipoPlanSuscripcion.PERSONALIZADO) {
                    OutlinedTextField(
                        value = diasCustomStr,
                        onValueChange = { diasCustomStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Cantidad de días", color = TextoSecundario) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento,
                            unfocusedBorderColor = TextoSecundario
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = FondoOscuro, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Iniciar enseguida (En Cola)",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = iniciarInmediato,
                        onCheckedChange = { iniciarInmediato = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NaranjaAcento,
                            checkedTrackColor = NaranjaAcento.copy(alpha = 0.5f),
                            uncheckedThumbColor = TextoSecundario,
                            uncheckedTrackColor = FondoOscuro
                        )
                    )
                }

                // Selector de calendario nativo si decide diferir la fecha de inicio
                if (!iniciarInmediato) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Fecha de inicio del ciclo:",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { mostrarDatePicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = FondoOscuro),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatearFechaCorto(fechaSeleccionadaMilis),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dias = diasCustomStr.toIntOrNull() ?: 30
                    onRenovar(planSeleccionado, dias, iniciarInmediato, fechaSeleccionadaMilis)
                }
            ) {
                Text("Asignar Plan", color = NaranjaAcento, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextoSecundario)
            }
        }
    )

    // Modal DatePickerDialog nativo de Compose Multiplatform (Zero JVM Dependencies)
    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Confirmar", color = NaranjaAcento, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = FondoTarjeta)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = TextoSecundario,
                    navigationContentColor = Color.White,
                    yearContentColor = Color.White,
                    selectedYearContainerColor = NaranjaAcento,
                    selectedDayContainerColor = NaranjaAcento,
                    selectedDayContentColor = FondoOscuro,
                    todayContentColor = NaranjaAcento,
                    todayDateBorderColor = NaranjaAcento
                )
            )
        }
    }
}