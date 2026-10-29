package com.kosking.app.modelo

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp

@Immutable
data class Permisos(
    val anotarFiados: Boolean = true,
    val registrarPagos: Boolean = true,
    val verTotal: Boolean = false
)

@Immutable
data class Local(
    val id: String = "",
    val nombre: String = "",
    val duenoUid: String = "",
    val codigo: String = "",
    val permisos: Permisos = Permisos(),
    val telefono: String = "",
    val tipo: String = "Kiosco",
    val direccion: String = "",
    val abierto: Boolean = true,
    val imagenUrl: String = "",
    val descripcion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Immutable
data class Cliente(
    val id: String = "",
    val localId: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val domicilio: String = "",
    val pin: String = "",
    val habilitado: Boolean = true,
    val deudaTotal: Double = 0.0,
    val codigoAcceso: String = "",
    val limiteCredito: Double = 0.0
)

@Immutable
data class Movimiento(
    val id: String = "",
    val localId: String = "",
    val clienteId: String = "",
    val tipo: String = "cargo",
    val producto: String = "",
    val cantidad: Int = 1,
    val monto: Double = 0.0,
    val fecha: Timestamp = Timestamp.now(),
    val anotadoPorUid: String = "",
    val anotadoPorNombre: String = "",
    val anulado: Boolean = false,
    val motivoAnulacion: String = "",
    val disputado: Boolean = false
)

@Immutable
data class Empleado(
    val id: String = "",
    val localId: String = "",
    val uid: String = "",
    val nombre: String = ""
)

@Immutable
data class Producto(
    val id: String = "",
    val localId: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val habilitado: Boolean = true,
    val esOferta: Boolean = false,
    val precioAnterior: Double = 0.0,
    val imagenUrl: String = ""
)

@Immutable
data class LocalGuardado(
    val localId: String = "",
    val clienteId: String = "",
    val nombreLocal: String = ""
)

@Immutable
data class Usuario(
    val uid: String = "",
    val nombre: String = "",
    val rol: String = "dueno",
    val localId: String = "",
    val empleadoLocalId: String = "",
    val empleadoNombreLocal: String = "",
    val fcmToken: String = ""
)

@Immutable
data class Notificacion(
    val id: String = "",
    val uid: String = "",
    val titulo: String = "",
    val cuerpo: String = "",
    val leida: Boolean = false,
    val fecha: Timestamp = Timestamp.now(),
    val tipo: String = "sistema"   // "oferta", "sistema", "local"
)
