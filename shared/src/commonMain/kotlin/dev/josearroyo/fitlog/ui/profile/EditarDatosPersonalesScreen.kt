package dev.josearroyo.fitlog.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.josearroyo.fitlog.data.model.Usuario
import dev.josearroyo.fitlog.data.model.RolUsuario

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarDatosPersonalesScreen(
    usuarioActual: Usuario,
    isSaving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onGuardarCambios: (
        nombres: String,
        apellidos: String,
        tipoDoc: String,
        numDoc: String,
        tel: String,
        fechaNac: Long?,
        tipoSangre: String?,
        nacionalidad: String?
    ) -> Unit
) {
    // 1. Estados de Campos Universales
    var nombres by remember { mutableStateOf(usuarioActual.nombres) }
    var apellidos by remember { mutableStateOf(usuarioActual.apellidos) }
    var tipoDocumento by remember { mutableStateOf(usuarioActual.tipoDocumento) }
    var numeroDocumento by remember { mutableStateOf(usuarioActual.numeroDocumento) }
    var telefono by remember { mutableStateOf(usuarioActual.telefono) }

    // 2. Estados de Campos Exclusivos del Atleta (Null-safe)
    var tipoSangre by remember { mutableStateOf(usuarioActual.tipoSangre) }
    var nacionalidad by remember { mutableStateOf(usuarioActual.nacionalidad) }

    // Control de Fecha de Nacimiento (Epoch Millis)
    var fechaNacimientoMilis by remember { mutableStateOf(usuarioActual.fechaNacimiento) }

    // Selectores dropdown
    var mostrarTiposDoc by remember { mutableStateOf(false) }
    var mostrarTiposSangre by remember { mutableStateOf(false) }

    val tiposDocumentoValidos = listOf("C.C.", "C.E.", "Pasaporte", "D.N.I.")
    val tiposSangreValidos = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    val esAtleta = usuarioActual.rol == RolUsuario.ATLETA

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Editar Datos Personales",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = NaranjaAcento)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 👥 SECCIÓN 1: DATOS UNIVERSALES
            // ==========================================
            Text(
                text = "Identificación de Usuario",
                color = NaranjaAcento,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = nombres,
                onValueChange = { nombres = it },
                label = { Text("Nombres", color = TextoSecundario) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                )
            )

            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                label = { Text("Apellidos", color = TextoSecundario) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                )
            )

            // Selector Tipo de Documento
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tipoDocumento,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Documento", color = TextoSecundario) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = NaranjaAcento,
                            modifier = Modifier.clickable { mostrarTiposDoc = true }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { mostrarTiposDoc = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                        focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                    )
                )
                DropdownMenu(
                    expanded = mostrarTiposDoc,
                    onDismissRequest = { mostrarTiposDoc = false },
                    modifier = Modifier.background(FondoTarjeta)
                ) {
                    tiposDocumentoValidos.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo, color = Color.White) },
                            onClick = {
                                tipoDocumento = tipo
                                mostrarTiposDoc = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = numeroDocumento,
                onValueChange = { numeroDocumento = it },
                label = { Text("Número de Documento", color = TextoSecundario) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                )
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono de Contacto", color = TextoSecundario) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                )
            )

            // ==========================================
            // 🏋️ SECCIÓN 2: EXCLUSIVA ATLETA
            // ==========================================
            if (esAtleta) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ficha Fisiológica del Atleta",
                    color = NaranjaAcento,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Selector de Tipo de Sangre
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tipoSangre,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Sangre", color = TextoSecundario) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = NaranjaAcento,
                                modifier = Modifier.clickable { mostrarTiposSangre = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable { mostrarTiposSangre = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                            focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                        )
                    )
                    DropdownMenu(
                        expanded = mostrarTiposSangre,
                        onDismissRequest = { mostrarTiposSangre = false },
                        modifier = Modifier.background(FondoTarjeta)
                    ) {
                        tiposSangreValidos.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo, color = Color.White) },
                                onClick = {
                                    tipoSangre = tipo
                                    mostrarTiposSangre = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nacionalidad,
                    onValueChange = { nacionalidad = it },
                    label = { Text("Nacionalidad", color = TextoSecundario) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = NaranjaAcento, unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                        focusedContainerColor = FondoTarjeta, unfocusedContainerColor = FondoTarjeta
                    )
                )
            }

            if (error != null) {
                Text(
                    text = error,
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1.5f))

            // Validaciones dinámicas basadas en rol
            val formValido = nombres.isNotBlank() &&
                    apellidos.isNotBlank() &&
                    numeroDocumento.isNotBlank() &&
                    (!esAtleta || (tipoSangre.isNotBlank() && nacionalidad.isNotBlank()))

            Button(
                onClick = {
                    onGuardarCambios(
                        nombres.trim(),
                        apellidos.trim(),
                        tipoDocumento.trim(),
                        numeroDocumento.trim(),
                        telefono.trim(),
                        fechaNacimientoMilis,
                        if (esAtleta) tipoSangre.trim() else null,
                        if (esAtleta) nacionalidad.trim() else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NaranjaAcento, contentColor = FondoOscuro),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving && formValido
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FondoOscuro)
                } else {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}