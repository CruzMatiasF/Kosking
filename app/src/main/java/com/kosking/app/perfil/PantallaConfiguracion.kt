package com.kosking.app.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.PreferenciasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    esDueno: Boolean,
    esEmpleado: Boolean,
    onVolver: () -> Unit,
    onMiLocal: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val context = LocalContext.current

    var notifDeuda by remember { mutableStateOf(PreferenciasRepository.getNotifDeuda(context)) }
    var notifAcuerdos by remember { mutableStateOf(PreferenciasRepository.getNotifAcuerdos(context)) }
    var limiteDefault by remember {
        mutableStateOf(PreferenciasRepository.getLimiteDefault(context).let {
            if (it > 0) "%.0f".format(it) else ""
        })
    }
    var empleadosVerDeuda by remember { mutableStateOf(PreferenciasRepository.getEmpleadosVerDeuda(context)) }

    val localLabel = when {
        esDueno -> "Editar datos del local"
        esEmpleado -> "Unirse a un local"
        else -> "Crear mi local"
    }
    val localSubtitulo = when {
        esDueno -> "Nombre, dirección y teléfono"
        esEmpleado -> "Ingresá el código de tu dueño"
        else -> "Registrá tu negocio en Kosking"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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

            // ── Mi local ───────────────────────────────────────────────
            Text(
                "Local",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
            ListItem(
                headlineContent = { Text(localLabel, fontWeight = FontWeight.Medium) },
                supportingContent = { Text(localSubtitulo, style = MaterialTheme.typography.bodySmall) },
                leadingContent = { Icon(Icons.Default.Store, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable(onClick = onMiLocal)
            )

            // ── Notificaciones ──────────────────────────────────────────
            HorizontalDivider()
            Text(
                "Notificaciones",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            SwitchItem(
                titulo = "Recordatorio de deudas",
                subtitulo = "Aviso semanal de clientes con deuda pendiente",
                checked = notifDeuda,
                onCheckedChange = { notifDeuda = it; PreferenciasRepository.setNotifDeuda(context, it) }
            )
            if (esDueno) {
                HorizontalDivider()
                SwitchItem(
                    titulo = "Acuerdos vencidos",
                    subtitulo = "Aviso diario de compromisos de pago vencidos",
                    checked = notifAcuerdos,
                    onCheckedChange = { notifAcuerdos = it; PreferenciasRepository.setNotifAcuerdos(context, it) }
                )
            }

            // ── Solo dueño ──────────────────────────────────────────────
            if (esDueno) {
                HorizontalDivider()
                Text(
                    "Mi negocio",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                OutlinedTextField(
                    value = limiteDefault,
                    onValueChange = { v ->
                        limiteDefault = v
                        val num = v.toDoubleOrNull()
                        if (num != null) PreferenciasRepository.setLimiteDefault(context, num)
                        else if (v.isBlank()) PreferenciasRepository.setLimiteDefault(context, 0.0)
                    },
                    label = { Text("Límite de crédito por defecto") },
                    placeholder = { Text("Sin límite") },
                    suffix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Se aplica al agregar un cliente nuevo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                SwitchItem(
                    titulo = "Empleados ven deuda total",
                    subtitulo = "Permitir que tus empleados vean el monto total adeudado",
                    checked = empleadosVerDeuda,
                    onCheckedChange = { empleadosVerDeuda = it; PreferenciasRepository.setEmpleadosVerDeuda(context, it) }
                )
            }
        }
    }
}

@Composable
private fun SwitchItem(
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
