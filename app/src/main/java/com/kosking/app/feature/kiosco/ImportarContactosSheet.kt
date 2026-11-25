package com.kosking.app.feature.kiosco

import android.content.ContentResolver
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.dueno.DuenoViewModel

data class ContactoTelefono(val nombre: String, val telefono: String)

private fun leerContactos(cr: ContentResolver): List<ContactoTelefono> {
    val resultado = mutableListOf<ContactoTelefono>()
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    cr.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null, null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
            val nombre = cursor.getString(nameIdx)?.trim() ?: continue
            val telefono = cursor.getString(phoneIdx)?.trim() ?: continue
            if (nombre.isNotBlank() && telefono.isNotBlank()) {
                resultado += ContactoTelefono(nombre, telefono)
            }
        }
    }
    // Deduplicar por nombre + teléfono normalizado
    return resultado
        .distinctBy { it.nombre.lowercase() + it.telefono.filter { c -> c.isDigit() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarContactosSheet(
    vm: DuenoViewModel,
    clientesExistentes: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var contactos by remember { mutableStateOf<List<ContactoTelefono>?>(null) }
    var permisoDenegado by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    val seleccionados = remember { mutableStateMapOf<String, ContactoTelefono>() }
    var importando by remember { mutableStateOf(false) }
    var importadosCount by remember { mutableStateOf<Int?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactos = leerContactos(context.contentResolver)
        } else {
            permisoDenegado = true
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(android.Manifest.permission.READ_CONTACTS)
    }

    // Nombres de clientes existentes (lowercase) para skip
    val nombresExistentes = remember(clientesExistentes) {
        clientesExistentes.map { it.lowercase() }.toSet()
    }

    val contactosFiltrados = remember(contactos, busqueda, nombresExistentes) {
        contactos?.filter { c ->
            (busqueda.isBlank() || c.nombre.contains(busqueda, ignoreCase = true)) &&
            c.nombre.lowercase() !in nombresExistentes
        } ?: emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Importar contactos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (contactos != null) {
                        Text(
                            "${contactosFiltrados.size} contactos disponibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }

            when {
                importadosCount != null -> {
                    // Resultado final
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Se importaron $importadosCount cliente${if (importadosCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(onClick = onDismiss) { Text("Listo") }
                        }
                    }
                }

                permisoDenegado -> {
                    Box(
                        Modifier.fillMaxWidth().height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContactPage,
                                null,
                                Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                "Sin acceso a contactos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Habilitá el permiso de contactos desde Ajustes del teléfono para importar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                contactos == null -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                contactosFiltrados.isEmpty() && busqueda.isEmpty() -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Todos tus contactos ya están importados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                else -> {
                    // Buscador
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(Modifier.padding(horizontal = 12.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                BasicTextField(
                                    value = busqueda,
                                    onValueChange = { busqueda = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                    decorationBox = { inner ->
                                        Box {
                                            if (busqueda.isEmpty()) Text("Buscar…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            inner()
                                        }
                                    }
                                )
                                if (busqueda.isNotEmpty()) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp).clickable { busqueda = "" }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Seleccionar todos
                    if (contactosFiltrados.isNotEmpty()) {
                        val todosSeleccionados = contactosFiltrados.all { seleccionados.containsKey(it.nombre + it.telefono) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (todosSeleccionados) {
                                        contactosFiltrados.forEach { seleccionados.remove(it.nombre + it.telefono) }
                                    } else {
                                        contactosFiltrados.forEach { seleccionados[it.nombre + it.telefono] = it }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = todosSeleccionados,
                                onCheckedChange = null
                            )
                            Text(
                                if (todosSeleccionados) "Deseleccionar todos" else "Seleccionar todos",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (seleccionados.isNotEmpty()) {
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${seleccionados.size} seleccionados",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(contactosFiltrados, key = { it.nombre + it.telefono }) { contacto ->
                            val key = contacto.nombre + contacto.telefono
                            val estaSeleccionado = seleccionados.containsKey(key)
                            ListItem(
                                headlineContent = {
                                    Text(contacto.nombre, fontWeight = FontWeight.Medium)
                                },
                                supportingContent = {
                                    Text(
                                        contacto.telefono,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingContent = {
                                    Checkbox(
                                        checked = estaSeleccionado,
                                        onCheckedChange = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (estaSeleccionado) seleccionados.remove(key)
                                    else seleccionados[key] = contacto
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            // Botón importar
            if (importadosCount == null && !permisoDenegado && contactos != null && seleccionados.isNotEmpty()) {
                HorizontalDivider()
                Button(
                    onClick = {
                        importando = true
                        val pares = seleccionados.values.map { it.nombre to it.telefono }
                        vm.importarContactos(pares) { n ->
                            importando = false
                            importadosCount = n
                        }
                    },
                    enabled = !importando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (importando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Importar ${seleccionados.size} cliente${if (seleccionados.size != 1) "s" else ""}")
                }
            }
        }
    }
}
