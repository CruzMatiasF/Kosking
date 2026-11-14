package com.kosking.app.datos

import android.content.Context
import java.security.MessageDigest

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

    fun getTema(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tema", "sistema") ?: "sistema"
    fun setTema(context: Context, value: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("tema", value).apply()

    // ── PIN de seguridad ───────────────────────────────────────────────────────
    fun isPinHabilitado(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("pin_hash")

    fun setPinHash(context: Context, pin: String) {
        val hash = hashPin(pin)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("pin_hash", hash).apply()
    }

    fun verificarPin(context: Context, pin: String): Boolean {
        val guardado = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("pin_hash", null)
            ?: return false
        return guardado == hashPin(pin)
    }

    fun quitarPin(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("pin_hash").remove("biometrico_habilitado").apply()
    }

    fun isBiometricoHabilitado(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("biometrico_habilitado", false)

    fun setBiometricoHabilitado(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("biometrico_habilitado", value).apply()

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
