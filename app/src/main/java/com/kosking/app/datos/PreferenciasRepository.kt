package com.kosking.app.datos

import android.content.Context
import java.security.MessageDigest

object PreferenciasRepository {

    private const val PREFS = "kosking_prefs"
    private const val KEY_ONBOARDING_VISTO = "onboarding_visto"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isOnboardingVisto(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_VISTO, false)

    fun marcarOnboardingVisto(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_VISTO, true).apply()
    }

    fun getNotifDeuda(context: Context) = prefs(context).getBoolean("notif_deuda", true)
    fun setNotifDeuda(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("notif_deuda", value).apply()

    fun getNotifAcuerdos(context: Context) = prefs(context).getBoolean("notif_acuerdos", true)
    fun setNotifAcuerdos(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("notif_acuerdos", value).apply()

    fun getLimiteDefault(context: Context) = prefs(context).getFloat("limite_default", 0f).toDouble()
    fun setLimiteDefault(context: Context, value: Double) =
        prefs(context).edit().putFloat("limite_default", value.toFloat()).apply()

    fun getEmpleadosVerDeuda(context: Context) = prefs(context).getBoolean("empleados_ver_deuda", true)
    fun setEmpleadosVerDeuda(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("empleados_ver_deuda", value).apply()

    fun getTema(context: Context): String = prefs(context).getString("tema", "sistema") ?: "sistema"
    fun setTema(context: Context, value: String) =
        prefs(context).edit().putString("tema", value).apply()

    // ── PIN de seguridad ───────────────────────────────────────────────────────
    fun isPinHabilitado(context: Context): Boolean = prefs(context).contains("pin_hash")

    fun setPinHash(context: Context, pin: String) {
        prefs(context).edit().putString("pin_hash", hashPin(pin)).apply()
    }

    fun verificarPin(context: Context, pin: String): Boolean {
        val guardado = prefs(context).getString("pin_hash", null) ?: return false
        return guardado == hashPin(pin)
    }

    fun quitarPin(context: Context) {
        prefs(context).edit().remove("pin_hash").remove("biometrico_habilitado").apply()
    }

    fun isBiometricoHabilitado(context: Context): Boolean =
        prefs(context).getBoolean("biometrico_habilitado", false)

    fun setBiometricoHabilitado(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean("biometrico_habilitado", value).apply()

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
