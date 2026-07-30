package dev.josearroyo.fitlog.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.josearroyo.fitlog.repository.AuthRepository
import dev.josearroyo.fitlog.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private val TextoSecundario = Color(0xFFB3AEC6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CambiarContrasenaScreen(
    uid: String,
    onPasswordChangedSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel { AuthViewModel() }
    val state by viewModel.activationState.collectAsState()
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val handleLogout = {
        scope.launch {
            authRepository.logout()
            onLogout()
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.resetActivationState()
            onPasswordChangedSuccess()
        }
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                title = { Text("Configurar Contraseña", fontWeight = FontWeight.Black, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoOscuro,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { handleLogout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = NaranjaAcento
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoOscuro)
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Por tu seguridad, debes cambiar la contraseña temporal que te asignó tu entrenador por una nueva clave privada.",
                color = TextoSecundario,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val contrasenaCorta = contrasena.isNotBlank() && contrasena.length < 6

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña Nueva") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = contrasenaCorta,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val imagenIcono = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = imagenIcono,
                            contentDescription = null,
                            tint = NaranjaAcento
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedLabelColor = NaranjaAcento,
                    unfocusedLabelColor = TextoSecundario,
                    cursorColor = NaranjaAcento,
                    errorTextColor = Color.White,
                    errorBorderColor = Color.Red,
                    errorLabelColor = NaranjaAcento
                ),
                supportingText = {
                    if (contrasenaCorta) {
                        Text(text = "La contraseña debe tener al menos 6 caracteres", color = Color.Red)
                    } else {
                        Text(text = "Mínimo 6 caracteres", color = TextoSecundario.copy(alpha = 0.7f))
                    }
                }
            )

            val noCoincide = contrasena.isNotBlank() && confirmarContrasena.isNotBlank() && contrasena != confirmarContrasena

            OutlinedTextField(
                value = confirmarContrasena,
                onValueChange = { confirmarContrasena = it },
                label = { Text("Confirmar Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = noCoincide,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val imagenIcono = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = imagenIcono,
                            contentDescription = null,
                            tint = NaranjaAcento
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NaranjaAcento,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.4f),
                    focusedLabelColor = NaranjaAcento,
                    unfocusedLabelColor = TextoSecundario,
                    cursorColor = NaranjaAcento,
                    errorTextColor = Color.White,
                    errorBorderColor = Color.Red,
                    errorLabelColor = NaranjaAcento
                ),
                supportingText = {
                    if (noCoincide) {
                        Text(text = "Las contraseñas no coinciden", color = Color.Red)
                    }
                }
            )

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isValid = contrasena.isNotBlank() && !noCoincide && contrasena.length >= 6
            val isButtonEnable = isValid && !state.isLoading

            Button(
                onClick = {
                    viewModel.actualizarContrasenaPrimeraVez(uid, contrasena)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isButtonEnable,
                border = if (!isButtonEnable) BorderStroke(2.dp, NaranjaAcento) else null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NaranjaAcento,
                    disabledContainerColor = FondoOscuro,
                    contentColor = FondoOscuro,
                    disabledContentColor = NaranjaAcento
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NaranjaAcento)
                } else {
                    Text(
                        text = "Guardar y Continuar",
                        fontWeight = FontWeight.Bold,
                        color = if (isButtonEnable) FondoOscuro else NaranjaAcento
                    )
                }
            }

            TextButton(onClick = { handleLogout() }) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = TextoSecundario)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cerrar Sesión", color = TextoSecundario, fontWeight = FontWeight.Medium)
            }
        }
    }
}