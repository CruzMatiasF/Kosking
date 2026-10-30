package com.kosking.app.explorar

import com.kosking.app.modelo.HorarioDia
import com.kosking.app.modelo.Local
import java.util.Calendar
import java.util.Locale

private val DIAS = listOf("domingo", "lunes", "martes", "miercoles", "jueves", "viernes", "sabado")

fun diaSemanaActual(): String {
    val cal = Calendar.getInstance()
    return DIAS[cal.get(Calendar.DAY_OF_WEEK) - 1]
}

// Convierte "HH:mm" a minutos desde medianoche
private fun horaAMinutos(hora: String): Int {
    val partes = hora.split(":")
    if (partes.size != 2) return -1
    return (partes[0].toIntOrNull() ?: return -1) * 60 + (partes[1].toIntOrNull() ?: return -1)
}

fun estaAbiertoAhora(local: Local): Boolean {
    // Si no tiene horarios cargados, usa el toggle manual
    if (local.horarios.isEmpty()) return local.abierto

    val dia = diaSemanaActual()
    val horario = local.horarios[dia] ?: return false
    if (!horario.tieneCarga) return false

    val cal = Calendar.getInstance()
    val minutosAhora = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val apertura = horaAMinutos(horario.apertura)
    val cierre = horaAMinutos(horario.cierre)
    if (apertura < 0 || cierre < 0) return false

    return if (cierre > apertura) {
        minutosAhora in apertura until cierre
    } else {
        // cruza medianoche (ej: 22:00-02:00)
        minutosAhora >= apertura || minutosAhora < cierre
    }
}

// "Abre a las 9:00" / "Cierra a las 20:00" / "Cerrado hoy" / ""
fun textoHorarioHoy(local: Local): String {
    if (local.horarios.isEmpty()) return ""
    val dia = diaSemanaActual()
    val horario = local.horarios[dia] ?: return "Cerrado hoy"
    if (!horario.tieneCarga) return "Cerrado hoy"

    val cal = Calendar.getInstance()
    val minutosAhora = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val apertura = horaAMinutos(horario.apertura)
    val cierre = horaAMinutos(horario.cierre)
    if (apertura < 0 || cierre < 0) return ""

    val abierto = if (cierre > apertura) minutosAhora in apertura until cierre
                  else minutosAhora >= apertura || minutosAhora < cierre

    return if (abierto) "Cierra a las ${horario.cierre}"
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
