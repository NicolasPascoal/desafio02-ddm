package com.example.delivery.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.delivery.model.StatusPedido
import com.example.delivery.model.Usuario
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// Restaurante: painel com abas Cardápio e Pedidos
@Composable
fun PainelRestauranteScreen(usuario: Usuario, onSair: () -> Unit) {
    var aba by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Cabecalho(usuario.nome) {
            TextButton(onClick = onSair) { Text("Sair") }
        }
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Cardápio") })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Pedidos") })
        }
        if (aba == 0) CardapioTab() else PedidosRecebidosTab()
    }
}

// RN01: só o restaurante cria itens
@Composable
private fun CardapioTab() {
    val repo = remember { RestauranteRepository() }
    val scope = rememberCoroutineScope()
    val itens = remember { mutableStateListOf<ItemMenu>() }

    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    suspend fun recarregar() {
        itens.clear()
        itens.addAll(repo.listarMeusItens())
    }

    LaunchedEffect(Unit) {
        try { recarregar() } catch (e: Exception) { erro = e.message }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Column {
            Text("Novo item", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(descricao, { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(preco, { preco = it }, label = { Text("Preço (ex: 45,90)") }, modifier = Modifier.fillMaxWidth())
            erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = {
                    val valor = preco.replace(",", ".").toDoubleOrNull()
                    if (nome.isBlank() || valor == null || valor <= 0) {
                        erro = "Informe nome e um preço válido"
                        return@Button
                    }
                    erro = null
                    scope.launch {
                        try {
                            repo.criarItem(nome, descricao, valor)
                            nome = ""; descricao = ""; preco = ""
                            recarregar()
                        } catch (e: Exception) {
                            erro = e.message
                        }
                    }
                }
            ) { Text("Adicionar ao cardápio") }
            Text("Meu cardápio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        } }

        items(itens, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.nome, style = MaterialTheme.typography.titleMedium)
                        Text(formatarPreco(item.preco))
                    }
                    Text(if (item.disponivel) "Disponível" else "Indisponível")
                    Switch(
                        checked = item.disponivel,
                        onCheckedChange = { novo ->
                            scope.launch {
                                try {
                                    repo.alterarDisponibilidade(item.id, novo)
                                    recarregar()
                                } catch (e: Exception) {
                                    erro = e.message
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// Restaurante: recebe pedidos em tempo real e atualiza o status
@Composable
private fun PedidosRecebidosTab() {
    val repo = remember { PedidoRepository() }
    val scope = rememberCoroutineScope()
    var erro by remember { mutableStateOf<String?>(null) }
    val fluxo = remember {
        repo.ouvirPedidosDoRestaurante().catch { erro = it.message; emit(emptyList()) }
    }
    val pedidos by fluxo.collectAsState(initial = emptyList())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        erro?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error) } }
        if (pedidos.isEmpty()) item { Text("Nenhum pedido ainda") }

        items(pedidos, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cliente: ${p.clienteNome}", style = MaterialTheme.typography.titleMedium)
                    p.itens.forEach { Text("${it.quantidade}x ${it.nome}") }
                    Text("Total: ${formatarPreco(p.total)}")
                    Text("Status: ${rotuloStatus(p.status)}")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        proximoStatus(p.status)?.let { prox ->
                            Button(onClick = {
                                scope.launch {
                                    try { repo.atualizarStatus(p.id, prox) } catch (e: Exception) { erro = e.message }
                                }
                            }) { Text(rotuloStatus(prox)) }
                        }
                        if (p.status == StatusPedido.PENDENTE) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try { repo.atualizarStatus(p.id, StatusPedido.CANCELADO) } catch (e: Exception) { erro = e.message }
                                }
                            }) { Text("Cancelar") }
                        }
                    }
                }
            }
        }
    }
}
