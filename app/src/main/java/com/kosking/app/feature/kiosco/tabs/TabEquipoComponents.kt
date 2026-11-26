package com.kosking.app.feature.kiosco.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat

@Composable
internal fun CardEmpleado(
    emp: Empleado,
    ultimoMov: Movimiento?,
    reclamos: Int,
    esActivoHoy: Boolean,
    inicioDia: Long,
    fmtFecha: SimpleDateFormat,
    fmtHora: SimpleDateFormat,
    onEliminar: () -> Unit,
    onToggleVerDeuda: (Boolean) -> Unit = {}
) {
    val avatarColors = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val (avatarBg, avatarFg) = avatarColors[Math.floorMod(emp.nombre.hashCode(), 3)]

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(
                start = 12.dp, end = 4.dp, top = 10.dp,
                bottom = if (esActivoHoy || reclamos > 0) 8.dp else 10.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        emp.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = avatarFg
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(emp.nombre, fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge)
                    if (ultimoMov != null) {
                        val accion = if (ultimoMov.tipo == "cargo") "Anotó" else "Cobró"
                        val esHoy = ultimoMov.fecha.toDate().time >= inicioDia
                        val fechaStr = if (esHoy) "hoy ${fmtHora.format(ultimoMov.fecha.toDate())}"
                                      else fmtFecha.format(ultimoMov.fecha.toDate())
                        Text("Último: $fechaStr · $accion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, "Desvincular",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            if (esActivoHoy || reclamos > 0) {
                Row(
                    Modifier.padding(start = 56.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (esActivoHoy) {
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Activo hoy",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    if (reclamos > 0) {
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer) {
                            Text("$reclamos reclamo${if (reclamos != 1) "s" else ""}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Puede ver deuda de clientes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = emp.puedeVerDeuda,
                    onCheckedChange = onToggleVerDeuda,
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}
