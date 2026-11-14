package com.kosking.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kosking.app.modelo.Cliente
import com.kosking.app.modelo.Movimiento
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtil {

    private val moneda = NumberFormat.getCurrencyInstance(Locale("es", "AR")).apply {
        maximumFractionDigits = 0
    }
    private val fmtFechaHora = SimpleDateFormat("dd/MM HH:mm", Locale("es", "AR"))
    private val fmtFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))
    private val fmtHora = SimpleDateFormat("HH:mm", Locale("es", "AR"))

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_H = 18f

    // ── Pinceles ──────────────────────────────────────────────────────────────

    private fun paintTitulo() = Paint().apply {
        textSize = 18f; isFakeBoldText = true; color = Color.BLACK
    }
    private fun paintSubtitulo() = Paint().apply {
        textSize = 13f; isFakeBoldText = true; color = Color.BLACK
    }
    private fun paintNormal() = Paint().apply {
        textSize = 11f; color = Color.BLACK
    }
    private fun paintGris() = Paint().apply {
        textSize = 10f; color = Color.GRAY
    }
    private fun paintMonto(cargo: Boolean) = Paint().apply {
        textSize = 11f; isFakeBoldText = true
        color = if (cargo) Color.rgb(180, 30, 30) else Color.rgb(30, 130, 30)
    }
    private fun paintLinea() = Paint().apply {
        color = Color.LTGRAY; strokeWidth = 1f
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Canvas.drawTextRight(text: String, x: Float, y: Float, paint: Paint) {
        val w = paint.measureText(text)
        drawText(text, x - w, y, paint)
    }

    private fun nuevaPagina(doc: PdfDocument, num: Int): Pair<PdfDocument.Page, Canvas> {
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, num).create()
        val page = doc.startPage(info)
        return page to page.canvas
    }

    data class Pagina(
        val doc: PdfDocument,
        var page: PdfDocument.Page,
        var canvas: Canvas,
        var y: Float,
        var num: Int
    ) {
        fun avanzar(delta: Float = LINE_H): Float { y += delta; return y }

        fun saltoPaginaSiNecesario(espacio: Float = 60f) {
            if (y + espacio > PAGE_HEIGHT - MARGIN) {
                doc.finishPage(page)
                num++
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, num).create()
                page = doc.startPage(info)
                canvas = page.canvas
                y = MARGIN + 10f
            }
        }
    }

    // ── Cabecera del documento ────────────────────────────────────────────────

    private fun Pagina.cabecera(titulo: String, subtitulo: String) {
        canvas.drawText(titulo, MARGIN, y, paintTitulo())
        avanzar(20f)
        canvas.drawText(subtitulo, MARGIN, y, paintGris())
        avanzar(LINE_H + 4f)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintLinea())
        avanzar(LINE_H)
    }

    // ── Reporte del día ───────────────────────────────────────────────────────

    fun generarResumenDia(
        context: Context,
        localNombre: String,
        movimientos: List<Movimiento>,
        clientes: List<Cliente>
    ): Intent {
        val doc = PdfDocument()
        val (page, canvas) = nuevaPagina(doc, 1)
        val p = Pagina(doc, page, canvas, MARGIN + 10f, 1)

        val hoy = Date()
        p.cabecera(
            "Resumen del día — $localNombre",
            "Generado el ${fmtFecha.format(hoy)}"
        )

        val clientesMap = clientes.associateBy { it.id }

        // Agrupar por clienteId, solo movimientos de hoy no anulados
        val cal = java.util.Calendar.getInstance()
        val hoyMedNoche = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.time

        val movHoy = movimientos
            .filter { !it.anulado && it.fecha.toDate() >= hoyMedNoche }
            .sortedBy { it.fecha }

        if (movHoy.isEmpty()) {
            p.canvas.drawText("Sin movimientos registrados hoy.", MARGIN, p.y, paintGris())
            p.avanzar()
        } else {
            val porCliente = movHoy.groupBy { it.clienteId }
            var totalDiaCargo = 0.0
            var totalDiaPago = 0.0

            porCliente.forEach { (clienteId, movs) ->
                p.saltoPaginaSiNecesario(80f)
                val nombreCliente = clientesMap[clienteId]?.nombre ?: "Cliente desconocido"
                val deudaActual = clientesMap[clienteId]?.deudaTotal ?: 0.0

                // Encabezado cliente
                p.canvas.drawRect(MARGIN, p.y - 13f, PAGE_WIDTH - MARGIN, p.y + 4f,
                    Paint().apply { color = Color.rgb(240, 240, 240) })
                p.canvas.drawText(nombreCliente, MARGIN + 4f, p.y, paintSubtitulo())
                p.avanzar(LINE_H + 2f)

                // Movimientos del cliente
                movs.forEach { mov ->
                    p.saltoPaginaSiNecesario(50f)
                    val esCargo = mov.tipo == "cargo"
                    val hora = fmtHora.format(mov.fecha.toDate())
                    val tipoLabel = if (esCargo) "Cargo" else "Pago"
                    p.canvas.drawText("$hora · $tipoLabel", MARGIN + 8f, p.y, paintGris())
                    p.avanzar(LINE_H)

                    if (esCargo && mov.producto.isNotBlank()) {
                        // Detalle de productos
                        mov.producto.split("\n").filter { it.isNotBlank() }.forEach { linea ->
                            p.saltoPaginaSiNecesario()
                            p.canvas.drawText("  · $linea", MARGIN + 16f, p.y, paintNormal())
                            p.avanzar(LINE_H)
                        }
                    }

                    val montoStr = (if (esCargo) "+ " else "- ") + moneda.format(mov.monto)
                    p.canvas.drawTextRight(montoStr, PAGE_WIDTH - MARGIN, p.y - LINE_H / 2,
                        paintMonto(esCargo))

                    if (esCargo) totalDiaCargo += mov.monto else totalDiaPago += mov.monto
                }

                // Deuda actual del cliente
                p.canvas.drawText(
                    "Deuda actual: ${moneda.format(deudaActual)}",
                    MARGIN + 8f, p.y, paintGris()
                )
                p.avanzar(LINE_H)
                p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y, paintLinea())
                p.avanzar(LINE_H + 4f)
            }

            // Totales del día
            p.saltoPaginaSiNecesario(60f)
            p.avanzar(4f)
            p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y,
                Paint().apply { color = Color.BLACK; strokeWidth = 1.5f })
            p.avanzar(LINE_H + 2f)
            p.canvas.drawText("TOTALES DEL DÍA", MARGIN, p.y, paintSubtitulo())
            p.avanzar(LINE_H + 2f)
            p.canvas.drawText("Total cargado:", MARGIN + 8f, p.y, paintNormal())
            p.canvas.drawTextRight(moneda.format(totalDiaCargo), PAGE_WIDTH - MARGIN, p.y, paintMonto(true))
            p.avanzar(LINE_H)
            p.canvas.drawText("Total cobrado:", MARGIN + 8f, p.y, paintNormal())
            p.canvas.drawTextRight(moneda.format(totalDiaPago), PAGE_WIDTH - MARGIN, p.y, paintMonto(false))
            p.avanzar(LINE_H)
            val neto = totalDiaCargo - totalDiaPago
            p.canvas.drawText("Neto del día:", MARGIN + 8f, p.y, paintSubtitulo())
            p.canvas.drawTextRight(moneda.format(neto), PAGE_WIDTH - MARGIN, p.y, paintMonto(neto > 0))
        }

        doc.finishPage(p.page)
        return compartirPdf(context, doc, "resumen_dia_${System.currentTimeMillis()}.pdf")
    }

    // ── Historial de un cliente ───────────────────────────────────────────────

    fun generarHistorialCliente(
        context: Context,
        localNombre: String,
        cliente: Cliente,
        movimientos: List<Movimiento>
    ): Intent {
        val doc = PdfDocument()
        val (page, canvas) = nuevaPagina(doc, 1)
        val p = Pagina(doc, page, canvas, MARGIN + 10f, 1)

        p.cabecera(
            "Historial — ${cliente.nombre}",
            "$localNombre · Generado el ${fmtFecha.format(Date())}"
        )

        // Resumen de cuenta
        p.canvas.drawText("Deuda actual: ${moneda.format(cliente.deudaTotal)}", MARGIN, p.y, paintSubtitulo())
        if (cliente.limiteCredito > 0) {
            p.avanzar(LINE_H)
            p.canvas.drawText("Límite de crédito: ${moneda.format(cliente.limiteCredito)}", MARGIN, p.y, paintGris())
        }
        p.avanzar(LINE_H + 4f)
        p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y, paintLinea())
        p.avanzar(LINE_H)

        val movOrdenados = movimientos.filter { !it.anulado }.sortedByDescending { it.fecha }

        if (movOrdenados.isEmpty()) {
            p.canvas.drawText("Sin movimientos registrados.", MARGIN, p.y, paintGris())
        } else {
            var totalCargo = 0.0
            var totalPago = 0.0

            movOrdenados.forEach { mov ->
                p.saltoPaginaSiNecesario(60f)
                val esCargo = mov.tipo == "cargo"
                val fecha = fmtFechaHora.format(mov.fecha.toDate())
                val tipoLabel = if (esCargo) "Cargo" else "Pago"

                p.canvas.drawText("$fecha · $tipoLabel", MARGIN + 4f, p.y, paintGris())
                p.avanzar(LINE_H)

                if (esCargo && mov.producto.isNotBlank()) {
                    mov.producto.split("\n").filter { it.isNotBlank() }.forEach { linea ->
                        p.saltoPaginaSiNecesario()
                        p.canvas.drawText("  · $linea", MARGIN + 12f, p.y, paintNormal())
                        p.avanzar(LINE_H)
                    }
                }

                val montoStr = (if (esCargo) "+ " else "- ") + moneda.format(mov.monto)
                p.canvas.drawTextRight(montoStr, PAGE_WIDTH - MARGIN, p.y - LINE_H / 2, paintMonto(esCargo))

                if (!mov.anotadoPorNombre.isNullOrBlank()) {
                    p.canvas.drawText("  Registrado por: ${mov.anotadoPorNombre}", MARGIN + 12f, p.y, paintGris())
                    p.avanzar(LINE_H)
                }

                p.canvas.drawLine(MARGIN + 4f, p.y, PAGE_WIDTH - MARGIN - 4f, p.y, paintLinea())
                p.avanzar(LINE_H)

                if (esCargo) totalCargo += mov.monto else totalPago += mov.monto
            }

            // Totales
            p.saltoPaginaSiNecesario(60f)
            p.avanzar(4f)
            p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y,
                Paint().apply { color = Color.BLACK; strokeWidth = 1.5f })
            p.avanzar(LINE_H + 2f)
            p.canvas.drawText("Total cargado:", MARGIN + 8f, p.y, paintNormal())
            p.canvas.drawTextRight(moneda.format(totalCargo), PAGE_WIDTH - MARGIN, p.y, paintMonto(true))
            p.avanzar(LINE_H)
            p.canvas.drawText("Total cobrado:", MARGIN + 8f, p.y, paintNormal())
            p.canvas.drawTextRight(moneda.format(totalPago), PAGE_WIDTH - MARGIN, p.y, paintMonto(false))
        }

        doc.finishPage(p.page)
        return compartirPdf(context, doc, "historial_${cliente.nombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
    }

    // ── Guardar y compartir ───────────────────────────────────────────────────

    private fun compartirPdf(context: Context, doc: PdfDocument, nombre: String): Intent {
        val dir = File(context.cacheDir, "pdfs").also { it.mkdirs() }
        val file = File(dir, nombre)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
