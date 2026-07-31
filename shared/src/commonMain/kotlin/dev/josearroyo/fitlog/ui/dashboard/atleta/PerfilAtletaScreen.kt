package dev.josearroyo.fitlog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.viewmodel.atleta.PerfilAtletaViewModel
import dev.josearroyo.fitlog.formatearFechaHistorial
import dev.josearroyo.fitlog.getCurrentTimeMillis

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilAtletaScreen(
    atletaId: String,
    onBack: () -> Unit,
    viewModel: PerfilAtletaViewModel = viewModel { PerfilAtletaViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val usuario = uiState.usuarioLogueado

    LaunchedEffect(atletaId) {
        viewModel.cargarPerfil(atletaId)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Información del Atleta", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = NaranjaAcento)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || usuario == null) {
            Box(Modifier.fillMaxSize().background(FondoOscuro), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NaranjaAcento)
            }
        } else {
            val u = usuario

            // 🟢 CORREGIDO: Evita renderizar marcas de tiempo 0L como fechas de nacimiento válidas de 1970
            val fechaNacimientoConEdadTexto = remember(u.fechaNacimiento) {
                if (u.fechaNacimiento <= 0L) {
                    "No registrada"
                } else {
                    val diferenciaMilis = getCurrentTimeMillis() - u.fechaNacimiento
                    val edadCalculada = (diferenciaMilis / 31557600000L).toInt()
                    "${formatearFechaHistorial(u.fechaNacimiento)} ($edadCalculada años)"
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoOscuro)
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Avatar Visual
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(FondoTarjeta, CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = NaranjaAcento)
                }

                Text(
                    text = "${u.nombres} ${u.apellidos}".trim().ifEmpty { "Atleta Sin Nombre" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // TARJETA 1: IDENTIFICACIÓN
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Datos de Identificación", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        DatoFila("Documento de identidad", "${u.tipoDocumento} ${u.numeroDocumento}")
                        DatoFila("Correo Electrónico", u.correo)
                        DatoFila("Fecha de Nacimiento", fechaNacimientoConEdadTexto) // 🟢 Pasamos la cadena sanitizada
                        DatoFila("Nacionalidad", u.nacionalidad)
                        DatoFila("Tipo de Sangre", u.tipoSangre)
                    }
                }

                // TARJETA 2: ESTADO COMERCIAL
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Estado Comercial", color = NaranjaAcento, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        DatoFila("Plan Asignado", u.planActivo)

                        val esActivo = u.estadoSuscripcion.name.contains("ACTIVO", ignoreCase = true)
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(text = "Estado Suscripción", style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
                            Text(
                                text = u.estadoSuscripcion.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (esActivo) Color(0xFF81C784) else Color(0xFFE57373)
                            )
                        }
                        DatoFila("Miembro desde", formatearFechaHistorial(u.fechaCreacion))
                    }
                }
            }
        }
    }
}

@Composable
fun DatoFila(label: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Text(text = valor.ifBlank { "No registrado" }, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
    }
}