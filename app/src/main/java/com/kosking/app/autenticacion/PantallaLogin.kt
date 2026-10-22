package com.kosking.app.autenticacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

// Fondo: azul marino (más luminoso que negro, claramente azul)
private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A))
)

// (sin constante extra — el outlined usa transparente)

private val campoColores @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Blanco,
    unfocusedTextColor = Blanco.copy(alpha = 0.9f),
    focusedContainerColor = Blanco.copy(alpha = 0.10f),
    unfocusedContainerColor = Blanco.copy(alpha = 0.06f),
    focusedBorderColor = Rojo.copy(alpha = 0.85f),
    unfocusedBorderColor = Blanco.copy(alpha = 0.40f),   // más visible sin foco
    focusedLabelColor = Rojo.copy(alpha = 0.9f),
    unfocusedLabelColor = Blanco.copy(alpha = 0.55f),
    cursorColor = Rojo,
    errorBorderColor = Color(0xFFFF6B6B),
)

@Composable
fun PantallaLogin(
    navController: NavController,
    vm: AuthViewModel = viewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.limpiarError() }
    LaunchedEffect(Unit) {
        vm.navegarAlHome.collect {
            navController.navigate(Rutas.HOME) { popUpTo(0) { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(80.dp))

            // Logo — más grande y con más aire alrededor
            Text("🏪", fontSize = 72.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Kosking",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Rojo,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "El mercado de tu barrio",
                fontSize = 15.sp,
                color = Blanco.copy(alpha = 0.50f),
                textAlign = TextAlign.Center,
            )

            // Separación generosa antes de los campos
            Spacer(Modifier.height(56.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; vm.limpiarError() },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Blanco.copy(alpha = 0.55f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = vm.error.isNotEmpty(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = campoColores,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; vm.limpiarError() },
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
                isError = vm.error.isNotEmpty(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = campoColores,
            )

            // Error + link en fila
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MensajeError(vm.error)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {}) {
                    Text("¿Olvidaste tu contraseña?", color = Blanco.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }

            // Separación clara entre campos y botones
            Spacer(Modifier.height(36.dp))

            Button(
                onClick = {
                    vm.login(email, password) {}
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
                    Text("Iniciar sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.3.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Botón outlined — pesa menos que el rojo, jerarquía clara
            OutlinedButton(
                onClick = { navController.navigate(Rutas.REGISTRO) },
                enabled = !vm.cargando,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Blanco.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blanco),
            ) {
                Text("Crear una cuenta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))

            // "Continuar sin cuenta" — visible pero discreto
            TextButton(
                onClick = {
                    navController.navigate(Rutas.HOME) { popUpTo(0) { inclusive = true } }
                },
                enabled = !vm.cargando,
            ) {
                Text(
                    "Continuar sin cuenta →",
                    color = Blanco.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
