package com.kosking.app.dueno.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local
import com.kosking.app.ui.theme.Verde
import java.util.Calendar

@Composable
internal fun TabMiLocal(
    localData: Local?,
    onEditarDatos: () -> Unit,
    onEditarDireccion: () -> Unit,
    onEditarHorarios: () -> Unit
) {
    if (localData == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val estaAbierto = remember(localData.horarios) {
        val cal = Calendar.getInstance()
        val diaNombre = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "lunes"
            Calendar.TUESDAY -> "martes"
            Calendar.WEDNESDAY -> "miercoles"
            Calendar.THURSDAY -> "jueves"
            Calendar.FRIDAY -> "viernes"
            Calendar.SATURDAY -> "sabado"
            else -> "domingo"
        }
        val h = localData.horarios[diaNombre]
        if (h?.tieneCarga == true) {
            val minutos = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            fun parse(t: String) = t.split(":").let { p ->
                if (p.size == 2) (p[0].toIntOrNull() ?: return@let null) * 60 + (p[1].toIntOrNull() ?: return@let null)
                else null
            }
            val abre = parse(h.apertura) ?: 0
            val cierra = parse(h.cierre) ?: 0
            minutos in abre until cierra
        } else false
    }

    // Agrupar días con mismo horario (Lun–Vie 08:00–20:00 en vez de 5 filas)
    data class GrupoHorario(val label: String, val horario: HorarioDia?)
    val diasClaves = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
    val diasCortos = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val grupos = remember(localData.horarios) {
        val result = mutableListOf<GrupoHorario>()
        var i = 0
        while (i < diasClaves.size) {
            val h = localData.horarios[diasClaves[i]]
            var j = i + 1
            while (j < diasClaves.size &&
                localData.horarios[diasClaves[j]]?.apertura == h?.apertura &&
                localData.horarios[diasClaves[j]]?.cierre == h?.cierre) j++
            val label = if (j - i == 1) diasCortos[i] else "${diasCortos[i]} – ${diasCortos[j - 1]}"
            result.add(GrupoHorario(label, h))
            i = j
        }
        result
    }
    val hayHorarios = localData.horarios.values.any { it.tieneCarga }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {

        // ── Header card ──────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            localData.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "K",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                localData.nombre.ifBlank { "Sin nombre" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (estaAbierto) Verde
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    if (estaAbierto) "Abierto" else "Cerrado",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (estaAbierto) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            localData.tipo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                        if (localData.descripcion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                localData.descripcion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (localData.telefono.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Default.Phone, null, Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                                    Text(localData.telefono, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                }
                            }
                            if (localData.direccion.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.weight(1f, fill = false)) {
                                    Icon(Icons.Default.LocationOn, null, Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                                    Text(localData.direccion, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditarDatos, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar datos")
                    }
                    OutlinedButton(
                        onClick = onEditarDireccion, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dirección")
                    }
                }
            }
        }

        // ── Horarios ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horarios", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onEditarHorarios, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (!hayHorarios) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column {
                            Text("Sin horarios configurados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tocá Editar para agregar tus horarios",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        grupos.forEachIndexed { i, grupo ->
                            val abierto = grupo.horario?.tieneCarga == true
                            ListItem(
                                headlineContent = { Text(grupo.label) },
                                trailingContent = {
                                    Text(
                                        if (abierto) "${grupo.horario!!.apertura} – ${grupo.horario.cierre}"
                                        else "Cerrado",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (abierto) FontWeight.Medium else FontWeight.Normal,
                                        color = if (abierto) Verde
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            if (i < grupos.lastIndex)
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

    }
}
