package com.kosking.app.busqueda

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.modelo.StockItemConTienda
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BusquedaViewModel : ViewModel() {

    var stockItems = mutableStateOf<List<StockItemConTienda>>(emptyList())
        private set

    var kioscos = mutableStateOf<List<Tienda>>(emptyList())
        private set

    var cargando = mutableStateOf(false)
        private set

    private var jobBusqueda: Job? = null

    fun esFiltroTipo(query: String) = query.startsWith("tipo:")
    fun tipoDeQuery(query: String) = query.removePrefix("tipo:")

    fun buscar(query: String) {
        jobBusqueda?.cancel()
        if (query.isBlank()) {
            stockItems.value = emptyList()
            kioscos.value = emptyList()
            return
        }
        jobBusqueda = viewModelScope.launch {
            delay(300)
            cargando.value = true
            if (esFiltroTipo(query)) {
                val tipo = tipoDeQuery(query)
                try {
                    kioscos.value = KioscoRepository.buscarKioscosPorTipo(tipo)
                } catch (_: Exception) { }
                stockItems.value = emptyList()
            } else {
                try {
                    val todasTiendas = KioscoRepository.todasLasTiendas()
                    val q = query.trim().lowercase()
                    kioscos.value = todasTiendas.filter { tienda ->
                        tienda.nombreBusqueda.contains(q) ||
                        tienda.nombre.lowercase().contains(q) ||
                        tienda.categoria.lowercase().contains(q) ||
                        tienda.direccion.lowercase().contains(q)
                    }
                    stockItems.value = KioscoRepository.buscarStock(query, todasTiendas)
                } catch (_: Exception) { }
            }
            cargando.value = false
        }
    }
}
