package com.kosking.app.empleado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.util.CrashLogger
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class EmpleadoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

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
        } catch (e: Exception) { CrashLogger.log(e, "cargarEmpleoDeFirestore") }
    }

    fun verificarCodigo(codigo: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val user = auth.currentUser
                if (user == null || user.isAnonymous) {
                    _error.value = "Necesitás iniciar sesión para ser empleado"
                    return@launch
                }
                val uid = user.uid

                val snap = db.collection("codigosEmpleado")
                    .whereEqualTo("codigo", codigo)
                    .get().await()

                val doc = snap.documents.firstOrNull { it.getBoolean("usado") == false }
                if (doc == null) {
                    _error.value = "Código incorrecto o ya usado"
                    return@launch
                }

                val localId = doc.getString("localId") ?: ""
                if (localId.isBlank()) {
                    _error.value = "Código inválido. Pedile uno nuevo al dueño"
                    return@launch
                }

                val local = FiadoRepository.obtenerLocal(localId)
                val usuario = FiadoRepository.obtenerUsuario(uid)
                val nombreEmpleado = listOf(usuario?.nombre, user.displayName)
                    .firstOrNull { !it.isNullOrBlank() } ?: "Empleado"

                doc.reference.update("usado", true, "usadoPorUid", uid).await()
                FiadoRepository.agregarEmpleado(Empleado(localId = localId, uid = uid, nombre = nombreEmpleado))
                FiadoRepository.guardarEmpleoEnUsuario(uid, localId, local?.nombre ?: "")

                _localId.value = localId
                _nombreLocal.value = local?.nombre ?: ""
                _nombreEmpleado.value = nombreEmpleado

                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "verificarCodigo empleado")
                _error.value = "No se pudo verificar el código. Intentá de nuevo"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) FiadoRepository.limpiarEmpleoEnUsuario(uid)
            } catch (e: Exception) { CrashLogger.log(e, "cerrarSesion") }
            _localId.value = ""
            _nombreLocal.value = ""
            _nombreEmpleado.value = ""
            auth.signOut()
        }
    }

    fun actualizarMiNombre(nombre: String) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                FiadoRepository.actualizarNombreEmpleadoPorUid(uid, _localId.value, nombre)
                _nombreEmpleado.value = nombre
            } catch (e: Exception) {
                CrashLogger.log(e, "actualizarMiNombre")
                _error.value = "No se pudo actualizar el nombre"
            }
        }
    }

    fun desvincularme(onExito: () -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val localId = _localId.value
                FiadoRepository.eliminarEmpleadoPorUid(uid, localId)
                FiadoRepository.limpiarEmpleoEnUsuario(uid)
                _localId.value = ""
                _nombreLocal.value = ""
                _nombreEmpleado.value = ""
                onExito()
            } catch (e: Exception) {
                CrashLogger.log(e, "desvincularme")
                _error.value = "No se pudo desvincular"
            } finally {
                _cargando.value = false
            }
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
                val uid = auth.currentUser?.uid ?: ""
                val grupoId = java.util.UUID.randomUUID().toString()
                val movs = items.map { (producto, monto) ->
                    Movimiento(
                        localId = _localId.value,
                        clienteId = clienteId,
                        tipo = tipo,
                        producto = producto,
                        monto = monto,
                        anotadoPorUid = uid,
                        anotadoPorNombre = _nombreEmpleado.value,
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
                CrashLogger.log(e, "agregarMovimientos empleado")
                _error.value = "No se pudo guardar"
            } finally {
                _cargando.value = false
            }
        }
    }
}
