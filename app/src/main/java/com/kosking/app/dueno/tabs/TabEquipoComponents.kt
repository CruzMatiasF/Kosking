package com.kosking.app.dueno.tabs

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
import com.kosking.app.ui.theme.Verde
import java.text.SimpleDateFormat

internal data class StatPersona(
    val uid: String,
    val nombre: String,
    val anoto: Int,
    val cobro: Double
)

@Composable
internal fun CardCajaHoy(
    statsHoy: List<StatPersona>,
    totalCobradoHoy: Double,
    duenoUid: String,
    onVerPeriodo: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Caja de hoy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cobrado $${"%.0f".format(totalCobradoHoy)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Verde,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = onVerPeriodo,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Ver período", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quién", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("Anotó", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
                Text("Cobró", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
            }
            HorizontalDivider()
            statsHoy.forEach { stat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stat.nombre,
                        fontWeight = if (stat.uid == duenoUid) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f))
                    Text("${stat.anoto}", modifier = Modifier.width(48.dp),
                        style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$${"%.0f".format(stat.cobro)}",
                        modifier = Modifier.width(72.dp),
                        color = if (stat.cobro > 0) Verde else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (stat.cobro > 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
internal fun CardEmpleado(
    emp: Empleado,
    ultimoMov: Movimiento?,
    reclamos: Int,
    esActivoHoy: Boolean,
    inicioDia: Long,
    fmtFecha: SimpleDateFormat,
    fmtHora: SimpleDateFormat,
    onEliminar: () -> Unit
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
        }
    }
}
