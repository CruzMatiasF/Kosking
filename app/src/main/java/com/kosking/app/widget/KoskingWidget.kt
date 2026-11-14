package com.kosking.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.kosking.app.MainActivity
import com.kosking.app.R

class KoskingWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            actualizar(context, appWidgetManager, id)
        }
    }

    companion object {
        fun actualizarTodos(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KoskingWidget::class.java))
            if (ids.isNotEmpty()) {
                val widget = KoskingWidget()
                widget.onUpdate(context, manager, ids)
            }
        }

        private fun actualizar(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("kosking_prefs", Context.MODE_PRIVATE)
            val totalDeuda = prefs.getFloat("total_deuda", 0f).toDouble()
            val clientesConDeuda = prefs.getInt("clientes_con_deuda", 0)
            val nombreLocal = prefs.getString("nombre_local", "") ?: ""
            val tieneSesion = nombreLocal.isNotBlank()

            val views = RemoteViews(context.packageName, R.layout.widget_kosking)

            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
                PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.background.let { R.layout.widget_kosking }, pendingIntent)

            if (tieneSesion) {
                views.setViewVisibility(R.id.widget_local_nombre, View.VISIBLE)
                views.setViewVisibility(R.id.widget_deuda_total, View.VISIBLE)
                views.setViewVisibility(R.id.widget_clientes_deuda, View.VISIBLE)
                views.setViewVisibility(R.id.widget_sin_sesion, View.GONE)

                views.setTextViewText(R.id.widget_local_nombre, nombreLocal)
                views.setTextViewText(R.id.widget_deuda_total, "\$${"%,.0f".format(totalDeuda)}")
                val texto = "$clientesConDeuda cliente${if (clientesConDeuda != 1) "s" else ""} deben"
                views.setTextViewText(R.id.widget_clientes_deuda, texto)
            } else {
                views.setViewVisibility(R.id.widget_local_nombre, View.GONE)
                views.setViewVisibility(R.id.widget_deuda_total, View.GONE)
                views.setViewVisibility(R.id.widget_clientes_deuda, View.GONE)
                views.setViewVisibility(R.id.widget_sin_sesion, View.VISIBLE)
            }

            views.setOnClickPendingIntent(R.id.widget_deuda_total, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_sin_sesion, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
