package com.kosking.app.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.LocalGuardado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClienteViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    val error = MutableStateFlow<String?>(null)
    val cargando = MutableStateFlow(false)

    private val _uid = MutableStateFlow(
        auth.currentUser?.takeIf { !it.isAnonymous }?.uid ?: ""
    )

    // Declarado ANTES del init para que esté inicializado cuando init corra
    val localesGuardados: StateFlow<List<LocalGuardado>> = _uid
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) FiadoRepository.localesGuardadosFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _localSeleccionado = MutableStateFlow<LocalGuardado?>(null)
    val localSeleccionado: StateFlow<LocalGuardado?> = _localSeleccionado

    val tieneLocales: Boolean get() = localesGuardados.value.isNotEmpty()

    var startupRedirectDone = false

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        _uid.value = fa.currentUser?.takeIf { !it.isAnonymous }?.uid ?: ""
    }

    init {
        auth.addAuthStateListener(authListener)
        // Auto-seleccionar local cuando cambia la lista
        viewModelScope.launch {
            localesGuardados.collect { lista ->
                val actual = _localSeleccionado.value
                when {
                    actual == null && lista.isNotEmpty() ->
                        _localSeleccionado.value = lista.first()
                    actual != null && lista.none { it.localId == actual.localId } ->
                        _localSeleccionado.value = lista.firstOrNull()
                }
            }
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    fun seleccionarLocal(localId: String) {
        _localSeleccionado.value = localesGuardados.value.firstOrNull { it.localId == localId }
    }

    fun verificarPIN(localId: String, nombreLocal: String, pin: String, onExito: () -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val uid = _uid.value
                if (uid.isBlank()) {
                    error.value = "Necesitás iniciar sesión primero"
                    return@launch
                }
                val cliente = FiadoRepository.obtenerClientePorPin(localId, pin)
                if (cliente != null) {
                    FiadoRepository.guardarLocalCliente(uid, localId, cliente.id, nombreLocal)
                    _localSeleccionado.value = LocalGuardado(localId, cliente.id, nombreLocal)
                    onExito()
                } else {
                    error.value = "PIN incorrecto. Verificá con el dueño del local"
                }
            } catch (e: Exception) {
                error.value = "No se pudo verificar. Revisá tu conexión e intentá de nuevo"
            } finally {
                cargando.value = false
            }
        }
    }

    fun verificarPorCodigoAcceso(codigoAcceso: String, onExito: () -> Unit) {
        viewModelScope.launch {
            cargando.value = true
            error.value = null
            try {
                val uid = _uid.value
                if (uid.isBlank()) {
                    error.value = "Necesitás iniciar sesión primero"
                    return@launch
                }
                val cliente = FiadoRepository.obtenerClientePorCodigoAcceso(codigoAcceso)
                if (cliente == null) {
                    error.value = "Código inválido. Pedíselo al dueño del local"
                    return@launch
                }
                val local = FiadoRepository.obtenerLocal(cliente.localId)
                if (local == null) {
                    error.value = "No se pudo encontrar el local"
                    return@launch
                }
                FiadoRepository.guardarLocalCliente(uid, local.id, cliente.id, local.nombre)
                _localSeleccionado.value = LocalGuardado(local.id, cliente.id, local.nombre)
                onExito()
            } catch (e: Exception) {
                error.value = "No se pudo conectar. Revisá tu conexión e intentá de nuevo"
            } finally {
                cargando.value = false
            }
        }
    }

    fun quitarLocal(localId: String) {
        viewModelScope.launch {
            val uid = _uid.value
            if (uid.isBlank()) return@launch
            try {
                FiadoRepository.quitarLocalCliente(uid, localId)
            } catch (_: Exception) {}
        }
    }

    val movimientos: StateFlow<List<Movimiento>> = _localSeleccionado
        .flatMapLatest { lg ->
            if (lg != null) FiadoRepository.movimientosFlow(lg.clienteId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cliente: StateFlow<Cliente?> = _localSeleccionado
        .flatMapLatest { lg ->
            if (lg != null) FiadoRepository.clienteFlow(lg.clienteId) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val productos: StateFlow<List<Producto>> = _localSeleccionado
        .flatMapLatest { lg ->
            if (lg != null) FiadoRepository.productosFlow(lg.localId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deuda de cada local guardado — para mostrar en la pantalla de selección
    val deudasPorLocal: StateFlow<Map<String, Double>> = localesGuardados
        .flatMapLatest { lista ->
            if (lista.isEmpty()) return@flatMapLatest flowOf(emptyMap())
            val flows = lista.map { lg ->
                FiadoRepository.clienteFlow(lg.clienteId)
                    .map { cliente -> lg.localId to (cliente?.deudaTotal ?: 0.0) }
            }
            combine(flows) { pares -> pares.toMap() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val localInfo: StateFlow<Local?> = _localSeleccionado
        .flatMapLatest { lg ->
            if (lg != null) FiadoRepository.localFlow(lg.localId) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun marcarDisputado(movimientoId: String) {
        viewModelScope.launch {
            try {
                FiadoRepository.marcarDisputado(movimientoId, true)
            } catch (e: Exception) {
                error.value = "No se pudo enviar el reporte"
            }
        }
    }
}
