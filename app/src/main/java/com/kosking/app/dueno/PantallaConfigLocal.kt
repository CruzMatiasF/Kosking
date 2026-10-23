package com.kosking.app.dueno

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.kosking.app.modelo.TIPOS_DE_LOCAL
import com.kosking.app.ui.theme.*

private val BG = Brush.verticalGradient(
    listOf(Color(0xFF0D1B3E), Color(0xFF122560), Color(0xFF1A3A8A)),
)

private val categorias = TIPOS_DE_LOCAL.map { it.nombre }
private val diasOptions = listOf("Todos los días", "Lunes a Sábado", "Lunes a Viernes")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConfigLocal(navController: NavController, vm: ConfigLocalViewModel = viewModel()) {
    val tienda by vm.tienda.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()

    // Formulario inicializado con los datos actuales de Firestore
    var nombre by remember(tienda?.id) { mutableStateOf(tienda?.nombre ?: "") }
    var direccion by remember(tienda?.id) { mutableStateOf(tienda?.direccion ?: "") }
    var horarioApertura by remember(tienda?.id) { mutableStateOf(tienda?.horarioApertura ?: "") }
    var horarioCierre by remember(tienda?.id) { mutableStateOf(tienda?.horarioCierre ?: "") }
    var diasActivos by remember(tienda?.id) { mutableStateOf(tienda?.diasActivos ?: "Todos los días") }
    var telefono by remember(tienda?.id) { mutableStateOf(tienda?.telefono ?: "") }
    var categoria by remember(tienda?.id) { mutableStateOf(tienda?.categoria ?: "Kiosco") }
    var categoriaExpanded by remember { mutableStateOf(false) }
    var diasExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var obteniendoUbicacion by remember { mutableStateOf(false) }
    val ubicacionGuardada = tienda?.let { it.latitud != 0.0 || it.longitud != 0.0 } ?: false

    @SuppressLint("MissingPermission")
    val ubicacionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            obteniendoUbicacion = true
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { loc ->
                    obteniendoUbicacion = false
                    if (loc != null) vm.guardarUbicacion(loc.latitude, loc.longitude)
                }
                .addOnFailureListener { obteniendoUbicacion = false }
        }
    }

    // Sync cuando llega la tienda de Firestore por primera vez
    LaunchedEffect(tienda?.id) {
        tienda?.let { t ->
            if (nombre.isEmpty()) nombre = t.nombre
            if (direccion.isEmpty()) direccion = t.direccion
            if (horarioApertura.isEmpty()) horarioApertura = t.horarioApertura
            if (horarioCierre.isEmpty()) horarioCierre = t.horarioCierre
            if (diasActivos == "Todos los días" && t.diasActivos.isNotBlank()) diasActivos = t.diasActivos
            if (telefono.isEmpty()) telefono = t.telefono
            if (categoria == "Kiosco" && t.categoria.isNotBlank()) categoria = t.categoria
        }
    }

    // Volver atrás al guardar con éxito
    LaunchedEffect(estado) {
        if (estado is EstadoConfig.Exito) {
            kotlinx.coroutines.delay(1200)
            navController.popBackStack()
        }
    }

    val haycambios = tienda?.let { t ->
        nombre != t.nombre || direccion != t.direccion ||
            horarioApertura != t.horarioApertura || horarioCierre != t.horarioCierre ||
            diasActivos != t.diasActivos || telefono != t.telefono || categoria != t.categoria
    } ?: false

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(BG).statusBarsPadding()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Configuración del local", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Blanco)
                        Text(tienda?.nombre ?: "Cargando...", fontSize = 12.sp, color = Blanco.copy(alpha = 0.65f))
                    }
                    // Estado calculado (solo lectura — el override se maneja desde el Panel)
                    val estadoCalculado = tienda?.let { com.kosking.app.util.estaAbierto(it) } ?: false
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (estadoCalculado) Verde.copy(alpha = 0.2f) else Gris500.copy(alpha = 0.2f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                null,
                                tint = if (estadoCalculado) Verde else Gris500,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                if (estadoCalculado) "Abierto" else "Cerrado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estadoCalculado) Verde else Gris500,
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Blanco, shadowElevation = 8.dp) {
                Button(
                    onClick = { vm.guardar(nombre, direccion, horarioApertura, horarioCierre, diasActivos, telefono, categoria) },
                    enabled = haycambios && estado !is EstadoConfig.Guardando && tienda != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Azul,
                        contentColor = Blanco,
                        disabledContainerColor = Gris200,
                        disabledContentColor = Gris500,
                    ),
                ) {
                    when (estado) {
                        is EstadoConfig.Guardando -> {
                            CircularProgressIndicator(color = Blanco, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardando...", fontWeight = FontWeight.Bold)
                        }
                        is EstadoConfig.Exito -> {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("¡Guardado!", fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar cambios", fontWeight = FontWeight.Bold)
                        }
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
            // ── Información básica ─────────────────────────────────────────────
            SeccionConfig("Información básica", Icons.Default.Store, Azul) {
                CampoConfig(
                    valor = nombre,
                    onCambio = { nombre = it },
                    etiqueta = "Nombre del local",
                    icono = Icons.Default.Store,
                    color = Azul,
                )
                Spacer(Modifier.height(12.dp))
                // Selector de categoría
                ExposedDropdownMenuBox(
                    expanded = categoriaExpanded,
                    onExpandedChange = { categoriaExpanded = it },
                ) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        leadingIcon = { Icon(Icons.Default.Category, null, tint = Azul, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = fieldColors(Azul),
                    )
                    ExposedDropdownMenu(expanded = categoriaExpanded, onDismissRequest = { categoriaExpanded = false }) {
                        TIPOS_DE_LOCAL.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text("${tipo.emoji}  ${tipo.nombre}", fontSize = 14.sp) },
                                onClick = { categoria = tipo.nombre; categoriaExpanded = false },
                                leadingIcon = if (tipo.nombre == categoria) {
                                    { Icon(Icons.Default.Check, null, tint = Azul, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }

            // ── Ubicación ─────────────────────────────────────────────────────
            SeccionConfig("Ubicación", Icons.Default.LocationOn, Verde) {
                CampoConfig(
                    valor = direccion,
                    onCambio = { direccion = it },
                    etiqueta = "Dirección",
                    icono = Icons.Default.LocationOn,
                    color = Verde,
                    placeholder = "Ej: San Martín 456, Palpalá",
                )
                Spacer(Modifier.height(12.dp))
                // Botón para guardar coordenadas GPS
                OutlinedButton(
                    onClick = {
                        ubicacionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ))
                    },
                    enabled = !obteniendoUbicacion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Verde),
                ) {
                    if (obteniendoUbicacion) {
                        CircularProgressIndicator(color = Verde, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Obteniendo ubicación...")
                    } else {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (ubicacionGuardada) "Actualizar ubicación en el mapa" else "Usar mi ubicación actual")
                    }
                }
                if (ubicacionGuardada) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Verde, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ubicación guardada · visible en el mapa", fontSize = 11.sp, color = Verde)
                    }
                }
            }

            // ── Horarios ───────────────────────────────────────────────────────
            SeccionConfig("Horarios", Icons.Default.Schedule, Naranja) {
                // Días activos
                ExposedDropdownMenuBox(
                    expanded = diasExpanded,
                    onExpandedChange = { diasExpanded = it },
                ) {
                    OutlinedTextField(
                        value = diasActivos,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Días de atención") },
                        leadingIcon = { Icon(Icons.Default.DateRange, null, tint = Naranja, modifier = Modifier.size(18.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diasExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = fieldColors(Naranja),
                    )
                    ExposedDropdownMenu(expanded = diasExpanded, onDismissRequest = { diasExpanded = false }) {
                        diasOptions.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion, fontSize = 14.sp) },
                                onClick = { diasActivos = opcion; diasExpanded = false },
                                leadingIcon = if (opcion == diasActivos) {
                                    { Icon(Icons.Default.Check, null, tint = Naranja, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Hora apertura y cierre
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoConfig(
                        valor = horarioApertura,
                        onCambio = { horarioApertura = it },
                        etiqueta = "Apertura",
                        icono = Icons.Default.LockOpen,
                        color = Verde,
                        placeholder = "08:00",
                        modifier = Modifier.weight(1f),
                    )
                    CampoConfig(
                        valor = horarioCierre,
                        onCambio = { horarioCierre = it },
                        etiqueta = "Cierre",
                        icono = Icons.Default.Lock,
                        color = Rojo,
                        placeholder = "22:00",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Podés cerrar antes o quedarte más tiempo desde el Panel del dueño.",
                    fontSize = 11.sp,
                    color = Gris500,
                )
            }

            // ── Contacto ──────────────────────────────────────────────────────
            SeccionConfig("Contacto", Icons.Default.Phone, Rojo) {
                CampoConfig(
                    valor = telefono,
                    onCambio = { telefono = it },
                    etiqueta = "Teléfono / WhatsApp",
                    icono = Icons.Default.Phone,
                    color = Rojo,
                    placeholder = "Ej: 0388 4123456",
                )
            }

            // ── Código de invitación ──────────────────────────────────────────
            tienda?.codigoInvitacion?.takeIf { it.isNotBlank() }?.let { codigo ->
                SeccionConfig("Empleados", Icons.Default.Group, Naranja) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Naranja.copy(alpha = 0.08f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.VpnKey, null, tint = Naranja, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Código de invitación", fontSize = 11.sp, color = Gris500)
                                Text(codigo, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AzulNavy, letterSpacing = 3.sp)
                                Text("Compartí este código con tus empleados", fontSize = 11.sp, color = Gris500)
                            }
                        }
                    }
                }
            }
        }
    }

    // Error
    if (estado is EstadoConfig.Error) {
        AlertDialog(
            onDismissRequest = { vm.estado.value = EstadoConfig.Idle },
            title = { Text("Error al guardar") },
            text = { Text((estado as EstadoConfig.Error).mensaje) },
            confirmButton = { TextButton(onClick = { vm.estado.value = EstadoConfig.Idle }) { Text("OK") } },
        )
    }
}

@Composable
private fun SeccionConfig(titulo: String, icono: ImageVector, color: Color, content: @Composable ColumnScope.() -> Unit) {
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
private fun CampoConfig(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    color: Color,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = Gris500, fontSize = 13.sp) }) else null,
        leadingIcon = { Icon(icono, null, tint = color, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        modifier = modifier,
        colors = fieldColors(color),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors(color: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = color,
    unfocusedBorderColor = Gris500.copy(alpha = 0.4f),
    focusedLabelColor = color,
    unfocusedLabelColor = Gris500,
    focusedLeadingIconColor = color,
    focusedTextColor = AzulNavy,
    unfocusedTextColor = AzulNavy,
    cursorColor = color,
)
