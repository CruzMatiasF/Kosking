package com.kosking.app.navegacion

object Rutas {
    const val EXPLORAR = "explorar"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val CREAR_LOCAL = "crear_local"
    const val INGRESAR_CODIGO = "ingresar_codigo"
    const val PANEL_EMPLEADO = "panel_empleado"
    const val DETALLE_CLIENTE_EMPLEADO = "detalle_cliente_empleado/{clienteId}"
    const val DETALLE_MOVIMIENTO = "detalle_movimiento/{movimientoId}"
    const val ONBOARDING = "onboarding"
    const val CONFIGURAR_LOCAL = "configurar_local"
    const val CLIENTES_EMPLEADO = "clientes_empleado"
    const val ELEGIR_RUBRO = "elegir_rubro"
    const val DESCARGANDO_RUBRO = "descargando_rubro/{tipo}"

    fun detalleClienteEmpleado(clienteId: String) = "detalle_cliente_empleado/$clienteId"
    fun detalleMovimiento(movimientoId: String) = "detalle_movimiento/$movimientoId"
    fun descargandoRubro(tipo: String) = "descargando_rubro/$tipo"
}
