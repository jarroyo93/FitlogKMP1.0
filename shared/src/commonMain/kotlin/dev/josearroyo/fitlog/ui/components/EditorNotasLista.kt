package dev.josearroyo.fitlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@Composable
fun EditorNotasLista(
    titulo: String,
    placeholder: String,
    notasTexto: String,
    onNotasChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nuevoTexto by remember { mutableStateOf("") }
    var indexEditando by remember { mutableStateOf<Int?>(null) }

    val listaItems = remember(notasTexto) {
        notasTexto.lines()
            .map { it.trimStart('•', ' ', '\t') }
            .filter { it.isNotBlank() }
    }

    fun guardarOAgregarItem() {
        if (nuevoTexto.isNotBlank()) {
            val nuevaLista = listaItems.toMutableList()
            val index = indexEditando
            if (index != null && index in nuevaLista.indices) {
                nuevaLista[index] = nuevoTexto.trim()
            } else {
                nuevaLista.add(nuevoTexto.trim())
            }
            onNotasChanged(nuevaLista.joinToString("\n") { "• $it" })
            nuevoTexto = ""
            indexEditando = null
        }
    }

    fun iniciarEdicion(index: Int, texto: String) {
        indexEditando = index
        nuevoTexto = texto
    }

    fun eliminarItem(index: Int) {
        if (indexEditando == index) {
            indexEditando = null
            nuevoTexto = ""
        }
        val nuevaLista = listaItems.toMutableList().apply { removeAt(index) }
        onNotasChanged(nuevaLista.joinToString("\n") { "• $it" })
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (listaItems.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    listaItems.forEachIndexed { index, item ->
                        val estaEditandoEste = indexEditando == index

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (estaEditandoEste) NaranjaAcento.copy(alpha = 0.15f) else FondoOscuro,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { iniciarEdicion(index, item) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ",
                                    color = NaranjaAcento,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = item,
                                    color = if (estaEditandoEste) NaranjaAcento else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (estaEditandoEste) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { iniciarEdicion(index, item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar nota",
                                        tint = TextoSecundario,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { eliminarItem(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Eliminar nota",
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nuevoTexto,
                    onValueChange = { nuevoTexto = it },
                    placeholder = {
                        Text(
                            text = if (indexEditando != null) "Editando viñeta..." else placeholder,
                            color = TextoSecundario.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { guardarOAgregarItem() }),
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

                IconButton(
                    onClick = { guardarOAgregarItem() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(NaranjaAcento, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (indexEditando != null) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (indexEditando != null) "Guardar Cambios" else "Agregar Nota",
                        tint = FondoOscuro
                    )
                }
            }
        }
    }
}