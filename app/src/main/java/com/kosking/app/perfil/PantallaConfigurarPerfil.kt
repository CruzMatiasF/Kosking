package com.kosking.app.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Local
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfigurarPerfil(
    nombreActual: String,
    emailActual: String,
    localData: Local? = null,
    onGuardarLocal: ((nombre: String, direccion: String, telefono: String) -> Unit)? = null,
    onVolver: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreActual) }
    var nombreLocal by remember(localData) { mutableStateOf(localData?.nombre ?: "") }
    var direccionLocal by remember(localData) { mutableStateOf(localData?.direccion ?: "") }
    var telefonoLocal by remember(localData) { mutableStateOf(localData?.telefono ?: "") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar perfil") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (nombre.isNotBlank()) {
                    Text(
                        nombre.first().uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Person, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Datos del usuario
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("TU CUENTA", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = androidx.compose.ui.Modifier.padding(start = 4.dp, bottom = 4.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = "" },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = error.isNotBlank()
                )
                OutlinedTextField(
                    value = emailActual,
                    onValueChange = {},
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            // Datos del local
            if (onGuardarLocal != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("TU LOCAL", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = androidx.compose.ui.Modifier.padding(start = 4.dp))
                    OutlinedTextField(
                        value = nombreLocal,
                        onValueChange = { nombreLocal = it },
                        label = { Text("Nombre del local") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = direccionLocal,
                        onValueChange = { direccionLocal = it },
                        label = { Text("Dirección") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = telefonoLocal,
                        onValueChange = { telefonoLocal = it },
                        label = { Text("Teléfono del local") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    if (nombre.isBlank()) { error = "El nombre no puede estar vacío"; return@Button }
                    guardando = true
                    scope.launch {
                        try {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                FiadoRepository.actualizarNombreUsuario(uid, nombre.trim())
                                FirebaseAuth.getInstance().currentUser?.updateProfile(
                                    userProfileChangeRequest { displayName = nombre.trim() }
                                )
                            }
                            if (onGuardarLocal != null && nombreLocal.isNotBlank()) {
                                onGuardarLocal(nombreLocal.trim(), direccionLocal.trim(), telefonoLocal.trim())
                            }
                            onVolver()
                        } catch (e: Exception) {
                            error = "No se pudo guardar. Intentá de nuevo."
                        } finally {
                            guardando = false
                        }
                    }
                },
                enabled = !guardando && nombre.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Guardar cambios")
            }
        }
    }
}
