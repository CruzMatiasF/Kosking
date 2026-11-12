package com.kosking.app.modelo

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp

@Immutable
data class Local(
    val id: String = "",
    val nombre: String = "",
    val duenoUid: String = "",
    val telefono: String = "",
    val tipo: String = "Kiosco",
    val direccion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val abierto: Boolean = false
)

@Immutable
data class Cliente(
    val id: String = "",
    val localId: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val domicilio: String = "",
    val habilitado: Boolean = true,
    val deudaTotal: Double = 0.0,
    val limiteCredito: Double = 0.0,
    val fechaPago: Timestamp? = null,
    val notaInterna: String = "",
    val alertaEmpleado: Boolean = false,
    val fechaCompromiso: Timestamp? = null
)

@Immutable
data class Movimiento(
    val id: String = "",
    val localId: String = "",
    val clienteId: String = "",
    val tipo: String = "cargo",
    val producto: String = "",
    val monto: Double = 0.0,
    val fecha: Timestamp = Timestamp.now(),
    val anotadoPorUid: String = "",
    val anotadoPorNombre: String = "",
    val anulado: Boolean = false,
    val motivoAnulacion: String = "",
    val disputado: Boolean = false,
    val grupoId: String = "",
    val visitante: String = ""
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
    val ultimoPrecio: Double = 0.0,
    val vecesUsado: Int = 0,
    val categoria: String = "",
    val imagenUrl: String = ""
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

