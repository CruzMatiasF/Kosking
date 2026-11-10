package com.kosking.app.onboarding

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kosking.app.cliente.ClienteViewModel
import com.kosking.app.dueno.DuenoViewModel
import com.kosking.app.dueno.NavDueno
import com.kosking.app.dueno.PanelDuenoEmbedido
import com.kosking.app.dueno.PantallaAgregarCliente
import com.kosking.app.dueno.PantallaAgregarFiado
import com.kosking.app.dueno.detalle.PantallaDetalleCliente
import com.kosking.app.dueno.detalle.PantallaDetalleMovimiento

@Composable
fun PantallaHome(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    clienteVm: ClienteViewModel,
    duenoVm: DuenoViewModel,
    mostrarPanelDueno: Boolean,
    onCerrarPanelDueno: () -> Unit,
    tabInicialDueno: Int,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onVerMisLocales: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrarSesionCuenta: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConfiguracion: () -> Unit = {},
    onAcercaDe: () -> Unit = {},
) {
    var vistaDueno by remember { mutableStateOf<NavDueno>(NavDueno.Panel) }
    var tabPanelDueno by remember { mutableIntStateOf(tabInicialDueno) }

    LaunchedEffect(mostrarPanelDueno) {
        if (mostrarPanelDueno) {
            vistaDueno = NavDueno.Panel
            tabPanelDueno = tabInicialDueno
        }
    }

    BackHandler(enabled = mostrarPanelDueno && vistaDueno !is NavDueno.Panel) {
        vistaDueno = when (val v = vistaDueno) {
            is NavDueno.AgregarFiado -> if (v.volverAPanel) NavDueno.Panel
                                        else NavDueno.DetalleCliente(v.clienteId)
            else -> NavDueno.Panel
        }
    }

    // Dueño logueado → ir directo al panel, sin pasar por bienvenida
    LaunchedEffect(estaLogueado, esDueno) {
        if (estaLogueado && esDueno && !mostrarPanelDueno) onSoyDueno()
    }

    if (mostrarPanelDueno) {
        when (val vista = vistaDueno) {
            is NavDueno.Panel -> PanelDuenoEmbedido(
                vm = duenoVm,
                tabSeleccionado = tabPanelDueno,
                onTabChange = { tabPanelDueno = it },
                onAbrirAgregarCliente = { vistaDueno = NavDueno.AgregarCliente },
                onAbrirDetalleCliente = { vistaDueno = NavDueno.DetalleCliente(it) },
                onAbrirAgregarFiado = { clienteId, tipo ->
                    vistaDueno = NavDueno.AgregarFiado(clienteId, tipo, volverAPanel = tabPanelDueno != 0)
                },
                onAbrirDetalleMovimiento = { vistaDueno = NavDueno.DetalleMovimiento(it) }
            )
            is NavDueno.AgregarCliente -> PantallaAgregarCliente(
                onVolver = { vistaDueno = NavDueno.Panel },
                vm = duenoVm
            )
            is NavDueno.DetalleCliente -> PantallaDetalleCliente(
                clienteId = vista.id,
                onVolver = { vistaDueno = NavDueno.Panel },
                onVerMovimiento = { movId ->
                    vistaDueno = NavDueno.DetalleMovimiento(movId, clienteId = vista.id)
                },
                onAnotarFiado = { clienteId -> vistaDueno = NavDueno.AgregarFiado(clienteId, "cargo") },
                onCobrar = { clienteId -> vistaDueno = NavDueno.AgregarFiado(clienteId, "pago") },
                vm = duenoVm
            )
            is NavDueno.AgregarFiado -> PantallaAgregarFiado(
                clienteId = vista.clienteId,
                onGuardado = {
                    vistaDueno = if (vista.volverAPanel) NavDueno.Panel
                                 else NavDueno.DetalleCliente(vista.clienteId)
                },
                onVolver = {
                    vistaDueno = if (vista.volverAPanel) NavDueno.Panel
                                 else NavDueno.DetalleCliente(vista.clienteId)
                },
                vm = duenoVm,
                tipoInicial = vista.tipoInicial
            )
            is NavDueno.DetalleMovimiento -> PantallaDetalleMovimiento(
                movimientoId = vista.id,
                onVolver = {
                    vistaDueno = vista.clienteId?.let { NavDueno.DetalleCliente(it) } ?: NavDueno.Panel
                },
                vm = duenoVm
            )
        }
    } else {
        PantallaBienvenida(
            estaLogueado = estaLogueado,
            nombreUsuario = nombreUsuario,
            esDueno = esDueno,
            esEmpleado = esEmpleado,
            onSoyDueno = onSoyDueno,
            onSoyEmpleado = onSoyEmpleado,
            onVerMisLocales = onVerMisLocales,
            onIniciarSesion = onIniciarSesion,
            onCerrarSesion = onCerrarSesionCuenta,
            onConfigurarPerfil = onConfigurarPerfil,
            onConfiguracion = onConfiguracion,
            onAcercaDe = onAcercaDe,
        )
    }
}

@Composable
private fun PantallaBienvenida(
    estaLogueado: Boolean,
    nombreUsuario: String,
    esDueno: Boolean,
    esEmpleado: Boolean,
    onSoyDueno: () -> Unit,
    onSoyEmpleado: () -> Unit,
    onVerMisLocales: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrarSesion: () -> Unit,
    onConfigurarPerfil: () -> Unit,
    onConfiguracion: () -> Unit,
    onAcercaDe: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "K",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Kosking",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Gestioná tus fiaditos por WhatsApp",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        if (estaLogueado) {
            // Usuario logueado pero sin panel activo (caso raro)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            nombreUsuario.firstOrNull()?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(nombreUsuario.ifBlank { "Mi cuenta" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (esDueno) {
                Button(onClick = onSoyDueno, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Store, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ir a mi panel")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
        } else {
            // Tres opciones como cards para mayor claridad visual
            OpcionEntrada(
                emoji = "🏪",
                titulo = "Soy dueño",
                subtitulo = "Registrate y manejá tu local",
                onClick = onSoyDueno,
                destacada = true
            )
            Spacer(Modifier.height(10.dp))
            OpcionEntrada(
                emoji = "👤",
                titulo = "Soy empleado",
                subtitulo = "Entrá con el código del local",
                onClick = onSoyEmpleado
            )
            Spacer(Modifier.height(10.dp))
            OpcionEntrada(
                emoji = "💰",
                titulo = "Ver mi deuda",
                subtitulo = "Chequeá lo que debés en un local",
                onClick = onVerMisLocales
            )
        }

        Spacer(Modifier.height(32.dp))
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "¡Usá Kosking para manejar los fiaditos de tu local! Es gratis para empezar. Buscalo como \"Kosking\" en Play Store 📱"
                    )
                }
                context.startActivity(Intent.createChooser(intent, "Compartir Kosking"))
            }
        ) {
            Icon(Icons.Default.Share, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Compartir con un amigo", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OpcionEntrada(
    emoji: String,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    destacada: Boolean = false
) {
    val containerColor = if (destacada) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (destacada) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = contentColor)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f))
            }
        }
    }
}
