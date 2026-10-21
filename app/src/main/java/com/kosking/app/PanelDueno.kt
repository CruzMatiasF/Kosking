package com.kosking.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ProductoCatalogo(
    val id: String = "",
    val nombre: String = "",
    val marca: String = "",
    val categoria: String = ""
)

data class ItemStock(
    val productoId: String = "",
    val nombre: String = "",
    val marca: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelDueno(modifier: Modifier = Modifier, onCerrarSesion: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var tiendaId by remember { mutableStateOf<String?>(null) }
    var nombreTienda by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        db.collection("tiendas")
            .whereEqualTo("duenoId", uid)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    tiendaId = doc.id
                    nombreTienda = doc.getString("nombre") ?: ""
                }
                cargando = false
            }
            .addOnFailureListener { cargando = false }
    }

    if (cargando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KoskingRed)
        }
        return
    }

    if (tiendaId == null) {
        PantallaCrearLocal(
            modifier = modifier,
            onLocalCreado = { id, nombre ->
                tiendaId = id
                nombreTienda = nombre
            },
            onCerrarSesion = onCerrarSesion
        )
    } else {
        PantallaStock(
            modifier = modifier,
            tiendaId = tiendaId!!,
            nombreTienda = nombreTienda,
            onCerrarSesion = onCerrarSesion
        )
    }
}

@Composable
fun PantallaCrearLocal(
    modifier: Modifier = Modifier,
    onLocalCreado: (String, String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var nombreLocal by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏪", fontSize = 64.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Creá tu local", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = KoskingRed)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¿Cómo se llama tu kiosco?",
            fontSize = 16.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nombreLocal,
            onValueChange = { nombreLocal = it },
            label = { Text("Nombre del local") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoskingRed,
                focusedLabelColor = KoskingRed,
                cursorColor = KoskingRed
            )
        )

        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = Color.Red, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nombreLocal.isBlank()) {
                    error = "Ingresá el nombre del local"
                    return@Button
                }
                cargando = true
                val codigo = (100000..999999).random().toString()
                val tienda = hashMapOf(
                    "nombre" to nombreLocal.trim(),
                    "duenoId" to uid,
                    "codigoInvitacion" to codigo,
                    "creadoEn" to Timestamp.now()
                )
                val ref = db.collection("tiendas").document()
                ref.set(tienda)
                    .addOnSuccessListener { onLocalCreado(ref.id, nombreLocal.trim()) }
                    .addOnFailureListener {
                        cargando = false
                        error = "Error al crear el local"
                    }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KoskingRed),
            enabled = !cargando
        ) {
            if (cargando) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            else Text("Crear local", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { FirebaseAuth.getInstance().signOut(); onCerrarSesion() }) {
            Text("Cerrar sesión", color = Color(0xFF888888))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaStock(
    modifier: Modifier = Modifier,
    tiendaId: String,
    nombreTienda: String,
    onCerrarSesion: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val stockItems = remember { mutableStateListOf<ItemStock>() }
    var mostrarDialogo by remember { mutableStateOf(false) }

    LaunchedEffect(tiendaId) {
        db.collection("tiendas").document(tiendaId)
            .collection("stock")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    stockItems.clear()
                    for (doc in snapshot.documents) {
                        stockItems.add(
                            ItemStock(
                                productoId = doc.id,
                                nombre = doc.getString("nombre") ?: "",
                                marca = doc.getString("marca") ?: "",
                                precio = doc.getDouble("precio") ?: 0.0,
                                cantidad = (doc.getLong("cantidad") ?: 0).toInt()
                            )
                        )
                    }
                }
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(nombreTienda, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${stockItems.size} productos", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    TextButton(onClick = { FirebaseAuth.getInstance().signOut(); onCerrarSesion() }) {
                        Text("Salir", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KoskingRed)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }, containerColor = KoskingRed) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto", tint = Color.White)
            }
        }
    ) { innerPadding ->
        if (stockItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Todavía no tenés productos", fontSize = 18.sp, color = Color(0xFF888888), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tocá el + para agregar uno", fontSize = 14.sp, color = Color(0xFFAAAAAA), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stockItems) { item ->
                    ItemStockCard(
                        item = item,
                        onEliminar = {
                            db.collection("tiendas").document(tiendaId)
                                .collection("stock").document(item.productoId)
                                .delete()
                        }
                    )
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogAgregarProducto(
            onDismiss = { mostrarDialogo = false },
            onAgregar = { productoId, nombre, marca, precio, cantidad ->
                val stockDoc = hashMapOf(
                    "nombre" to nombre,
                    "marca" to marca,
                    "precio" to precio,
                    "cantidad" to cantidad
                )
                db.collection("tiendas").document(tiendaId)
                    .collection("stock").document(productoId)
                    .set(stockDoc)
                mostrarDialogo = false
            }
        )
    }
}

