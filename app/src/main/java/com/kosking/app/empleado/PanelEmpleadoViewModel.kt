package com.kosking.app.empleado

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

sealed class EstadoVentaEmpleado {
    object Idle : EstadoVentaEmpleado()
    object Cargando : EstadoVentaEmpleado()
    object Exito : EstadoVentaEmpleado()
    data class Error(val mensaje: String) : EstadoVentaEmpleado()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PanelEmpleadoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Nombre del usuario (email sin dominio como fallback)
    val nombreEmpleado: String = FirebaseAuth.getInstance().currentUser?.displayName
        ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@") ?: "Empleado"

    // Carga el tiendaId desde el documento del usuario
    private val _tiendaId = MutableStateFlow("")
    val tiendaId: StateFlow<String> = _tiendaId

    val tienda: StateFlow<Tienda?> = tiendaId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(null)
            else KioscoRepository.kioscosFlow().map { lista -> lista.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stock: StateFlow<List<StockItem>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.stockFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ventas: StateFlow<List<Venta>> = tiendaId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else KioscoRepository.ventasFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ventas del turno (desde inicio del día actual)
    val ventasDelTurno: StateFlow<List<Venta>> = ventas.map { lista ->
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        lista.filter { v -> v.fecha?.toDate()?.after(hoy) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTurno: StateFlow<Double> = ventasDelTurno
        .map { lista -> lista.sumOf { it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Carrito para registrar venta
    private val _carrito = MutableStateFlow<Map<String, com.kosking.app.dueno.ItemCarrito>>(emptyMap())
    val carrito: StateFlow<Map<String, com.kosking.app.dueno.ItemCarrito>> = _carrito

    val totalCarrito: StateFlow<Double> = _carrito
        .map { mapa -> mapa.values.sumOf { it.stockItem.precio * it.cantidad } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val estadoVenta = MutableStateFlow<EstadoVentaEmpleado>(EstadoVentaEmpleado.Idle)

    // Búsqueda de stock para registrar venta
    val queryStock = MutableStateFlow("")
    val stockFiltrado: StateFlow<List<StockItem>> = combine(stock, queryStock) { lista, q ->
        if (q.isBlank()) lista else lista.filter { it.nombre.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        cargarTiendaId()
    }

    private fun cargarTiendaId() {
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                _tiendaId.value = doc.getString("empleadoEnTiendaId") ?: ""
            } catch (_: Exception) {}
        }
    }

    fun agregarAlCarrito(item: StockItem) {
        val actual = _carrito.value.toMutableMap()
        val existente = actual[item.productoId]
        if (existente != null && existente.cantidad < item.cantidad) {
            actual[item.productoId] = existente.copy(cantidad = existente.cantidad + 1)
        } else if (existente == null && item.cantidad > 0) {
            actual[item.productoId] = com.kosking.app.dueno.ItemCarrito(item, 1)
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

    fun limpiarCarrito() { _carrito.value = emptyMap() }

    fun registrarVenta() {
        val id = _tiendaId.value
        if (id.isBlank() || _carrito.value.isEmpty()) return
        viewModelScope.launch {
            estadoVenta.value = EstadoVentaEmpleado.Cargando
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
                    total = totalCarrito.value,
                    fecha = Timestamp.now(),
                )
                db.collection("tiendas").document(id)
                    .collection("ventas").document()
                    .set(venta).await()

                val batch = db.batch()
                _carrito.value.values.forEach { ic ->
                    val ref = db.collection("tiendas").document(id)
                        .collection("stock").document(ic.stockItem.productoId)
                    batch.update(ref, "cantidad", ic.stockItem.cantidad - ic.cantidad)
                }
                batch.commit().await()

                _carrito.value = emptyMap()
                estadoVenta.value = EstadoVentaEmpleado.Exito
            } catch (e: Exception) {
                estadoVenta.value = EstadoVentaEmpleado.Error(e.message ?: "Error al registrar")
            }
        }
    }
}
