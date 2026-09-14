package dev.josearroyo.fitlog.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.FitnessCenter
import dev.josearroyo.fitlog.resources.*
import org.jetbrains.compose.resources.painterResource

private val FondoTarjeta = Color(0xFF2F254E)
private val NaranjaAcento = Color(0xFFFF9F6D)

@Composable
fun obtenerPainterEjercicio(imagenRes: String = "", nombreEjercicio: String = ""): Painter {
    val clave = if (imagenRes.isNotBlank()) {
        imagenRes.lowercase().trim()
    } else {
        normalizarNombreEjercicio(nombreEjercicio)
    }

    return when (clave) {
        // 🟢 Ejercicios con imagen propia WebP lista
        "prensa_de_piernas", "prensa_piernas" -> painterResource(Res.drawable.ejercicio_prensa_piernas)

        /* Descomentar a medida que agregues las demás imágenes WebP a drawable:
        "sentadilla_libre_con_barra", "sentadilla_libre" -> painterResource(Res.drawable.ejercicio_sentadilla_libre)
        "elevacion_de_talones_sentado", "elevacion_talones" -> painterResource(Res.drawable.ejercicio_elevacion_talones_sentado)
        "press_de_banca_plano_con_barra", "press_plano" -> painterResource(Res.drawable.ejercicio_press_banca_plano)
        "curl_de_biceps_con_barra_z", "curl_biceps_barra_z" -> painterResource(Res.drawable.ejercicio_curl_biceps_barra_z)
        "buenos_dias_con_barra", "buenos_dias" -> painterResource(Res.drawable.ejercicio_buenos_dias)
        "trote_en_cinta_eliptica", "trote_cinta" -> painterResource(Res.drawable.ejercicio_trote_cinta)
        "zancadas_estocadas", "avanzadas", "zancadas" -> painterResource(Res.drawable.ejercicio_zancadas)
        "peso_muerto_rumano" -> painterResource(Res.drawable.ejercicio_peso_muerto_rumano)
        "hip_thrust_empuje_de_cadera", "hip_thrust" -> painterResource(Res.drawable.ejercicio_hip_thrust)
        "remo_con_barra" -> painterResource(Res.drawable.ejercicio_remo_barra)
        "press_inclinado_con_mancuernas", "press_inclinado" -> painterResource(Res.drawable.ejercicio_press_inclinado_mancuernas)
        "rueda_abdominal" -> painterResource(Res.drawable.ejercicio_rueda_abdominal)
        "jalon_al_pecho_en_polea_agarre_abierto", "jalon_al_pecho" -> painterResource(Res.drawable.ejercicio_jalon_pecho)
        "elevaciones_laterales_con_mancuernas", "elevaciones_laterales" -> painterResource(Res.drawable.ejercicio_elevaciones_laterales)
        "plancha_abdominal_plank", "plancha_abdominal" -> painterResource(Res.drawable.ejercicio_plancha_abdominal)
        "flexiones_de_pecho_push_ups", "flexiones_pecho" -> painterResource(Res.drawable.ejercicio_flexiones_pecho)
        "extension_de_triceps_en_polea_alta", "extension_triceps_polea" -> painterResource(Res.drawable.ejercicio_extension_triceps_polea)
        "press_militar_con_mancuernas", "press_militar" -> painterResource(Res.drawable.ejercicio_press_militar_mancuernas)
        "dominadas_pull_ups", "dominadas" -> painterResource(Res.drawable.ejercicio_dominadas)
        */

        // 📷 Imagen por defecto para todos los ejercicios sin infografía propia
        else -> painterResource(Res.drawable.ejercicio_placeholder)
    }
}

private fun normalizarNombreEjercicio(nombre: String): String {
    return nombre.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
        .replace("ñ", "n")
        .replace(Regex("[^a-z0-9]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
}

/**
 * Miniatura interactiva: Muestra un preview pequeño y al tocar abre la infografía a pantalla completa.
 */
@Composable
fun EjercicioImageThumbnail(
    imagenRes: String = "",
    nombreEjercicio: String = "",
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
    permitirVistaAmpliada: Boolean = true
) {
    var mostrarPantallaCompleta by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(10.dp))
            .background(FondoTarjeta)
            .then(
                if (permitirVistaAmpliada) {
                    Modifier.clickable { mostrarPantallaCompleta = true }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = obtenerPainterEjercicio(imagenRes, nombreEjercicio),
            contentDescription = contentDescription ?: nombreEjercicio,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Indicador visual discreto de zoom en la miniatura
        if (permitirVistaAmpliada) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Ampliar infografía",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(tamano / 2)
                )
            }
        }
    }

    // Modal a pantalla completa cuando el usuario toca la miniatura
    if (mostrarPantallaCompleta) {
        EjercicioImageFullDialog(
            imagenRes = imagenRes,
            nombreEjercicio = nombreEjercicio,
            onDismiss = { mostrarPantallaCompleta = false }
        )
    }
}

/**
 * Diálogo flotante que proyecta la infografía vertical completa en alta resolución.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioImageFullDialog(
    imagenRes: String = "",
    nombreEjercicio: String = "",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // 🚀 Extiende el visor a todo el alto y ancho de la pantalla
        )
    ) {
        Scaffold(
            containerColor = Color.Black.copy(alpha = 0.95f),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = nombreEjercicio,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = NaranjaAcento
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable { onDismiss() } // Permite cerrar también al tocar el fondo
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = obtenerPainterEjercicio(imagenRes, nombreEjercicio),
                    contentDescription = nombreEjercicio,
                    contentScale = ContentScale.Fit, // 🟢 Escala la infografía completa sin recortar bordes ni textos
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}