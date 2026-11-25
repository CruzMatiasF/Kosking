package com.kosking.app.dueno

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.util.CrashLogger
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.ItemAnotado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Venta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DuenoViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    // true mientras no llegaron aún los primeros datos de Firestore
    private val _cargandoInicial = MutableStateFlow(true)
    val cargandoInicial: StateFlow<Boolean> = _cargandoInicial

    private val _localId = MutableStateFlow("")
    val localId: StateFlow<String> = _localId

    private val _nombreLocal = MutableStateFlow("")
    val nombreLocal: StateFlow<String> = _nombreLocal

    val localData: StateFlow<Local?> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.localFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _nombreDueno = MutableStateFlow("")

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            cerrarSesion()
        } else if (_localId.value.isBlank()) {
            viewModelScope.launch {
                val usuario = try { FiadoRepository.obtenerUsuario(uid) } catch (_: Exception) { null }
                if (usuario?.localId?.isNotBlank() == true) {
                    cargarDatos(usuario)
                } else {
                    _cargandoInicial.value = false
                }
            }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    private suspend fun cargarDatos(usuario: com.kosking.app.modelo.Usuario) {
        _nombreDueno.value = usuario.nombre
        if (usuario.localId.isNotBlank()) {
            _localId.value = usuario.localId
            val local = FiadoRepository.obtenerLocal(usuario.localId)
            _nombreLocal.value = local?.nombre ?: ""
            // Espera el primer emit real de Firestore (drop(1) saltea el emptyList() inicial del StateFlow).
            // Timeout de 8s por si no hay internet — el panel se muestra igual aunque esté vacío.
            withTimeoutOrNull(8_000) {
                val a = async { clientes.drop(1).first() }
                val b = async { movimientosLocal.drop(1).first() }
                a.await(); b.await()
            }
            _cargandoInicial.value = false
        } else {
            _cargandoInicial.value = false
        }
    }

    fun crearLocal(
        nombre: String,
        tipo: String = "Kiosco",
        direccion: String = "",
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val localId = FiadoRepository.nuevoLocalId()
                val local = Local(id = localId, nombre = nombre, duenoUid = uid,
                    tipo = tipo, direccion = direccion)
                FiadoRepository.crearLocal(local)
                FiadoRepository.actualizarLocalIdUsuario(uid, localId)
                _localId.value = localId
                _nombreLocal.value = nombre
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "crearLocal")
                _error.value = "No se pudo crear el local"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun actualizarLocalCompleto(
        nombre: String,
        direccion: String,
        telefono: String,
        horarios: Map<String, com.kosking.app.modelo.HorarioDia>,
        onExito: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val local = localData.value?.copy(
                    nombre = nombre,
                    direccion = direccion,
                    telefono = telefono,
                    horarios = horarios
                ) ?: return@launch
                FiadoRepository.actualizarLocal(local)
                _nombreLocal.value = nombre
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "actualizarLocalCompleto")
                _error.value = "No se pudo guardar"
            } finally {
                _cargando.value = false
            }
        }
    }

    val clientes: StateFlow<List<Cliente>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.clientesFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private var syncProductosHecho = false
        const val LIMITE_CLIENTES_GRATIS = 150
    }

    fun agregarCliente(nombre: String, telefono: String, domicilio: String, limiteCredito: Double = 0.0, deudaInicial: Double = 0.0, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                if (clientes.value.size >= LIMITE_CLIENTES_GRATIS) {
                    _error.value = "Límite de $LIMITE_CLIENTES_GRATIS clientes alcanzado."
                    return@launch
                }
                val cliente = Cliente(
                    localId = _localId.value,
                    nombre = nombre,
                    telefono = telefono,
                    domicilio = domicilio,
                    limiteCredito = limiteCredito
                )
                val clienteId = FiadoRepository.agregarCliente(cliente)
                if (deudaInicial > 0) {
                    val uid = auth.currentUser?.uid ?: ""
                    val mov = Movimiento(
                        localId = _localId.value,
                        clienteId = clienteId,
                        tipo = "cargo",
                        producto = "Deuda inicial",
                        monto = deudaInicial,
                        cantidad = 1,
                        anotadoPorUid = uid,
                        anotadoPorNombre = _nombreDueno.value
                    )
                    FiadoRepository.agregarMovimientos(listOf(mov))
                }
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "agregarCliente")
                _error.value = "No se pudo agregar el cliente"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun agregarClienteYObtener(nombre: String, telefono: String = "", onExito: (Cliente) -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                if (clientes.value.size >= LIMITE_CLIENTES_GRATIS) {
                    _error.value = "Límite de $LIMITE_CLIENTES_GRATIS clientes alcanzado."
                    return@launch
                }
                val cliente = Cliente(localId = _localId.value, nombre = nombre, telefono = telefono)
                val id = FiadoRepository.agregarCliente(cliente)
                onExito(cliente.copy(id = id))
            } catch (e: Exception) {
                CrashLogger.log(e, "agregarClienteYObtener")
                _error.value = "No se pudo agregar el cliente"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun importarContactos(contactos: List<Pair<String, String>>, onExito: (Int) -> Unit) {
        viewModelScope.launch {
            var importados = 0
            for ((nombre, telefono) in contactos) {
                if (clientes.value.size + importados >= LIMITE_CLIENTES_GRATIS) break
                try {
                    val cliente = Cliente(localId = _localId.value, nombre = nombre, telefono = telefono)
                    FiadoRepository.agregarCliente(cliente)
                    importados++
                } catch (e: Exception) {
                    CrashLogger.log(e, "importarContactos")
                }
            }
            onExito(importados)
        }
    }

    fun actualizarCliente(cliente: Cliente) {
        viewModelScope.launch {
            try { FiadoRepository.actualizarCliente(cliente) }
            catch (e: Exception) { CrashLogger.log(e, "actualizarCliente") }
        }
    }

    fun setFechaCompromiso(clienteId: String, fecha: com.google.firebase.Timestamp?) {
        viewModelScope.launch {
            try {
                FiadoRepository.setFechaCompromiso(clienteId, fecha)
            } catch (e: Exception) {
                CrashLogger.log(e, "setFechaCompromiso")
                _error.value = "No se pudo guardar la fecha"
            }
        }
    }

    // ── Movimiento seleccionado (para detalle) ────────────────────────────────

    private val _movimientoSeleccionado = MutableStateFlow<Movimiento?>(null)
    val movimientoSeleccionado: StateFlow<Movimiento?> = _movimientoSeleccionado.asStateFlow()

    fun seleccionarMovimiento(id: String) {
        val enCache = movimientosLocal.value.find { it.id == id }
            ?: movimientosDetalle.value.find { it.id == id }
        _movimientoSeleccionado.value = enCache
        if (enCache == null) {
            viewModelScope.launch {
                try {
                    val mov = FiadoRepository.obtenerMovimiento(id)
                    _movimientoSeleccionado.value = mov
                    if (mov == null) _error.value = "No se encontró el movimiento"
                } catch (e: Exception) {
                    CrashLogger.log(e, "seleccionarMovimiento")
                    _error.value = "No se pudo cargar el movimiento"
                }
            }
        }
    }

    fun eliminarCliente(cliente: Cliente, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val tieneMov = FiadoRepository.tieneMovimientos(cliente.id)
                if (tieneMov) {
                    _error.value = "No se puede eliminar: el cliente tiene movimientos registrados"
                    return@launch
                }
                FiadoRepository.eliminarCliente(cliente.id)
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "eliminarCliente")
                _error.value = "No se pudo eliminar el cliente"
            } finally {
                _cargando.value = false
            }
        }
    }

    // ── Movimientos del detalle de cliente ───────────────────────────────────

    private val _detalleClienteId = MutableStateFlow("")

    val movimientosDetalle: StateFlow<List<Movimiento>> = _detalleClienteId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.movimientosClienteFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun seleccionarCliente(clienteId: String) {
        _detalleClienteId.value = clienteId
    }

    fun inicializarDesdeEmpleado(localId: String, nombreEmpleado: String = "") {
        if (_localId.value.isBlank()) {
            _localId.value = localId
        }
        if (nombreEmpleado.isNotBlank()) {
            _nombreDueno.value = nombreEmpleado
        }
    }

    fun registrarVenta(
        items: List<ItemAnotado>,
        onExito: () -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val total = items.sumOf { it.precio * it.cantidad }
                val venta = Venta(
                    localId = _localId.value,
                    items = items.map { "${it.nombre} x${it.cantidad}" },
                    total = total,
                    registradoPorUid = uid,
                    registradoPorNombre = _nombreDueno.value
                )
                FiadoRepository.registrarVenta(venta)
                FiadoRepository.upsertProductosBatch(
                    _localId.value,
                    items.map { Triple(it.nombre, it.precio * it.cantidad, it.cantidad) },
                    productos.value
                )
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "registrarVenta")
                _error.value = "No se pudo registrar la venta"
                onError()
            } finally {
                _cargando.value = false
            }
        }
    }

    fun agregarMovimientos(
        clienteId: String,
        items: List<Triple<String, Double, Int>>, // producto, monto, cantidad
        tipo: String,
        visitante: String = "",
        onExito: () -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val grupoId = java.util.UUID.randomUUID().toString()
                val movs = items.map { (producto, monto, cantidad) ->
                    Movimiento(
                        localId = _localId.value,
                        clienteId = clienteId,
                        tipo = tipo,
                        producto = producto,
                        monto = monto,
                        cantidad = cantidad,
                        anotadoPorUid = uid,
                        anotadoPorNombre = _nombreDueno.value,
                        grupoId = grupoId,
                        visitante = visitante
                    )
                }
                FiadoRepository.agregarMovimientos(movs)
                FiadoRepository.upsertProductosBatch(_localId.value, items, productos.value)
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "agregarMovimientos dueno")
                _error.value = "No se pudo guardar"
                onError()
            } finally {
                _cargando.value = false
            }
        }
    }

    val movimientosLocal: StateFlow<List<Movimiento>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.movimientosLocalFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ventasLocal: StateFlow<List<Venta>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.ventasLocalFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ventasHoy: StateFlow<Double> = ventasLocal
        .map { ventas ->
            val inicioDia = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            ventas.filter { it.fecha.toDate().time >= inicioDia }.sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ── Estadísticas derivadas (calculadas en background, no en el hilo de UI) ─

    val totalDeuda: StateFlow<Double> = clientes
        .map { lista -> lista.sumOf { it.deudaTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val sobreLimite: StateFlow<List<Cliente>> = clientes
        .map { lista -> lista.filter { it.limiteCredito > 0 && it.deudaTotal > it.limiteCredito } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resumenHoy: StateFlow<Triple<List<Movimiento>, Double, Double>> = movimientosLocal
        .map { movs ->
            val inicioDia = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            // movimientosLocal ya viene filtrado por los últimos 90 días desde Firestore
            val hoy = movs.filter { !it.anulado && it.fecha.toDate().time >= inicioDia }
            val fiado = hoy.filter { it.tipo == "cargo" }.sumOf { it.monto }
            val cobrado = hoy.filter { it.tipo == "pago" }.sumOf { it.monto }
            Triple(hoy, fiado, cobrado)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), 0.0, 0.0))

    val disputados: StateFlow<List<Movimiento>> = movimientosLocal
        .map { movs -> movs.filter { it.disputado && !it.anulado } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recalcularDeuda(clienteId: String, onExito: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                FiadoRepository.recalcularDeuda(clienteId)
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "recalcularDeuda")
                _error.value = "No se pudo recalcular la deuda"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun anularMovimiento(movimientoId: String, clienteId: String, monto: Double, tipo: String, motivo: String, onExito: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                FiadoRepository.anularMovimiento(movimientoId, clienteId, monto, tipo, motivo)
                if (_movimientoSeleccionado.value?.id == movimientoId) {
                    _movimientoSeleccionado.value = _movimientoSeleccionado.value?.copy(anulado = true, motivoAnulacion = motivo)
                }
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "anularMovimiento")
                _error.value = "No se pudo anular el movimiento"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun corregirMontoMovimiento(movimientoId: String, clienteId: String, montoViejo: Double, montoNuevo: Double, tipo: String, onExito: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                FiadoRepository.corregirMontoMovimiento(movimientoId, clienteId, montoViejo, montoNuevo, tipo)
                if (_movimientoSeleccionado.value?.id == movimientoId) {
                    _movimientoSeleccionado.value = _movimientoSeleccionado.value?.copy(monto = montoNuevo)
                }
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "corregirMontoMovimiento")
                _error.value = "No se pudo corregir el monto"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun marcarDisputado(movimientoId: String, disputado: Boolean) {
        viewModelScope.launch {
            try {
                FiadoRepository.marcarDisputado(movimientoId, disputado)
                if (_movimientoSeleccionado.value?.id == movimientoId) {
                    _movimientoSeleccionado.value = _movimientoSeleccionado.value?.copy(disputado = disputado)
                }
            } catch (e: Exception) {
                CrashLogger.log(e, "marcarDisputado dueno")
                _error.value = "No se pudo actualizar el movimiento"
            }
        }
    }

    val productos: StateFlow<List<Producto>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.productosFlow(id) else flowOf(emptyList())
        }
        .map { lista -> lista.sortedByDescending { it.vecesUsado } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (!syncProductosHecho) viewModelScope.launch {
            val lista = productos.first { it.isNotEmpty() }
            if (!syncProductosHecho) {
                syncProductosHecho = true
                launch(Dispatchers.Default) {
                    try { FiadoRepository.syncProductosGlobales(lista.map { it.nombre }) }
                    catch (e: Exception) { CrashLogger.log(e, "syncProductosGlobales") }
                }
            }
        }
    }

    suspend fun buscarProductosGlobales(query: String): List<String> =
        FiadoRepository.buscarProductosGlobales(query)

    private suspend fun resolverLocalId(): String? {
        val existing = _localId.value
        if (existing.isNotBlank()) return existing
        val uid = auth.currentUser?.uid ?: run { _error.value = "Sesión no válida. Cerrá y volvé a abrir la app."; return null }
        val usuario = FiadoRepository.obtenerUsuario(uid) ?: run { _error.value = "No se encontró el usuario. Cerrá y volvé a abrir la app."; return null }
        if (usuario.localId.isBlank()) { _error.value = "No tenés un local configurado."; return null }
        _localId.value = usuario.localId
        return usuario.localId
    }

    fun guardarProducto(nombre: String, precio: Double, productoId: String?, categoria: String = "", imagenUri: Uri? = null, codigoBarras: String = "", presentacion: String = "", onExito: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val localId = resolverLocalId() ?: return@launch
                val targetId = productoId ?: FiadoRepository.nuevoProductoId()
                val imagenUrl = if (imagenUri != null) {
                    try { FiadoRepository.subirImagenProducto(localId, targetId, imagenUri) }
                    catch (e: Exception) { CrashLogger.log(e, "subirImagenProducto"); "" }
                } else ""
                if (productoId != null) {
                    FiadoRepository.editarProducto(productoId, nombre, precio, categoria, imagenUrl, codigoBarras, presentacion)
                } else {
                    FiadoRepository.upsertProducto(localId, nombre, precio, categoria, imagenUrl, targetId, codigoBarras, presentacion)
                    try { FiadoRepository.upsertProductoGlobal(nombre) }
                    catch (e: Exception) { CrashLogger.log(e, "upsertProductoGlobal") }
                }
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "guardarProducto")
                _error.value = "No se pudo guardar el producto"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun actualizarPrecios(cambios: Map<String, Double>, onExito: () -> Unit) {
        viewModelScope.launch {
            try {
                cambios.forEach { (productoId, nuevoPrecio) ->
                    val prod = productos.value.find { it.id == productoId } ?: return@forEach
                    FiadoRepository.editarProducto(productoId, prod.nombre, nuevoPrecio, prod.categoria, prod.imagenUrl, prod.codigoBarras, prod.presentacion)
                }
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "actualizarPrecios")
                _error.value = "No se pudieron actualizar los precios"
            }
        }
    }

    fun eliminarProducto(producto: Producto) {
        viewModelScope.launch {
            try {
                FiadoRepository.eliminarProducto(producto.id)
            } catch (e: Exception) {
                CrashLogger.log(e, "eliminarProducto")
                _error.value = "No se pudo eliminar el producto"
            }
        }
    }

    val empleados: StateFlow<List<Empleado>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.empleadosFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun eliminarEmpleado(empleadoId: String) {
        viewModelScope.launch {
            try {
                FiadoRepository.eliminarEmpleado(empleadoId)
            } catch (e: Exception) {
                CrashLogger.log(e, "eliminarEmpleado")
                _error.value = "No se pudo eliminar el empleado"
            }
        }
    }

    fun actualizarPermisoVerDeuda(empleadoId: String, puede: Boolean) {
        viewModelScope.launch {
            try {
                FiadoRepository.actualizarCampoEmpleado(empleadoId, "puedeVerDeuda", puede)
            } catch (e: Exception) {
                CrashLogger.log(e, "actualizarPermisoVerDeuda")
            }
        }
    }

    fun generarCodigoEmpleado(onExito: (String) -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val localId = resolverLocalId() ?: return@launch
                if (_nombreLocal.value.isBlank()) {
                    val local = FiadoRepository.obtenerLocal(localId)
                    _nombreLocal.value = local?.nombre ?: ""
                }
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                val codigo = (1..6).map { chars.random() }.joinToString("")
                FiadoRepository.guardarCodigoEmpleado(localId, _nombreLocal.value, codigo)
                onExito(codigo)
            } catch (e: Exception) {
                CrashLogger.log(e, "generarCodigoEmpleado")
                _error.value = "No se pudo generar el código"
            } finally {
                _cargando.value = false
            }
        }
    }

    val tieneSession: StateFlow<Boolean> = _localId
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun cerrarSesion() {
        _localId.value = ""
        _nombreLocal.value = ""
        _nombreDueno.value = ""
        _movimientoSeleccionado.value = null
        _detalleClienteId.value = ""
    }
}
