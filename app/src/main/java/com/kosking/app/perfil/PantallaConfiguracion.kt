package com.kosking.app.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.PreferenciasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfiguracion(
    esDueno: Boolean,
    esEmpleado: Boolean,
    tieneLocalesFiado: Boolean,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    var pantallaInicio by remember {
        mutableStateOf(PreferenciasRepository.getPantallaInicio(context))
    }

    data class Opcion(val valor: String, val titulo: String, val descripcion: String, val icono: ImageVector)

    val opciones = buildList {
        add(Opcion(
            PreferenciasRepository.PANTALLA_EXPLORAR,
            "Inicio",
            "Ver locales, productos y novedades",
            Icons.Default.Home
        ))
        if (esEmpleado) add(Opcion(
            PreferenciasRepository.PANTALLA_EMPLEADO,
            "Panel de empleado",
            "Ir directo a los clientes y precios",
            Icons.Default.Badge
        ))
        if (esDueno) add(Opcion(
            PreferenciasRepository.PANTALLA_DUENO,
            "Panel del dueño",
            "Ir directo a administrar mi local",
            Icons.Default.Store
        ))
        if (esDueno || esEmpleado) add(Opcion(
            PreferenciasRepository.PANTALLA_PRODUCTOS,
            "Ver precios",
            "Ir directo a la lista de precios del local",
            Icons.Default.ShoppingCart
        ))
        if (tieneLocalesFiado) add(Opcion(
            PreferenciasRepository.PANTALLA_FIADO,
            "Ver mi fiado",
            "Ir directo a ver lo que debo",
            Icons.Default.Receipt
        ))
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Pantalla de inicio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Elegí qué pantalla ver cuando abrís la app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        opciones.forEachIndexed { index, opcion ->
                            val seleccionada = pantallaInicio == opcion.valor
                            ListItem(
                                headlineContent = {
                                    Text(opcion.titulo, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = { Text(opcion.descripcion) },
                                leadingContent = {
                                    Icon(
                                        opcion.icono, null,
                                        tint = if (seleccionada) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    RadioButton(
                                        selected = seleccionada,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    pantallaInicio = opcion.valor
                                    PreferenciasRepository.setPantallaInicio(context, opcion.valor)
                                }
                            )
                            if (index < opciones.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "El cambio se aplica la próxima vez que abrís la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
