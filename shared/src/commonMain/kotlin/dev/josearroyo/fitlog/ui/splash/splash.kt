package dev.josearroyo.fitlog.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.josearroyo.fitlog.resources.Res
import dev.josearroyo.fitlog.resources.logo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Estados de animación para escala y opacidad
    val escala = remember { Animatable(0f) }
    val opacidad = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Ejecutar animaciones de escala y opacidad en paralelo
        launch {
            escala.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            opacidad.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }

        // Tiempo de espera total mostrando el logo antes de pasar al login
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF241B3C)), // El color exacto FondoOscuro
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.logo), // 🔥 Cambiado al motor multiplataforma
            contentDescription = "Logo FitLog",
            modifier = Modifier
                .size(260.dp)
                .scale(escala.value)
                .alpha(opacidad.value)
        )
    }
}