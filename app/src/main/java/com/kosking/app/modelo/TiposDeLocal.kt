package com.kosking.app.modelo

data class TipoLocal(val nombre: String, val emoji: String)

val TIPOS_DE_LOCAL = listOf(
    TipoLocal("Kiosco",       "🏪"),
    TipoLocal("Almacén",      "🛒"),
    TipoLocal("Maxikiosco",   "🏬"),
    TipoLocal("Ropa",         "👕"),
    TipoLocal("Corralón",     "🧱"),
    TipoLocal("Repuestos",    "🔧"),
    TipoLocal("Electrónica",  "📱"),
    TipoLocal("Panadería",    "🍞"),
    TipoLocal("Farmacia",     "💊"),
    TipoLocal("Otro",         "📦"),
)
