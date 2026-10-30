package com.kosking.app.dueno

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Calendar

class DuenoViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val error = MutableStateFlow<String?>(null)
    val cargando = MutableStateFlow(false)

    private val _localId = MutableStateFlow("")
    val localId: StateFlow<String> = _localId

    private val _nombreLocal = MutableStateFlow("")
    val nombreLocal: StateFlow<String> = _nombreLocal

    @OptIn(ExperimentalCoroutinesApi::class)
    val localData: StateFlow<Local?> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.localFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _codigoLocal = MutableStateFlow("")
    val codigoLocal: StateFlow<String> = _codigoLocal

    private val _nombreUsuario = MutableStateFlow("")
    val nombreUsuario: StateFlow<String> = _nombreUsuario

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
        _nombreUsuario.value = usuario.nombre
        if (usuario.localId.isNotBlank()) {
            _localId.value = usuario.localId
            val local = FiadoRepository.obtenerLocal(usuario.localId)
            _nombreLocal.value = local?.nombre ?: ""
            _codigoLocal.value = local?.codigo ?: ""
        }
    }

    fun cargarDatosLocal() { /* localData ya es reactivo, no se necesita carga manual */ }

    fun actualizarLocal(
        nombre: String,
        tipo: String,
        direccion: String,
        telefono: String,
        descripcion: String,
        abierto: Boolean,
        lat: Double? = null,
        lng: Double? = null,
        horarios: Map<String, com.kosking.app.modelo.HorarioDia>? = null,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val local = localData.value ?: return@launch
                val actualizado = local.copy(
                    nombre = nombre,
                    tipo = tipo,
                    direccion = direccion,
                    telefono = telefono,
                    descripcion = descripcion,
                    abierto = abierto,
                    lat = lat ?: local.lat,
                    lng = lng ?: local.lng,
                    horarios = horarios ?: local.horarios
                )
                FiadoRepository.actualizarLocal(actualizado)
                _nombreLocal.value = nombre
                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo guardar"
            } finally {
                cargando.value = false
            }
        }
    }

    fun crearLocal(
        nombre: String,
        tipo: String = "Kiosco",
        direccion: String = "",
        lat: Double = 0.0,
        lng: Double = 0.0,
        horarios: Map<String, com.kosking.app.modelo.HorarioDia> = emptyMap(),
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val ref = db.collection("locales").document()
                val codigo = ref.id.take(6).uppercase()
                val local = Local(id = ref.id, nombre = nombre, duenoUid = uid, codigo = codigo,
                    tipo = tipo, direccion = direccion, lat = lat, lng = lng, horarios = horarios)
                FiadoRepository.crearLocal(local)
                db.collection("usuarios").document(uid).update("localId", ref.id).await()
                _localId.value = ref.id
                _nombreLocal.value = nombre
                _codigoLocal.value = codigo
                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo crear el local"
            } finally {
                cargando.value = false
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val clientes: StateFlow<List<Cliente>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.clientesFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregarCliente(nombre: String, telefono: String, domicilio: String, onExito: (pin: String, codigoAcceso: String) -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val pin = (1000..9999).random().toString()
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                val codigoAcceso = "KSK-" + (1..4).map { chars.random() }.joinToString("")
                val cliente = Cliente(
                    localId = _localId.value,
                    nombre = nombre,
                    telefono = telefono,
                    domicilio = domicilio,
                    pin = pin,
                    codigoAcceso = codigoAcceso
                )
                val clienteId = FiadoRepository.agregarCliente(cliente)
                FiadoRepository.guardarCodigoCliente(codigoAcceso, clienteId, _localId.value)
                onExito(pin, codigoAcceso)
            } catch (e: Exception) {
                error.value = "No se pudo agregar el cliente"
            } finally {
                cargando.value = false
            }
        }
    }

    fun toggleHabilitado(cliente: Cliente) {
        viewModelScope.launch {
            FiadoRepository.actualizarCliente(cliente.copy(habilitado = !cliente.habilitado))
        }
    }

    fun eliminarCliente(cliente: Cliente, onExito: () -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val tieneMov = FiadoRepository.tieneMovimientos(cliente.id)
                if (tieneMov) {
                    error.value = "No se puede eliminar: el cliente tiene movimientos registrados"
                    return@launch
                }
                FiadoRepository.eliminarCliente(cliente.id, cliente.codigoAcceso)
                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo eliminar el cliente"
            } finally {
                cargando.value = false
            }
        }
    }

    // ── Movimientos del detalle de cliente ───────────────────────────────────

    private val _detalleClienteId = MutableStateFlow("")

    val movimientosDetalle: StateFlow<List<Movimiento>> = _detalleClienteId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.movimientosFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun seleccionarCliente(clienteId: String) {
        _detalleClienteId.value = clienteId
    }

    fun agregarMovimiento(
        clienteId: String,
        producto: String,
        cantidad: Int,
        monto: Double,
        tipo: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val mov = Movimiento(
                    localId = _localId.value,
                    clienteId = clienteId,
                    tipo = tipo,
                    producto = producto,
                    cantidad = cantidad,
                    monto = monto,
                    anotadoPorUid = uid,
                    anotadoPorNombre = _nombreUsuario.value
                )
                FiadoRepository.agregarMovimiento(mov)
                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo guardar"
            } finally {
                cargando.value = false
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

    val clientesConDeuda: StateFlow<Int> = clientes
        .map { lista -> lista.count { it.deudaTotal > 0 && it.habilitado } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sobreLimite: StateFlow<List<Cliente>> = clientes
        .map { lista -> lista.filter { it.limiteCredito > 0 && it.deudaTotal > it.limiteCredito } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resumenHoy: StateFlow<Triple<List<Movimiento>, Double, Double>> = movimientosLocal
        .map { movs ->
            val inicioDia = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val hoy = movs.filter { !it.anulado && it.fecha.toDate().time >= inicioDia }
            val fiado = hoy.filter { it.tipo == "cargo" }.sumOf { it.monto }
            val cobrado = hoy.filter { it.tipo == "pago" }.sumOf { it.monto }
            Triple(hoy, fiado, cobrado)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(emptyList(), 0.0, 0.0))

    val disputados: StateFlow<List<Movimiento>> = movimientosLocal
        .map { movs -> movs.filter { it.disputado && !it.anulado } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun anularMovimiento(movimientoId: String, clienteId: String, monto: Double, tipo: String, motivo: String) {
        viewModelScope.launch {
            try {
                FiadoRepository.anularMovimiento(movimientoId, clienteId, monto, tipo, motivo)
            } catch (e: Exception) {
                error.value = "No se pudo anular el movimiento"
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
                error.value = "No se pudo eliminar el empleado"
            }
        }
    }

    fun generarCodigoEmpleado(nombre: String, onExito: (String) -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                // Si _localId está vacío, intentar recargarlo
                val localId = if (_localId.value.isNotBlank()) {
                    _localId.value
                } else {
                    val uid = auth.currentUser?.uid ?: run {
                        error.value = "Sesión no válida"
                        return@launch
                    }
                    val usuario = FiadoRepository.obtenerUsuario(uid) ?: run {
                        error.value = "No se encontró el usuario"
                        return@launch
                    }
                    if (usuario.localId.isBlank()) {
                        error.value = "No tenés un local creado"
                        return@launch
                    }
                    _localId.value = usuario.localId
                    val local = FiadoRepository.obtenerLocal(usuario.localId)
                    _nombreLocal.value = local?.nombre ?: ""
                    usuario.localId
                }
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                val codigo = (1..6).map { chars.random() }.joinToString("")
                FiadoRepository.guardarCodigoEmpleado(localId, _nombreLocal.value, nombre, codigo)
                onExito(codigo)
            } catch (e: Exception) {
                error.value = "No se pudo generar el código"
            } finally {
                cargando.value = false
            }
        }
    }

    // ── Productos ────────────────────────────────────────────────────────────

    val productos: StateFlow<List<Producto>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.productosFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregarProducto(nombre: String, precio: Double, descripcion: String, habilitado: Boolean = true, esOferta: Boolean = false, precioAnterior: Double = 0.0) {
        viewModelScope.launch {
            try {
                val producto = Producto(
                    localId = _localId.value,
                    nombre = nombre,
                    precio = precio,
                    descripcion = descripcion,
                    habilitado = habilitado,
                    esOferta = esOferta,
                    precioAnterior = precioAnterior
                )
                FiadoRepository.agregarProducto(producto)
            } catch (e: Exception) {
                error.value = "No se pudo agregar el producto"
            }
        }
    }

    fun actualizarProducto(producto: Producto) {
        viewModelScope.launch {
            try {
                FiadoRepository.actualizarProducto(producto)
            } catch (e: Exception) {
                error.value = "No se pudo actualizar el producto"
            }
        }
    }

    fun eliminarProducto(productoId: String) {
        viewModelScope.launch {
            try {
                FiadoRepository.eliminarProducto(productoId)
            } catch (e: Exception) {
                error.value = "No se pudo eliminar el producto"
            }
        }
    }

    fun subirFotoLocal(uri: Uri, onExito: (String) -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val url = FiadoRepository.subirImagenLocal(_localId.value, uri)
                FiadoRepository.actualizarImagenLocal(_localId.value, url)
                onExito(url)
            } catch (e: Exception) {
                error.value = "No se pudo subir la foto"
            } finally {
                cargando.value = false
            }
        }
    }

    fun subirFotoProducto(producto: Producto, uri: Uri, onExito: (String) -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val url = FiadoRepository.subirImagenProducto(_localId.value, producto.id, uri)
                FiadoRepository.actualizarImagenProducto(producto.id, url)
                onExito(url)
            } catch (e: Exception) {
                error.value = "No se pudo subir la foto"
            } finally {
                cargando.value = false
            }
        }
    }

    // true si Firebase Auth tiene usuario Y ya cargamos el localId
    val tieneSession: Boolean
        get() = auth.currentUser != null && _localId.value.isNotBlank()

    fun cerrarSesion() {
        _localId.value = ""
        _nombreLocal.value = ""
        _codigoLocal.value = ""
        auth.signOut()
    }
}
