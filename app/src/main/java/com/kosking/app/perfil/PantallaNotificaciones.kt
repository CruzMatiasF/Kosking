package com.kosking.app.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Notificacion
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNotificaciones(onVolver: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val notificaciones = remember { mutableStateListOf<Notificacion>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FiadoRepository.notificacionesFlow(uid).collect { lista ->
            notificaciones.clear()
            notificaciones.addAll(lista)
        }
    }

    val sinLeer = notificaciones.count { !it.leida }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (sinLeer > 0) {
                        TextButton(onClick = {
                            scope.launch {
                                if (uid != null) FiadoRepository.marcarTodasLeidas(uid)
                            }
                        }) {
                            Text("Marcar leídas")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notificaciones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications, null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Sin notificaciones",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Acá vas a ver avisos de tus locales y novedades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                if (sinLeer > 0) {
                    item {
                        Text(
                            "Sin leer ($sinLeer)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(notificaciones.filter { !it.leida }, key = { it.id }) { notif ->
                        ItemNotificacion(notif) {
                            scope.launch { FiadoRepository.marcarNotificacionLeida(notif.id) }
                        }
                        HorizontalDivider()
                    }
                }

                val leidas = notificaciones.filter { it.leida }
                if (leidas.isNotEmpty()) {
                    item {
                        Text(
                            "Anteriores",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(leidas, key = { it.id }) { notif ->
                        ItemNotificacion(notif, onClick = null)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemNotificacion(notif: Notificacion, onClick: (() -> Unit)?) {
    val fmt = remember { SimpleDateFormat("d MMM · HH:mm", Locale.forLanguageTag("es")) }
    val icono: ImageVector = when (notif.tipo) {
        "oferta" -> Icons.Default.LocalOffer
        "local" -> Icons.Default.Store
        else -> Icons.Default.Notifications
    }

    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (!notif.leida) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icono, null,
                    modifier = Modifier.size(20.dp),
                    tint = if (!notif.leida) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        headlineContent = {
            Text(
                notif.titulo,
                fontWeight = if (!notif.leida) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Column {
                if (notif.cuerpo.isNotBlank()) {
                    Text(notif.cuerpo, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    fmt.format(notif.fecha.toDate()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        },
        trailingContent = if (!notif.leida) ({
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }) else null,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(
            containerColor = if (!notif.leida) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surface
        )
    )
}
