package com.kosking.app.explorar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.datos.FiadoRepository
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Publicacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VidreraPublicaViewModel : ViewModel() {

    private val _local = MutableStateFlow<Local?>(null)
    val local: StateFlow<Local?> = _local

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando

    fun cargar(localId: String) {
        viewModelScope.launch {
            _cargando.value = true
            val loc = FiadoRepository.obtenerLocal(localId)
            _local.value = loc
            _cargando.value = false
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null && loc != null) {
                try { FiadoRepository.registrarVisitaLocal(uid, localId, loc.nombre) } catch (_: Exception) {}
            }
        }
    }

    fun publicacionesFlow(localId: String): StateFlow<List<Publicacion>> =
        FiadoRepository.publicacionesFlow(localId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
