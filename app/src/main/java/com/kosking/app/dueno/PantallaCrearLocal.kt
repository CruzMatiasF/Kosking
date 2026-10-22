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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
)

private val categoriasLocal = listOf("Kiosco", "Almacén", "Maxikiosco", "Despensa", "Minimercado", "Otro")

// Pasos del wizard
private enum class Paso { INFO_BASICA, UBICACION_HORARIO, LISTO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearLocal(navController: NavController, vm: CrearLocalViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    var paso by remember { mutableStateOf(Paso.INFO_BASICA) }
    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Kiosco") }
    var direccion by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var categoriaExpanded by remember { mutableStateOf(false) }

    // Navegar al panel cuando se crea el local
    LaunchedEffect(estado) {
        if (estado is EstadoCrearLocal.Exito) {
            paso = Paso.LISTO
        }
    }

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(BG).statusBarsPadding()) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (paso != Paso.LISTO) {
                            IconButton(onClick = {
                                when (paso) {
                                    Paso.INFO_BASICA -> navController.popBackStack()
                                    Paso.UBICACION_HORARIO -> paso = Paso.INFO_BASICA
                                    Paso.LISTO -> {}
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when (paso) {
                                    Paso.INFO_BASICA -> "Crear mi local"
                                    Paso.UBICACION_HORARIO -> "Ubicación y horario"
                                    Paso.LISTO -> "¡Local creado!"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Blanco,
                            )
                            Text(
                                when (paso) {
                                    Paso.INFO_BASICA -> "Paso 1 de 2 · Información básica"
                                    Paso.UBICACION_HORARIO -> "Paso 2 de 2 · Dónde y cuándo"
                                    Paso.LISTO -> "Ya podés empezar a vender"
                                },
                                fontSize = 12.sp,
                                color = Blanco.copy(alpha = 0.65f),
                            )
                        }
                    }
                    // Barra de progreso
                    if (paso != Paso.LISTO) {
                        LinearProgressIndicator(
                            progress = { if (paso == Paso.INFO_BASICA) 0.5f else 1f },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                            color = Rojo,
                            trackColor = Blanco.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (paso) {
            Paso.INFO_BASICA -> PasoInfoBasica(
                nombre = nombre,
                onNombre = { nombre = it },
                categoria = categoria,
                onCategoria = { categoria = it },
                categoriaExpanded = categoriaExpanded,
                onExpandCategoria = { categoriaExpanded = it },
                onSiguiente = { paso = Paso.UBICACION_HORARIO },
                modifier = Modifier.padding(padding),
            )
            Paso.UBICACION_HORARIO -> PasoUbicacionHorario(
                nombreLocal = nombre,
                direccion = direccion,
                onDireccion = { direccion = it },
                horario = horario,
                onHorario = { horario = it },
                telefono = telefono,
                onTelefono = { telefono = it },
                guardando = estado is EstadoCrearLocal.Guardando,
                onCrear = { vm.crearLocal(nombre, direccion, categoria, horario, telefono) },
                modifier = Modifier.padding(padding),
            )
            Paso.LISTO -> PantallaExito(
                nombre = nombre,
                onIrAlPanel = {
                    navController.navigate(Rutas.PANEL_DUENO) {
                        popUpTo(Rutas.HOME) { inclusive = false }
                    }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (estado is EstadoCrearLocal.Error) {
        AlertDialog(
            onDismissRequest = { vm.estado.value = EstadoCrearLocal.Idle },
            title = { Text("Error al crear el local") },
            text = { Text((estado as EstadoCrearLocal.Error).mensaje) },
            confirmButton = { TextButton(onClick = { vm.estado.value = EstadoCrearLocal.Idle }) { Text("OK") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasoInfoBasica(
    nombre: String,
    onNombre: (String) -> Unit,
    categoria: String,
    onCategoria: (String) -> Unit,
    categoriaExpanded: Boolean,
    onExpandCategoria: (Boolean) -> Unit,
    onSiguiente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Ilustración / intro
        Surface(shape = RoundedCornerShape(20.dp), color = Blanco, shadowElevation = 2.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🏪", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Contanos sobre tu local",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = AzulNavy,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta información la verán tus clientes en la app",
                    fontSize = 13.sp,
                    color = Gris500,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = Blanco, shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = onNombre,
                    label = { Text("Nombre del local *") },
                    placeholder = { Text("Ej: Kiosco El Cielo", color = Gris500, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Store, null, tint = Azul, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColorsCrear(Azul),
                )
                ExposedDropdownMenuBox(expanded = categoriaExpanded, onExpandedChange = onExpandCategoria) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de local *") },
                        leadingIcon = { Icon(Icons.Default.Category, null, tint = Azul, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = fieldColorsCrear(Azul),
                    )
                    ExposedDropdownMenu(expanded = categoriaExpanded, onDismissRequest = { onExpandCategoria(false) }) {
                        categoriasLocal.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontSize = 14.sp) },
                                onClick = { onCategoria(cat); onExpandCategoria(false) },
                                trailingIcon = if (cat == categoria) {
                                    { Icon(Icons.Default.Check, null, tint = Azul, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onSiguiente,
            enabled = nombre.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Azul, contentColor = Blanco,
                disabledContainerColor = Gris200, disabledContentColor = Gris500,
            ),
        ) {
            Text("Siguiente", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PasoUbicacionHorario(
    nombreLocal: String,
    direccion: String,
    onDireccion: (String) -> Unit,
    horario: String,
    onHorario: (String) -> Unit,
    telefono: String,
    onTelefono: (String) -> Unit,
    guardando: Boolean,
    onCrear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Vista previa del local
        Surface(shape = RoundedCornerShape(16.dp), color = Blanco, shadowElevation = 2.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🏪", fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(nombreLocal, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulNavy)
                    if (direccion.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Verde, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(direccion, fontSize = 12.sp, color = Gris500)
                        }
                    }
                    if (horario.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Azul, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(horario, fontSize = 12.sp, color = Gris500)
                        }
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = Blanco, shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = direccion,
                    onValueChange = onDireccion,
                    label = { Text("Dirección *") },
                    placeholder = { Text("Ej: San Martín 456, Palpalá", color = Gris500, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Verde, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColorsCrear(Verde),
                )
                OutlinedTextField(
                    value = horario,
                    onValueChange = onHorario,
                    label = { Text("Horario de atención") },
                    placeholder = { Text("Ej: Lun–Sáb 8:00–22:00 · Dom 9:00–20:00", color = Gris500, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = Naranja, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColorsCrear(Naranja),
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = onTelefono,
                    label = { Text("Teléfono / WhatsApp") },
                    placeholder = { Text("Ej: 0388 4123456", color = Gris500, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = Rojo, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColorsCrear(Rojo),
                )
            }
        }

        Button(
            onClick = onCrear,
            enabled = direccion.isNotBlank() && !guardando,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Rojo, contentColor = Blanco,
                disabledContainerColor = Gris200, disabledContentColor = Gris500,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        ) {
            if (guardando) {
                CircularProgressIndicator(color = Blanco, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Creando tu local...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Store, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Crear mi local", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun PantallaExito(nombre: String, onIrAlPanel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 72.sp)
        Spacer(Modifier.height(20.dp))
        Text("¡Tu local está listo!", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = AzulNavy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "\"$nombre\" ya está en Kosking.\nAhora podés agregar tu stock y empezar a vender.",
            fontSize = 15.sp,
            color = Gris700,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onIrAlPanel,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Blanco),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        ) {
            Icon(Icons.Default.Dashboard, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ir a mi panel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColorsCrear(color: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = color,
    focusedLabelColor = color,
    focusedLeadingIconColor = color,
)
