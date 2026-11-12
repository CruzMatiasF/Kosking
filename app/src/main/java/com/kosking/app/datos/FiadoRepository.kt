package com.kosking.app.datos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Usuario
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FiadoRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── Local ────────────────────────────────────────────────────────────────

    suspend fun crearLocal(local: Local) {
        db.collection("locales").document(local.id).set(local).await()
    }

    suspend fun obtenerLocal(localId: String): Local? {
        return db.collection("locales").document(localId).get().await()
            .toObject(Local::class.java)
    }

    suspend fun actualizarLocal(local: Local) {
        db.collection("locales").document(local.id).set(local).await()
    }

    fun localFlow(localId: String): Flow<Local?> = callbackFlow {
        val sub = db.collection("locales").document(localId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(Local::class.java))
            }
        awaitClose { sub.remove() }
    }

    // ── Usuario ──────────────────────────────────────────────────────────────

    suspend fun guardarUsuario(usuario: Usuario) {
        db.collection("usuarios").document(usuario.uid).set(usuario).await()
    }

    suspend fun guardarFcmToken(uid: String, token: String) {
        db.collection("usuarios").document(uid).update("fcmToken", token).await()
    }

    suspend fun obtenerUsuario(uid: String): Usuario? {
        return db.collection("usuarios").document(uid).get().await()
            .toObject(Usuario::class.java)
    }

    suspend fun actualizarNombreUsuario(uid: String, nombre: String) {
        db.collection("usuarios").document(uid).update("nombre", nombre).await()
    }

    // ── Clientes ─────────────────────────────────────────────────────────────

    fun clientesFlow(localId: String): Flow<List<Cliente>> = callbackFlow {
        val sub = db.collection("clientes")
            .whereEqualTo("localId", localId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Cliente::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun agregarCliente(cliente: Cliente): String {
        val ref = db.collection("clientes").document()
        val conId = cliente.copy(id = ref.id)
        ref.set(conId).await()
        return ref.id
    }

    suspend fun actualizarCliente(cliente: Cliente) {
        // .update() en lugar de .set() para no pisar deudaTotal (gestionado por transacción)
        db.collection("clientes").document(cliente.id).update(
            mapOf(
                "nombre" to cliente.nombre,
                "telefono" to cliente.telefono,
                "domicilio" to cliente.domicilio,
                "habilitado" to cliente.habilitado,
                "limiteCredito" to cliente.limiteCredito,
                "fechaPago" to cliente.fechaPago,
                "notaInterna" to cliente.notaInterna,
                "alertaEmpleado" to cliente.alertaEmpleado
            )
        ).await()
    }

    suspend fun eliminarCliente(clienteId: String) {
        db.collection("clientes").document(clienteId).delete().await()
    }

    suspend fun tieneMovimientos(clienteId: String): Boolean {
        val snap = db.collection("movimientos")
            .whereEqualTo("clienteId", clienteId)
            .limit(1)
            .get().await()
        return !snap.isEmpty
    }

    fun clienteFlow(clienteId: String): Flow<Cliente?> = callbackFlow {
        val sub = db.collection("clientes").document(clienteId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(Cliente::class.java))
            }
        awaitClose { sub.remove() }
    }

    // ── Movimientos ──────────────────────────────────────────────────────────

    fun movimientosLocalFlow(localId: String, diasAtras: Int = 90): Flow<List<Movimiento>> = callbackFlow {
        val desdeTs = com.google.firebase.Timestamp(System.currentTimeMillis() / 1000 - diasAtras * 86400L, 0)
        val sub = db.collection("movimientos")
            .whereEqualTo("localId", localId)
            .whereGreaterThan("fecha", desdeTs)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(300)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Movimiento::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun obtenerMovimiento(id: String): Movimiento? =
        db.collection("movimientos").document(id).get().await().toObject(Movimiento::class.java)

    fun movimientosClienteFlow(clienteId: String): Flow<List<Movimiento>> = callbackFlow {
        val sub = db.collection("movimientos")
            .whereEqualTo("clienteId", clienteId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Movimiento::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun agregarMovimientos(movimientos: List<Movimiento>) {
        if (movimientos.isEmpty()) return
        val clienteId = movimientos.first().clienteId
        val tipo = movimientos.first().tipo
        val total = movimientos.sumOf { it.monto }
        val delta = if (tipo == "cargo") total else -total
        val batch = db.batch()
        movimientos.forEach { mov ->
            val ref = db.collection("movimientos").document()
            batch.set(ref, mov.copy(id = ref.id))
        }
        val clienteRef = db.collection("clientes").document(clienteId)
        batch.update(clienteRef, "deudaTotal", com.google.firebase.firestore.FieldValue.increment(delta))
        batch.commit().await()
    }

    suspend fun anularMovimiento(movimientoId: String, clienteId: String, monto: Double, tipo: String, motivo: String) {
        val delta = if (tipo == "cargo") -monto else monto
        val batch = db.batch()
        batch.update(db.collection("movimientos").document(movimientoId), "anulado", true, "motivoAnulacion", motivo)
        batch.update(db.collection("clientes").document(clienteId), "deudaTotal", com.google.firebase.firestore.FieldValue.increment(delta))
        batch.commit().await()
    }

    suspend fun corregirMontoMovimiento(movimientoId: String, clienteId: String, montoViejo: Double, montoNuevo: Double, tipo: String) {
        val delta = if (tipo == "cargo") montoNuevo - montoViejo else montoViejo - montoNuevo
        val batch = db.batch()
        batch.update(db.collection("movimientos").document(movimientoId), "monto", montoNuevo)
        batch.update(db.collection("clientes").document(clienteId), "deudaTotal",
            com.google.firebase.firestore.FieldValue.increment(delta))
        batch.commit().await()
    }

    suspend fun marcarDisputado(movimientoId: String, disputado: Boolean) {
        db.collection("movimientos").document(movimientoId)
            .update("disputado", disputado).await()
    }

    suspend fun recalcularDeuda(clienteId: String) {
        val movs = db.collection("movimientos")
            .whereEqualTo("clienteId", clienteId)
            .whereEqualTo("anulado", false)
            .get().await()
            .toObjects(Movimiento::class.java)
        val deudaReal = movs.sumOf { if (it.tipo == "cargo") it.monto else -it.monto }
            .coerceAtLeast(0.0)
        db.collection("clientes").document(clienteId)
            .update("deudaTotal", deudaReal).await()
    }

    // ── Empleados ────────────────────────────────────────────────────────────

    fun empleadosFlow(localId: String): Flow<List<Empleado>> = callbackFlow {
        val sub = db.collection("empleados")
            .whereEqualTo("localId", localId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Empleado::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun agregarEmpleado(empleado: Empleado) {
        val ref = db.collection("empleados").document()
        ref.set(empleado.copy(id = ref.id)).await()
    }

    suspend fun eliminarEmpleado(empleadoId: String) {
        db.collection("empleados").document(empleadoId).delete().await()
    }

    suspend fun guardarCodigoEmpleado(localId: String, nombreLocal: String, codigo: String) {
        val datos = mapOf(
            "localId" to localId,
            "nombreLocal" to nombreLocal,
            "codigo" to codigo,
            "usado" to false
        )
        db.collection("codigosEmpleado").document().set(datos).await()
    }

    suspend fun actualizarNombreEmpleadoPorUid(uid: String, localId: String, nombre: String) {
        val snap = db.collection("empleados")
            .whereEqualTo("uid", uid)
            .whereEqualTo("localId", localId)
            .get().await()
        snap.documents.forEach { it.reference.update("nombre", nombre).await() }
    }

    suspend fun eliminarEmpleadoPorUid(uid: String, localId: String) {
        val snap = db.collection("empleados")
            .whereEqualTo("uid", uid)
            .whereEqualTo("localId", localId)
            .get().await()
        snap.documents.forEach { it.reference.delete().await() }
    }

    // ── Empleo del usuario ───────────────────────────────────────────────────

    suspend fun guardarEmpleoEnUsuario(uid: String, localId: String, nombreLocal: String) {
        db.collection("usuarios").document(uid)
            .update("empleadoLocalId", localId, "empleadoNombreLocal", nombreLocal)
            .await()
    }

    suspend fun limpiarEmpleoEnUsuario(uid: String) {
        db.collection("usuarios").document(uid)
            .update("empleadoLocalId", "", "empleadoNombreLocal", "")
            .await()
    }

    // ── Productos ────────────────────────────────────────────────────────────

    fun productosFlow(localId: String): Flow<List<Producto>> = callbackFlow {
        val sub = db.collection("productos")
            .whereEqualTo("localId", localId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val lista = snap?.toObjects(Producto::class.java) ?: emptyList()
                trySend(lista.sortedByDescending { it.vecesUsado })
            }
        awaitClose { sub.remove() }
    }

    suspend fun upsertProducto(localId: String, nombre: String, precio: Double) {
        if (nombre.isBlank()) return
        val snap = db.collection("productos")
            .whereEqualTo("localId", localId)
            .whereEqualTo("nombre", nombre)
            .limit(1)
            .get().await()
        if (snap.isEmpty) {
            val ref = db.collection("productos").document()
            ref.set(Producto(id = ref.id, localId = localId, nombre = nombre, ultimoPrecio = precio, vecesUsado = 1)).await()
        } else {
            snap.documents.first().reference.update(
                "ultimoPrecio", precio,
                "vecesUsado", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
        }
    }

    suspend fun editarProducto(productoId: String, nombre: String, precio: Double) {
        db.collection("productos").document(productoId)
            .update("nombre", nombre, "ultimoPrecio", precio).await()
    }

    suspend fun eliminarProducto(productoId: String) {
        db.collection("productos").document(productoId).delete().await()
    }

    suspend fun obtenerEmpleoDeUsuario(uid: String): Pair<String, String>? {
        val doc = db.collection("usuarios").document(uid).get().await()
        val localId = doc.getString("empleadoLocalId") ?: ""
        val nombreLocal = doc.getString("empleadoNombreLocal") ?: ""
        return if (localId.isNotBlank()) Pair(localId, nombreLocal) else null
    }
}
