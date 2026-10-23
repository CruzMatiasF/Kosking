package com.kosking.app.datos

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FavoritosRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun uid() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private fun coleccion() = db.collection("usuarios").document(uid()).collection("favoritos")

    fun favoritosFlow(): Flow<Set<String>> = callbackFlow {
        val uid = uid()
        if (uid.isBlank()) { trySend(emptySet()); awaitClose { }; return@callbackFlow }
        val sub = coleccion().addSnapshotListener { snap, _ ->
            val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            trySend(ids)
        }
        awaitClose { sub.remove() }
    }

    suspend fun agregar(tiendaId: String) {
        val uid = uid(); if (uid.isBlank()) return
        coleccion().document(tiendaId)
            .set(mapOf("tiendaId" to tiendaId, "guardadoAt" to Timestamp.now()))
            .await()
    }

    suspend fun quitar(tiendaId: String) {
        val uid = uid(); if (uid.isBlank()) return
        coleccion().document(tiendaId).delete().await()
    }
}
