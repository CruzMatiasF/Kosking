package com.kosking.app.datos

import android.content.Context

object PreferenciasRepository {

    private const val PREFS = "kosking_prefs"
    private const val KEY_PANTALLA_INICIO = "pantalla_inicio"

    const val PANTALLA_EXPLORAR = "explorar"
    const val PANTALLA_EMPLEADO = "panel_empleado"
    const val PANTALLA_DUENO = "panel_dueno"
    const val PANTALLA_FIADO = "ver_fiado"
    const val PANTALLA_PRODUCTOS = "productos"

    fun getPantallaInicio(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PANTALLA_INICIO, PANTALLA_EXPLORAR) ?: PANTALLA_EXPLORAR

    fun setPantallaInicio(context: Context, pantalla: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PANTALLA_INICIO, pantalla).apply()
    }
}
