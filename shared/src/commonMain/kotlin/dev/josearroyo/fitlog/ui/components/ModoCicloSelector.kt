package dev.josearroyo.fitlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.josearroyo.fitlog.data.model.ModoCiclo

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModoCicloSection(
    modoCiclo: ModoCiclo,
    duracionTexto: String,
    onModoCicloChange: (ModoCiclo) -> Unit,
    onDuracionChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuración del Ciclo",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Selector de Modo (Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = modoCiclo == ModoCiclo.CALENDARIO_SEMANAL,
                    enabled = enabled,
                    onClick = { onModoCicloChange(ModoCiclo.CALENDARIO_SEMANAL) },
                    label = { Text("Semanal (Días fijos)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaranjaAcento,
                        selectedLabelColor = FondoOscuro,
                        containerColor = FondoOscuro,
                        labelColor = TextoSecundario,
                        disabledContainerColor = FondoOscuro,
                        disabledLabelColor = TextoSecundario.copy(alpha = 0.3f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = TextoSecundario.copy(alpha = 0.3f),
                        selectedBorderColor = NaranjaAcento,
                        enabled = enabled,
                        selected = modoCiclo == ModoCiclo.CALENDARIO_SEMANAL
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = modoCiclo == ModoCiclo.SECUENCIAL_RODANTE,
                    enabled = enabled,
                    onClick = { onModoCicloChange(ModoCiclo.SECUENCIAL_RODANTE) },
                    label = { Text("Secuencial / Rodante", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaranjaAcento,
                        selectedLabelColor = FondoOscuro,
                        containerColor = FondoOscuro,
                        labelColor = TextoSecundario,
                        disabledContainerColor = FondoOscuro,
                        disabledLabelColor = TextoSecundario.copy(alpha = 0.3f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = TextoSecundario.copy(alpha = 0.3f),
                        selectedBorderColor = NaranjaAcento,
                        enabled = enabled,
                        selected = modoCiclo == ModoCiclo.SECUENCIAL_RODANTE
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Campo de Duración Dinámico
            val isSemanal = modoCiclo == ModoCiclo.CALENDARIO_SEMANAL
            val labelTexto = if (isSemanal) "Duración (Semanas)" else "Duración (Días exactos)"
            val helperTexto = if (isSemanal) {
                val semanas = duracionTexto.toIntOrNull() ?: 0
                "Equivale a ${semanas * 7} días de entrenamiento"
            } else {
                "Duración total del ciclo en días"
            }

            OutlinedTextField(
                value = duracionTexto,
                onValueChange = { nuevoTexto ->
                    if (nuevoTexto.all { it.isDigit() }) {
                        onDuracionChange(nuevoTexto)
                    }
                },
                enabled = enabled,
                label = { Text(labelTexto) },
                supportingText = { Text(helperTexto, color = TextoSecundario.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = NaranjaAcento,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    disabledBorderColor = TextoSecundario.copy(alpha = 0.2f),
                    focusedContainerColor = FondoOscuro,
                    unfocusedContainerColor = FondoOscuro,
                    disabledContainerColor = FondoOscuro,
                    focusedLabelColor = NaranjaAcento,
                    unfocusedLabelColor = TextoSecundario,
                    disabledLabelColor = TextoSecundario.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}