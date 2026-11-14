package com.kosking.app.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    nombreUsuario: String,
    rol: String,
    esDueno: Boolean,
    onVolver: () -> Unit,
    onInformacionPersonal: () -> Unit,
    onSeguridad: () -> Unit = {},
    onPrivacidad: () -> Unit,
    onNotificaciones: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
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
        ) {
            // Header avatar + nombre
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nombreUsuario.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        nombreUsuario.ifBlank { "Mi cuenta" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        rol,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Secciones
            PerfilItem(
                icono = Icons.Default.Person,
                titulo = "Información personal",
                subtitulo = "Nombre, email, teléfono",
                onClick = onInformacionPersonal
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            PerfilItem(
                icono = Icons.Default.Lock,
                titulo = "Seguridad",
                subtitulo = "Contraseña y acceso",
                onClick = onSeguridad
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            PerfilItem(
                icono = Icons.Default.PrivacyTip,
                titulo = "Privacidad",
                subtitulo = "Política de privacidad",
                onClick = onPrivacidad
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
            PerfilItem(
                icono = Icons.Default.People,
                titulo = "Comunidades",
                subtitulo = "Próximamente",
                onClick = {},
                habilitado = false
            )
        }
    }
}

@Composable
private fun PerfilItem(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    habilitado: Boolean = true
) {
    ListItem(
        headlineContent = {
            Text(
                titulo,
                color = if (habilitado) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        },
        supportingContent = {
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (habilitado) 1f else 0.4f
                )
            )
        },
        leadingContent = {
            Icon(
                icono, null,
                Modifier.size(22.dp),
                tint = if (habilitado) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        },
        trailingContent = {
            if (habilitado) {
                Icon(
                    Icons.Default.ChevronRight, null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = if (habilitado) Modifier.clickable(onClick = onClick) else Modifier
    )
}
