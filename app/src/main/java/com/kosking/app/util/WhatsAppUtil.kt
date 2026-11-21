package com.kosking.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppUtil {

    fun ticketVisita(
        nombreCliente: String,
        nombreLocal: String,
        items: List<Triple<String, Double, Int>>,
        tipo: String,
        hora: String
    ): String {
        val sb = StringBuilder()
        if (tipo == "pago") {
            val montoFmt = "%.0f".format(items.sumOf { it.second })
            sb.append("✅ Hola $nombreCliente, registramos tu pago de *\$$montoFmt* en *$nombreLocal*.")
        } else {
            sb.append("🧾 *$nombreLocal*\n")
            sb.append("────────────────────\n")
            items.filter { it.first.isNotBlank() }.forEach { (concepto, _, cantidad) ->
                val linea = if (cantidad > 1) "${cantidad}× $concepto" else concepto
                sb.append("$linea\n")
            }
            sb.append("────────────────────\n")
            sb.append("📋 Confirmá que esto es lo que te llevaste.")
        }
        sb.append("\n\n🕐 $hora · 📱 Kosking")
        return sb.toString()
    }

    fun mensajeResumen(
        nombreCliente: String,
        nombreLocal: String,
        deudaTotal: Double
    ): String {
        val totalFmt = "%.0f".format(deudaTotal)
        return "Hola $nombreCliente, tu deuda actual en $nombreLocal es \$$totalFmt. " +
               "Cualquier consulta avisanos. 📱 Kosking"
    }


    fun mensajeResumenDia(
        nombreLocal: String,
        fiadoHoy: Double,
        cobradoHoy: Double,
        cantFiados: Int,
        cantPagos: Int
    ): String {
        val sb = StringBuilder()
        sb.append("📊 Resumen de hoy — $nombreLocal\n\n")
        if (fiadoHoy > 0) sb.append("🔴 Debe: \$${"%.0f".format(fiadoHoy)} ($cantFiados operación${if (cantFiados != 1) "es" else ""})\n")
        if (cobradoHoy > 0) sb.append("✅ Cobrado: \$${"%.0f".format(cobradoHoy)} ($cantPagos pago${if (cantPagos != 1) "s" else ""})\n")
        val saldo = cobradoHoy - fiadoHoy
        sb.append("\nSaldo neto del día: ${if (saldo >= 0) "+" else ""}\$${"%.0f".format(saldo)}")
        return sb.toString()
    }

    fun mensajeResumenPeriodo(
        nombreLocal: String,
        labelPeriodo: String,
        fiadoTotal: Double,
        cobradoTotal: Double,
        cantFiados: Int,
        cantPagos: Int,
        topClientes: List<Pair<String, Double>>
    ): String {
        val saldo = cobradoTotal - fiadoTotal
        val sb = StringBuilder()
        sb.append("📊 Resumen $labelPeriodo — $nombreLocal\n\n")
        if (fiadoTotal > 0) sb.append("🔴 Debe: \$${"%.0f".format(fiadoTotal)} ($cantFiados op.)\n")
        if (cobradoTotal > 0) sb.append("✅ Cobrado: \$${"%.0f".format(cobradoTotal)} ($cantPagos pagos)\n")
        sb.append("Saldo neto: ${if (saldo >= 0) "+" else ""}\$${"%.0f".format(saldo)}\n")
        if (topClientes.isNotEmpty()) {
            sb.append("\nTop clientes:\n")
            topClientes.take(5).forEach { (nombre, monto) ->
                sb.append("• $nombre — \$${"%.0f".format(monto)}\n")
            }
        }
        sb.append("\n📱 Kosking")
        return sb.toString()
    }


    fun compartir(context: Context, texto: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Compartir resumen").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    // Limpia el teléfono y arma el link wa.me (Argentina: prefijo 549)
    fun abrirWhatsApp(context: Context, telefono: String, mensaje: String) {
        val numero = limpiarTelefono(telefono)
        val url = "https://wa.me/$numero?text=${Uri.encode(mensaje)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    private fun limpiarTelefono(telefono: String): String {
        val limpio = telefono.trim()
        if (limpio.startsWith("+")) return limpio.drop(1).filter { it.isDigit() }
        if (limpio.startsWith("00")) return limpio.drop(2).filter { it.isDigit() }
        val d = limpio.filter { it.isDigit() }
        return when {
            d.startsWith("54")  -> if (d.startsWith("549") || d.startsWith("5411")) d else "549${d.drop(2)}"
            d.startsWith("598") || d.startsWith("56") || d.startsWith("595") ||
            d.startsWith("591") || d.startsWith("51") || d.startsWith("57") ||
            d.startsWith("58")  -> d
            d.startsWith("15")  -> "549${d.drop(2)}"
            d.startsWith("0")   -> "549${d.drop(1)}"
            d.length == 10      -> "549$d"
            else                -> "549$d"
        }
    }
}
