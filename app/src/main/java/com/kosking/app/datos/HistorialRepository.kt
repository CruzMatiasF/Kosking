package com.kosking.app.datos

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

object HistorialRepository {

    private const val PREFS = "kosking_historial"
    private const val KEY = "queries"
    private const val MAX = 20

    private lateinit var ctx: Context

    fun init(context: Context) {
        ctx = context.applicationContext
    }

    fun obtener(): List<String> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { arr.getString(it) }
    }

    fun guardar(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val lista = obtener().toMutableList()
        lista.remove(q)
        lista.add(0, q)
        if (lista.size > MAX) lista.subList(MAX, lista.size).clear()
        val arr = JSONArray(lista)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, arr.toString()) }
    }

    fun eliminar(query: String) {
        val lista = obtener().toMutableList().also { it.remove(query) }
        val arr = JSONArray(lista)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, arr.toString()) }
    }

    fun limpiar() {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, "[]") }
    }
}
