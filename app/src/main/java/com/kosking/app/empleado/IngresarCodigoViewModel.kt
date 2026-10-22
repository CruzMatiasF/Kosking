package com.kosking.app.empleado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kosking.app.modelo.Tienda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class EstadoCodigo {
    object Idle : EstadoCodigo()
    object Buscando : EstadoCodigo()
    data class Exito(val tienda: Tienda) : EstadoCodigo()
    object CodigoInvalido : EstadoCodigo()
    data class Error(val mensaje: String) : EstadoCodigo()
}

class IngresarCodigoViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val estado = MutableStateFlow<EstadoCodigo>(EstadoCodigo.Idle)

    fun unirseConCodigo(codigo: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            estado.value = EstadoCodigo.Buscando
            try {
                val snap = db.collection("tiendas")
                    .whereEqualTo("codigoInvitacion", codigo.trim().uppercase())
                    .get().await()

                if (snap.isEmpty) {
                    estado.value = EstadoCodigo.CodigoInvalido
                    return@launch
                }

                val doc = snap.documents.first()
                val tienda = doc.toObject(Tienda::class.java)?.copy(id = doc.id)
                    ?: run { estado.value = EstadoCodigo.CodigoInvalido; return@launch }

                // Guarda el vínculo en el documento del empleado
                db.collection("usuarios").document(uid)
                    .set(
                        mapOf(
                            "uid" to uid,
                            "empleadoEnTiendaId" to tienda.id,
                            "email" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
                        ),
                        com.google.firebase.firestore.SetOptions.merge(),
                    ).await()

                estado.value = EstadoCodigo.Exito(tienda)
            } catch (e: Exception) {
                estado.value = EstadoCodigo.Error(e.message ?: "Error al buscar el código")
            }
        }
    }
}
