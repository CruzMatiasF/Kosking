package com.kosking.app.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kosking.app.autenticacion.AuthViewModel
import com.kosking.app.autenticacion.PantallaLogin
import com.kosking.app.autenticacion.PantallaRegistro
import com.kosking.app.busqueda.PantallaResultados
import com.kosking.app.detalle.PantallaDetalleKiosco
import com.kosking.app.detalle.PantallaDetalleProducto
import com.kosking.app.dueno.PantallaAgregarProducto
import com.kosking.app.dueno.PantallaConfigLocal
import com.kosking.app.dueno.PantallaCrearLocal
import com.kosking.app.dueno.PantallaInventario
import com.kosking.app.dueno.PantallaMisOfertas
import com.kosking.app.dueno.PantallaPanelDueno
import com.kosking.app.dueno.PantallaRegistrarVenta
import com.kosking.app.empleado.PantallaIngresarCodigo
import com.kosking.app.empleado.PantallaPanelEmpleado
import com.kosking.app.home.PantallaElegirRol
import com.kosking.app.home.PantallaHome
import com.kosking.app.home.PantallaMas

object Rutas {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val LOGIN = "login"
    const val REGISTRO = "registro"

    // Menú inferior
    const val MAPA = "mapa"
    const val FAVORITOS = "favoritos"
    const val PERFIL = "perfil"

    // Cliente
    const val RESULTADOS = "resultados/{query}"
    const val DETALLE_PRODUCTO = "producto/{productoId}"
    const val DETALLE_LOCAL = "local/{tiendaId}"
    const val DETALLE_OFERTA = "oferta/{tiendaId}/{ofertaId}"

    // Dueño
    const val PANEL_DUENO = "panel_dueno"
    const val INVENTARIO = "inventario"
    const val AGREGAR_PRODUCTO = "agregar_producto"
    const val MIS_OFERTAS = "mis_ofertas"
    const val CREAR_OFERTA = "crear_oferta"
    const val CONFIG_LOCAL = "config_local"
    const val CREAR_LOCAL = "crear_local"

    // Empleado
    const val PANEL_EMPLEADO = "panel_empleado"
    const val REGISTRAR_VENTA = "registrar_venta"
    const val INGRESAR_CODIGO = "ingresar_codigo"
    const val ELEGIR_ROL = "elegir_rol"
    const val MAS = "mas"

    // Helpers para navegar con argumentos
    fun resultados(query: String) = "resultados/$query"
    fun detalleProducto(id: String) = "producto/$id"
    fun detalleLocal(id: String) = "local/$id"
    fun detalleOferta(tiendaId: String, ofertaId: String) = "oferta/$tiendaId/$ofertaId"
}

@Composable
fun KoskingNavHost(
    navController: NavHostController = rememberNavController(),
    inicio: String = Rutas.HOME,
) {
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = inicio) {
        composable(Rutas.SPLASH) {
            // PantallaSplash(navController)
        }
        composable(Rutas.HOME) {
            PantallaHome(navController = navController)
        }
        composable(Rutas.LOGIN) {
            PantallaLogin(navController = navController, vm = authViewModel)
        }
        composable(Rutas.REGISTRO) {
            PantallaRegistro(navController = navController, vm = authViewModel)
        }
        composable(Rutas.MAPA) {
            // PantallaMapa(navController)
        }
        composable(Rutas.FAVORITOS) {
            // PantallaFavoritos(navController)
        }
        composable(Rutas.PERFIL) {
            // PantallaPerfil(navController)
        }
        composable(Rutas.RESULTADOS) { backStack ->
            val query = backStack.arguments?.getString("query") ?: ""
            PantallaResultados(queryInicial = query, navController = navController)
        }
        composable(Rutas.DETALLE_PRODUCTO) { backStack ->
            val productoId = backStack.arguments?.getString("productoId") ?: ""
            PantallaDetalleProducto(productoId = productoId, navController = navController)
        }
        composable(Rutas.DETALLE_LOCAL) { backStack ->
            val tiendaId = backStack.arguments?.getString("tiendaId") ?: ""
            PantallaDetalleKiosco(tiendaId = tiendaId, navController = navController)
        }
        composable(Rutas.DETALLE_OFERTA) { backStack ->
            val tiendaId = backStack.arguments?.getString("tiendaId") ?: ""
            val ofertaId = backStack.arguments?.getString("ofertaId") ?: ""
            // PantallaDetalleOferta(tiendaId, ofertaId, navController)
        }
        composable(Rutas.PANEL_DUENO) {
            PantallaPanelDueno(navController = navController)
        }
        composable(Rutas.INVENTARIO) {
            PantallaInventario(navController = navController)
        }
        composable(Rutas.AGREGAR_PRODUCTO) {
            PantallaAgregarProducto(navController = navController)
        }
        composable(Rutas.MIS_OFERTAS) {
            PantallaMisOfertas(navController = navController)
        }
        composable(Rutas.CREAR_OFERTA) {
            // PantallaCrearOferta(navController)
        }
        composable(Rutas.CONFIG_LOCAL) {
            PantallaConfigLocal(navController = navController)
        }
        composable(Rutas.CREAR_LOCAL) {
            PantallaCrearLocal(navController = navController)
        }
        composable(Rutas.PANEL_EMPLEADO) {
            PantallaPanelEmpleado(navController = navController)
        }
        composable(Rutas.REGISTRAR_VENTA) {
            PantallaRegistrarVenta(navController = navController)
        }
        composable(Rutas.INGRESAR_CODIGO) {
            PantallaIngresarCodigo(navController = navController)
        }
        composable(Rutas.ELEGIR_ROL) {
            PantallaElegirRol(navController = navController)
        }
        composable(Rutas.MAS) {
            PantallaMas(navController = navController)
        }
    }
}
