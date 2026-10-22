package com.kosking.app.busqueda

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.datos.ProductoRepository
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BusquedaViewModel : ViewModel() {

    var productos = mutableStateOf<List<Producto>>(emptyList())
        private set

    var kioscos = mutableStateOf<List<Tienda>>(emptyList())
        private set

    var cargando = mutableStateOf(false)
        private set

    private var jobBusqueda: Job? = null

    fun buscar(query: String) {
        jobBusqueda?.cancel()
        if (query.isBlank()) {
            productos.value = emptyList()
            kioscos.value = emptyList()
            return
        }
        jobBusqueda = viewModelScope.launch {
            delay(300) // debounce — espera que el usuario pare de escribir
            cargando.value = true
            try {
                productos.value = ProductoRepository.buscarProductos(query)
                kioscos.value = KioscoRepository.buscarKioscos(query)
            } catch (_: Exception) { }
            cargando.value = false
        }
    }
}
