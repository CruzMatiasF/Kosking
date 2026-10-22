package com.kosking.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaInicio(
    modifier: Modifier = Modifier,
    onDuenoClick: () -> Unit,
    onEmpleadoClick: () -> Unit,
    onClienteClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Kosking",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = KoskingRed
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "El barrio en tu bolsillo",
            fontSize = 16.sp,
            color = KoskingGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = "¿Cómo querés usar Kosking?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = KoskingDark
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDuenoClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KoskingRed)
        ) {
            Text("Soy dueño", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEmpleadoClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, KoskingRed)
        ) {
            Text("Soy empleado", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = KoskingRed)
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onClienteClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, KoskingRed)
        ) {
            Text("Soy cliente", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = KoskingRed)
        }
    }
}
