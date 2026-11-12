package com.kosking.app.dueno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.util.CrashLogger
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DuenoViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _localId = MutableStateFlow("")
    val localId: StateFlow<String> = _localId

    private val _nombreLocal = MutableStateFlow("")
    val nombreLocal: StateFlow<String> = _nombreLocal

    val localData: StateFlow<Local?> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.localFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _codigoLocal = MutableStateFlow("")
    val codigoLocal: StateFlow<String> = _codigoLocal

    private val _nombreDueno = MutableStateFlow("")

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null && _localId.value.isBlank()) {
            viewModelScope.launch { cargarDatos(uid) }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    private suspend fun cargarDatos(uid: String) {
        val usuario = FiadoRepository.obtenerUsuario(uid) ?: return
        _nombreDueno.value = usuario.nombre
        if (usuario.localId.isNotBlank()) {
            _localId.value = usuario.localId
            val local = FiadoRepository.obtenerLocal(usuario.localId)
            _nombreLocal.value = local?.nombre ?: ""
            _codigoLocal.value = ""
        }
    }

    fun actualizarLocal(
        nombre: String,
        tipo: String,
        direccion: String,
        telefono: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val local = localData.value ?: return@launch
                val actualizado = local.copy(
                    nombre = nombre,
                    tipo = tipo,
                    direccion = direccion,
                    telefono = telefono
                )
                FiadoRepository.actualizarLocal(actualizado)
                _nombreLocal.value = nombre
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "actualizarLocal")
                _error.value = "No se pudo guardar"
            } finally {
                _cargando.value = false
            }
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
                val ref = db.collection("locales").document()
                val local = Local(id = ref.id, nombre = nombre, duenoUid = uid,
                    tipo = tipo, direccion = direccion)
                FiadoRepository.crearLocal(local)
                db.collection("usuarios").document(uid).update("localId", ref.id).await()
                _localId.value = ref.id
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

    val clientes: StateFlow<List<Cliente>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.clientesFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object { const val LIMITE_CLIENTES_GRATIS = 50 }

    fun agregarCliente(nombre: String, telefono: String, domicilio: String, limiteCredito: Double = 0.0, onExito: () -> Unit) {
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
                FiadoRepository.agregarCliente(cliente)
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "agregarCliente")
                _error.value = "No se pudo agregar el cliente"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun actualizarCliente(cliente: Cliente) {
        viewModelScope.launch { FiadoRepository.actualizarCliente(cliente) }
    }

    // ── Movimiento seleccionado (para detalle) ────────────────────────────────

    private val _movimientoSeleccionado = MutableStateFlow<Movimiento?>(null)
    val movimientoSeleccionado: StateFlow<Movimiento?> = _movimientoSeleccionado.asStateFlow()

    fun seleccionarMovimiento(id: String) {
        _movimientoSeleccionado.value =
            movimientosLocal.value.find { it.id == id }
            ?: movimientosDetalle.value.find { it.id == id }
        viewModelScope.launch {
            if (_movimientoSeleccionado.value == null) {
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

    fun agregarMovimientos(
        clienteId: String,
        items: List<Pair<String, Double>>, // producto, monto
        tipo: String,
        visitante: String = "",
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val grupoId = java.util.UUID.randomUUID().toString()
                val movs = items.map { (producto, monto) ->
                    Movimiento(
                        localId = _localId.value,
                        clienteId = clienteId,
                        tipo = tipo,
                        producto = producto,
                        monto = monto,
                        anotadoPorUid = uid,
                        anotadoPorNombre = _nombreDueno.value,
                        grupoId = grupoId,
                        visitante = visitante
                    )
                }
                FiadoRepository.agregarMovimientos(movs)
                items.forEach { (producto, monto) ->
                    if (producto.isNotBlank()) FiadoRepository.upsertProducto(_localId.value, producto, monto)
                }
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "agregarMovimientos dueno")
                _error.value = "No se pudo guardar"
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun guardarProducto(nombre: String, precio: Double, productoId: String?, onExito: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val localId = _localId.value.ifBlank {
                    val uid = auth.currentUser?.uid ?: run {
                        _error.value = "Sesión no válida. Cerrá y volvé a abrir la app."
                        return@launch
                    }
                    val usuario = FiadoRepository.obtenerUsuario(uid) ?: run {
                        _error.value = "No se encontró el usuario. Cerrá y volvé a abrir la app."
                        return@launch
                    }
                    if (usuario.localId.isBlank()) {
                        _error.value = "No tenés un local configurado."
                        return@launch
                    }
                    _localId.value = usuario.localId
                    usuario.localId
                }
                if (productoId != null) {
                    FiadoRepository.editarProducto(productoId, nombre, precio)
                } else {
                    FiadoRepository.upsertProducto(localId, nombre, precio)
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

    fun generarCodigoEmpleado(onExito: (String) -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                // Si _localId está vacío, intentar recargarlo
                val localId = _localId.value.ifBlank {
                    val uid = auth.currentUser?.uid ?: run {
                        _error.value = "Sesión no válida"
                        return@launch
                    }
                    val usuario = FiadoRepository.obtenerUsuario(uid) ?: run {
                        _error.value = "No se encontró el usuario"
                        return@launch
                    }
                    if (usuario.localId.isBlank()) {
                        _error.value = "No tenés un local creado"
                        return@launch
                    }
                    _localId.value = usuario.localId
                    val local = FiadoRepository.obtenerLocal(usuario.localId)
                    _nombreLocal.value = local?.nombre ?: ""
                    usuario.localId
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
        _codigoLocal.value = ""
    }
}
