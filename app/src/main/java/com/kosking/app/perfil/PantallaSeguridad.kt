package com.kosking.app.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeguridad(onVolver: () -> Unit) {
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }

    mensaje?.let {
        AlertDialog(
            onDismissRequest = { mensaje = null },
            icon = {
                Icon(
                    if (esError) Icons.Default.Error else Icons.Default.CheckCircle,
                    null,
                    tint = if (esError) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(if (esError) "Error" else "Listo") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { mensaje = null }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguridad") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Cambiar contraseña", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Te enviamos un email para resetear tu contraseña") },
                leadingContent = {
                    Icon(Icons.Default.Lock, null,
                        Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, null,
                        Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable {
                    if (email.isNotBlank()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                esError = false
                                mensaje = "Te enviamos un email a $email para cambiar tu contraseña."
                            }
                            .addOnFailureListener {
                                esError = true
                                mensaje = "No se pudo enviar el email. Intentá de nuevo."
                            }
                    }
                }
            )
        }
    }
}
