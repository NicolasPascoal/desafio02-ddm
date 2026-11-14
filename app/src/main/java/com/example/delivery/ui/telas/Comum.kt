package com.example.delivery.ui.telas

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.delivery.model.StatusPedido
import java.util.Locale

@Composable
fun Cabecalho(
    titulo: String,
    onVoltar: (() -> Unit)? = null,
    acoes: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onVoltar != null) {
            TextButton(onClick = onVoltar) { Text("← Voltar") }
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        acoes()
    }
}

fun formatarPreco(valor: Double): String =
    String.format(Locale("pt", "BR"), "R$ %.2f", valor)

fun rotuloStatus(status: String): String = when (status) {
    StatusPedido.PENDENTE -> "Pendente"
    StatusPedido.EM_PREPARO -> "Em preparo"
    StatusPedido.SAIU_PARA_ENTREGA -> "Saiu para entrega"
    StatusPedido.ENTREGUE -> "Entregue"
    StatusPedido.CANCELADO -> "Cancelado"
    else -> status
}

// Próximo passo do pedido (usado pelo restaurante)
fun proximoStatus(status: String): String? = when (status) {
    StatusPedido.PENDENTE -> StatusPedido.EM_PREPARO
    StatusPedido.EM_PREPARO -> StatusPedido.SAIU_PARA_ENTREGA
    StatusPedido.SAIU_PARA_ENTREGA -> StatusPedido.ENTREGUE
    else -> null
}
