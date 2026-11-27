package com.kosking.app.feature.kiosco.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.modelo.CategoriaLocal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaCategorias(
    categorias: List<CategoriaLocal>,
    onGuardar: (nombre: String, esPorPeso: Boolean, oldNombre: String?) -> Unit,
    onEliminar: (nombre: String) -> Unit,
    onVolver: () -> Unit
) {
    var mostrarDialog by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<CategoriaLocal?>(null) }

    BackHandler(onBack = onVolver)

    if (mostrarDialog) {
        DialogCategoria(
            categoria = editando,
            onGuardar = { nombre, esPorPeso ->
                onGuardar(nombre, esPorPeso, editando?.nombre)
                mostrarDialog = false
                editando = null
            },
            onEliminar = if (editando != null) ({
                onEliminar(editando!!.nombre)
                mostrarDialog = false
                editando = null
            }) else null,
            onDismiss = { mostrarDialog = false; editando = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { editando = null; mostrarDialog = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva")
                    }
                }
            )
        }
    ) { padding ->
        if (categorias.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.List, null,
                        Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Sin categorías",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Creá categorías para organizar tus productos.\nPodés indicar si se venden por unidad o por peso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { editando = null; mostrarDialog = true }) {
                        Text("Crear primera categoría")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(categorias, key = { it.nombre }) { cat ->
                    CategoriaCard(
                        categoria = cat,
                        onClick = { editando = cat; mostrarDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriaCard(
    categoria: CategoriaLocal,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                categoria.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        if (categoria.esPorPeso) "Por peso" else "Unidad",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun DialogCategoria(
    categoria: CategoriaLocal?,
    onGuardar: (String, Boolean) -> Unit,
    onEliminar: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var nombre by remember(categoria) { mutableStateOf(categoria?.nombre ?: "") }
    var esPorPeso by remember(categoria) { mutableStateOf(categoria?.esPorPeso ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoria != null) "Editar categoría" else "Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !esPorPeso,
                        onClick = { esPorPeso = false },
                        label = { Text("Unidad") }
                    )
                    FilterChip(
                        selected = esPorPeso,
                        onClick = { esPorPeso = true },
                        label = { Text("Por peso") }
                    )
                }
                if (onEliminar != null) {
                    TextButton(
                        onClick = onEliminar,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar categoría")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank()) onGuardar(nombre.trim(), esPorPeso) },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
