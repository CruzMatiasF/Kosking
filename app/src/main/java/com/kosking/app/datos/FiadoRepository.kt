package com.kosking.app.datos

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Empleado
import com.kosking.app.modelo.Local
import com.kosking.app.modelo.Movimiento
import com.kosking.app.modelo.Producto
import com.kosking.app.modelo.Usuario
import com.kosking.app.modelo.Venta
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FiadoRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── Local ────────────────────────────────────────────────────────────────

    fun nuevoLocalId(): String = db.collection("locales").document().id

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

    suspend fun actualizarLocalIdUsuario(uid: String, localId: String) {
        db.collection("usuarios").document(uid).update("localId", localId).await()
    }

    suspend fun actualizarNombreUsuario(uid: String, nombre: String) {
        db.collection("usuarios").document(uid).update("nombre", nombre).await()
    }

    suspend fun actualizarDatosPersonales(uid: String, nombre: String, telefono: String, dni: String) {
        db.collection("usuarios").document(uid)
            .update("nombre", nombre, "telefono", telefono, "dni", dni).await()
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
                "alertaEmpleado" to cliente.alertaEmpleado,
                "fechaCompromiso" to cliente.fechaCompromiso
            )
        ).await()
    }

    suspend fun setFechaCompromiso(clienteId: String, fecha: com.google.firebase.Timestamp?) {
        db.collection("clientes").document(clienteId)
            .update("fechaCompromiso", fecha).await()
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
            .limit(1500)
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
            .limit(1000)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
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

    suspend fun registrarVenta(venta: Venta) {
        val ref = db.collection("ventas").document()
        ref.set(venta.copy(id = ref.id)).await()
    }

    fun ventasLocalFlow(localId: String, diasAtras: Int = 7): Flow<List<Venta>> = callbackFlow {
        val desdeTs = com.google.firebase.Timestamp(System.currentTimeMillis() / 1000 - diasAtras * 86400L, 0)
        val sub = db.collection("ventas")
            .whereEqualTo("localId", localId)
            .whereGreaterThan("fecha", desdeTs)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Venta::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
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

    fun empleadoFlowPorUid(uid: String, localId: String): Flow<Empleado?> = callbackFlow {
        val sub = db.collection("empleados")
            .whereEqualTo("uid", uid)
            .whereEqualTo("localId", localId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Empleado::class.java)?.firstOrNull())
            }
        awaitClose { sub.remove() }
    }

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

    suspend fun actualizarCampoEmpleado(empleadoId: String, campo: String, valor: Any) {
        db.collection("empleados").document(empleadoId).update(campo, valor).await()
    }

    suspend fun verificarYUsarCodigoEmpleado(codigo: String, uid: String): String? {
        val snap = db.collection("codigosEmpleado")
            .whereEqualTo("codigo", codigo)
            .get().await()
        val doc = snap.documents.firstOrNull { it.getBoolean("usado") == false } ?: return null
        val localId = doc.getString("localId")?.takeIf { it.isNotBlank() } ?: return null
        doc.reference.update("usado", true, "usadoPorUid", uid).await()
        return localId
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
                trySend(snap?.toObjects(Producto::class.java) ?: emptyList())
            }
        awaitClose { sub.remove() }
    }

    fun nuevoProductoId(): String = db.collection("productos").document().id

    suspend fun subirImagenProducto(localId: String, productoId: String, uri: Uri): String {
        val ref = FirebaseStorage.getInstance()
            .reference.child("locales/$localId/productos/$productoId.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // Versión batch: usa la lista en memoria para evitar N lecturas a Firestore.
    // items: (nombre, precio, cantidad)
    suspend fun upsertProductosBatch(localId: String, items: List<Triple<String, Double, Int>>, productosEnMemoria: List<Producto>) {
        if (items.isEmpty()) return
        val batch = db.batch()
        items.filter { it.first.isNotBlank() }.forEach { (nombre, precio, cantidad) ->
            val precioUnit = if (cantidad > 0) precio / cantidad else precio
            val existente = productosEnMemoria.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
            if (existente != null) {
                batch.set(
                    db.collection("productos").document(existente.id),
                    mapOf(
                        "ultimoPrecio" to precioUnit,
                        "vecesUsado" to com.google.firebase.firestore.FieldValue.increment(1)
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } else {
                val ref = db.collection("productos").document()
                batch.set(ref, Producto(id = ref.id, localId = localId, nombre = nombre, ultimoPrecio = precioUnit, vecesUsado = 1))
            }
        }
        batch.commit().await()
    }

    suspend fun upsertProducto(localId: String, nombre: String, precio: Double, categoria: String = "", imagenUrl: String = "", productoId: String? = null, codigoBarras: String = "", presentacion: String = "", unidadVenta: String = "") {
        if (nombre.isBlank()) return
        val snap = db.collection("productos")
            .whereEqualTo("localId", localId)
            .whereEqualTo("nombre", nombre)
            .limit(1)
            .get().await()
        if (snap.isEmpty) {
            val ref = if (productoId != null) db.collection("productos").document(productoId)
                      else db.collection("productos").document()
            ref.set(Producto(id = ref.id, localId = localId, nombre = nombre, ultimoPrecio = precio, vecesUsado = 1, categoria = categoria, imagenUrl = imagenUrl, codigoBarras = codigoBarras, presentacion = presentacion, unidadVenta = unidadVenta)).await()
        } else {
            val updates = mutableMapOf<String, Any>(
                "ultimoPrecio" to precio,
                "vecesUsado" to com.google.firebase.firestore.FieldValue.increment(1),
                "unidadVenta" to unidadVenta
            )
            if (categoria.isNotBlank()) updates["categoria"] = categoria
            if (imagenUrl.isNotBlank()) updates["imagenUrl"] = imagenUrl
            if (codigoBarras.isNotBlank()) updates["codigoBarras"] = codigoBarras
            if (presentacion.isNotBlank()) updates["presentacion"] = presentacion
            snap.documents.first().reference.update(updates).await()
        }
    }

    suspend fun editarProducto(productoId: String, nombre: String, precio: Double, categoria: String = "", imagenUrl: String = "", codigoBarras: String = "", presentacion: String = "", unidadVenta: String = "") {
        val updates = mutableMapOf<String, Any>("nombre" to nombre, "ultimoPrecio" to precio, "categoria" to categoria, "presentacion" to presentacion, "unidadVenta" to unidadVenta)
        if (imagenUrl.isNotBlank()) updates["imagenUrl"] = imagenUrl
        if (codigoBarras.isNotBlank()) updates["codigoBarras"] = codigoBarras
        db.collection("productos").document(productoId).update(updates).await()
    }

    suspend fun eliminarProducto(productoId: String) {
        db.collection("productos").document(productoId).delete().await()
    }

    // ── Catálogo global de productos ─────────────────────────────────────────

    private var cacheGlobal: List<String>? = null

    suspend fun buscarProductosGlobales(query: String): List<String> {
        if (query.length < 2) return emptyList()
        val catalogo = cacheGlobal ?: run {
            val snap = db.collection("productos_globales")
                .orderBy("vecesUsado", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(200)
                .get().await()
            snap.documents.mapNotNull { it.getString("nombre") }.also { cacheGlobal = it }
        }
        val q = query.trim().lowercase()
        return catalogo.filter { nombre ->
            nombre.lowercase().split(" ").any { it.startsWith(q) }
        }.take(10)
    }

    suspend fun upsertProductoGlobal(nombre: String) {
        val normalizado = normalizarNombreProducto(nombre)
        if (normalizado.isBlank()) return
        val id = normalizado.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        db.collection("productos_globales").document(id)
            .set(
                mapOf(
                    "nombre" to normalizado,
                    "tokens" to tokenizar(normalizado),
                    "vecesUsado" to com.google.firebase.firestore.FieldValue.increment(1)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        cacheGlobal = null
    }

    suspend fun syncProductosGlobales(nombres: List<String>) {
        if (nombres.isEmpty()) return
        nombres.filter { it.isNotBlank() }
            .map { normalizarNombreProducto(it) }
            .distinct()
            .chunked(500).forEach { lote ->
                val batch = db.batch()
                lote.forEach { normalizado ->
                    val id = normalizado.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
                    val ref = db.collection("productos_globales").document(id)
                    batch.set(
                        ref,
                        mapOf(
                            "nombre" to normalizado,
                            "tokens" to tokenizar(normalizado),
                            "vecesUsado" to com.google.firebase.firestore.FieldValue.increment(1)
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
                batch.commit().await()
            }
    }

    private fun tokenizar(nombre: String): List<String> =
        nombre.lowercase().split(" ").filter { it.isNotBlank() }.distinct()

    private fun normalizarNombreProducto(nombre: String): String =
        nombre.trim().lowercase().split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    suspend fun obtenerEmpleoDeUsuario(uid: String): Pair<String, String>? {
        val doc = db.collection("usuarios").document(uid).get().await()
        val localId = doc.getString("empleadoLocalId") ?: ""
        val nombreLocal = doc.getString("empleadoNombreLocal") ?: ""
        return if (localId.isNotBlank()) Pair(localId, nombreLocal) else null
    }
}
