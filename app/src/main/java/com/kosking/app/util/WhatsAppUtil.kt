package com.kosking.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale

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


    fun historialCliente(
        nombreCliente: String,
        nombreLocal: String,
        deudaTotal: Double,
        movimientos: List<Movimiento>
    ): String {
        val fmtFecha = SimpleDateFormat("d MMM", Locale.forLanguageTag("es-AR"))
        val activos = movimientos.filter { !it.anulado }.sortedByDescending { it.fecha.toDate().time }
        val sb = StringBuilder()
        sb.append("📋 *Lo que te llevaste en $nombreLocal*\n")
        sb.append("─────────────────\n")

        // Agrupar por visita (grupoId o fecha si no tiene)
        val fmtKey = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val visitas = activos
            .filter { it.tipo == "cargo" }
            .groupBy { it.grupoId.ifBlank { fmtKey.format(it.fecha.toDate()) } }
            .entries
            .sortedByDescending { (_, movs) -> movs.first().fecha.toDate().time }

        if (visitas.isEmpty()) {
            sb.append("Sin compras registradas.\n")
        } else {
            visitas.forEach { (_, movs) ->
                val fecha = fmtFecha.format(movs.first().fecha.toDate())
                val itemsTexto = movs
                    .filter { it.producto.isNotBlank() }
                    .groupBy { it.producto }
                    .map { (prod, ms) ->
                        val total = ms.sumOf { it.cantidad }
                        if (total > 1) "${total}× $prod" else prod
                    }
                    .joinToString(", ")
                if (itemsTexto.isNotBlank()) sb.append("$fecha — $itemsTexto\n")
            }
        }

        sb.append("─────────────────\n")
        sb.append("*Deuda total: \$${"%.0f".format(deudaTotal)}*\n")
        sb.append("📱 Kosking")
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
        context.startActivity(intent)
    }

    private fun limpiarTelefono(telefono: String): String {
        val limpio = telefono.trim()
        // Si ya tiene + al principio, solo quitar el +
        if (limpio.startsWith("+")) return limpio.drop(1).filter { it.isDigit() }
        // Si empieza con 00 (formato internacional alternativo)
        if (limpio.startsWith("00")) return limpio.drop(2).filter { it.isDigit() }
        val soloDigitos = limpio.filter { it.isDigit() }
        return when {
            // Ya tiene código de país conocido
            soloDigitos.startsWith("549")  -> soloDigitos               // Argentina móvil
            soloDigitos.startsWith("5411") -> soloDigitos               // Argentina fijo BsAs
            soloDigitos.startsWith("54")   -> "549${soloDigitos.drop(2)}" // Argentina sin 9
            soloDigitos.startsWith("598")  -> soloDigitos               // Uruguay
            soloDigitos.startsWith("56")   -> soloDigitos               // Chile
            soloDigitos.startsWith("595")  -> soloDigitos               // Paraguay
            soloDigitos.startsWith("591")  -> soloDigitos               // Bolivia
            soloDigitos.startsWith("51")   -> soloDigitos               // Perú
            soloDigitos.startsWith("57")   -> soloDigitos               // Colombia
            soloDigitos.startsWith("58")   -> soloDigitos               // Venezuela
            // Formato local Argentina (sin código de país)
            soloDigitos.startsWith("0")    -> "549${soloDigitos.drop(1)}"
            soloDigitos.startsWith("15")   -> "549${soloDigitos.drop(2)}"
            soloDigitos.length == 10       -> "549$soloDigitos"
            // Fallback: asumir Argentina
            else                           -> "549$soloDigitos"
        }
    }
}
