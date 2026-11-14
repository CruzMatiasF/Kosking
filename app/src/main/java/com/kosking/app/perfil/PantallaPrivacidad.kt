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
fun PantallaPrivacidad(
    onVolver: () -> Unit,
    onPoliticaPrivacidad: () -> Unit = {},
    onCuentaEliminada: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()

    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar cuenta") },
            text = {
                Text(
                    "Se eliminarán permanentemente tu cuenta y todos los datos de tu local, clientes y movimientos. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        auth.currentUser?.delete()
                            ?.addOnSuccessListener { onCuentaEliminada() }
                            ?.addOnFailureListener {
                                mensajeError = "Para eliminar la cuenta necesitás volver a iniciar sesión primero."
                            }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    mensajeError?.let {
        AlertDialog(
            onDismissRequest = { mensajeError = null },
            title = { Text("Error") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { mensajeError = null }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidad") },
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
                headlineContent = { Text("Política de privacidad", fontWeight = FontWeight.Medium) },
                supportingContent = { Text("Cómo usamos y protegemos tus datos") },
                leadingContent = {
                    Icon(Icons.Default.PrivacyTip, null,
                        Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, null,
                        Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable(onClick = onPoliticaPrivacidad)
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

            Spacer(Modifier.height(16.dp))

            ListItem(
                headlineContent = {
                    Text("Eliminar cuenta", fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error)
                },
                supportingContent = {
                    Text("Elimina tu cuenta y todos tus datos permanentemente",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingContent = {
                    Icon(Icons.Default.DeleteForever, null,
                        Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable { mostrarDialogoEliminar = true }
            )
        }
    }
}
