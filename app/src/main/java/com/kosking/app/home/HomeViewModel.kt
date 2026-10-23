package com.kosking.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.datos.KioscoRepository
import com.kosking.app.datos.ProductoRepository
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class RolUsuario { CARGANDO, SIN_ROL, DUENO, EMPLEADO }

class HomeViewModel : ViewModel() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db = FirebaseFirestore.getInstance()

    val kioscos: StateFlow<List<Tienda>> = KioscoRepository.kioscosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rolUsuario = MutableStateFlow(RolUsuario.CARGANDO)

    var productosPopulares = MutableStateFlow<List<Producto>>(emptyList())
        private set

    var cargando = androidx.compose.runtime.mutableStateOf(false)
        private set

    init {
        cargarProductosPopulares()
        detectarRol()
    }

    fun recargarRol() {
        detectarRol()
    }

    private fun detectarRol() {
        if (uid.isBlank()) { rolUsuario.value = RolUsuario.SIN_ROL; return }
        viewModelScope.launch {
            try {
                val doc = db.collection("usuarios").document(uid).get().await()
                val empleadoEnTiendaId = doc.getString("empleadoEnTiendaId")
                rolUsuario.value = when {
                    !empleadoEnTiendaId.isNullOrBlank() -> RolUsuario.EMPLEADO
                    else -> RolUsuario.SIN_ROL
                }
            } catch (_: Exception) {
                rolUsuario.value = RolUsuario.SIN_ROL
            }
        }
    }

    private fun cargarProductosPopulares() {
        viewModelScope.launch {
            cargando.value = true
            try {
                productosPopulares.value = ProductoRepository.productosPopulares()
            } catch (_: Exception) { }
            cargando.value = false
        }
    }
}
