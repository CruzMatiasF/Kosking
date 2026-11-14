package com.kosking.app.ajustes

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.PreferenciasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaApariencia(
    onVolver: () -> Unit,
    onTemaChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var temaSeleccionado by remember { mutableStateOf(PreferenciasRepository.getTema(context)) }

    val opciones = listOf(
        Triple("sistema", Icons.Default.PhoneAndroid, "Según el sistema"),
        Triple("claro", Icons.Default.LightMode, "Claro"),
        Triple("oscuro", Icons.Default.DarkMode, "Oscuro"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia") },
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "TEMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            opciones.forEach { (valor, icono, etiqueta) ->
                val seleccionado = temaSeleccionado == valor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (seleccionado) 2.dp else 1.dp,
                            color = if (seleccionado) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            temaSeleccionado = valor
                            PreferenciasRepository.setTema(context, valor)
                            onTemaChanged(valor)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        icono, null,
                        modifier = Modifier.size(22.dp),
                        tint = if (seleccionado) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        etiqueta,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (seleccionado) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (seleccionado) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
