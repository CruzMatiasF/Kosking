package com.kosking.app.feature.kiosco

sealed class NavDueno {
    object Panel : NavDueno()
    object Productos : NavDueno()
    object Clientes : NavDueno()
    object AgregarCliente : NavDueno()
    data class DetalleCliente(val id: String) : NavDueno()
    data class AgregarFiado(
        val clienteId: String,
        val tipoInicial: String = "cargo",
        val volverAPanel: Boolean = false
    ) : NavDueno()
    data class DetalleMovimiento(val id: String, val clienteId: String? = null) : NavDueno()
    object HistorialVentas : NavDueno()
}
