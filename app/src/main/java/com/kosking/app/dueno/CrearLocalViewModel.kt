package com.kosking.app.dueno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class EstadoCrearLocal {
    object Idle : EstadoCrearLocal()
    object Guardando : EstadoCrearLocal()
    data class Exito(val tiendaId: String) : EstadoCrearLocal()
    data class Error(val mensaje: String) : EstadoCrearLocal()
}

class CrearLocalViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val estado = MutableStateFlow<EstadoCrearLocal>(EstadoCrearLocal.Idle)

    fun crearLocal(
        nombre: String,
        direccion: String,
        categoria: String,
        horario: String,
        telefono: String,
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            estado.value = EstadoCrearLocal.Error("No hay sesión activa. Volvé a iniciar sesión.")
            return
        }
        viewModelScope.launch {
            estado.value = EstadoCrearLocal.Guardando
            try {
                // Evitar locales duplicados
                val yaEsDueno = db.collection("tiendas")
                    .whereEqualTo("duenioUid", uid).get().await().isEmpty.not()
                if (yaEsDueno) {
                    estado.value = EstadoCrearLocal.Error("Ya tenés un local registrado. Administralo desde el Panel del dueño.")
                    return@launch
                }
                // Evitar que un empleado cree un local sin desvincularse antes
                val usuarioDoc = db.collection("usuarios").document(uid).get().await()
                val empleadoEnId = usuarioDoc.getString("empleadoEnTiendaId")
                if (!empleadoEnId.isNullOrBlank()) {
                    estado.value = EstadoCrearLocal.Error("Sos empleado de un local. Desvinculáte primero desde Configuración.")
                    return@launch
                }
                val codigo = generarCodigo()
                val tienda = hashMapOf(
                    "nombre" to nombre.trim(),
                    "nombreBusqueda" to nombre.trim().lowercase(),
                    "duenioUid" to uid,
                    "codigoInvitacion" to codigo,
                    "direccion" to direccion.trim(),
                    "categoria" to categoria,
                    "horario" to horario.trim(),
                    "telefono" to telefono.trim(),
                    "abierto" to true,
                    "ventasDelDia" to 0.0,
                    "creadoAt" to Timestamp.now(),
                )
                // Fire-and-forget: no esperamos confirmación del servidor
                val ref = db.collection("tiendas").document()
                ref.set(tienda)
                estado.value = EstadoCrearLocal.Exito(ref.id)
            } catch (e: Exception) {
                estado.value = EstadoCrearLocal.Error(e.message ?: "Error al crear el local")
            }
        }
    }

    private fun generarCodigo(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
