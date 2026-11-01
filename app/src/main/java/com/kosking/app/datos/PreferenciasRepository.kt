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

    private const val KEY_ONBOARDING_VISTO = "onboarding_visto"

    fun getPantallaInicio(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PANTALLA_INICIO, PANTALLA_EXPLORAR) ?: PANTALLA_EXPLORAR

    fun setPantallaInicio(context: Context, pantalla: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PANTALLA_INICIO, pantalla).apply()
    }

    fun isOnboardingVisto(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_VISTO, false)

    fun marcarOnboardingVisto(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_VISTO, true).apply()
    }
}
