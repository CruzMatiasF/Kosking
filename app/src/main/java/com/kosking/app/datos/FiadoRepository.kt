package com.kosking.app.datos

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.LocalGuardado
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Notificacion
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

    fun localesFlow(): Flow<List<Local>> = callbackFlow {
        val sub = db.collection("locales")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Local::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun obtenerLocal(localId: String): Local? {
        return db.collection("locales").document(localId).get().await()
            .toObject(Local::class.java)
    }

    fun localFlow(localId: String): Flow<Local?> = callbackFlow {
        val sub = db.collection("locales").document(localId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(Local::class.java))
            }
        awaitClose { sub.remove() }
    }

    suspend fun obtenerLocalPorCodigo(codigo: String): Local? {
        val snap = db.collection("locales")
            .whereEqualTo("codigo", codigo.uppercase())
            .get().await()
        return snap.toObjects(Local::class.java).firstOrNull()
    }

    // ── Usuario ──────────────────────────────────────────────────────────────

    suspend fun guardarUsuario(usuario: Usuario) {
        db.collection("usuarios").document(usuario.uid).set(usuario).await()
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
        db.collection("clientes").document(cliente.id).set(cliente).await()
    }

    suspend fun eliminarCliente(clienteId: String, codigoAcceso: String) {
        db.collection("clientes").document(clienteId).delete().await()
        if (codigoAcceso.isNotBlank()) {
            db.collection("codigosCliente").document(codigoAcceso.uppercase()).delete().await()
        }
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

    suspend fun guardarCodigoCliente(codigo: String, clienteId: String, localId: String) {
        db.collection("codigosCliente").document(codigo.uppercase())
            .set(mapOf("clienteId" to clienteId, "localId" to localId))
            .await()
    }

    suspend fun obtenerClientePorCodigoAcceso(codigo: String): Cliente? {
        val ref = db.collection("codigosCliente").document(codigo.uppercase()).get().await()
        val clienteId = ref.getString("clienteId") ?: return null
        return db.collection("clientes").document(clienteId).get().await()
            .toObject(Cliente::class.java)
    }

    suspend fun obtenerClientePorPin(localId: String, pin: String): Cliente? {
        val snap = db.collection("clientes")
            .whereEqualTo("localId", localId)
            .whereEqualTo("pin", pin)
            .get().await()
        return snap.toObjects(Cliente::class.java).firstOrNull()
    }

    // ── Movimientos ──────────────────────────────────────────────────────────

    fun movimientosLocalFlow(localId: String): Flow<List<Movimiento>> = callbackFlow {
        val sub = db.collection("movimientos")
            .whereEqualTo("localId", localId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Movimiento::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    fun movimientosFlow(clienteId: String): Flow<List<Movimiento>> = callbackFlow {
        val sub = db.collection("movimientos")
            .whereEqualTo("clienteId", clienteId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Movimiento::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun agregarMovimiento(movimiento: Movimiento) {
        val ref = db.collection("movimientos").document()
        val conId = movimiento.copy(id = ref.id)
        ref.set(conId).await()
        actualizarDeuda(movimiento.clienteId, movimiento.monto, movimiento.tipo)
    }

    suspend fun anularMovimiento(movimientoId: String, clienteId: String, monto: Double, tipo: String, motivo: String) {
        db.collection("movimientos").document(movimientoId)
            .update("anulado", true, "motivoAnulacion", motivo).await()
        // Revertir el efecto en la deuda
        val tipoInverso = if (tipo == "cargo") "pago" else "cargo"
        actualizarDeuda(clienteId, monto, tipoInverso)
    }

    suspend fun marcarDisputado(movimientoId: String, disputado: Boolean) {
        db.collection("movimientos").document(movimientoId)
            .update("disputado", disputado).await()
    }

    private suspend fun actualizarDeuda(clienteId: String, monto: Double, tipo: String) {
        val ref = db.collection("clientes").document(clienteId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val actual = snap.getDouble("deudaTotal") ?: 0.0
            val nueva = if (tipo == "cargo") actual + monto else (actual - monto).coerceAtLeast(0.0)
            tx.update(ref, "deudaTotal", nueva)
        }.await()
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

    // ── Productos ────────────────────────────────────────────────────────────

    fun productosFlow(localId: String): Flow<List<Producto>> = callbackFlow {
        val sub = db.collection("productos")
            .whereEqualTo("localId", localId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Producto::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    fun productosOfertaFlow(): Flow<List<Producto>> = callbackFlow {
        val sub = db.collection("productos")
            .whereEqualTo("esOferta", true)
            .whereEqualTo("habilitado", true)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Producto::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    fun todosProductosFlow(limite: Int = 20): Flow<List<Producto>> = callbackFlow {
        val sub = db.collection("productos")
            .whereEqualTo("habilitado", true)
            .limit(limite.toLong())
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Producto::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun agregarProducto(producto: Producto) {
        val ref = db.collection("productos").document()
        ref.set(producto.copy(id = ref.id)).await()
    }

    suspend fun actualizarProducto(producto: Producto) {
        db.collection("productos").document(producto.id).set(producto).await()
    }

    suspend fun eliminarProducto(productoId: String) {
        db.collection("productos").document(productoId).delete().await()
    }

    suspend fun guardarCodigoEmpleado(localId: String, nombreLocal: String, nombreEmpleado: String, codigo: String) {
        val datos = mapOf(
            "localId" to localId,
            "nombreLocal" to nombreLocal,
            "nombreEmpleado" to nombreEmpleado,
            "codigo" to codigo,
            "usado" to false
        )
        db.collection("codigosEmpleado").document().set(datos).await()
    }

    // ── Locales guardados por cliente (subcolección de usuarios) ────────────

    fun localesGuardadosFlow(uid: String): Flow<List<LocalGuardado>> = callbackFlow {
        val sub = db.collection("usuarios").document(uid)
            .collection("localesGuardados")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(LocalGuardado::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun guardarLocalCliente(uid: String, localId: String, clienteId: String, nombreLocal: String) {
        db.collection("usuarios").document(uid)
            .collection("localesGuardados").document(localId)
            .set(mapOf("localId" to localId, "clienteId" to clienteId, "nombreLocal" to nombreLocal))
            .await()
    }

    // ── Notificaciones ───────────────────────────────────────────────────────

    fun notificacionesFlow(uid: String): Flow<List<Notificacion>> = callbackFlow {
        val sub = db.collection("notificaciones")
            .whereEqualTo("uid", uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Notificacion::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    suspend fun marcarNotificacionLeida(id: String) {
        db.collection("notificaciones").document(id).update("leida", true).await()
    }

    suspend fun marcarTodasLeidas(uid: String) {
        val snap = db.collection("notificaciones")
            .whereEqualTo("uid", uid)
            .whereEqualTo("leida", false)
            .get().await()
        val batch = db.batch()
        snap.documents.forEach { batch.update(it.reference, "leida", true) }
        batch.commit().await()
    }

    suspend fun quitarLocalCliente(uid: String, localId: String) {
        db.collection("usuarios").document(uid)
            .collection("localesGuardados").document(localId)
            .delete().await()
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

    suspend fun obtenerEmpleoDeUsuario(uid: String): Pair<String, String>? {
        val doc = db.collection("usuarios").document(uid).get().await()
        val localId = doc.getString("empleadoLocalId") ?: ""
        val nombreLocal = doc.getString("empleadoNombreLocal") ?: ""
        return if (localId.isNotBlank()) Pair(localId, nombreLocal) else null
    }
}
