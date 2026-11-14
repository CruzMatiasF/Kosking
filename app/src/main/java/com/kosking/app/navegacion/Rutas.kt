package com.kosking.app.navegacion

object Rutas {
    const val EXPLORAR = "explorar"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val CREAR_LOCAL = "crear_local"
    const val INGRESAR_CODIGO = "ingresar_codigo"
    const val PANEL_EMPLEADO = "panel_empleado"
    const val AGREGAR_FIADO_EMPLEADO = "agregar_fiado_empleado/{clienteId}"
    const val DETALLE_MOVIMIENTO = "detalle_movimiento/{movimientoId}"
    const val CONFIGURAR_PERFIL = "configurar_perfil"
    const val CONFIGURACION = "configuracion"
    const val ACERCA_DE = "acerca_de"
    const val PRIVACIDAD = "privacidad"
    const val POLITICA_PRIVACIDAD = "politica_privacidad"
    const val SEGURIDAD = "seguridad"
    const val ONBOARDING = "onboarding"
    const val PERFIL = "perfil"
    const val COLABORADORES = "colaboradores"
    const val CONFIGURAR_LOCAL = "configurar_local"

    fun agregarFiadoEmpleado(clienteId: String) = "agregar_fiado_empleado/$clienteId"
    fun detalleMovimiento(movimientoId: String) = "detalle_movimiento/$movimientoId"
}
