package com.kosking.app.datos

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

data class ItemCatalogo(val nombre: String, val categoria: String)

object CatalogoRepository {

    private val db = FirebaseFirestore.getInstance()
    private var cache: List<ItemCatalogo>? = null

    suspend fun buscar(context: Context, query: String): List<ItemCatalogo> {
        if (query.isBlank()) return emptyList()
        val catalogo = cache ?: cargarDeFirestore(context).also { cache = it }
        val q = query.trim().lowercase()
        return catalogo.filter { item ->
            item.nombre.lowercase().split(" ").any { it.startsWith(q) }
        }.take(8)
    }

    suspend fun migrarSiVacio(context: Context) {
        val snap = db.collection("catalogo_base").limit(1).get().await()
        if (!snap.isEmpty) return
        val items = cargarDelJson(context)
        if (items.isEmpty()) return
        items.chunked(500).forEach { lote ->
            val batch = db.batch()
            lote.forEach { item ->
                val id = item.nombre.trim().lowercase()
                    .replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
                val ref = db.collection("catalogo_base").document(id)
                batch.set(ref, mapOf("nombre" to item.nombre, "categoria" to item.categoria))
            }
            batch.commit().await()
        }
    }

    private suspend fun cargarDeFirestore(context: Context): List<ItemCatalogo> {
        return try {
            val snap = db.collection("catalogo_base")
                .orderBy("categoria")
                .limit(500)
                .get().await()
            snap.documents.map {
                ItemCatalogo(
                    nombre = it.getString("nombre") ?: "",
                    categoria = it.getString("categoria") ?: ""
                )
            }.filter { it.nombre.isNotBlank() }
        } catch (e: Exception) {
            cargarDelJson(context)
        }
    }

    private fun cargarDelJson(context: Context): List<ItemCatalogo> {
        return try {
            val json = context.assets.open("catalogo.json").bufferedReader().readText()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ItemCatalogo(
                    nombre = obj.getString("nombre"),
                    categoria = obj.optString("categoria", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
