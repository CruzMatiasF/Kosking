package com.kosking.app.feature.kiosco.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.ui.theme.Verde

@Composable
internal fun CardsResumenDueno(
    localData: Local?,
    totalDeuda: Double,
    ventasHoy: Double = 0.0,
    clientesConDeuda: List<Cliente>,
    onAnotar: () -> Unit,
    onVender: () -> Unit = {},
    onAbrirProductos: () -> Unit,
    esEmpleado: Boolean = false,
    puedeVerDeuda: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (localData != null && localData.nombre.isNotBlank()) {
            Text(
                localData.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
            )
            if (localData.direccion.isNotBlank()) {
                Text(
                    localData.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                )
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }

        CardDeudaTotal(
            totalDeuda = totalDeuda,
            ventasHoy = ventasHoy,
            clientesConDeuda = clientesConDeuda,
            onAnotar = onAnotar,
            onVender = onVender,
            onAbrirProductos = onAbrirProductos,
            esEmpleado = esEmpleado,
            puedeVerDeuda = puedeVerDeuda,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 8.dp)
        )

        HorizontalDivider()
    }
}

@Composable
private fun CardDeudaTotal(
    totalDeuda: Double,
    ventasHoy: Double = 0.0,
    clientesConDeuda: List<Cliente>,
    onAnotar: () -> Unit,
    onVender: () -> Unit = {},
    onAbrirProductos: () -> Unit,
    esEmpleado: Boolean = false,
    puedeVerDeuda: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alDia = remember(clientesConDeuda) { clientesConDeuda.count { it.deudaTotal <= 0 } }
    var montoVisible by rememberSaveable { mutableStateOf(!esEmpleado) }

    if (esEmpleado) {
        Card(
            modifier = modifier.height(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Tu turno hoy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (ventasHoy > 0) {
                    Text(
                        "\$${"%.0f".format(ventasHoy)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Verde
                    )
                } else {
                    Text(
                        "\$0",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (ventasHoy > 0) "vendido en este turno" else "todavía nada vendido hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AtajoBtn(Icons.Default.ShoppingCart, "Vender", onVender, primary = true)
                    AtajoBtn(Icons.Default.Edit, "Anotar / Cobrar", onAnotar)
                    AtajoBtn(Icons.Default.Storefront, "Productos", onAbrirProductos)
                }
            }
        }
    } else {
        Card(
            modifier = modifier.height(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Te deben en total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ventasHoy > 0) {
                        Text(
                            "Vendido hoy \$${"%.0f".format(ventasHoy)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Verde
                        )
                    }
                }
                Text(
                    "$${"%.0f".format(totalDeuda)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (totalDeuda > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    buildString {
                        if (clientesConDeuda.isNotEmpty()) append("${clientesConDeuda.size} deben")
                        if (alDia > 0) append(" · $alDia al día")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AtajoBtn(Icons.Default.ShoppingCart, "Vender", onVender, primary = true)
                    AtajoBtn(Icons.Default.Edit, "Anotar / Cobrar", onAnotar)
                    AtajoBtn(Icons.Default.Storefront, "Productos", onAbrirProductos)
                }
            }
        }
    }
}


@Composable
private fun AtajoBtn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val bgColor = if (primary) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (primary) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = iconColor
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

