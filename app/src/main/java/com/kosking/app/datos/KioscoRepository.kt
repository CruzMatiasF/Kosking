package com.kosking.app.datos

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.kosking.app.modelo.Oferta
import com.kosking.app.modelo.StockItem
import com.kosking.app.modelo.StockItemConTienda
import com.kosking.app.modelo.Tienda
import com.kosking.app.modelo.Venta
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object KioscoRepository {

    private val db = FirebaseFirestore.getInstance()

    // Escucha en tiempo real todos los kioscos activos
    fun kioscosFlow(): Flow<List<Tienda>> = callbackFlow {
        val sub = db.collection("tiendas")
            .addSnapshotListener { snap, _ ->
                val lista = snap?.documents?.mapNotNull { doc ->
                    doc.toObject<Tienda>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { sub.remove() }
    }

    // Escucha el stock de un kiosco en tiempo real
    fun stockFlow(tiendaId: String): Flow<List<StockItem>> = callbackFlow {
        val sub = db.collection("tiendas").document(tiendaId)
            .collection("stock")
            .addSnapshotListener { snap, _ ->
                val lista = snap?.documents?.mapNotNull { doc ->
                    doc.toObject<StockItem>()?.copy(productoId = doc.id)
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { sub.remove() }
    }

    // Escucha las ofertas activas de un kiosco en tiempo real
    fun ofertasFlow(tiendaId: String): Flow<List<Oferta>> = callbackFlow {
        val sub = db.collection("tiendas").document(tiendaId)
            .collection("ofertas")
            .whereEqualTo("activa", true)
            .addSnapshotListener { snap, _ ->
                val lista = snap?.documents?.mapNotNull { doc ->
                    doc.toObject<Oferta>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { sub.remove() }
    }

    // Escucha las ventas de un kiosco en tiempo real (ordenadas por fecha desc)
    fun ventasFlow(tiendaId: String): Flow<List<Venta>> = callbackFlow {
        val sub = db.collection("tiendas").document(tiendaId)
            .collection("ventas")
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                val lista = snap?.documents?.mapNotNull { doc ->
                    doc.toObject<Venta>()?.copy(id = doc.id)
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { sub.remove() }
    }

    // Trae hasta `maxItems` stock items de los primeros `maxTiendas` kioscos (para Home)
    suspend fun stockPopular(tiendas: List<Tienda>, maxTiendas: Int = 5, maxItems: Int = 10): List<StockItemConTienda> {
        val result = mutableListOf<StockItemConTienda>()
        for (tienda in tiendas.take(maxTiendas)) {
            val snap = db.collection("tiendas").document(tienda.id)
                .collection("stock")
                .whereGreaterThan("cantidad", 0)
                .limit(3)
                .get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject<StockItem>()?.copy(productoId = doc.id)
            }.forEach { result.add(StockItemConTienda(it, tienda.id, tienda.nombre)) }
            if (result.size >= maxItems) break
        }
        return result.take(maxItems)
    }

    suspend fun todasLasTiendas(): List<Tienda> {
        val snap = db.collection("tiendas").get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject<Tienda>()?.copy(id = doc.id)
        }
    }

    // Busca kioscos por nombre o dirección
    suspend fun buscarKioscos(query: String): List<Tienda> {
        val q = query.trim().lowercase()
        return todasLasTiendas().filter { tienda ->
            tienda.nombreBusqueda.contains(q) ||
            tienda.nombre.lowercase().contains(q) ||
            tienda.categoria.lowercase().contains(q) ||
            tienda.direccion.lowercase().contains(q)
        }
    }

    // Busca productos en el stock de todas las tiendas
    suspend fun buscarStock(query: String, tiendas: List<Tienda>): List<StockItemConTienda> {
        val q = query.trim().lowercase()
        val result = mutableListOf<StockItemConTienda>()
        for (tienda in tiendas.take(12)) {
            val snap = db.collection("tiendas").document(tienda.id)
                .collection("stock")
                .get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject<StockItem>()?.copy(productoId = doc.id)
            }.filter { item ->
                item.nombre.lowercase().contains(q) ||
                item.marca.lowercase().contains(q)
            }.forEach { result.add(StockItemConTienda(it, tienda.id, tienda.nombre)) }
            if (result.size >= 20) break
        }
        return result.take(20)
    }

    // Filtra kioscos por tipo de local (campo categoria)
    suspend fun buscarKioscosPorTipo(tipo: String): List<Tienda> {
        val snap = db.collection("tiendas")
            .whereEqualTo("categoria", tipo)
            .get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject<Tienda>()?.copy(id = doc.id)
        }
    }
}
