package com.kosking.app.dueno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

sealed class EstadoEdicion {
    object Idle : EstadoEdicion()
    object Guardando : EstadoEdicion()
    object Exito : EstadoEdicion()
    data class Error(val mensaje: String) : EstadoEdicion()
}

@OptIn(ExperimentalCoroutinesApi::class)
class InventarioViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val tienda: StateFlow<Tienda?> = KioscoRepository.kioscosFlow()
        .map { lista -> lista.find { it.duenioUid == uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val tiendaId: StateFlow<String> = tienda
        .map { it?.id ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val stock: StateFlow<List<StockItem>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.stockFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val query = MutableStateFlow("")
    val filtro = MutableStateFlow(FiltroStock.TODOS)

    val stockFiltrado: StateFlow<List<StockItem>> = combine(stock, query, filtro) { lista, q, f ->
        lista
            .filter { if (q.isBlank()) true else it.nombre.contains(q, ignoreCase = true) }
            .filter { item ->
                when (f) {
                    FiltroStock.TODOS -> true
                    FiltroStock.BAJO -> item.cantidad in 1..5
                    FiltroStock.AGOTADO -> item.cantidad == 0
                }
            }
            .sortedBy { it.nombre }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estadoEdicion = MutableStateFlow<EstadoEdicion>(EstadoEdicion.Idle)

    fun actualizarStock(productoId: String, nuevaCantidad: Int, nuevoPrecio: Double) {
        val id = tiendaId.value
        if (id.isBlank()) return
        viewModelScope.launch {
            estadoEdicion.value = EstadoEdicion.Guardando
            try {
                db.collection("tiendas").document(id)
                    .collection("stock").document(productoId)
                    .update(mapOf("cantidad" to nuevaCantidad, "precio" to nuevoPrecio))
                    .await()
                estadoEdicion.value = EstadoEdicion.Exito
            } catch (e: Exception) {
                estadoEdicion.value = EstadoEdicion.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun eliminarProducto(productoId: String) {
        val id = tiendaId.value
        if (id.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("tiendas").document(id)
                    .collection("stock").document(productoId)
                    .delete().await()
            } catch (_: Exception) { }
        }
    }
}

enum class FiltroStock { TODOS, BAJO, AGOTADO }
