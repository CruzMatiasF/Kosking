package com.kosking.app.empleado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmpleadoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val error = MutableStateFlow<String?>(null)
    val cargando = MutableStateFlow(false)

    private val _localId = MutableStateFlow("")
    val localId: StateFlow<String> = _localId

    private val _nombreLocal = MutableStateFlow("")
    val nombreLocal: StateFlow<String> = _nombreLocal

    private val _nombreEmpleado = MutableStateFlow("")
    val nombreEmpleado: StateFlow<String> = _nombreEmpleado

    val tieneSession: StateFlow<Boolean> = _localId
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        if (user != null && !user.isAnonymous) {
            viewModelScope.launch { cargarEmpleoDeFirestore(user.uid) }
        } else {
            _localId.value = ""
            _nombreLocal.value = ""
            _nombreEmpleado.value = ""
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    private suspend fun cargarEmpleoDeFirestore(uid: String) {
        try {
            val empleo = FiadoRepository.obtenerEmpleoDeUsuario(uid)
            if (empleo != null) {
                _localId.value = empleo.first
                _nombreLocal.value = empleo.second
            }
            val usuario = FiadoRepository.obtenerUsuario(uid)
            _nombreEmpleado.value = usuario?.nombre ?: auth.currentUser?.displayName ?: ""
        } catch (_: Exception) {}
    }

    fun verificarCodigo(codigo: String, onExito: () -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val user = auth.currentUser
                if (user == null || user.isAnonymous) {
                    error.value = "Necesitás iniciar sesión para ser empleado"
                    return@launch
                }
                val uid = user.uid

                val snap = db.collection("codigosEmpleado")
                    .whereEqualTo("codigo", codigo)
                    .get().await()

                val doc = snap.documents.firstOrNull { it.getBoolean("usado") == false }
                if (doc == null) {
                    error.value = "Código incorrecto o ya usado"
                    return@launch
                }

                val localId = doc.getString("localId") ?: ""
                if (localId.isBlank()) {
                    error.value = "Código inválido. Pedile uno nuevo al dueño"
                    return@launch
                }

                val local = FiadoRepository.obtenerLocal(localId)
                val usuario = FiadoRepository.obtenerUsuario(uid)
                val nombreEmpleado = usuario?.nombre ?: user.displayName ?: ""

                doc.reference.update("usado", true, "usadoPorUid", uid).await()
                FiadoRepository.agregarEmpleado(Empleado(localId = localId, uid = uid, nombre = nombreEmpleado))
                FiadoRepository.guardarEmpleoEnUsuario(uid, localId, local?.nombre ?: "")

                _localId.value = localId
                _nombreLocal.value = local?.nombre ?: ""
                _nombreEmpleado.value = nombreEmpleado

                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo verificar el código. Intentá de nuevo"
            } finally {
                cargando.value = false
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) FiadoRepository.limpiarEmpleoEnUsuario(uid)
            } catch (_: Exception) {}
            _localId.value = ""
            _nombreLocal.value = ""
            _nombreEmpleado.value = ""
        }
    }

    val productos: StateFlow<List<Producto>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.productosFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientesHabilitados: StateFlow<List<Cliente>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.clientesFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val movimientosLocal: StateFlow<List<Movimiento>> = _localId
        .flatMapLatest { id ->
            if (id.isNotBlank()) FiadoRepository.movimientosLocalFlow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregarProducto(nombre: String, precio: Double, descripcion: String, habilitado: Boolean = true, esOferta: Boolean = false, precioAnterior: Double = 0.0) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                FiadoRepository.agregarProducto(
                    Producto(localId = _localId.value, nombre = nombre, precio = precio,
                        descripcion = descripcion, habilitado = habilitado,
                        esOferta = esOferta, precioAnterior = precioAnterior)
                )
            } catch (e: Exception) {
                error.value = "No se pudo agregar el producto"
            } finally { cargando.value = false }
        }
    }

    fun actualizarProducto(producto: Producto) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                FiadoRepository.actualizarProducto(producto)
            } catch (e: Exception) {
                error.value = "No se pudo actualizar el producto"
            } finally { cargando.value = false }
        }
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
                val mov = Movimiento(
                    localId = _localId.value,
                    clienteId = clienteId,
                    tipo = tipo,
                    producto = producto,
                    cantidad = cantidad,
                    monto = monto,
                    anotadoPorNombre = _nombreEmpleado.value
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
}
