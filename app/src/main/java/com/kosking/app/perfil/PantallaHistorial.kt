package com.kosking.app.perfil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.FiadoRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(
    onVolver: () -> Unit,
    onVerLocal: (String) -> Unit = {}
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val historial = remember { mutableStateListOf<Map<String, Any>>() }

    LaunchedEffect(uid) {
        if (uid != null) {
            FiadoRepository.historialLocalesFlow(uid).collect { lista ->
                historial.clear()
                historial.addAll(lista)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (uid == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Iniciá sesión para ver tu historial",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        if (historial.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Todavía no visitaste ningún local",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Explorá y tocá un local para que aparezca acá.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text("Locales visitados recientemente",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            items(historial) { entry ->
                val localId = entry["localId"] as? String ?: return@items
                val nombreLocal = entry["nombreLocal"] as? String ?: "Local"
                val ts = entry["ultimaVisita"] as? Timestamp
                val fecha = ts?.toDate()?.let {
                    SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(it)
                } ?: ""

                ListItem(
                    headlineContent = { Text(nombreLocal, fontWeight = FontWeight.Medium) },
                    supportingContent = { if (fecha.isNotBlank()) Text(fecha) },
                    leadingContent = {
                        Icon(Icons.Default.Store, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onVerLocal(localId) }
                )
                HorizontalDivider()
            }
        }
    }
}
