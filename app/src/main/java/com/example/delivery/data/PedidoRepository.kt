package com.example.delivery.data

import com.example.delivery.model.ItemPedido
import com.example.delivery.model.Pedido
import com.example.delivery.model.StatusPedido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PedidoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val pedidos = db.collection("pedidos")
    private fun uidLogado() = auth.currentUser?.uid ?: throw IllegalStateException("Não autenticado")

    // RF02 + RF03: cliente faz o pedido; o documento guarda quem pediu (clienteId/clienteNome)
    suspend fun criarPedido(restauranteId: String, clienteNome: String, itens: List<ItemPedido>): String {
        require(itens.isNotEmpty()) { "Carrinho vazio" }
        val total = itens.sumOf { it.preco * it.quantidade }

        val pedido = Pedido(
            clienteId = uidLogado(),
            clienteNome = clienteNome,
            restauranteId = restauranteId,
            itens = itens,
            total = total,
            status = StatusPedido.PENDENTE
        )
        return pedidos.add(pedido).await().id
    }

    // Tempo real: pedidos do cliente logado
    fun ouvirPedidosDoCliente(): Flow<List<Pedido>> = ouvir("clienteId")

    // Tempo real: pedidos que chegaram para o restaurante logado
    fun ouvirPedidosDoRestaurante(): Flow<List<Pedido>> = ouvir("restauranteId")

    private fun ouvir(campo: String): Flow<List<Pedido>> = callbackFlow {
        val registro = pedidos
            .whereEqualTo(campo, uidLogado())
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, erro ->
                if (erro != null) { close(erro); return@addSnapshotListener }
                trySend(snap?.toObjects(Pedido::class.java).orEmpty())
            }
        awaitClose { registro.remove() }
    }

    // Restaurante atualiza status de preparo/entrega
    suspend fun atualizarStatus(pedidoId: String, novoStatus: String) {
        pedidos.document(pedidoId)
            .update(mapOf("status" to novoStatus, "atualizadoEm" to FieldValue.serverTimestamp()))
            .await()
    }
}
