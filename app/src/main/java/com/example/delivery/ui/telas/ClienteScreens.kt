package com.example.delivery.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.delivery.data.PedidoRepository
import com.example.delivery.data.RestauranteRepository
import com.example.delivery.model.ItemMenu
import com.example.delivery.model.ItemPedido
import com.example.delivery.model.Pedido
import com.example.delivery.model.Restaurante
import com.example.delivery.model.Usuario
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// Cliente: lista de restaurantes
@Composable
fun RestaurantesScreen(
    usuario: Usuario,
    onAbrir: (Restaurante) -> Unit,
    onMeusPedidos: () -> Unit,
    onSair: () -> Unit
) {
    val repo = remember { RestauranteRepository() }
    val restaurantes = remember { mutableStateListOf<Restaurante>() }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            restaurantes.clear()
            restaurantes.addAll(repo.listarRestaurantes())
        } catch (e: Exception) {
            erro = e.message
        }
    }

    Column(Modifier.fillMaxSize()) {
        Cabecalho("Olá, ${usuario.nome}") {
            TextButton(onClick = onMeusPedidos) { Text("Pedidos") }
            TextButton(onClick = onSair) { Text("Sair") }
        }
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        if (restaurantes.isEmpty() && erro == null) {
            Text("Nenhum restaurante encontrado", modifier = Modifier.padding(16.dp))
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(restaurantes, key = { it.id }) { r ->
                Card(Modifier.fillMaxWidth().clickable { onAbrir(r) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(r.nome.ifBlank { "(sem nome)" }, style = MaterialTheme.typography.titleMedium)
                        if (r.descricao.isNotBlank()) Text(r.descricao)
                    }
                }
            }
        }
    }
}

// RF02: cliente escolhe itens do menu e faz o pedido
@Composable
fun MenuScreen(
    restaurante: Restaurante,
    usuario: Usuario,
    onVoltar: () -> Unit,
    onPedidoFeito: () -> Unit
) {
    val restRepo = remember { RestauranteRepository() }
    val pedidoRepo = remember { PedidoRepository() }
    val scope = rememberCoroutineScope()

    val itens = remember { mutableStateListOf<ItemMenu>() }
    val carrinho = remember { mutableStateMapOf<String, Int>() } // itemId -> quantidade
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(restaurante.id) {
        try {
            itens.clear()
            itens.addAll(restRepo.listarMenu(restaurante.id))
        } catch (e: Exception) {
            erro = e.message
        }
    }

    val total = itens.sumOf { (carrinho[it.id] ?: 0) * it.preco }

    Column(Modifier.fillMaxSize()) {
        Cabecalho(restaurante.nome.ifBlank { "Cardápio" }, onVoltar = onVoltar)
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(itens, key = { it.id }) { item ->
                val qtd = carrinho[item.id] ?: 0
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.nome, style = MaterialTheme.typography.titleMedium)
                            if (item.descricao.isNotBlank()) Text(item.descricao)
                            Text(formatarPreco(item.preco))
                        }
                        OutlinedButton(onClick = {
                            if (qtd > 1) carrinho[item.id] = qtd - 1 else carrinho.remove(item.id)
                        }, enabled = qtd > 0) { Text("-") }
                        Text("$qtd", modifier = Modifier.padding(horizontal = 8.dp))
                        OutlinedButton(onClick = { carrinho[item.id] = qtd + 1 }) { Text("+") }
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text("Total: ${formatarPreco(total)}", style = MaterialTheme.typography.titleMedium)
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = carrinho.isNotEmpty() && !enviando,
                onClick = {
                    val itensPedido = itens.filter { (carrinho[it.id] ?: 0) > 0 }.map {
                        ItemPedido(itemId = it.id, nome = it.nome, preco = it.preco, quantidade = carrinho[it.id]!!)
                    }
                    enviando = true
                    scope.launch {
                        try {
                            pedidoRepo.criarPedido(restaurante.id, usuario.nome, itensPedido)
                            onPedidoFeito()
                        } catch (e: Exception) {
                            erro = e.message
                        } finally {
                            enviando = false
                        }
                    }
                }
            ) { Text(if (enviando) "Enviando..." else "Fazer pedido") }
        }
    }
}

// Cliente: acompanha os próprios pedidos em tempo real
@Composable
fun MeusPedidosScreen(onVoltar: () -> Unit) {
    val repo = remember { PedidoRepository() }
    var erro by remember { mutableStateOf<String?>(null) }
    val fluxo = remember {
        repo.ouvirPedidosDoCliente().catch { erro = it.message; emit(emptyList()) }
    }
    val pedidos by fluxo.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        Cabecalho("Meus pedidos", onVoltar = onVoltar)
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        ListaPedidos(pedidos)
    }
}

@Composable
private fun ListaPedidos(pedidos: List<Pedido>) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pedidos, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status: ${rotuloStatus(p.status)}", style = MaterialTheme.typography.titleMedium)
                    p.itens.forEach { Text("${it.quantidade}x ${it.nome}") }
                    Text("Total: ${formatarPreco(p.total)}")
                }
            }
        }
    }
}