@Composable
fun ItemStockCard(item: ItemStock, onEliminar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF222222))
                if (item.marca.isNotBlank()) {
                    Text(item.marca, fontSize = 13.sp, color = Color(0xFF888888))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$${String.format("%.2f", item.precio)}",
                    fontSize = 14.sp,
                    color = KoskingRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${item.cantidad}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.cantidad > 5) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
                Text("unid.", fontSize = 11.sp, color = Color(0xFF888888))
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFAAAAAA))
            }
        }
    }
}

@Composable
fun DialogAgregarProducto(
    onDismiss: () -> Unit,
    onAgregar: (String, String, String, Double, Int) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var busqueda by remember { mutableStateOf("") }
    var resultados = remember { mutableStateListOf<ProductoCatalogo>() }
    var productoSeleccionado by remember { mutableStateOf<ProductoCatalogo?>(null) }
    var precio by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var marcaNueva by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    LaunchedEffect(busqueda) {
        if (busqueda.length < 2) {
            resultados.clear()
            return@LaunchedEffect
        }
        buscando = true
        val busquedaLower = busqueda.lowercase().trim()
        db.collection("productos")
            .orderBy("nombreBusqueda")
            .startAt(busquedaLower)
            .endAt(busquedaLower + "")
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                resultados.clear()
                for (doc in snapshot.documents) {
                    resultados.add(
                        ProductoCatalogo(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            marca = doc.getString("marca") ?: "",
                            categoria = doc.getString("categoria") ?: ""
                        )
                    )
                }
                buscando = false
            }
            .addOnFailureListener { buscando = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar producto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (productoSeleccionado == null) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it; error = "" },
                        label = { Text("Buscar producto") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KoskingRed,
                            focusedLabelColor = KoskingRed,
                            cursorColor = KoskingRed
                        )
                    )

                    if (buscando) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = KoskingRed, strokeWidth = 2.dp)
                        }
                    }

                    if (resultados.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column {
                                resultados.forEachIndexed { index, prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { productoSeleccionado = prod }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            if (prod.marca.isNotBlank())
                                                Text(prod.marca, fontSize = 12.sp, color = Color(0xFF888888))
                                        }
                                    }
                                    if (index < resultados.lastIndex) Divider()
                                }
                            }
                        }
                    }

                    if (busqueda.length >= 2 && resultados.isEmpty() && !buscando) {
                        Text(
                            "No encontrado — se creará \"$busqueda\" en el catálogo",
                            fontSize = 12.sp,
                            color = Color(0xFF888888)
                        )
                        OutlinedTextField(
                            value = marcaNueva,
                            onValueChange = { marcaNueva = it },
                            label = { Text("Marca (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KoskingRed,
                                focusedLabelColor = KoskingRed,
                                cursorColor = KoskingRed
                            )
                        )
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(productoSeleccionado!!.nombre, fontWeight = FontWeight.SemiBold)
                                if (productoSeleccionado!!.marca.isNotBlank())
                                    Text(productoSeleccionado!!.marca, fontSize = 12.sp, color = Color(0xFF888888))
                            }
                            TextButton(onClick = { productoSeleccionado = null; busqueda = "" }) {
                                Text("Cambiar", color = KoskingRed, fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KoskingRed,
                        focusedLabelColor = KoskingRed,
                        cursorColor = KoskingRed
                    )
                )

                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad en stock") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KoskingRed,
                        focusedLabelColor = KoskingRed,
                        cursorColor = KoskingRed
                    )
                )

                if (error.isNotEmpty()) {
                    Text(text = error, color = Color.Red, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (guardando) return@Button
                    val precioNum = precio.toDoubleOrNull()
                    val cantidadNum = cantidad.toIntOrNull()
                    when {
                        precioNum == null || precioNum < 0 -> error = "Precio inválido"
                        cantidadNum == null || cantidadNum < 0 -> error = "Cantidad inválida"
                        productoSeleccionado != null -> {
                            guardando = true
                            onAgregar(
                                productoSeleccionado!!.id,
                                productoSeleccionado!!.nombre,
                                productoSeleccionado!!.marca,
                                precioNum,
                                cantidadNum
                            )
                        }
                        busqueda.length >= 2 -> {
                            guardando = true
                            val nuevoProd = hashMapOf(
                                "nombre" to busqueda.trim(),
                                "nombreBusqueda" to busqueda.trim().lowercase(),
                                "marca" to marcaNueva.trim(),
                                "categoria" to ""
                            )
                            val ref = db.collection("productos").document()
                            ref.set(nuevoProd)
                                .addOnSuccessListener {
                                    onAgregar(ref.id, busqueda.trim(), marcaNueva.trim(), precioNum, cantidadNum)
                                }
                                .addOnFailureListener {
                                    guardando = false
                                    error = "Error al guardar, intentá de nuevo"
                                }
                        }
                        else -> error = "Buscá o escribí un producto"
                    }
                },
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(containerColor = KoskingRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (guardando) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                else Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF888888))
            }
        }
    )
}
