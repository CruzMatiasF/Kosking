package com.kosking.app.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.scanner.components.*
import com.kosking.app.util.WhatsAppUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEscaner(
    vm: DuenoViewModel,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val escanerVm: EscanerViewModel = viewModel(factory = EscanerViewModel.factory(vm))

    val productos by vm.productos.collectAsState()
    val clientes by vm.clientes.collectAsState()
    val movimientos by vm.movimientosLocal.collectAsState()
    val cargandoVm by vm.cargando.collectAsState()
    val nombreLocal by vm.nombreLocal.collectAsState()

    val clientesOrdenados = remember(clientes, movimientos) {
        clientes.sortedByDescending { c ->
            movimientos.filter { it.clienteId == c.id }.maxOfOrNull { it.fecha.seconds } ?: 0L
        }
    }
    val filtradosCliente = remember(escanerVm.busquedaCliente, clientesOrdenados) {
        if (escanerVm.busquedaCliente.isBlank()) clientesOrdenados
        else clientesOrdenados.filter { it.nombre.contains(escanerVm.busquedaCliente, ignoreCase = true) }
    }
    val sugerencias = remember(escanerVm.queryBusqueda, productos) {
        if (escanerVm.queryBusqueda.isBlank()) emptyList()
        else productos.filter { it.nombre.contains(escanerVm.queryBusqueda, ignoreCase = true) }.take(3)
    }

    var permiso by remember { mutableStateOf(false) }
    val focusCliente = remember { FocusRequester() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permiso = granted }

    LaunchedEffect(Unit) {
        permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!permiso) launcher.launch(Manifest.permission.CAMERA)
    }

    // ── Pantalla confirmar (elige cliente al final) ──
    if (escanerVm.mostrarConfirmar) {
        PantallaConfirmarAnotacion(
            items = escanerVm.itemsAnotados,
            clientes = clientesOrdenados,
            movimientos = movimientos,
            cargando = cargandoVm,
            onConfirmar = { escanerVm.confirmarAnotacion(it, onCerrar) },
            onCrearYConfirmar = { nombre, onClienteCreado ->
                vm.agregarClienteYObtener(nombre = nombre, onExito = onClienteCreado)
            },
            onVolver = { escanerVm.mostrarConfirmar = false }
        )
        return
    }

    // ── Pantalla éxito ──
    escanerVm.anotacionExitosa?.let { (cliente, items) ->
        val hora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
        AnotacionExitosaScreen(
            cliente = cliente,
            items = items,
            onEnviarWhatsApp = {
                WhatsAppUtil.abrirWhatsApp(
                    context, cliente.telefono,
                    WhatsAppUtil.ticketVisita(
                        nombreCliente = cliente.nombre,
                        nombreLocal = nombreLocal,
                        items = items.map { Triple(it.nombre, it.precio, it.cantidad) },
                        tipo = "cargo",
                        hora = hora
                    )
                )
                escanerVm.anotacionExitosa = null
                onCerrar()
            },
            onCerrar = { escanerVm.anotacionExitosa = null; onCerrar() }
        )
        return
    }

    // ── Pantalla principal ──
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Área de cámara
        Box(modifier = Modifier.fillMaxWidth().weight(0.45f)) {
            if (permiso) {
                CamaraEscaner(
                    onCodigo = { escanerVm.onCodigo(it) },
                    isYaDetecto = { escanerVm.yaDetecto.get() },
                    buscandoProducto = escanerVm.buscandoProducto,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MarcaEscaner(
                        escaneoExitoso = escanerVm.escaneoExitoso,
                        modifier = Modifier.size(220.dp)
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Dar permiso de cámara")
                    }
                }
            }

            IconButton(
                onClick = onCerrar,
                modifier = Modifier.statusBarsPadding().padding(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }

            if (escanerVm.modo == ModoEscaner.ANOTAR) {
                ClienteOverlay(
                    clienteSeleccionado = escanerVm.clienteSeleccionado,
                    busquedaCliente = escanerVm.busquedaCliente,
                    mostrarBuscarCliente = escanerVm.mostrarBuscarCliente,
                    clientesOrdenados = clientesOrdenados,
                    filtradosCliente = filtradosCliente,
                    focusCliente = focusCliente,
                    onClienteSelected = {
                        escanerVm.clienteSeleccionado = it
                        escanerVm.mostrarBuscarCliente = false
                        escanerVm.busquedaCliente = ""
                    },
                    onBusquedaChange = { escanerVm.busquedaCliente = it; escanerVm.mostrarBuscarCliente = true },
                    onMostrarBuscar = { escanerVm.busquedaCliente = ""; escanerVm.mostrarBuscarCliente = true },
                    onLimpiarCliente = { escanerVm.clienteSeleccionado = null },
                    onNuevoCliente = {
                        escanerVm.nombreNuevoCliente = it
                        escanerVm.telefonoNuevoCliente = ""
                        escanerVm.mostrarBuscarCliente = false
                        escanerVm.busquedaCliente = ""
                        escanerVm.mostrarRegistroCliente = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .fillMaxWidth()
                )
            }
        }

        // Panel inferior
        Surface(modifier = Modifier.fillMaxWidth().weight(0.55f), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Tabs de modo
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabModifier = Modifier.weight(1f).height(36.dp)
                    val tabShape = RoundedCornerShape(50)
                    val tabPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

                    if (escanerVm.modo == ModoEscaner.ANOTAR) {
                        Button(onClick = {}, modifier = tabModifier, shape = tabShape, contentPadding = tabPadding) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anotar", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(onClick = { escanerVm.modo = ModoEscaner.ANOTAR; escanerVm.yaDetecto.set(false) }, modifier = tabModifier, shape = tabShape, contentPadding = tabPadding) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anotar", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (escanerVm.modo == ModoEscaner.CARGAR_NUEVO) {
                        Button(onClick = {}, modifier = tabModifier, shape = tabShape, contentPadding = tabPadding) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar nuevo", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (escanerVm.itemsAnotados.isNotEmpty()) escanerVm.mostrarDialogoCambioModo = true
                                else { escanerVm.modo = ModoEscaner.CARGAR_NUEVO; escanerVm.yaDetecto.set(false) }
                            },
                            modifier = tabModifier, shape = tabShape, contentPadding = tabPadding
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cargar nuevo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                HorizontalDivider()

                when (escanerVm.modo) {
                    ModoEscaner.ANOTAR -> EscanerAnotarPanel(
                        itemsAnotados = escanerVm.itemsAnotados,
                        queryBusqueda = escanerVm.queryBusqueda,
                        sugerencias = sugerencias,
                        clienteSeleccionado = escanerVm.clienteSeleccionado,
                        cargandoAnotacion = escanerVm.cargandoAnotacion,
                        onQueryChange = { escanerVm.queryBusqueda = it },
                        onAgregarSugerencia = { escanerVm.agregarALista(it) },
                        onAgregarManual = { escanerVm.agregarALista(ItemAnotado(it, 0.0)) },
                        onCambiarCantidad = { item, nueva ->
                            escanerVm.itemsAnotados = if (nueva <= 0)
                                escanerVm.itemsAnotados - item
                            else
                                escanerVm.itemsAnotados.map { if (it === item) it.copy(cantidad = nueva) else it }
                        },
                        onConfirmar = {
                            val cliente = escanerVm.clienteSeleccionado
                            if (cliente != null) escanerVm.confirmarAnotacion(cliente, onCerrar)
                            else escanerVm.mostrarConfirmar = true
                        }
                    )
                    ModoEscaner.CARGAR_NUEVO -> EscanerCargarPanel(
                        productoParaCargar = escanerVm.productoParaCargar,
                        nombreCarga = escanerVm.nombreCarga,
                        precioCarga = escanerVm.precioCarga,
                        cargandoVm = cargandoVm,
                        onNombreChange = { escanerVm.nombreCarga = it },
                        onPrecioChange = { escanerVm.precioCarga = it.filter { c -> c.isDigit() || c == '.' } },
                        onGuardar = { escanerVm.guardarProducto() },
                        onEscanearOtro = {
                            escanerVm.productoParaCargar = null
                            escanerVm.yaDetecto.set(false)
                            escanerVm.precioCarga = ""
                            escanerVm.nombreCarga = ""
                        }
                    )
                }
            }
        }
    }

    // ── Sheet registro de nuevo cliente ──
    if (escanerVm.mostrarRegistroCliente) {
        RegistroClienteSheet(
            nombreInicial = escanerVm.nombreNuevoCliente,
            telefonoInicial = escanerVm.telefonoNuevoCliente,
            onNombreChange = { escanerVm.nombreNuevoCliente = it },
            onTelefonoChange = { escanerVm.telefonoNuevoCliente = it },
            cargando = false,
            onCrear = {
                escanerVm.agregarClienteYObtener(
                    nombre = escanerVm.nombreNuevoCliente.trim(),
                    telefono = escanerVm.telefonoNuevoCliente.trim()
                ) { nuevo ->
                    escanerVm.clienteSeleccionado = nuevo
                    escanerVm.mostrarRegistroCliente = false
                }
            },
            onDismiss = { escanerVm.mostrarRegistroCliente = false }
        )
    }

    // ── Diálogo cambio de modo ──
    if (escanerVm.mostrarDialogoCambioModo) {
        AlertDialog(
            onDismissRequest = { escanerVm.mostrarDialogoCambioModo = false },
            title = { Text("¿Cambiar de modo?") },
            text = {
                val n = escanerVm.itemsAnotados.size
                Text("Tenés $n producto${if (n != 1) "s" else ""} en la lista. Si cambiás a Cargar nuevo los perdés.")
            },
            confirmButton = {
                TextButton(onClick = {
                    escanerVm.mostrarDialogoCambioModo = false
                    escanerVm.itemsAnotados = emptyList()
                    escanerVm.modo = ModoEscaner.CARGAR_NUEVO
                    escanerVm.yaDetecto.set(false)
                }) { Text("Sí, cambiar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { escanerVm.mostrarDialogoCambioModo = false }) { Text("Cancelar") }
            }
        )
    }
}
