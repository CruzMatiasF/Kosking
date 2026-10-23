package com.kosking.app.modelo

import com.google.firebase.Timestamp

data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val email: String = "",
    val tiendaId: String? = null,
    val empleadoEnTiendaId: String? = null,
    val creadoAt: Timestamp? = null
)

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val nombreBusqueda: String = "",
    val marca: String = "",
    val categoria: String = "",
    val creadoPor: String = "",
    val creadoAt: Timestamp? = null
)

data class Tienda(
    val id: String = "",
    val nombre: String = "",
    val nombreBusqueda: String = "",
    val duenioUid: String = "",
    val codigoInvitacion: String = "",
    val direccion: String = "",
    val categoria: String = "",
    val telefono: String = "",
    val ventasDelDia: Double = 0.0,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    // Horario estructurado
    val horarioApertura: String = "",
    val horarioCierre: String = "",
    val diasActivos: String = "Todos los días",
    // Override manual (expira a medianoche)
    val overrideAbierto: Boolean? = null,
    val overrideHasta: Timestamp? = null,
    val creadoAt: Timestamp? = null
)

data class StockItem(
    val productoId: String = "",
    val nombre: String = "",
    val marca: String = "",
    val nombreBusqueda: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 0,
    val updatedAt: Timestamp? = null
)

data class Oferta(
    val id: String = "",
    val productoId: String = "",
    val nombreProducto: String = "",
    val precioNormal: Double = 0.0,
    val precioOferta: Double = 0.0,
    val descripcion: String? = null,
    val venceAt: Timestamp? = null,
    val activa: Boolean = true,
    val creadoAt: Timestamp? = null
)

data class ItemVenta(
    val productoId: String = "",
    val nombre: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0
)

data class Venta(
    val id: String = "",
    val empleadoUid: String = "",
    val items: List<ItemVenta> = emptyList(),
    val total: Double = 0.0,
    val fecha: Timestamp? = null
)
