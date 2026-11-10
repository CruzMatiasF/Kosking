package com.kosking.app.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kosking.app.cliente.components.PasoConectarse
import com.kosking.app.cliente.components.TarjetaLocal
import com.kosking.app.modelo.LocalGuardado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListaLocales(
    locales: List<LocalGuardado>,
    deudasPorLocal: Map<String, Double>,
    onSeleccionar: (LocalGuardado) -> Unit,
    onQuitarLocal: (LocalGuardado) -> Unit,
    onVolver: () -> Unit,
    onConectarme: () -> Unit,
    isRefrescando: Boolean = false,
    onRefrescar: () -> Unit = {}
) {
    var localAConfirmarQuitar by remember { mutableStateOf<LocalGuardado?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ver mi fiado") })
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefrescando,
            onRefresh = onRefrescar,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (locales.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Todavía no tenés locales conectados",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PasoConectarse(numero = "1", texto = "Andá al kiosco")
                                    PasoConectarse(numero = "2", texto = "Pedile al dueño que te agregue como cliente y te dé tu PIN")
                                    PasoConectarse(numero = "3", texto = "Volvé acá y tocá \"Conectarme\"")
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = onConectarme) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Conectarme a un local")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "Tocá un local para ver tus movimientos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    items(locales, key = { it.localId }) { local ->
                        val deuda = deudasPorLocal[local.localId] ?: 0.0
                        TarjetaLocal(
                            local = local,
                            deuda = deuda,
                            onClick = { onSeleccionar(local) },
                            onDesvincularse = { localAConfirmarQuitar = local }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = onConectarme,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Conectarme a otro local")
                        }
                    }
                }
            }
        }
    }

    localAConfirmarQuitar?.let { local ->
        AlertDialog(
            onDismissRequest = { localAConfirmarQuitar = null },
            title = { Text("Desvincularse del local") },
            text = { Text("¿Querés desvincularte de ${local.nombreLocal}? Podés volver a conectarte con tu código cuando quieras.") },
            confirmButton = {
                TextButton(onClick = {
                    onQuitarLocal(local)
                    localAConfirmarQuitar = null
                }) { Text("Desvincularse", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { localAConfirmarQuitar = null }) { Text("Cancelar") }
            }
        )
    }
}
