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

    // â"€â"€ Pinceles â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private val paintTitulo = Paint().apply {
        textSize = 18f; isFakeBoldText = true; color = Color.BLACK
    }
    private val paintSubtitulo = Paint().apply {
        textSize = 13f; isFakeBoldText = true; color = Color.BLACK
    }
    private val paintNormal = Paint().apply {
        textSize = 11f; color = Color.BLACK
    }
    private val paintGris = Paint().apply {
        textSize = 10f; color = Color.GRAY
    }
    private fun paintMonto(cargo: Boolean) = Paint().apply {
        textSize = 11f; isFakeBoldText = true
        color = if (cargo) Color.rgb(180, 30, 30) else Color.rgb(30, 130, 30)
    }
    private val paintLinea = Paint().apply {
        color = Color.LTGRAY; strokeWidth = 1f
    }

    // â"€â"€ Helpers â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

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

    // â"€â"€ Cabecera del documento â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private fun Pagina.cabecera(titulo: String, subtitulo: String) {
        canvas.drawText(titulo, MARGIN, y, paintTitulo)
        avanzar(20f)
        canvas.drawText(subtitulo, MARGIN, y, paintGris)
        avanzar(LINE_H + 4f)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paintLinea)
        avanzar(LINE_H)
    }

    // â"€â"€ Reporte del día â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

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
            p.canvas.drawText("Sin movimientos registrados hoy.", MARGIN, p.y, paintGris)
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
                p.canvas.drawText(nombreCliente, MARGIN + 4f, p.y, paintSubtitulo)
                p.avanzar(LINE_H + 2f)

                // Movimientos del cliente
                movs.forEach { mov ->
                    p.saltoPaginaSiNecesario(50f)
                    val esCargo = mov.tipo == "cargo"
                    val hora = fmtHora.format(mov.fecha.toDate())
                    val tipoLabel = if (esCargo) "Cargo" else "Pago"
                    p.canvas.drawText("$hora · $tipoLabel", MARGIN + 8f, p.y, paintGris)
                    p.avanzar(LINE_H)

                    if (esCargo && mov.producto.isNotBlank()) {
                        p.saltoPaginaSiNecesario()
                        val linea = if (mov.cantidad > 1) "${mov.cantidad}× ${mov.producto}" else mov.producto
                        p.canvas.drawText("  · $linea", MARGIN + 16f, p.y, paintNormal)
                        p.avanzar(LINE_H)
                    }

                    val montoStr = (if (esCargo) "+ " else "- ") + moneda.format(mov.monto)
                    p.canvas.drawTextRight(montoStr, PAGE_WIDTH - MARGIN, p.y - LINE_H / 2,
                        paintMonto(esCargo))

                    if (esCargo) totalDiaCargo += mov.monto else totalDiaPago += mov.monto
                }

                // Deuda actual del cliente
                p.canvas.drawText(
                    "Deuda actual: ${moneda.format(deudaActual)}",
                    MARGIN + 8f, p.y, paintGris
                )
                p.avanzar(LINE_H)
                p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y, paintLinea)
                p.avanzar(LINE_H + 4f)
            }

            // Totales del día
            p.saltoPaginaSiNecesario(60f)
            p.avanzar(4f)
            p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y,
                Paint().apply { color = Color.BLACK; strokeWidth = 1.5f })
            p.avanzar(LINE_H + 2f)
            p.canvas.drawText("TOTALES DEL DÍA", MARGIN, p.y, paintSubtitulo)
            p.avanzar(LINE_H + 2f)
            p.canvas.drawText("Total cargado:", MARGIN + 8f, p.y, paintNormal)
            p.canvas.drawTextRight(moneda.format(totalDiaCargo), PAGE_WIDTH - MARGIN, p.y, paintMonto(true))
            p.avanzar(LINE_H)
            p.canvas.drawText("Total cobrado:", MARGIN + 8f, p.y, paintNormal)
            p.canvas.drawTextRight(moneda.format(totalDiaPago), PAGE_WIDTH - MARGIN, p.y, paintMonto(false))
            p.avanzar(LINE_H)
            val neto = totalDiaCargo - totalDiaPago
            p.canvas.drawText("Neto del día:", MARGIN + 8f, p.y, paintSubtitulo)
            p.canvas.drawTextRight(moneda.format(neto), PAGE_WIDTH - MARGIN, p.y, paintMonto(neto > 0))
        }

        doc.finishPage(p.page)
        return compartirPdf(context, doc, "resumen_dia_${System.currentTimeMillis()}.pdf")
    }

    // â"€â"€ Historial de un cliente â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

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
            "Lo que te llevaste — ${cliente.nombre}",
            "$localNombre · Generado el ${fmtFecha.format(Date())}"
        )

        val movActivos = movimientos.filter { !it.anulado }.sortedByDescending { it.fecha.toDate().time }

        if (movActivos.isEmpty()) {
            p.canvas.drawText("Sin movimientos registrados.", MARGIN, p.y, paintGris)
        } else {
            // Visitas (cargos agrupados por grupoId)
            val fmtKey = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
            val visitas = movActivos
                .filter { it.tipo == "cargo" }
                .groupBy { it.grupoId.ifBlank { fmtKey.format(it.fecha.toDate()) } }
                .entries
                .sortedByDescending { (_, movs) -> movs.first().fecha.toDate().time }

            // Pagos
            val pagos = movActivos.filter { it.tipo == "pago" }

            if (visitas.isNotEmpty()) {
                p.canvas.drawText("Compras", MARGIN, p.y, paintSubtitulo)
                p.avanzar(LINE_H + 2f)

                visitas.forEach { (_, movs) ->
                    p.saltoPaginaSiNecesario(50f)
                    val fecha = fmtFecha.format(movs.first().fecha.toDate())
                    val totalVisita = movs.sumOf { it.monto }

                    // Fecha + total de visita
                    p.canvas.drawText(fecha, MARGIN + 4f, p.y, paintGris)
                    p.canvas.drawTextRight(moneda.format(totalVisita), PAGE_WIDTH - MARGIN, p.y, paintGris)
                    p.avanzar(LINE_H)

                    // Productos con cantidad y precio
                    movs.filter { it.producto.isNotBlank() }.forEach { mov ->
                        p.saltoPaginaSiNecesario()
                        val linea = if (mov.cantidad > 1) "${mov.cantidad}× ${mov.producto}" else mov.producto
                        p.canvas.drawText("  · $linea", MARGIN + 12f, p.y, paintNormal)
                        p.canvas.drawTextRight(moneda.format(mov.monto), PAGE_WIDTH - MARGIN, p.y, paintNormal)
                        p.avanzar(LINE_H)
                    }

                    p.canvas.drawLine(MARGIN + 4f, p.y, PAGE_WIDTH - MARGIN - 4f, p.y, paintLinea)
                    p.avanzar(LINE_H)
                }
            }

            // Pagos realizados
            if (pagos.isNotEmpty()) {
                p.saltoPaginaSiNecesario(40f)
                p.avanzar(4f)
                p.canvas.drawText("Pagos realizados", MARGIN, p.y, paintSubtitulo)
                p.avanzar(LINE_H + 2f)

                pagos.forEach { pago ->
                    p.saltoPaginaSiNecesario()
                    val fecha = fmtFechaHora.format(pago.fecha.toDate())
                    p.canvas.drawText(fecha, MARGIN + 4f, p.y, paintGris)
                    p.canvas.drawTextRight(moneda.format(pago.monto), PAGE_WIDTH - MARGIN, p.y, paintMonto(false))
                    p.avanzar(LINE_H)
                }

                p.canvas.drawLine(MARGIN + 4f, p.y, PAGE_WIDTH - MARGIN - 4f, p.y, paintLinea)
                p.avanzar(LINE_H)
            }

            // Deuda actual
            p.saltoPaginaSiNecesario(50f)
            p.avanzar(4f)
            p.canvas.drawLine(MARGIN, p.y, PAGE_WIDTH - MARGIN, p.y,
                Paint().apply { color = Color.BLACK; strokeWidth = 1.5f })
            p.avanzar(LINE_H + 2f)
            p.canvas.drawText("Deuda actual:", MARGIN + 8f, p.y, paintSubtitulo)
            p.canvas.drawTextRight(moneda.format(cliente.deudaTotal), PAGE_WIDTH - MARGIN, p.y, paintMonto(true))
        }

        doc.finishPage(p.page)
        return compartirPdf(context, doc, "historial_${cliente.nombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
    }

    // â"€â"€ Guardar y compartir â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

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

