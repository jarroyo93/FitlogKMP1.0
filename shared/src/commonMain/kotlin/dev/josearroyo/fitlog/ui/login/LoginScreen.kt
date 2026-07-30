package dev.josearroyo.fitlog.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.data.model.RolUsuario
import dev.josearroyo.fitlog.resources.Res
import dev.josearroyo.fitlog.resources.logo
import dev.josearroyo.fitlog.viewmodel.AuthState
import dev.josearroyo.fitlog.viewmodel.AuthViewModel
import org.jetbrains.compose.resources.painterResource

val FondoOscuro = Color(0xFF241B3C)
val NaranjaAcento = Color(0xFFFF9F6D)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel { AuthViewModel() },
    onLoginSuccess: (uid: String, rol: RolUsuario, requiereCambioContrasena: Boolean) -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoginEnable = email.isNotBlank() && password.isNotBlank() && authState !is AuthState.Loading

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val success = authState as AuthState.Success
            onLoginSuccess(success.uid, success.rol, success.requiereCambioContrasena)
            authViewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = FondoOscuro)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ImagenLogo()

        Spacer(modifier = Modifier.height(32.dp))

        EmailField(email = email, onTextChange = { email = it })

        Spacer(modifier = Modifier.height(16.dp))

        Password(password = password, onTextChange = { password = it })

        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator(color = NaranjaAcento)
        } else {
            BotonLogin(
                isLoginEnable = isLoginEnable,
                onLoginClicked = {
                    authViewModel.login(email.trim(), password)
                }
            )
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            val errorRaw = (authState as AuthState.Error).message
            Text(
                text = mapearMensajeErrorAuth(errorRaw),
                color = Color(0xFFEF5350),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun BotonLogin(
    isLoginEnable: Boolean,
    onLoginClicked: () -> Unit
) {
    Button(
        onClick = onLoginClicked,
        enabled = isLoginEnable,
        border = if (!isLoginEnable) BorderStroke(2.dp, NaranjaAcento) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = NaranjaAcento,
            disabledContainerColor = FondoOscuro,
            contentColor = FondoOscuro,
            disabledContentColor = NaranjaAcento
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(
            text = "Iniciar Sesión",
            color = if (isLoginEnable) FondoOscuro else NaranjaAcento,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmailField(
    email: String,
    onTextChange: (String) -> Unit
) {
    TextField(
        value = email,
        onValueChange = onTextChange,
        label = { Text("Correo electrónico", color = NaranjaAcento) },
        maxLines = 1,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = NaranjaAcento,
            unfocusedTextColor = NaranjaAcento,
            focusedIndicatorColor = NaranjaAcento,
            unfocusedIndicatorColor = NaranjaAcento,
            focusedContainerColor = FondoOscuro,
            unfocusedContainerColor = FondoOscuro,
            cursorColor = NaranjaAcento
        )
    )
}

@Composable
fun Password(password: String, onTextChange: (String) -> Unit) {
    var passwordVisibility by remember { mutableStateOf(false) }

    TextField(
        value = password,
        onValueChange = onTextChange,
        label = { Text("Contraseña", color = NaranjaAcento) },
        maxLines = 1,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = NaranjaAcento,
            unfocusedTextColor = NaranjaAcento,
            focusedIndicatorColor = NaranjaAcento,
            unfocusedIndicatorColor = NaranjaAcento,
            focusedContainerColor = FondoOscuro,
            unfocusedContainerColor = FondoOscuro,
            cursorColor = NaranjaAcento
        ),
        trailingIcon = {
            val imagen = if (passwordVisibility) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                Icon(
                    imageVector = imagen,
                    contentDescription = "Mostrar contraseña",
                    tint = NaranjaAcento
                )
            }
        },
        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
    )
}

@Composable
fun ImagenLogo() {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = "Logo principal",
        modifier = Modifier
            .width(280.dp)
            .height(240.dp),
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter
    )
}

fun mapearMensajeErrorAuth(errorRaw: String): String {
    return when {
        errorRaw.contains("incorrect, malformed or has expired", ignoreCase = true) ||
                errorRaw.contains("invalid-credential", ignoreCase = true) ||
                errorRaw.contains("wrong-password", ignoreCase = true) -> {
            "El correo o la contraseña son incorrectos. Compruébalos e inténtalo de nuevo."
        }
        errorRaw.contains("user-not-found", ignoreCase = true) -> {
            "No hay ningún usuario registrado con este correo electrónico."
        }
        errorRaw.contains("invalid-email", ignoreCase = true) -> {
            "El formato del correo ingresado no es válido."
        }
        errorRaw.contains("too-many-requests", ignoreCase = true) -> {
            "Demasiados intentos fallidos. Acceso bloqueado temporalmente por seguridad."
        }
        errorRaw.contains("network error", ignoreCase = true) ||
                errorRaw.contains("timeout", ignoreCase = true) ||
                errorRaw.contains("unreachable", ignoreCase = true) ||
                errorRaw.contains("network-request-failed", ignoreCase = true) -> {
            "No tienes conexión a internet. Revisa tu red e inténtalo de nuevo."
        }
        else -> errorRaw
    }
}