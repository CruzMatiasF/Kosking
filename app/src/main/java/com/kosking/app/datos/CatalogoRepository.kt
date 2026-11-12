package com.kosking.app.datos

import android.content.Context
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
