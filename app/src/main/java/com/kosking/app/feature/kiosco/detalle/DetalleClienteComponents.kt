package com.kosking.app.feature.kiosco.detalle

import android.content.Context
import com.kosking.app.dueno.DuenoViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.ui.theme.Verde
import com.kosking.app.ui.theme.VerdeClaro
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fmtCompromiso = SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR"))

@Composable
internal fun HeroCliente(cliente: Cliente, localNombre: String = "") {
    val sinDeuda = cliente.deudaTotal <= 0

    val bgColor = if (sinDeuda) MaterialTheme.colorScheme.tertiaryContainer
                  else MaterialTheme.colorScheme.errorContainer
    val onBgColor = if (sinDeuda) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
    ) {
        Text(
            "Deuda total",
            style = MaterialTheme.typography.labelSmall,
            color = onBgColor.copy(alpha = 0.65f),
            letterSpacing = 0.8.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$${"%.0f".format(cliente.deudaTotal)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = onBgColor
            )
        }

        if (cliente.limiteCredito > 0) {
            Spacer(Modifier.height(8.dp))
            val progreso = (cliente.deudaTotal / cliente.limiteCredito).coerceIn(0.0, 1.0).toFloat()
            val superado = cliente.deudaTotal > cliente.limiteCredito
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = if (superado) MaterialTheme.colorScheme.error else Verde,
                trackColor = onBgColor.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (superado) "Superó el límite" else "${"%.0f".format(progreso * 100)}% usado",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBgColor.copy(alpha = 0.65f)
                )
                Text(
                    "Límite $${"%.0f".format(cliente.limiteCredito)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = onBgColor.copy(alpha = 0.65f)
                )
            }
        }

        if (cliente.domicilio.isNotBlank() || cliente.telefono.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cliente.domicilio.isNotBlank()) InfoChip(Icons.Default.Home, cliente.domicilio, onBgColor)
                if (cliente.telefono.isNotBlank()) InfoChip(Icons.Default.Phone, cliente.telefono, onBgColor)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, texto: String, onBgColor: Color) {
    Row(
        modifier = Modifier
            .background(
                color = onBgColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, Modifier.size(13.dp), tint = onBgColor.copy(alpha = 0.7f))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = onBgColor.copy(alpha = 0.9f))
    }
}

@Composable
internal fun ConfigTilesColapsable(cliente: Cliente, vm: DuenoViewModel) {
    var expandido by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandido = !expandido }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Configuración del cliente",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expandido) {
            ConfigTiles(cliente, vm)
        }
        HorizontalDivider()
    }
}
