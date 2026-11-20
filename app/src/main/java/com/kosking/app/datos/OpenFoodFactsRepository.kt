package com.kosking.app.datos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class ProductoExterno(
    val nombre: String,
    val marca: String = "",
    val codigoBarras: String = "",
    val imagenUrl: String = ""
)

object OpenFoodFactsRepository {

    suspend fun buscarPorCodigo(codigo: String): ProductoExterno? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v0/product/$codigo.json")
            val response = url.readText()
            val json = JSONObject(response)
            if (json.optInt("status", 0) != 1) return@withContext null
            val product = json.optJSONObject("product") ?: return@withContext null
            val nombre = product.optString("product_name")
                .ifBlank { product.optString("product_name_es") }
                .ifBlank { product.optString("generic_name") }
                .ifBlank { return@withContext null }
            val marca = product.optString("brands").split(",").firstOrNull()?.trim() ?: ""
            val imagen = product.optString("image_front_small_url")
                .ifBlank { product.optString("image_url") }
            ProductoExterno(
                nombre = nombre.replaceFirstChar { it.uppercaseChar() },
                marca = marca,
                codigoBarras = codigo,
                imagenUrl = imagen
            )
        } catch (_: Exception) {
            null
        }
    }
}
