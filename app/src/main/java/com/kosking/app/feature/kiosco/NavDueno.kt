package com.kosking.app.dueno

sealed class NavDueno {
    object Panel : NavDueno()
    object AgregarCliente : NavDueno()
    data class DetalleCliente(val id: String) : NavDueno()
    data class AgregarFiado(
        val clienteId: String,
        val tipoInicial: String = "cargo",
        val volverAPanel: Boolean = false
    ) : NavDueno()
    data class DetalleMovimiento(val id: String, val clienteId: String? = null) : NavDueno()
}
