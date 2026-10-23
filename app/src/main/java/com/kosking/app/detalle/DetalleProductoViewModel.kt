package com.kosking.app.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.ProductoRepository
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TiendaConPrecio(
    val tiendaId: String,
    val nombre: String,
    val direccion: String,
    val precio: Double,
    val cantidad: Int,
)

class DetalleProductoViewModel(private val productoId: String) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val producto = MutableStateFlow<Producto?>(null)
    val tiendas = MutableStateFlow<List<TiendaConPrecio>>(emptyList())
    val cargando = MutableStateFlow(true)

    init {
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            try {
                producto.value = ProductoRepository.obtenerProducto(productoId)

                val stockSnap = db.collectionGroup("stock")
                    .whereEqualTo("productoId", productoId)
                    .whereGreaterThan("cantidad", 0)
                    .get().await()

                val kioscos: Map<String, Tienda> = db.collection("tiendas").get().await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(Tienda::class.java)?.copy(id = doc.id)
                    }.associateBy { it.id }

                tiendas.value = stockSnap.documents.mapNotNull { doc ->
                    val item = doc.toObject(StockItem::class.java) ?: return@mapNotNull null
                    val tiendaId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                    val tienda = kioscos[tiendaId] ?: return@mapNotNull null
                    TiendaConPrecio(
                        tiendaId = tiendaId,
                        nombre = tienda.nombre,
                        direccion = tienda.direccion,
                        precio = item.precio,
                        cantidad = item.cantidad,
                    )
                }.sortedBy { it.precio }
            } catch (_: Exception) { }
            cargando.value = false
        }
    }

    companion object {
        fun factory(productoId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) =
                DetalleProductoViewModel(productoId) as T
        }
    }
}
