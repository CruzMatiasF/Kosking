package com.kosking.app.autenticacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RubroOpcion(
    val tipo: String,
    val emoji: String,
    val descripcion: String,
    val disponible: Boolean = true
)

private val rubrosDisponibles = listOf(
    RubroOpcion("Kiosco",      "🏪", "Fiado, productos y clientes"),
    RubroOpcion("Almacén",     "🛒", "Próximamente", disponible = false),
    RubroOpcion("Peluquería",  "✂️", "Próximamente", disponible = false),
    RubroOpcion("Farmacia",    "💊", "Próximamente", disponible = false),
)

@Composable
fun PantallaElegirRubro(
    onRubroElegido: (tipo: String) -> Unit
) {
    var seleccionado by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Text("¿Qué tipo de negocio tenés?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Vamos a preparar las herramientas para tu local.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(rubrosDisponibles) { rubro ->
                TarjetaRubro(
                    rubro = rubro,
                    seleccionado = seleccionado == rubro.tipo,
                    onClick = { if (rubro.disponible) seleccionado = rubro.tipo }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { seleccionado?.let { onRubroElegido(it) } },
            enabled = seleccionado != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Continuar", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TarjetaRubro(
    rubro: RubroOpcion,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (rubro.disponible) 1f else 0.4f
    OutlinedCard(
        onClick = onClick,
        enabled = rubro.disponible,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (seleccionado)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(rubro.emoji, fontSize = 32.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                rubro.tipo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                rubro.descripcion,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        }
    }
}
