package com.kosking.app.dueno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.ui.theme.*

private val categoriasProducto = listOf(
    "Bebidas", "Alimentos", "Snacks", "Lácteos", "Limpieza",
    "Farmacia", "Cigarrillos", "Golosinas", "Fiambres", "Otro",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarProducto(navController: NavController, vm: AgregarProductoViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    var nombre by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var precioTexto by remember { mutableStateOf("") }
    var cantidadTexto by remember { mutableStateOf("1") }
    var categoria by remember { mutableStateOf("Bebidas") }
    var categoriaExpanded by remember { mutableStateOf(false) }

    val precio = precioTexto.toDoubleOrNull() ?: 0.0
    val cantidad = cantidadTexto.toIntOrNull() ?: 0
    val formularioValido = nombre.isNotBlank() && precio > 0 && cantidad >= 0

    // Al guardar con éxito, ofrecer agregar otro o volver
    val exito = estado is EstadoAgregar.Exito

    LaunchedEffect(exito) {
        if (exito) {
            kotlinx.coroutines.delay(1500)
            // Reset para agregar otro
            nombre = ""; marca = ""; precioTexto = ""; cantidadTexto = "1"
            vm.estado.value = EstadoAgregar.Idle
        }
    }

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(HeaderGradient).statusBarsPadding()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Agregar producto", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                        Text("Nuevo ítem al inventario", fontSize = 12.sp, color = Blanco.copy(alpha = 0.65f))
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Blanco, shadowElevation = 8.dp) {
                Column(modifier = Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.agregar(nombre, marca, precio, cantidad, categoria) },
                        enabled = formularioValido && estado !is EstadoAgregar.Guardando,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Azul, contentColor = Blanco,
                            disabledContainerColor = Gris200, disabledContentColor = Gris500,
                        ),
                    ) {
                        when (estado) {
                            is EstadoAgregar.Guardando -> {
                                CircularProgressIndicator(color = Blanco, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Guardando...", fontWeight = FontWeight.Bold)
                            }
                            is EstadoAgregar.Exito -> {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("¡Agregado! Podés agregar otro", fontWeight = FontWeight.Bold)
                            }
                            else -> {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar al stock", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gris700),
                    ) {
                        Text("Volver al panel", fontSize = 14.sp)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Vista previa del producto ─────────────────────────────────────
            Surface(shape = RoundedCornerShape(16.dp), color = Blanco, shadowElevation = 2.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).background(AzulFondo, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(categoriaEmoji(categoria), fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            nombre.ifBlank { "Nombre del producto" },
                            fontWeight = FontWeight.Bold,
                            color = if (nombre.isBlank()) Gris500 else AzulNavy,
                            fontSize = 15.sp,
                        )
                        Text(
                            marca.ifBlank { "Marca" },
                            fontSize = 12.sp,
                            color = Gris500,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (precio > 0) Text("$${"%.0f".format(precio)}", fontWeight = FontWeight.ExtraBold, color = Rojo, fontSize = 16.sp)
                            if (cantidad >= 0) Text("Stock: $cantidad", fontSize = 12.sp, color = if (cantidad > 3) Verde else Naranja)
                        }
                    }
                }
            }

            // ── Información del producto ──────────────────────────────────────
            SeccionAgregar("Producto", Icons.Default.Inventory, Azul) {
                CampoAgregar(
                    valor = nombre, onCambio = { nombre = it },
                    etiqueta = "Nombre *", icono = Icons.Default.Label, color = Azul,
                    placeholder = "Ej: Coca-Cola 500ml",
                    caps = KeyboardCapitalization.Words,
                )
                Spacer(Modifier.height(12.dp))
                CampoAgregar(
                    valor = marca, onCambio = { marca = it },
                    etiqueta = "Marca", icono = Icons.Default.Sell, color = Azul,
                    placeholder = "Ej: Coca-Cola",
                    caps = KeyboardCapitalization.Words,
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = categoriaExpanded, onExpandedChange = { categoriaExpanded = it }) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        leadingIcon = { Text(categoriaEmoji(categoria), fontSize = 16.sp, modifier = Modifier.padding(start = 2.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = campoColors(Azul),
                    )
                    ExposedDropdownMenu(expanded = categoriaExpanded, onDismissRequest = { categoriaExpanded = false }) {
                        categoriasProducto.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(categoriaEmoji(cat), fontSize = 16.sp)
                                        Text(cat, fontSize = 14.sp)
                                    }
                                },
                                onClick = { categoria = cat; categoriaExpanded = false },
                                trailingIcon = if (cat == categoria) {
                                    { Icon(Icons.Default.Check, null, tint = Azul, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }

            // ── Precio y stock ────────────────────────────────────────────────
            SeccionAgregar("Precio y stock", Icons.Default.AttachMoney, Verde) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = precioTexto,
                        onValueChange = { if (it.all { c -> c.isDigit() }) precioTexto = it },
                        label = { Text("Precio *") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = campoColors(Verde),
                        isError = precioTexto.isNotBlank() && precio <= 0,
                        supportingText = if (precioTexto.isNotBlank() && precio <= 0) ({ Text("Debe ser mayor a 0") }) else null,
                    )
                    OutlinedTextField(
                        value = cantidadTexto,
                        onValueChange = { if (it.all { c -> c.isDigit() }) cantidadTexto = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = campoColors(Verde),
                        suffix = { Text("uds") },
                    )
                }
                // Indicador visual de nivel de stock
                if (cantidad > 0) {
                    Spacer(Modifier.height(8.dp))
                    val (color, label) = when {
                        cantidad > 10 -> Verde to "Stock suficiente"
                        cantidad > 3 -> Naranja to "Stock moderado"
                        else -> Rojo to "Stock bajo"
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.08f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Inventory, null, tint = color, modifier = Modifier.size(14.dp))
                            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (estado is EstadoAgregar.Error) {
        AlertDialog(
            onDismissRequest = { vm.estado.value = EstadoAgregar.Idle },
            title = { Text("Error") },
            text = { Text((estado as EstadoAgregar.Error).mensaje) },
            confirmButton = { TextButton(onClick = { vm.estado.value = EstadoAgregar.Idle }) { Text("OK") } },
        )
    }
}

private fun categoriaEmoji(categoria: String) = when (categoria) {
    "Bebidas" -> "🥤"
    "Alimentos" -> "🍞"
    "Snacks" -> "🍪"
    "Lácteos" -> "🥛"
    "Limpieza" -> "🧹"
    "Farmacia" -> "💊"
    "Cigarrillos" -> "🚬"
    "Golosinas" -> "🍬"
    "Fiambres" -> "🥩"
    else -> "📦"
}

@Composable
private fun SeccionAgregar(titulo: String, icono: ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Blanco, shadowElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AzulNavy)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoAgregar(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    color: Color,
    placeholder: String = "",
    caps: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = Gris500, fontSize = 13.sp) }) else null,
        leadingIcon = { Icon(icono, null, tint = color, modifier = Modifier.size(18.dp)) },
        keyboardOptions = KeyboardOptions(capitalization = caps),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = campoColors(color),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun campoColors(color: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = color,
    unfocusedBorderColor = Gris500.copy(alpha = 0.4f),
    focusedLabelColor = color,
    unfocusedLabelColor = Gris500,
    focusedLeadingIconColor = color,
    focusedTextColor = AzulNavy,
    unfocusedTextColor = AzulNavy,
    cursorColor = color,
)
