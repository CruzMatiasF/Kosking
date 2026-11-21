package com.kosking.app.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.OpenFoodFactsRepository
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.ItemAnotado
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal const val PREFIJO_CLIENTE_QR = "ksk_c_"

internal enum class ModoEscaner { ANOTAR, CARGAR_NUEVO }

internal data class ProductoDetectado(
    val nombre: String,
    val precio: Double,
    val productoId: String? = null,
    val codigoBarras: String = ""
)

internal class EscanerViewModel(private val duenoVm: DuenoViewModel) : ViewModel() {

    var modo by mutableStateOf(ModoEscaner.ANOTAR)
    var clienteSeleccionado by mutableStateOf<Cliente?>(null)
    var mostrarBuscarCliente by mutableStateOf(false)
    var busquedaCliente by mutableStateOf("")
    var mostrarRegistroCliente by mutableStateOf(false)
    var nombreNuevoCliente by mutableStateOf("")
    var telefonoNuevoCliente by mutableStateOf("")
    var itemsAnotados by mutableStateOf(listOf<ItemAnotado>())
    var productoParaCargar by mutableStateOf<ProductoDetectado?>(null)
    var buscandoProducto by mutableStateOf(false)
    var cargandoAnotacion by mutableStateOf(false)
    var queryBusqueda by mutableStateOf("")
    var precioCarga by mutableStateOf("")
    var nombreCarga by mutableStateOf("")
    var mostrarConfirmar by mutableStateOf(false)
    var anotacionExitosa by mutableStateOf<Pair<Cliente, List<ItemAnotado>>?>(null)
    var mostrarDialogoCambioModo by mutableStateOf(false)
    var escaneoExitoso by mutableStateOf(false)

    // AtomicBoolean: leído desde el hilo del analizador de cámara
    val yaDetecto = AtomicBoolean(false)
    private var ultimoCodigo = ""
    private var ultimoCodigoTs = 0L

    fun agregarALista(item: ItemAnotado) {
        val idx = itemsAnotados.indexOfFirst { it.productoId != null && it.productoId == item.productoId }
        itemsAnotados = if (idx >= 0)
            itemsAnotados.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
        else itemsAnotados + item
        queryBusqueda = ""
    }

    fun confirmarAnotacion(cliente: Cliente, onCerrar: () -> Unit) {
        val itemsSnap = itemsAnotados.toList()
        cargandoAnotacion = true
        duenoVm.agregarMovimientos(
            clienteId = cliente.id,
            items = itemsSnap.map { Triple(it.nombre, it.precio, it.cantidad) },
            tipo = "cargo",
            onError = { cargandoAnotacion = false },
            onExito = {
                cargandoAnotacion = false
                mostrarConfirmar = false
                if (cliente.telefono.isNotBlank()) {
                    anotacionExitosa = Pair(cliente, itemsSnap)
                } else {
                    onCerrar()
                }
            }
        )
    }

    fun agregarClienteYObtener(nombre: String, telefono: String, onExito: (Cliente) -> Unit) {
        duenoVm.agregarClienteYObtener(nombre = nombre, telefono = telefono, onExito = onExito)
    }

    fun guardarProducto() {
        val prod = productoParaCargar ?: return
        duenoVm.guardarProducto(
            nombre = nombreCarga.trim().ifBlank { prod.nombre.ifBlank { prod.codigoBarras } },
            precio = precioCarga.toDoubleOrNull() ?: 0.0,
            productoId = prod.productoId,
            categoria = "",
            imagenUri = null
        ) {
            productoParaCargar = null
            precioCarga = ""
            nombreCarga = ""
            yaDetecto.set(false)
        }
    }

    fun onCodigo(codigo: String) {
        val ahora = System.currentTimeMillis()
        if (codigo == ultimoCodigo && ahora - ultimoCodigoTs < 1500L) return
        if (yaDetecto.get()) return
        yaDetecto.set(true)
        ultimoCodigo = codigo
        ultimoCodigoTs = ahora

        // QR de cliente: setea silenciosamente sin interrumpir el escaneo
        if (codigo.startsWith(PREFIJO_CLIENTE_QR)) {
            val clienteId = codigo.removePrefix(PREFIJO_CLIENTE_QR)
            val cliente = duenoVm.clientes.value.find { it.id == clienteId }
            if (cliente != null) {
                clienteSeleccionado = cliente
                mostrarBuscarCliente = false
                busquedaCliente = ""
            }
            yaDetecto.set(false)
            return
        }

        buscandoProducto = true
        val productos = duenoVm.productos.value
        viewModelScope.launch {
            try {
                val enLocal = productos.firstOrNull { it.codigoBarras == codigo }
                val nombre: String
                val precio: Double
                val productoId: String?
                if (enLocal != null) {
                    nombre = enLocal.nombre; precio = enLocal.ultimoPrecio; productoId = enLocal.id
                } else {
                    val ext = OpenFoodFactsRepository.buscarPorCodigo(codigo)
                    nombre = ext?.nombre ?: ""; precio = 0.0; productoId = null
                }
                buscandoProducto = false
                when (modo) {
                    ModoEscaner.ANOTAR -> {
                        val item = ItemAnotado(nombre.ifBlank { "Código: $codigo" }, precio, productoId)
                        val idx = itemsAnotados.indexOfFirst { it.productoId != null && it.productoId == productoId }
                        itemsAnotados = if (idx >= 0)
                            itemsAnotados.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
                        else itemsAnotados + item
                        queryBusqueda = ""
                        escaneoExitoso = true
                        delay(400)
                        escaneoExitoso = false
                        yaDetecto.set(false)
                    }
                    ModoEscaner.CARGAR_NUEVO -> {
                        productoParaCargar = ProductoDetectado(nombre, precio, productoId, codigo)
                        nombreCarga = nombre
                        precioCarga = if (precio > 0) precio.toInt().toString() else ""
                    }
                }
            } catch (_: Exception) {
                buscandoProducto = false
                yaDetecto.set(false)
            }
        }
    }

    companion object {
        fun factory(duenoVm: DuenoViewModel) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EscanerViewModel(duenoVm) as T
        }
    }
}
