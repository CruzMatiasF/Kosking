package com.kosking.app.navegacion

object Rutas {
    // Entrada pública
    const val EXPLORAR = "explorar"

    // Autenticación dueño
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val CREAR_LOCAL = "crear_local"

    // Dueño (bottom nav destinos)
    const val PANEL_DUENO = "panel_dueno"

    // Empleado
    const val INGRESAR_CODIGO = "ingresar_codigo"
    const val PANEL_EMPLEADO = "panel_empleado"
    const val AGREGAR_FIADO_EMPLEADO = "agregar_fiado_empleado/{clienteId}"

    // Cliente
    const val INGRESAR_PIN = "ingresar_pin/{localId}/{nombreLocal}"
    const val VER_FIADO = "ver_fiado"

    // Compartidas
    const val VIDRIERA_PUBLICA = "vidriera_publica/{localId}"
    const val DETALLE_MOVIMIENTO = "detalle_movimiento/{movimientoId}"
    const val CONFIGURAR_PERFIL = "configurar_perfil"
    const val CONFIGURACION = "configuracion"
    const val NOTIFICACIONES = "notificaciones"
    const val ACERCA_DE = "acerca_de"
    const val EDITAR_LOCAL = "editar_local"
    const val HISTORIAL = "historial"

    fun vidreraPublica(localId: String) = "vidriera_publica/$localId"
    fun ingresarPIN(localId: String, nombreLocal: String) =
        "ingresar_pin/$localId/${nombreLocal.encodeUrl()}"
    fun agregarFiadoEmpleado(clienteId: String) = "agregar_fiado_empleado/$clienteId"
    fun detalleMovimiento(movimientoId: String) = "detalle_movimiento/$movimientoId"

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
}
