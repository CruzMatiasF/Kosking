package com.kosking.app.datos

import android.content.Context

object PreferenciasRepository {

    private const val PREFS = "kosking_prefs"
    private const val KEY_ONBOARDING_VISTO = "onboarding_visto"
    fun isOnboardingVisto(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_VISTO, false)

    fun marcarOnboardingVisto(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_VISTO, true).apply()
    }

    fun getNotifDeuda(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("notif_deuda", true)
    fun setNotifDeuda(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("notif_deuda", value).apply()

    fun getNotifAcuerdos(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("notif_acuerdos", true)
    fun setNotifAcuerdos(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("notif_acuerdos", value).apply()

    fun getLimiteDefault(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat("limite_default", 0f).toDouble()
    fun setLimiteDefault(context: Context, value: Double) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat("limite_default", value.toFloat()).apply()

    fun getEmpleadosVerDeuda(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("empleados_ver_deuda", true)
    fun setEmpleadosVerDeuda(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("empleados_ver_deuda", value).apply()
}
