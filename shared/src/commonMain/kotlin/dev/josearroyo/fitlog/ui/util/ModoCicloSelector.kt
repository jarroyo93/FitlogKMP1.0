package dev.josearroyo.fitlog.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.josearroyo.fitlog.data.model.ModoCiclo

// Suponiendo la existencia del Enum ModoCiclo
// enum class ModoCiclo { CALENDARIO_SEMANAL, SECUENCIAL_RODANTE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModoCicloSection(
    modoCiclo: ModoCiclo,
    duracionTexto: String,
    onModoCicloChange: (ModoCiclo) -> Unit,
    onDuracionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Selector de Modo (Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = modoCiclo == ModoCiclo.CALENDARIO_SEMANAL,
                    onClick = { onModoCicloChange(ModoCiclo.CALENDARIO_SEMANAL) },
                    label = { Text("Semanal (Días fijos)") },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = modoCiclo == ModoCiclo.SECUENCIAL_RODANTE,
                    onClick = { onModoCicloChange(ModoCiclo.SECUENCIAL_RODANTE) },
                    label = { Text("Secuencial / Rodante") },
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
                    // Solo permitir dígitos
                    if (nuevoTexto.all { it.isDigit() }) {
                        onDuracionChange(nuevoTexto)
                    }
                },
                label = { Text(labelTexto) },
                supportingText = { Text(helperTexto) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}