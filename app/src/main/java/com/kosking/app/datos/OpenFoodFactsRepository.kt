package com.kosking.app.datos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ProductoExterno(
    val nombre: String,
    val marca: String = "",
    val codigoBarras: String = "",
    val imagenUrl: String = ""
)

object OpenFoodFactsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun buscarPorCodigo(codigo: String): ProductoExterno? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://world.openfoodfacts.org/api/v0/product/$codigo.json")
                .build()
            val response = client.newCall(request).execute().use { it.body?.string() } ?: return@withContext null
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
