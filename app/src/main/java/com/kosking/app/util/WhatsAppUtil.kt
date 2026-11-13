package com.kosking.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kosking.app.modelo.Movimiento
import java.text.SimpleDateFormat
import java.util.Locale

object WhatsAppUtil {

    fun mensajeDeuda(
        nombreCliente: String,
        monto: Double,
        descripcion: String,
        nombreLocal: String,
        deudaTotal: Double,
        tipo: String
    ): String {
        val accion = if (tipo == "pago") "registramos tu pago de" else "te anotamos"
        val montoFmt = "%.0f".format(monto)
        val totalFmt = "%.0f".format(deudaTotal)
        return "Hola $nombreCliente, $accion \$$montoFmt de $descripcion en $nombreLocal. " +
               "Tu deuda total es \$$totalFmt. 📱 Gestionado con Kosking"
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

    fun mensajeResumenDetallado(
        nombreCliente: String,
        nombreLocal: String,
        deudaTotal: Double,
        ultimosMovimientos: List<Movimiento>
    ): String {
        val totalFmt = "%.0f".format(deudaTotal)
        val sb = StringBuilder()
        sb.append("Hola $nombreCliente 👋\n")
        sb.append("Tu deuda en $nombreLocal es *\$$totalFmt*.\n")
        if (ultimosMovimientos.isNotEmpty()) {
            sb.append("\nÚltimos movimientos:\n")
            ultimosMovimientos.take(5).forEach { mov ->
                val signo = if (mov.tipo == "pago") "✅ −" else "🔴 +"
                val montoFmt = "%.0f".format(mov.monto)
                val concepto = mov.producto.ifBlank { if (mov.tipo == "pago") "Pago" else "Cargo" }
                sb.append("$signo\$$montoFmt $concepto\n")
            }
        }
        sb.append("\nCualquier consulta avisanos. 📱 Kosking")
        return sb.toString()
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
        val fmtFecha = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val activos = movimientos.filter { !it.anulado }.sortedByDescending { it.fecha.toDate().time }
        val sb = StringBuilder()
        sb.append("📋 *Historial de $nombreCliente*\n")
        sb.append("Local: $nombreLocal\n")
        sb.append("─────────────────\n")
        activos.forEach { mov ->
            val fecha = fmtFecha.format(mov.fecha.toDate())
            val montoFmt = "%.0f".format(mov.monto)
            if (mov.tipo == "pago") {
                sb.append("✅ $fecha — Abonó \$$montoFmt a cuenta\n")
            } else {
                val concepto = mov.producto.ifBlank { "Cargo" }
                sb.append("🔴 $fecha — \$$montoFmt ($concepto)\n")
            }
        }
        sb.append("─────────────────\n")
        sb.append("*Deuda actual: \$${"%.0f".format(deudaTotal)}*\n")
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
