package com.kosking.app.autenticacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.components.MensajeError
import com.kosking.app.ui.theme.*

private val campoColores @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Blanco,
    unfocusedTextColor = Blanco.copy(alpha = 0.9f),
    focusedContainerColor = Blanco.copy(alpha = 0.10f),
    unfocusedContainerColor = Blanco.copy(alpha = 0.06f),
    focusedBorderColor = Rojo.copy(alpha = 0.85f),
    unfocusedBorderColor = Blanco.copy(alpha = 0.40f),
    focusedLabelColor = Rojo.copy(alpha = 0.9f),
    unfocusedLabelColor = Blanco.copy(alpha = 0.55f),
    cursorColor = Rojo,
    errorBorderColor = Color(0xFFFF6B6B),
)

@Composable
fun PantallaRegistro(
    navController: NavController,
    vm: AuthViewModel = viewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }
    var errorLocal by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.limpiarError() }
    LaunchedEffect(Unit) {
        vm.navegarAlHome.collect {
            navController.navigate(Rutas.MAPA_SIMPLE) { popUpTo(0) { inclusive = true } }
        }
    }

    val hayError = vm.error.isNotEmpty() || errorLocal.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().background(HeaderGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(60.dp))

            Text("🏪", fontSize = 60.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Crear cuenta",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Rojo,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Gratis, sin elegir rol.\nDespués configurás lo que necesitás.",
                fontSize = 14.sp,
                color = Blanco.copy(alpha = 0.50f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; errorLocal = ""; vm.limpiarError() },
                label = { Text("Tu nombre") },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Blanco.copy(alpha = 0.55f)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                isError = hayError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = campoColores,
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorLocal = ""; vm.limpiarError() },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Blanco.copy(alpha = 0.55f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = hayError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = campoColores,
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorLocal = ""; vm.limpiarError() },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Blanco.copy(alpha = 0.55f)) },
                trailingIcon = {
                    IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                        Icon(
                            if (mostrarPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null,
                            tint = Blanco.copy(alpha = 0.55f),
                        )
                    }
                },
                visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = hayError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = campoColores,
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmarPassword,
                onValueChange = { confirmarPassword = it; errorLocal = "" },
                label = { Text("Repetir contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Blanco.copy(alpha = 0.55f)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = errorLocal.isNotEmpty(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = campoColores,
            )

            Spacer(Modifier.height(6.dp))
            MensajeError(errorLocal.ifEmpty { vm.error })

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = {
                    if (password != confirmarPassword) { errorLocal = "Las contraseñas no coinciden"; return@Button }
                    vm.registrar(nombre, email, password) {}
                },
                enabled = !vm.cargando,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            ) {
                if (vm.cargando) {
                    CircularProgressIndicator(color = Blanco, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Crear cuenta", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.3.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Link claro al Login
            Row(horizontalArrangement = Arrangement.Center) {
                Text("¿Ya tenés cuenta? ", color = Blanco.copy(alpha = 0.50f), fontSize = 14.sp)
                TextButton(
                    onClick = { navController.popBackStack() },
                    enabled = !vm.cargando,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("Iniciá sesión", color = Rojo.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
