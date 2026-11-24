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

enum class ModoEscaner { VENDER, ANOTAR, CARGAR_NUEVO }

internal data class ProductoDetectado(
    val nombre: String,
    val precio: Double,
    val productoId: String? = null,
    val codigoBarras: String = ""
)

/** Estado inmutable de un carrito individual. */
data class CarritoState(
    val id: Int,
    val itemsAnotados: List<ItemAnotado> = emptyList(),
    val clienteSeleccionado: Cliente? = null,
    val busquedaCliente: String = ""
)

internal class EscanerViewModel(private val duenoVm: DuenoViewModel) : ViewModel() {

    // ── Multi-carrito ──────────────────────────────────────────────────────────
    var carritos by mutableStateOf(listOf(CarritoState(id = 1)))
    var carritoActivoIdx by mutableStateOf(0)

    val carritoActivo: CarritoState get() = carritos[carritoActivoIdx]

    private fun actualizarCarritoActivo(block: CarritoState.() -> CarritoState) {
        carritos = carritos.mapIndexed { i, c -> if (i == carritoActivoIdx) c.block() else c }
    }

    fun agregarCarrito() {
        if (carritos.size >= 3) return
        val newId = (carritos.maxOf { it.id }) + 1
        carritos = carritos + CarritoState(id = newId)
        carritoActivoIdx = carritos.size - 1
    }

    fun cerrarCarrito(idx: Int) {
        if (carritos.size <= 1) return
        carritos = carritos.filterIndexed { i, _ -> i != idx }
        carritoActivoIdx = minOf(carritoActivoIdx, carritos.size - 1)
    }

    fun seleccionarCarrito(idx: Int) {
        carritoActivoIdx = idx
    }

    // ── Propiedades delegadas al carrito activo (compatibilidad con código existente) ──
    var itemsAnotados: List<ItemAnotado>
        get() = carritoActivo.itemsAnotados
        set(value) { actualizarCarritoActivo { copy(itemsAnotados = value) } }

    var clienteSeleccionado: Cliente?
        get() = carritoActivo.clienteSeleccionado
        set(value) { actualizarCarritoActivo { copy(clienteSeleccionado = value) } }

    var busquedaCliente: String
        get() = carritoActivo.busquedaCliente
        set(value) { actualizarCarritoActivo { copy(busquedaCliente = value) } }

    // ── Estado global (no pertenece a ningún carrito) ─────────────────────────
    var modo by mutableStateOf(ModoEscaner.VENDER)
    var mostrarRegistroCliente by mutableStateOf(false)
    var nombreNuevoCliente by mutableStateOf("")
    var telefonoNuevoCliente by mutableStateOf("")
    var productoParaCargar by mutableStateOf<ProductoDetectado?>(null)
    var buscandoProducto by mutableStateOf(false)
    var cargandoAnotacion by mutableStateOf(false)
    var queryBusqueda by mutableStateOf("")
    var precioCarga by mutableStateOf("")
    var nombreCarga by mutableStateOf("")
    var mostrarConfirmar by mutableStateOf(false)
    var anotacionExitosa by mutableStateOf<Triple<Cliente, List<ItemAnotado>, String>?>(null)
    var ventaExitosa by mutableStateOf<Pair<List<ItemAnotado>, Double>?>(null)
    var mostrarDialogoCambioModo by mutableStateOf(false)
    var escaneoExitoso by mutableStateOf(false)

    val yaDetecto = AtomicBoolean(false)
    private var ultimoCodigo = ""
    private var ultimoCodigoTs = 0L

    fun resetear(modoInicial: ModoEscaner = ModoEscaner.VENDER) {
        modo = modoInicial
        carritos = listOf(CarritoState(id = 1))
        carritoActivoIdx = 0
        mostrarConfirmar = false
        ventaExitosa = null
        anotacionExitosa = null
        queryBusqueda = ""
        yaDetecto.set(false)
    }

    fun agregarALista(item: ItemAnotado) {
        val idx = itemsAnotados.indexOfFirst {
            if (it.productoId != null && item.productoId != null)
                it.productoId == item.productoId
            else
                it.nombre.lowercase() == item.nombre.lowercase()
        }
        itemsAnotados = if (idx >= 0)
            itemsAnotados.mapIndexed { i, it -> if (i == idx) it.copy(cantidad = it.cantidad + 1) else it }
        else itemsAnotados + item
        queryBusqueda = ""
    }

    fun confirmarAnotacion(cliente: Cliente) {
        val tipo = if (modo == ModoEscaner.VENDER) "venta" else "cargo"
        val itemsSnap = itemsAnotados.toList()
        cargandoAnotacion = true
        duenoVm.agregarMovimientos(
            clienteId = cliente.id,
            items = itemsSnap.map { Triple(it.nombre, it.precio * it.cantidad, it.cantidad) },
            tipo = tipo,
            onError = { cargandoAnotacion = false },
            onExito = {
                cargandoAnotacion = false
                mostrarConfirmar = false
                anotacionExitosa = Triple(cliente, itemsSnap, tipo)
                // Limpiar carrito confirmado
                actualizarCarritoActivo { copy(itemsAnotados = emptyList(), clienteSeleccionado = null, busquedaCliente = "") }
            }
        )
    }

    fun registrarVenta() {
        val itemsSnap = itemsAnotados.toList()
        cargandoAnotacion = true
        duenoVm.registrarVenta(
            items = itemsSnap,
            onError = { cargandoAnotacion = false },
            onExito = {
                cargandoAnotacion = false
                mostrarConfirmar = false
                val total = itemsSnap.sumOf { it.precio * it.cantidad }
                ventaExitosa = itemsSnap to total
                actualizarCarritoActivo { copy(itemsAnotados = emptyList()) }
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
            imagenUri = null,
            codigoBarras = prod.codigoBarras
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

        // QR de cliente: ignorar en modo Vender (no requiere cliente)
        if (codigo.startsWith(PREFIJO_CLIENTE_QR)) {
            if (modo != ModoEscaner.VENDER) {
                val clienteId = codigo.removePrefix(PREFIJO_CLIENTE_QR)
                val cliente = duenoVm.clientes.value.find { it.id == clienteId }
                if (cliente != null) {
                    clienteSeleccionado = cliente
                    busquedaCliente = ""
                }
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
                    ModoEscaner.VENDER, ModoEscaner.ANOTAR -> {
                        if (enLocal != null) {
                            agregarALista(ItemAnotado(nombre, precio, productoId))
                            escaneoExitoso = true
                            delay(400)
                            escaneoExitoso = false
                            yaDetecto.set(false)
                        } else {
                            // Producto no existe en el catálogo → ir a cargarlo
                            productoParaCargar = ProductoDetectado(nombre, precio, productoId, codigo)
                            nombreCarga = nombre
                            precioCarga = if (precio > 0) precio.toInt().toString() else ""
                            modo = ModoEscaner.CARGAR_NUEVO
                        }
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
