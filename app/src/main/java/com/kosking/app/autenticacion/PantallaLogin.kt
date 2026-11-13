package com.kosking.app.autenticacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLogin(
    onLoginExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    onVolver: (() -> Unit)? = null,
    vm: AuthViewModel = viewModel()
) {
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarRecuperar by remember { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }

    Scaffold(
        topBar = {
            if (onVolver != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            }
        }
    ) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kosking", style = MaterialTheme.typography.headlineLarge)
        Text("Llevá la cuenta de tus clientes", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (email.isNotBlank() && password.isNotBlank() && !cargando)
                    vm.login(email, password) { onLoginExitoso() }
            }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus)
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { mostrarRecuperar = true }) {
                Text("Olvidé mi contraseña", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { vm.login(email, password) { onLoginExitoso() } },
            enabled = !cargando && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Entrar")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onIrARegistro) {
            Text("¿No tenés cuenta? Registrate")
        }
    }
    } // Scaffold

    if (mostrarRecuperar) {
        DialogRecuperarPassword(
            emailInicial = email,
            vm = vm,
            onDismiss = { mostrarRecuperar = false }
        )
    }
}

@Composable
private fun DialogRecuperarPassword(
    emailInicial: String,
    vm: AuthViewModel,
    onDismiss: () -> Unit
) {
    var emailRecuperar by remember { mutableStateOf(emailInicial) }
    var enviado by remember { mutableStateOf(false) }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (enviado) "Email enviado" else "Recuperar contraseña") },
        text = {
            if (enviado) {
                Text("Te mandamos un email a $emailRecuperar con las instrucciones para restablecer tu contraseña.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresá tu email y te mandamos un link para restablecer la contraseña.",
                        style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = emailRecuperar,
                        onValueChange = { emailRecuperar = it; errorLocal = null },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = errorLocal != null,
                        supportingText = errorLocal?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (enviado) {
                TextButton(onClick = onDismiss) { Text("Listo") }
            } else {
                TextButton(
                    onClick = {
                        vm.recuperarPassword(
                            emailRecuperar.trim(),
                            onExito = { enviado = true },
                            onError = { errorLocal = it }
                        )
                    },
                    enabled = emailRecuperar.isNotBlank()
                ) { Text("Enviar") }
            }
        },
        dismissButton = {
            if (!enviado) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
