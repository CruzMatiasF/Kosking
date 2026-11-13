package com.kosking.app.dueno

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.datos.CatalogoRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardPrimerUso(
    cargando: Boolean,
    onGuardarProducto: (nombre: String, precio: Double, onExito: () -> Unit) -> Unit,
    onGuardarCliente: (nombre: String, telefono: String, onExito: () -> Unit) -> Unit,
    onTerminar: () -> Unit
) {
    // 0 = producto, 1 = cliente, 2 = listo
    var paso by remember { mutableStateOf(0) }

    val context = LocalContext.current
    var nombreProducto by remember { mutableStateOf("") }
    var precioTexto by remember { mutableStateOf("") }
    var nombreCliente by remember { mutableStateOf("") }
    var telefonoCliente by remember { mutableStateOf("") }
    var productoGuardado by remember { mutableStateOf(false) }
    var clienteGuardado by remember { mutableStateOf(false) }
    var guardandoLocal by remember { mutableStateOf(false) }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Indicador de pasos
            PasosIndicador(pasoActual = paso, total = 3)

            AnimatedContent(
                targetState = paso,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
                },
                label = "wizard_paso"
            ) { p ->
                when (p) {
                    0 -> PasoProducto(
                        nombre = nombreProducto,
                        precio = precioTexto,
                        cargando = cargando || guardandoLocal,
                        errorLocal = errorLocal,
                        onNombreChange = { nombreProducto = it; errorLocal = null },
                        onPrecioChange = { precioTexto = it.filter { c -> c.isDigit() || c == '.' } },
                        onSiguiente = {
                            val precio = precioTexto.toDoubleOrNull() ?: 0.0
                            guardandoLocal = true
                            errorLocal = null
                            if (!CatalogoRepository.existeEnCache(context, nombreProducto)) {
                                CatalogoRepository.contribuir(nombreProducto.trim())
                            }
                            onGuardarProducto(nombreProducto.trim(), precio) {
                                guardandoLocal = false
                                productoGuardado = true
                                paso = 1
                            }
                        },
                        onSaltar = { paso = 1 }
                    )
                    1 -> PasoCliente(
                        nombre = nombreCliente,
                        telefono = telefonoCliente,
                        cargando = cargando || guardandoLocal,
                        errorLocal = errorLocal,
                        onNombreChange = { nombreCliente = it; errorLocal = null },
                        onTelefonoChange = { telefonoCliente = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                        onSiguiente = {
                            guardandoLocal = true
                            errorLocal = null
                            onGuardarCliente(nombreCliente.trim(), telefonoCliente.trim()) {
                                guardandoLocal = false
                                clienteGuardado = true
                                paso = 2
                            }
                        },
                        onSaltar = { paso = 2 }
                    )
                    else -> PasoListo(
                        productoGuardado = productoGuardado,
                        clienteGuardado = clienteGuardado,
                        onEmpezar = onTerminar
                    )
                }
            }
        }
    }
}

@Composable
private fun PasosIndicador(pasoActual: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val activo = i == pasoActual
            val completado = i < pasoActual
            Box(
                modifier = Modifier
                    .size(if (activo) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            completado -> MaterialTheme.colorScheme.primary
                            activo -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
            if (i < total - 1) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(
                            if (i < pasoActual) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun PasoProducto(
    nombre: String,
    precio: String,
    cargando: Boolean,
    errorLocal: String?,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onSaltar: () -> Unit
) {
    val context = LocalContext.current
    val sugerencias = remember(nombre) {
        if (nombre.length >= 2) CatalogoRepository.buscar(context, nombre).take(5)
        else emptyList()
    }
    val mostrarSugerencias = sugerencias.isNotEmpty() && precio.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconoCentrado(Icons.Default.Storefront)

        Text(
            "¿Qué vendés?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Buscá en el catálogo y poné tu precio. Si no existe, lo agregás vos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Buscar producto") },
            placeholder = { Text("Ej: Coca, pan, aceite...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Sugerencias del catálogo
        if (mostrarSugerencias) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(sugerencias) { item ->
                        ListItem(
                            headlineContent = { Text(item.nombre) },
                            supportingContent = { Text(item.categoria, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.clickable { onNombreChange(item.nombre) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        )
                        HorizontalDivider()
                    }
                    // Opción de agregar si no hay coincidencia exacta
                    if (sugerencias.none { it.nombre.equals(nombre.trim(), ignoreCase = true) }) {
                        item {
                            ListItem(
                                headlineContent = { Text("Agregar \"${nombre.trim()}\"") },
                                leadingContent = { Icon(Icons.Default.Add, null, Modifier.size(20.dp)) },
                                modifier = Modifier.clickable { /* nombre ya está puesto, solo cerramos sugerencias */ onNombreChange(nombre) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }
        }

        // Campo de precio (aparece cuando hay nombre)
        if (nombre.isNotBlank()) {
            OutlinedTextField(
                value = precio,
                onValueChange = onPrecioChange,
                label = { Text("Precio $") },
                placeholder = { Text("Ej: 500") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        errorLocal?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onSiguiente,
            enabled = nombre.isNotBlank() && !cargando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else Text("Guardar y continuar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = onSaltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ahora no, saltar este paso", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PasoCliente(
    nombre: String,
    telefono: String,
    cargando: Boolean,
    errorLocal: String?,
    onNombreChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onSaltar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconoCentrado(Icons.Default.People)

        Text(
            "¿Quién te debe?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Agregá tu primer cliente. El teléfono es opcional pero te permite avisarle la deuda por WhatsApp con un toque.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre del cliente") },
            placeholder = { Text("Ej: Juan, La señora del 3ro...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = telefono,
            onValueChange = onTelefonoChange,
            label = { Text("Teléfono (opcional)") },
            placeholder = { Text("Ej: 3413001234") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        errorLocal?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onSiguiente,
            enabled = nombre.isNotBlank() && !cargando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else Text("Guardar y continuar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = onSaltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ahora no, saltar este paso", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PasoListo(
    productoGuardado: Boolean,
    clienteGuardado: Boolean,
    onEmpezar: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            "¡Todo listo!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (productoGuardado) {
                FilaResumenWizard("Producto cargado", true)
            }
            if (clienteGuardado) {
                FilaResumenWizard("Cliente agregado", true)
            }
            FilaResumenWizard("Podés anotar desde Inicio o tocando + en un cliente", true)
            FilaResumenWizard("Agregá más productos y clientes cuando quieras", true)
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onEmpezar,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Empezar a usar Kosking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilaResumenWizard(texto: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IconoCentrado(icon: ImageVector) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
