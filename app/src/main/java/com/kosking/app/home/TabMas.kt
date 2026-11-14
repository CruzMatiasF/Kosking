package com.kosking.app.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun TabMas(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onIniciarSesion: () -> Unit,
    onRegistrarse: () -> Unit = {},
    onCerrarSesion: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onConfiguracion: () -> Unit,
    onNotificaciones: () -> Unit = {},
    onApariencia: () -> Unit = {},
    onExportar: () -> Unit = {},
    onEstadisticas: () -> Unit = {},
    onSoporte: () -> Unit = {},
    onAcercaDe: () -> Unit,
) {
    val context = LocalContext.current
    var mostrarDialogRol by remember { mutableStateOf(false) }

    if (mostrarDialogRol) {
        AlertDialog(
            onDismissRequest = { mostrarDialogRol = false },
            title = { Text("¿Cómo querés usar Kosking?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        onClick = { mostrarDialogRol = false; onSoyDueno() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏪", style = MaterialTheme.typography.headlineSmall)
                            Column {
                                Text("Soy el dueño", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Creo y administro mi local",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                        }
                    }
                    Card(
                        onClick = { mostrarDialogRol = false; onSoyEmpleado() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👥", style = MaterialTheme.typography.headlineSmall)
                            Column {
                                Text("Soy empleado", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Me uno con el código del dueño",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarDialogRol = false }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── PERFIL / CUENTA ──────────────────────────────────────────
        if (!estaLogueado) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Crear una cuenta es gratis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Con tu cuenta podés crear o unirte a un local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onRegistrarse,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Crear cuenta") }
            OutlinedButton(
                onClick = onIniciarSesion,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar sesión")
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAbrirPerfil)
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nombreUsuario.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombreUsuario.ifBlank { "Mi cuenta" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            esDueno -> "Dueño de local"
                            esEmpleado -> "Empleado"
                            else -> "Cuenta personal"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── BOTÓN PANEL ──────────────────────────────────────────────
        if (esDueno) {
            Button(onClick = onSoyDueno, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Store, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir mi panel")
            }
        }
        if (esEmpleado) {
            Button(onClick = onSoyEmpleado, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Badge, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ir a mi panel")
            }
        }
        if (estaLogueado && !esDueno && !esEmpleado) {
            Button(onClick = { mostrarDialogRol = true }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Store, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Configurar mi local")
            }
        }

        if (estaLogueado) {

            // ── MI LOCAL (solo dueño) ────────────────────────────────
            if (esDueno) {
                Spacer(Modifier.height(4.dp))
                SeccionHeader("MI LOCAL")
                Card(modifier = Modifier.fillMaxWidth()) {
                    MasItem(Icons.Default.Settings, "Configuración",
                        "Local, límite de crédito, empleados", onConfiguracion)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    MasItem(Icons.Default.BarChart, "Estadísticas",
                        "Deuda total, cobros del mes, actividad", onEstadisticas)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    MasItem(Icons.Default.PictureAsPdf, "Exportar datos",
                        "Generá PDFs del resumen del día o historial", onExportar)
                }
            }

            // ── PREFERENCIAS ─────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            SeccionHeader("PREFERENCIAS")
            Card(modifier = Modifier.fillMaxWidth()) {
                MasItem(Icons.Default.Notifications, "Notificaciones",
                    "Alertas de deudas y acuerdos", onNotificaciones)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                MasItem(Icons.Default.Palette, "Apariencia",
                    "Tema claro, oscuro o según el sistema", onApariencia)
            }

            // ── APP ──────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            SeccionHeader("APP")
            Card(modifier = Modifier.fillMaxWidth()) {
                MasItem(Icons.Default.HelpOutline, "Ayuda y soporte",
                    "WhatsApp, correo y preguntas frecuentes", onSoporte)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                MasItem(Icons.Default.Info, "Acerca de Kosking",
                    "Versión y créditos", onAcercaDe)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                MasItem(
                    icono = Icons.Default.Share,
                    titulo = "Compartir con un amigo",
                    subtitulo = "Recomendá Kosking",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT,
                                "¡Usá Kosking para manejar los fiaditos de tu local! Es gratis para empezar. Buscalo como \"Kosking\" en Play Store 📱")
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir Kosking"))
                    }
                )
            }

            // ── CERRAR SESIÓN ─────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cerrar sesión")
            }
        } else {
            // No logueado: solo sección APP
            Spacer(Modifier.height(4.dp))
            SeccionHeader("APP")
            Card(modifier = Modifier.fillMaxWidth()) {
                MasItem(Icons.Default.HelpOutline, "Ayuda y soporte",
                    "WhatsApp, correo y preguntas frecuentes", onSoporte)
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                MasItem(Icons.Default.Info, "Acerca de Kosking", "Versión y créditos", onAcercaDe)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SeccionHeader(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun MasItem(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = {
            Text(subtitulo, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(icono, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
