package com.kosking.app.datos

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.kosking.app.modelo.Oferta
import com.kosking.app.modelo.StockItem
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

    // Busca kioscos por nombre
    suspend fun buscarKioscos(query: String): List<Tienda> {
        val q = query.trim().lowercase()
        return db.collection("tiendas")
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject<Tienda>()?.copy(id = doc.id)
            }.filter { it.nombreBusqueda.contains(q) }
    }
}
