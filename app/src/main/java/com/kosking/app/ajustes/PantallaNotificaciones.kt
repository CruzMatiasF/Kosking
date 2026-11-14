package com.kosking.app.ajustes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.PreferenciasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNotificaciones(
    esDueno: Boolean,
    onVolver: () -> Unit
) {
    val context = LocalContext.current

    var notifDeuda by remember { mutableStateOf(PreferenciasRepository.getNotifDeuda(context)) }
    var notifAcuerdos by remember { mutableStateOf(PreferenciasRepository.getNotifAcuerdos(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                "Alertas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )

            NotifSwitchItem(
                icono = Icons.Default.Notifications,
                titulo = "Recordatorio de deudas",
                subtitulo = "Aviso semanal de clientes con deuda pendiente",
                checked = notifDeuda,
                onCheckedChange = {
                    notifDeuda = it
                    PreferenciasRepository.setNotifDeuda(context, it)
                }
            )

            if (esDueno) {
                HorizontalDivider()
                NotifSwitchItem(
                    icono = Icons.Default.Event,
                    titulo = "Acuerdos vencidos",
                    subtitulo = "Aviso diario de compromisos de pago vencidos",
                    checked = notifAcuerdos,
                    onCheckedChange = {
                        notifAcuerdos = it
                        PreferenciasRepository.setNotifAcuerdos(context, it)
                    }
                )
            }
        }
    }
}

@Composable
private fun NotifSwitchItem(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icono, null,
            modifier = Modifier.size(22.dp).padding(end = 0.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
