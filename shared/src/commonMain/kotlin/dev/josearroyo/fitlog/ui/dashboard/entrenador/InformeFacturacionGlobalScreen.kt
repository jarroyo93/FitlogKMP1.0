package dev.josearroyo.fitlog.ui.dashboard.entrenador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.RegistroContable
import dev.josearroyo.fitlog.viewmodel.entrenador.InformeFacturacionGlobalViewModel
import dev.josearroyo.fitlog.formatearFechaCorto // 🟢 Tu función del platform
import dev.josearroyo.fitlog.formatearFechaHora  // 🟢 Tu función del platform

private val FondoOscuro = Color(0xFF241B3C)
private val NaranjaAcento = Color(0xFFFF9F6D)
private val FondoTarjeta = Color(0xFF2F254E)
private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformeFacturacionGlobalScreen(
    entrenadorId: String,
    onBack: () -> Unit,
    viewModel: InformeFacturacionGlobalViewModel = viewModel { InformeFacturacionGlobalViewModel() }
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(entrenadorId) {
        viewModel.cargarInformeGlobal(entrenadorId)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Historial Contable Global", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoOscuro)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoOscuro)
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NaranjaAcento)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Planes Vendidos", color = TextoSecundario, fontSize = 12.sp)
                            Text("${state.registros.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Vigentes / Cola", color = Color(0xFF81C784), fontSize = 12.sp)
                            Text("${state.planesActivosContador}", color = Color(0xFF81C784), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Anulaciones", color = Color(0xFFEF5350), fontSize = 12.sp)
                            Text("${state.planesCanceladosContador}", color = Color(0xFFEF5350), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (state.registros.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se registran transacciones en la plataforma.", color = TextoSecundario)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.registros) { recibo ->
                            ItemReciboContable(recibo = recibo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemReciboContable(recibo: RegistroContable) {
    val esCancelado = recibo.estado == "CANCELADO"
    val colorEstado = if (esCancelado) Color(0xFFEF5350) else Color(0xFF81C784)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta.copy(alpha = if (esCancelado) 0.5f else 1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(FondoOscuro, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = if (esCancelado) TextoSecundario else NaranjaAcento
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = recibo.atletaNombreSnapshot,
                        fontWeight = FontWeight.Bold,
                        color = if (esCancelado) TextoSecundario else Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (esCancelado) "ANULADO" else "VENDIDO",
                        color = colorEstado,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "Plan ${recibo.tipoPlan.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = NaranjaAcento,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 🟢 Usamos tus formateadores de plataforma: corto para los límites del ciclo y completo con hora para la transacción contable
                Text(
                    text = "Ciclo: ${formatearFechaCorto(recibo.fechaInicio)} al ${formatearFechaCorto(recibo.fechaFin)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = "Transacción: ${formatearFechaHora(recibo.fechaRegistroTransaccion)}",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
        }
    }
}