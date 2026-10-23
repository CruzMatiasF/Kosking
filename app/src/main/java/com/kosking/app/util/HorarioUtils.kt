package com.kosking.app.util

import com.kosking.app.modelo.Tienda
import java.util.Calendar

fun estaAbierto(tienda: Tienda): Boolean {
    // Override activo y no expirado tiene prioridad
    val override = tienda.overrideAbierto
    if (override != null) {
        val hasta = tienda.overrideHasta?.toDate()?.time ?: 0L
        if (System.currentTimeMillis() < hasta) return override
    }

    // Sin horario → cerrado
    if (tienda.horarioApertura.isBlank() || tienda.horarioCierre.isBlank()) return false

    val cal = Calendar.getInstance()

    // Día de la semana (Calendar: 1=Dom, 2=Lun, ..., 7=Sáb)
    val diaOk = when (tienda.diasActivos) {
        "Lunes a Viernes" -> cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
        "Lunes a Sábado"  -> cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.SATURDAY
        else              -> true // "Todos los días"
    }
    if (!diaOk) return false

    val ahora   = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val apertura = tienda.horarioApertura.aMinutos()
    val cierre   = tienda.horarioCierre.aMinutos()
    if (apertura < 0 || cierre < 0) return false

    return ahora in apertura until cierre
}

fun textoHorario(tienda: Tienda): String {
    if (tienda.horarioApertura.isBlank()) return "Sin horario configurado"

    val override = tienda.overrideAbierto
    if (override != null) {
        val hasta = tienda.overrideHasta?.toDate()?.time ?: 0L
        if (System.currentTimeMillis() < hasta) {
            return if (override) "Abierto · excepción hoy" else "Cerró antes hoy"
        }
    }

    val dias = when (tienda.diasActivos) {
        "Lunes a Viernes" -> "Lun–Vie"
        "Lunes a Sábado"  -> "Lun–Sáb"
        else              -> "Todos los días"
    }
    return "$dias · ${tienda.horarioApertura}–${tienda.horarioCierre}"
}

private fun String.aMinutos(): Int {
    val partes = split(":")
    if (partes.size != 2) return -1
    val h = partes[0].toIntOrNull() ?: return -1
    val m = partes[1].toIntOrNull() ?: return -1
    return h * 60 + m
}
