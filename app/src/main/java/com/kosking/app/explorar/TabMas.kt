package com.kosking.app.explorar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun TabMas(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    tieneLocalesFiado: Boolean,
    onIniciarSesion: () -> Unit,
    onCerrarSesionCuenta: () -> Unit,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onVerMiFiado: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConectarmeComoCliente: () -> Unit = {},
    onConfiguracion: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
    onHistorial: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (!estaLogueado) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Ingresar a Kosking", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Registrate para administrar tu local, entrar como empleado o llevar el control de tu fiado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth()) {
                            Text("Iniciar sesión / Crear cuenta")
                        }
                    }
                }
            }

            item {
                Text("Sin cuenta podés", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Explorar locales", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Buscá kioscos, almacenes y más cerca tuyo") },
                            leadingContent = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Ver ofertas", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Mirá los precios y promociones de cada negocio") },
                            leadingContent = { Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.secondary) }
                        )
                    }
                }
            }

        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth(), onClick = onConfigurarPerfil) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (nombreUsuario.firstOrNull() ?: "?").toString().uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nombreUsuario.ifBlank { "Mi cuenta" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            val rolesActivos = buildList {
                                if (esDueno) add("Dueño")
                                if (esEmpleado) add("Empleado")
                                if (tieneLocalesFiado) add("Cliente")
                            }
                            Text(
                                if (rolesActivos.isEmpty()) "Cuenta activa" else rolesActivos.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Text("Mi fiado", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Ver mi fiado", fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(if (tieneLocalesFiado) "Revisá lo que debés en tus locales guardados"
                                 else "Conectate a un local para ver tu fiado")
                        },
                        leadingContent = { Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(
                            onClick = if (tieneLocalesFiado) onVerMiFiado else onConectarmeComoCliente
                        )
                    )
                }
            }

            if (esEmpleado) {
                item {
                    Text("Trabajo", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onSoyEmpleado) {
                        ListItem(
                            headlineContent = { Text("Panel de empleado", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Registrá fiados y pagos de los clientes") },
                            leadingContent = { Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.secondary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                        )
                    }
                }
            }

            if (esDueno) {
                item {
                    Text("Mi negocio", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(modifier = Modifier.fillMaxWidth(), onClick = onSoyDueno) {
                        ListItem(
                            headlineContent = { Text("Administrar mi local", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Clientes, productos, empleados y actividad") },
                            leadingContent = { Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                        )
                    }
                }
            }

            val sinRoles = !esDueno && !esEmpleado && !tieneLocalesFiado
            if (sinRoles || (!esDueno && !esEmpleado)) {
                item {
                    Text("Unirse como", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            if (!esDueno) {
                                ListItem(
                                    headlineContent = { Text("Dueño de un local", fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text("Creá tu local y empezá a gestionar fiados") },
                                    leadingContent = { Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                    modifier = Modifier.clickable(onClick = onSoyDueno)
                                )
                            }
                            if (!esDueno && !esEmpleado) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (!esEmpleado) {
                                ListItem(
                                    headlineContent = { Text("Empleado", fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text("Ingresá el código que te dio el dueño") },
                                    leadingContent = { Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.secondary) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                                    modifier = Modifier.clickable(onClick = onSoyEmpleado)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text("Actividad", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Notificaciones", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Avisos de tus locales y novedades") },
                            leadingContent = { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable(onClick = onNotificaciones)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Historial", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Lo que viste y visitaste últimamente") },
                            leadingContent = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable(onClick = onHistorial)
                        )
                    }
                }
            }

            item {
                Text("Descubrir", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Ofertazos", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Las mejores promociones del momento") },
                            leadingContent = { Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.secondary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Categorías", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Explorá por tipo de local o producto") },
                            leadingContent = { Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.secondary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { }
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onCerrarSesionCuenta, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            Text("Kosking", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Tutoriales", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Aprendé a usar Kosking paso a paso") },
                        leadingContent = { Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable { }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Configuración", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Preferencias de la aplicación") },
                        leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(onClick = onConfiguracion)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Acerca de Kosking", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Versión, información y contacto") },
                        leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable(onClick = onAcercaDe)
                    )
                }
            }
        }

        item {
            Text("Kosking v1.0", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}
