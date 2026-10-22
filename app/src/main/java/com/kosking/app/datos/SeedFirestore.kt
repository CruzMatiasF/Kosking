package com.kosking.app.datos

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Carga datos de prueba en Firestore. Llamar una sola vez desde la app.
object SeedFirestore {

    private val db = FirebaseFirestore.getInstance()

    suspend fun poblar() {
        poblarProductos()
        poblarTiendas()
    }

    private suspend fun poblarProductos() {
        val productos = listOf(
            mapOf("nombre" to "Coca-Cola 500ml",         "nombreBusqueda" to "coca cola 500ml",          "marca" to "Coca-Cola", "categoria" to "Bebidas"),
            mapOf("nombre" to "Coca-Cola 1.5L",          "nombreBusqueda" to "coca cola 1.5l",           "marca" to "Coca-Cola", "categoria" to "Bebidas"),
            mapOf("nombre" to "Pepsi 500ml",             "nombreBusqueda" to "pepsi 500ml",              "marca" to "Pepsi",     "categoria" to "Bebidas"),
            mapOf("nombre" to "Sprite 500ml",            "nombreBusqueda" to "sprite 500ml",             "marca" to "Sprite",    "categoria" to "Bebidas"),
            mapOf("nombre" to "Oreo 124g",               "nombreBusqueda" to "oreo 124g",                "marca" to "Oreo",      "categoria" to "Snacks"),
            mapOf("nombre" to "Lays Clásicas 150g",      "nombreBusqueda" to "lays clasicas 150g",       "marca" to "Lays",      "categoria" to "Snacks"),
            mapOf("nombre" to "Jugo Cepita Naranja 1L",  "nombreBusqueda" to "jugo cepita naranja 1l",   "marca" to "Cepita",    "categoria" to "Bebidas"),
            mapOf("nombre" to "Leche La Serenísima 1L",  "nombreBusqueda" to "leche la serenisima 1l",   "marca" to "Serenísima","categoria" to "Lácteos"),
            mapOf("nombre" to "Galletitas Maná",         "nombreBusqueda" to "galletitas mana",          "marca" to "Maná",      "categoria" to "Snacks"),
            mapOf("nombre" to "Agua Villavicencio 500ml","nombreBusqueda" to "agua villavicencio 500ml", "marca" to "Villavicencio","categoria" to "Bebidas"),
        )
        productos.forEach { prod ->
            db.collection("productos").add(prod).await()
        }
    }

    private suspend fun poblarTiendas() {
        val tiendas = listOf(
            Triple(
                mapOf(
                    "nombre" to "Kiosco El Cielo",
                    "nombreBusqueda" to "kiosco el cielo",
                    "duenioUid" to "",
                    "codigoInvitacion" to "CIELO01",
                    "direccion" to "San Martín 456, Palpalá",
                    "categoria" to "Kiosco",
                    "ventasDelDia" to 0.0,
                ),
                listOf(
                    mapOf("nombre" to "Coca-Cola 500ml",   "nombreBusqueda" to "coca cola 500ml",  "precio" to 1000.0, "cantidad" to 12),
                    mapOf("nombre" to "Coca-Cola 1.5L",    "nombreBusqueda" to "coca cola 1.5l",   "precio" to 1800.0, "cantidad" to 6),
                    mapOf("nombre" to "Oreo 124g",         "nombreBusqueda" to "oreo 124g",        "precio" to 1200.0, "cantidad" to 8),
                    mapOf("nombre" to "Lays Clásicas 150g","nombreBusqueda" to "lays clasicas 150g","precio" to 950.0, "cantidad" to 15),
                ),
                listOf(
                    mapOf("nombreProducto" to "Coca-Cola 1.5L","precioNormal" to 1800.0,"precioOferta" to 1500.0,"activa" to true),
                )
            ),
            Triple(
                mapOf(
                    "nombre" to "Kiosco La Esquina",
                    "nombreBusqueda" to "kiosco la esquina",
                    "duenioUid" to "",
                    "codigoInvitacion" to "ESQUINA01",
                    "direccion" to "Belgrano 123, Palpalá",
                    "categoria" to "Kiosco",
                    "ventasDelDia" to 0.0,
                ),
                listOf(
                    mapOf("nombre" to "Pepsi 500ml",            "nombreBusqueda" to "pepsi 500ml",           "precio" to 950.0,  "cantidad" to 10),
                    mapOf("nombre" to "Jugo Cepita Naranja 1L", "nombreBusqueda" to "jugo cepita naranja 1l","precio" to 1100.0, "cantidad" to 4),
                    mapOf("nombre" to "Leche La Serenísima 1L", "nombreBusqueda" to "leche la serenisima 1l","precio" to 1800.0, "cantidad" to 10),
                    mapOf("nombre" to "Agua Villavicencio 500ml","nombreBusqueda" to "agua villavicencio 500ml","precio" to 700.0,"cantidad" to 20),
                ),
                listOf(
                    mapOf("nombreProducto" to "Oreo 124g","precioNormal" to 1200.0,"precioOferta" to 990.0,"activa" to true),
                )
            ),
            Triple(
                mapOf(
                    "nombre" to "Maxikiosco 24",
                    "nombreBusqueda" to "maxikiosco 24",
                    "duenioUid" to "",
                    "codigoInvitacion" to "MAXI01",
                    "direccion" to "Rivadavia 789, Palpalá",
                    "categoria" to "Maxikiosco",
                    "ventasDelDia" to 0.0,
                ),
                listOf(
                    mapOf("nombre" to "Coca-Cola 500ml",    "nombreBusqueda" to "coca cola 500ml",   "precio" to 1050.0, "cantidad" to 20),
                    mapOf("nombre" to "Sprite 500ml",       "nombreBusqueda" to "sprite 500ml",      "precio" to 950.0,  "cantidad" to 18),
                    mapOf("nombre" to "Galletitas Maná",    "nombreBusqueda" to "galletitas mana",   "precio" to 800.0,  "cantidad" to 30),
                    mapOf("nombre" to "Lays Clásicas 150g", "nombreBusqueda" to "lays clasicas 150g","precio" to 980.0,  "cantidad" to 12),
                ),
                emptyList()
            ),
        )

        tiendas.forEach { (tienda, stock, ofertas) ->
            val ref = db.collection("tiendas").add(tienda).await()
            stock.forEach { item -> ref.collection("stock").add(item).await() }
            ofertas.forEach { oferta -> ref.collection("ofertas").add(oferta).await() }
        }
    }
}
