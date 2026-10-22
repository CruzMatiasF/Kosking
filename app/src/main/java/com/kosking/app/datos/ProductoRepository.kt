package com.kosking.app.datos

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.kosking.app.modelo.Producto
import kotlinx.coroutines.tasks.await

object ProductoRepository {

    private val db = FirebaseFirestore.getInstance()

    // Busca productos por nombre en el catálogo global
    suspend fun buscarProductos(query: String): List<Producto> {
        val q = query.trim().lowercase()
        return db.collection("productos")
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject<Producto>()?.copy(id = doc.id)
            }.filter { it.nombreBusqueda.contains(q) }
    }

    // Trae los productos más populares (limitado a 10)
    suspend fun productosPopulares(): List<Producto> {
        return db.collection("productos")
            .limit(10)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject<Producto>()?.copy(id = doc.id)
            }
    }

    suspend fun obtenerProducto(id: String): Producto? {
        return db.collection("productos").document(id)
            .get().await()
            .toObject<Producto>()?.copy(id = id)
    }
}
