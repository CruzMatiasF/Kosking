package com.kosking.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kosking.app.ui.theme.*

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    icono: @Composable (() -> Unit)? = null,
    iconoFinal: @Composable (() -> Unit)? = null,
    transformacion: VisualTransformation = VisualTransformation.None,
    opciones: KeyboardOptions = KeyboardOptions.Default,
    error: Boolean = false,
    habilitado: Boolean = true,
    lineaUnica: Boolean = true,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta, color = AzulOscuro) },
        leadingIcon = icono,
        trailingIcon = iconoFinal,
        visualTransformation = transformacion,
        keyboardOptions = opciones,
        isError = error,
        enabled = habilitado,
        singleLine = lineaUnica,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AzulNavy,
            unfocusedTextColor = AzulNavy,
            disabledTextColor = Gris500,
            focusedContainerColor = Blanco,
            unfocusedContainerColor = Blanco,
            focusedBorderColor = Azul,
            unfocusedBorderColor = AzulOscuro.copy(alpha = 0.4f),
            focusedLabelColor = Azul,
            cursorColor = Azul,
        ),
    )
}

@Composable
fun BotonPrincipal(
    texto: String,
    alTocar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    containerColor: androidx.compose.ui.graphics.Color = Rojo,
    contentColor: androidx.compose.ui.graphics.Color = Blanco,
) {
    Button(
        onClick = alTocar,
        enabled = habilitado && !cargando,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(color = contentColor, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(texto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MensajeError(texto: String) {
    if (texto.isNotEmpty()) {
        Text(
            text = texto,
            color = Rojo,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
