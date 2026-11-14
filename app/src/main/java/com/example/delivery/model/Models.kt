package com.example.delivery.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

// RN01: todo usuário é CLIENTE ou RESTAURANTE (salvo em minúsculo no Firestore)
object TipoUsuario {
    const val CLIENTE = "cliente"
    const val RESTAURANTE = "restaurante"
}

object StatusPedido {
    const val PENDENTE = "pendente"
    const val EM_PREPARO = "em_preparo"
    const val SAIU_PARA_ENTREGA = "saiu_para_entrega"
    const val ENTREGUE = "entregue"
    const val CANCELADO = "cancelado"
}

// Coleção: usuarios/{uid}
// Valores padrão são obrigatórios para o Firestore conseguir desserializar (toObject)
data class Usuario(
    @DocumentId val uid: String = "",
    val nome: String = "",
    val email: String = "",
    val tipo: String = TipoUsuario.CLIENTE,
    @ServerTimestamp val criadoEm: Timestamp? = null
)

// Coleção: restaurante/{uid}  (id do restaurante = uid do dono)
data class Restaurante(
    @DocumentId val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    @ServerTimestamp val criadoEm: Timestamp? = null
)

// Subcoleção: restaurante/{restauranteId}/itens/{itemId}
data class ItemMenu(
    @DocumentId val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val preco: Double = 0.0,
    val disponivel: Boolean = true
)

data class ItemPedido(
    val itemId: String = "",
    val nome: String = "",
    val preco: Double = 0.0,
    val quantidade: Int = 1
)

// Coleção: pedidos/{pedidoId}  (RF03: guarda quem pediu)
data class Pedido(
    @DocumentId val id: String = "",
    val clienteId: String = "",
    val clienteNome: String = "",
    val restauranteId: String = "",
    val itens: List<ItemPedido> = emptyList(),
    val total: Double = 0.0,
    val status: String = StatusPedido.PENDENTE,
    @ServerTimestamp val criadoEm: Timestamp? = null,
    @ServerTimestamp val atualizadoEm: Timestamp? = null
)
