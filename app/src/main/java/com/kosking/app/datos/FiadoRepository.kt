package com.kosking.app.datos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
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
                "notaInterna" to cliente.notaInterna
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

    suspend fun agregarMovimiento(movimiento: Movimiento) {
        val ref = db.collection("movimientos").document()
        val conId = movimiento.copy(id = ref.id)
        ref.set(conId).await()
        actualizarDeuda(movimiento.clienteId, movimiento.monto, movimiento.tipo)
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

    suspend fun obtenerEmpleoDeUsuario(uid: String): Pair<String, String>? {
        val doc = db.collection("usuarios").document(uid).get().await()
        val localId = doc.getString("empleadoLocalId") ?: ""
        val nombreLocal = doc.getString("empleadoNombreLocal") ?: ""
        return if (localId.isNotBlank()) Pair(localId, nombreLocal) else null
    }
}
