package com.kosking.app.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Notificacion
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorarViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    val busqueda = MutableStateFlow("")

    private val _todosLocales: StateFlow<List<Local>> = FiadoRepository.localesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cargandoInicio: StateFlow<Boolean> = _todosLocales
        .take(1).map { false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val locales: StateFlow<List<Local>> = combine(_todosLocales, busqueda) { lista, query ->
        if (query.isBlank()) lista
        else lista.filter { it.nombre.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todosLocales: StateFlow<List<Local>> = _todosLocales

    val productosOferta: StateFlow<List<Producto>> = FiadoRepository.productosOfertaFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Paginación de productos ───────────────────────────────────────────────

    private val _limite = MutableStateFlow(20)
    val cargandoMas = MutableStateFlow(false)

    private val _todosProductos: StateFlow<List<Producto>> = _limite
        .flatMapLatest { lim -> FiadoRepository.todosProductosFlow(lim) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todosProductos: StateFlow<List<Producto>> = _todosProductos

    val hayMasProductos: StateFlow<Boolean> = combine(_todosProductos, _limite) { prods, lim ->
        prods.size >= lim
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun cargarMasProductos() {
        if (cargandoMas.value) return
        viewModelScope.launch {
            cargandoMas.value = true
            _limite.value += 20
            cargandoMas.value = false
        }
    }

    val productosFiltrados: StateFlow<List<Producto>> = combine(_todosProductos, busqueda) { prods, query ->
        if (query.isBlank()) prods
        else prods.filter { it.nombre.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Notificaciones ────────────────────────────────────────────────────────

    private val _uid = MutableStateFlow(auth.currentUser?.uid ?: "")

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        _uid.value = fa.currentUser?.uid ?: ""
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    val notificaciones: StateFlow<List<Notificacion>> = _uid
        .flatMapLatest { uid ->
            if (uid.isNotBlank()) FiadoRepository.notificacionesFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificacionesNoLeidas: StateFlow<Int> = notificaciones
        .map { lista -> lista.count { !it.leida } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun marcarTodasLeidas() {
        val uid = _uid.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            try { FiadoRepository.marcarTodasLeidas(uid) } catch (_: Exception) {}
        }
    }

    fun marcarLeida(id: String) {
        viewModelScope.launch {
            try { FiadoRepository.marcarNotificacionLeida(id) } catch (_: Exception) {}
        }
    }
}
