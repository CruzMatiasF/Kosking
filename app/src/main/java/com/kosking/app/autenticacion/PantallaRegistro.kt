package com.kosking.app.autenticacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistro(
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tocadoEmail by remember { mutableStateOf(false) }
    var tocadoPassword by remember { mutableStateOf(false) }

    val emailValido = EMAIL_REGEX.matches(email.trim())
    val passwordValida = password.length >= 6
    val formularioValido = nombre.isNotBlank() && emailValido && passwordValida

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Tu nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; tocadoEmail = true },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = tocadoEmail && !emailValido,
                supportingText = if (tocadoEmail && !emailValido) {
                    { Text("Ingresá un email válido") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; tocadoPassword = true },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = tocadoPassword && !passwordValida,
                supportingText = if (tocadoPassword && !passwordValida) {
                    { Text("Mínimo 6 caracteres") }
                } else {
                    { Text("Mínimo 6 caracteres", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { vm.registrar(nombre, email.trim(), password, onRegistroExitoso) },
                enabled = !cargando && formularioValido,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Crear cuenta")
            }
        }
    }
}
