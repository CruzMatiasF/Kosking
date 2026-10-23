package com.kosking.app.dueno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.ItemVenta
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.Tienda
import com.kosking.app.modelo.Venta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ItemCarrito(
    val stockItem: StockItem,
    val cantidad: Int,
)

sealed class EstadoRegistro {
    object Idle : EstadoRegistro()
    object Cargando : EstadoRegistro()
    object Exito : EstadoRegistro()
    data class Error(val mensaje: String) : EstadoRegistro()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrarVentaViewModel : ViewModel() {

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

    // Búsqueda dentro del stock
    val query = MutableStateFlow("")
    val stockFiltrado: StateFlow<List<StockItem>> = stock
        .combine(query) { lista, q ->
            if (q.isBlank()) lista else lista.filter { it.nombre.contains(q, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Carrito
    private val _carrito = MutableStateFlow<Map<String, ItemCarrito>>(emptyMap())
    val carrito: StateFlow<Map<String, ItemCarrito>> = _carrito

    val total: StateFlow<Double> = _carrito
        .map { mapa -> mapa.values.sumOf { it.stockItem.precio * it.cantidad } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val estado = MutableStateFlow<EstadoRegistro>(EstadoRegistro.Idle)

    fun agregarAlCarrito(item: StockItem) {
        val actual = _carrito.value.toMutableMap()
        val existente = actual[item.productoId]
        if (existente != null && existente.cantidad < item.cantidad) {
            actual[item.productoId] = existente.copy(cantidad = existente.cantidad + 1)
        } else if (existente == null && item.cantidad > 0) {
            actual[item.productoId] = ItemCarrito(item, 1)
        }
        _carrito.value = actual
    }

    fun quitarDelCarrito(productoId: String) {
        val actual = _carrito.value.toMutableMap()
        val existente = actual[productoId] ?: return
        if (existente.cantidad <= 1) actual.remove(productoId)
        else actual[productoId] = existente.copy(cantidad = existente.cantidad - 1)
        _carrito.value = actual
    }

    fun registrarVenta() {
        val id = tiendaId.value
        if (id.isBlank() || _carrito.value.isEmpty()) return
        viewModelScope.launch {
            estado.value = EstadoRegistro.Cargando
            try {
                val items = _carrito.value.values.map { ic ->
                    ItemVenta(
                        productoId = ic.stockItem.productoId,
                        nombre = ic.stockItem.nombre,
                        cantidad = ic.cantidad,
                        precioUnitario = ic.stockItem.precio,
                    )
                }
                val venta = Venta(
                    empleadoUid = uid,
                    items = items,
                    total = total.value,
                    fecha = Timestamp.now(),
                )
                val ventaRef = db.collection("tiendas").document(id)
                    .collection("ventas").document()
                ventaRef.set(venta).await()

                // Descontar stock
                val batch = db.batch()
                _carrito.value.values.forEach { ic ->
                    val stockRef = db.collection("tiendas").document(id)
                        .collection("stock").document(ic.stockItem.productoId)
                    batch.update(stockRef, "cantidad", ic.stockItem.cantidad - ic.cantidad)
                }
                batch.commit().await()

                _carrito.value = emptyMap()
                estado.value = EstadoRegistro.Exito
            } catch (e: Exception) {
                estado.value = EstadoRegistro.Error(e.message ?: "Error al registrar")
            }
        }
    }
}

// Extension para combinar dos flows (Kotlin no lo tiene built-in en todas las versiones)
private fun <T1, T2, R> kotlinx.coroutines.flow.Flow<T1>.combine(
    other: kotlinx.coroutines.flow.Flow<T2>,
    transform: suspend (T1, T2) -> R,
): kotlinx.coroutines.flow.Flow<R> = kotlinx.coroutines.flow.combine(this, other, transform)
