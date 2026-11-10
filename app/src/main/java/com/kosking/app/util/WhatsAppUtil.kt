package com.kosking.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

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
        ultimosMovimientos: List<Triple<String, Double, String>> // (descripcion, monto, tipo)
    ): String {
        val totalFmt = "%.0f".format(deudaTotal)
        val sb = StringBuilder()
        sb.append("Hola $nombreCliente 👋\n")
        sb.append("Tu deuda en $nombreLocal es *\$$totalFmt*.\n")
        if (ultimosMovimientos.isNotEmpty()) {
            sb.append("\nÚltimos movimientos:\n")
            ultimosMovimientos.take(5).forEach { (desc, monto, tipo) ->
                val signo = if (tipo == "pago") "✅ −" else "🔴 +"
                val montoFmt = "%.0f".format(monto)
                val concepto = desc.ifBlank { if (tipo == "pago") "Pago" else "Fiado" }
                sb.append("$signo\$$montoFmt $concepto\n")
            }
        }
        sb.append("\nCualquier consulta avisanos. 📱 Kosking")
        return sb.toString()
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
