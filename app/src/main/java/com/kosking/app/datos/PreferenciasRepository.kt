package com.kosking.app.datos

import android.content.Context

object PreferenciasRepository {

    private const val PREFS = "kosking_prefs"
    private const val KEY_ONBOARDING_VISTO = "onboarding_visto"
    private const val KEY_WIZARD_COMPLETADO = "wizard_primer_uso_completado"

    fun isOnboardingVisto(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_VISTO, false)

    fun marcarOnboardingVisto(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_VISTO, true).apply()
    }

    fun isWizardCompletado(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WIZARD_COMPLETADO, false)

    fun marcarWizardCompletado(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_WIZARD_COMPLETADO, true).apply()
    }
}
