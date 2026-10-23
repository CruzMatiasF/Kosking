package com.kosking.app.favoritos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.kosking.app.modelo.Tienda
import com.kosking.app.navegacion.Rutas
import com.kosking.app.ui.theme.*
import com.kosking.app.util.estaAbierto
import com.kosking.app.util.textoHorario

@Composable
fun PantallaFavoritos(navController: NavController, vm: FavoritosViewModel = viewModel()) {
    if (FirebaseAuth.getInstance().currentUser == null) {
        LaunchedEffect(Unit) { navController.navigate(Rutas.LOGIN) { popUpTo(Rutas.MAPA_SIMPLE) } }
        return
    }
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = AzulFondo,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Blanco)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            "Favoritos",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Blanco,
                        )
                        Text(
                            "${favoritos.size} kioscos guardados",
                            fontSize = 12.sp,
                            color = Blanco.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (favoritos.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        null,
                        tint = Gris500,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Todavía no guardaste ningún kiosco",
                        fontWeight = FontWeight.SemiBold,
                        color = AzulNavy,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tocá el corazón en el detalle de un kiosco\npara guardarlo acá.",
                        fontSize = 13.sp,
                        color = Gris500,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Rutas.HOME) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Explorar kioscos")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(favoritos, key = { it.id }) { tienda ->
                    FavoritoCard(
                        tienda = tienda,
                        onVerClick = { navController.navigate(Rutas.detalleLocal(tienda.id)) },
                        onQuitarClick = { vm.quitar(tienda.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritoCard(tienda: Tienda, onVerClick: () -> Unit, onQuitarClick: () -> Unit) {
    val abierto = estaAbierto(tienda)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Blanco,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ícono
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (abierto) Verde.copy(alpha = 0.1f) else Gris200,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Store,
                        null,
                        tint = if (abierto) Verde else Gris500,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(tienda.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AzulNavy)
                Spacer(Modifier.height(2.dp))
                Text(textoHorario(tienda), fontSize = 11.sp, color = Gris500)
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (abierto) Verde.copy(alpha = 0.12f) else Gris200,
                ) {
                    Text(
                        if (abierto) "Abierto" else "Cerrado",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        color = if (abierto) Verde else Gris500,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // Acciones
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = onVerClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Rojo.copy(alpha = 0.1f), contentColor = Rojo),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Ver", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onQuitarClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Favorite, null, tint = Rojo, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
