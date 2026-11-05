package com.kosking.app.explorar

import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DIAS = listOf("domingo", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado")
private val fmtFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun fechaHoy(): String = fmtFecha.format(Calendar.getInstance().time)

fun diaSemanaActual(): String {
    val cal = Calendar.getInstance()
    return DIAS[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

// Devuelve "cerrado", "abierto", o "" si no hay excepción para hoy
fun excepcionHoy(local: Local): String {
    if (local.excepcionFecha != fechaHoy()) return ""
    return local.excepcionTipo
}

// Convierte "HH:mm" a minutos desde medianoche
private fun horaAMinutos(hora: String): Int {
    val partes = hora.split(":")
    if (partes.size != 2) return -1
    return (partes[0].toIntOrNull() ?: return -1) * 60 + (partes[1].toIntOrNull() ?: return -1)
}

// Devuelve la hora de cierre efectiva de hoy (excepcionCierre si está activa, sino la del horario)
fun cierreEfectivoHoy(local: Local): String? {
    val dia = diaSemanaActual()
    val horario = local.horarios[dia]?.takeIf { it.tieneCarga } ?: return null
    val excepcion = excepcionHoy(local)
    // excepcionCierre aplica solo si no hay excepción de día completo
    return if (excepcion.isEmpty() && local.excepcionFecha == fechaHoy() && local.excepcionCierre.isNotEmpty())
        local.excepcionCierre
    else
        horario.cierre
}

fun estaAbiertoAhora(local: Local): Boolean {
    val excepcion = excepcionHoy(local)
    if (excepcion == "cerrado") return false
    if (excepcion == "abierto") return true

    if (local.horarios.isEmpty()) return local.abierto

    val dia = diaSemanaActual()
    val horario = local.horarios[dia] ?: return false
    if (!horario.tieneCarga) return false

    val cal = Calendar.getInstance()
    val minutosAhora = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val apertura = horaAMinutos(horario.apertura)
    // Usa cierre personalizado de hoy si existe
    val cierreStr = if (local.excepcionFecha == fechaHoy() && local.excepcionCierre.isNotEmpty())
        local.excepcionCierre else horario.cierre
    val cierre = horaAMinutos(cierreStr)
    if (apertura < 0 || cierre < 0) return false

    return if (cierre > apertura) minutosAhora in apertura until cierre
           else minutosAhora >= apertura || minutosAhora < cierre
}

// "Abre a las 9:00" / "Cierra a las 20:00" / "Cerrado hoy" / ""
fun textoHorarioHoy(local: Local): String {
    val excepcion = excepcionHoy(local)
    if (excepcion == "cerrado") return "Cerrado hoy por excepción"
    if (excepcion == "abierto") return "Abierto hoy por excepción"

    if (local.horarios.isEmpty()) return ""
    val dia = diaSemanaActual()
    val horario = local.horarios[dia] ?: return "Cerrado hoy"
    if (!horario.tieneCarga) return "Cerrado hoy"

    val cal = Calendar.getInstance()
    val minutosAhora = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val apertura = horaAMinutos(horario.apertura)
    val cierreStr = if (local.excepcionFecha == fechaHoy() && local.excepcionCierre.isNotEmpty())
        local.excepcionCierre else horario.cierre
    val cierre = horaAMinutos(cierreStr)
    if (apertura < 0 || cierre < 0) return ""

    val abierto = if (cierre > apertura) minutosAhora in apertura until cierre
                  else minutosAhora >= apertura || minutosAhora < cierre

    val sufijo = if (local.excepcionFecha == fechaHoy() && local.excepcionCierre.isNotEmpty()) " (hoy)" else ""
    return if (abierto) "Cierra a las $cierreStr$sufijo"
           else "Abre a las ${horario.apertura}"
}

fun textoHorarioSemana(horarios: Map<String, HorarioDia>): String {
    if (horarios.isEmpty()) return ""
    return DIAS.mapNotNull { dia ->
        val h = horarios[dia] ?: return@mapNotNull null
        if (!h.tieneCarga) return@mapNotNull null
        val nombre = dia.replaceFirstChar { it.uppercase(Locale.getDefault()) }
        "$nombre ${h.apertura}–${h.cierre}"
    }.joinToString("\n")
}
