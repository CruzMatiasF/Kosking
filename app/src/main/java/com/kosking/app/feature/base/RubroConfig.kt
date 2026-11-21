package com.kosking.app.feature.base

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TabRubroId {
    DASHBOARD,
    CLIENTES,
    PRODUCTOS,
    // Futuros — agregar aquí y en tabs() cuando se implementen:
    // TURNOS,
    // SERVICIOS,
    // STOCK,
}

data class TabRubroConfig(
    val id: TabRubroId,
    val titulo: String,
    val icono: ImageVector
)

object RubroConfig {

    fun tabs(tipo: String): List<TabRubroConfig> = when (tipo.lowercase().trim()) {
        // Futuros rubros — descomentar y agregar pantallas cuando se implementen:
        // "peluquería", "peluqueria" -> tabsPeluqueria()
        // "farmacia"                 -> tabsFarmacia()
        else -> tabsBase()
    }

    // Base para kiosco, almacén, y cualquier rubro sin config propia todavía
    fun tabsBase() = listOf(
        TabRubroConfig(TabRubroId.DASHBOARD, "Inicio",    Icons.Default.Home),
        TabRubroConfig(TabRubroId.CLIENTES,  "Clientes",  Icons.Default.People),
        TabRubroConfig(TabRubroId.PRODUCTOS, "Productos", Icons.Default.Sell),
    )

    // Futuro: descomentar cuando se agregue peluquería
    // private fun tabsPeluqueria() = listOf(
    //     TabRubroConfig(TabRubroId.DASHBOARD, "Inicio",    Icons.Default.Home),
    //     TabRubroConfig(TabRubroId.CLIENTES,  "Clientes",  Icons.Default.People),
    //     TabRubroConfig(TabRubroId.TURNOS,    "Turnos",    Icons.Default.CalendarMonth),
    //     TabRubroConfig(TabRubroId.SERVICIOS, "Servicios", Icons.Default.ContentCut),
    // )

    // Futuro: descomentar cuando se agregue farmacia
    // private fun tabsFarmacia() = listOf(
    //     TabRubroConfig(TabRubroId.DASHBOARD, "Inicio",    Icons.Default.Home),
    //     TabRubroConfig(TabRubroId.CLIENTES,  "Clientes",  Icons.Default.People),
    //     TabRubroConfig(TabRubroId.PRODUCTOS, "Productos", Icons.Default.Medication),
    //     TabRubroConfig(TabRubroId.STOCK,     "Stock",     Icons.Default.Inventory),
    // )
}
