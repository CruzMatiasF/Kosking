package com.kosking.app.datos

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

data class ItemCatalogo(val nombre: String, val categoria: String)

object CatalogoRepository {

    private var cache: List<ItemCatalogo>? = null

    fun buscar(context: Context, query: String): List<ItemCatalogo> {
        val catalogo = cache ?: cargar(context).also { cache = it }
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return catalogo.filter { it.nombre.lowercase().contains(q) }
    }

    fun existeEnCache(context: Context, nombre: String): Boolean {
        val catalogo = cache ?: cargar(context).also { cache = it }
        return catalogo.any { it.nombre.equals(nombre.trim(), ignoreCase = true) }
    }

    // Contribuye un producto nuevo al catálogo global en Firestore
    fun contribuir(nombre: String, categoria: String = "Otros") {
        val db = FirebaseFirestore.getInstance()
        val id = nombre.trim().lowercase().replace(" ", "_")
        db.collection("catalogo").document(id)
            .set(mapOf("nombre" to nombre.trim(), "categoria" to categoria, "contribuido" to true))
    }

    private fun cargar(context: Context): List<ItemCatalogo> {
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
