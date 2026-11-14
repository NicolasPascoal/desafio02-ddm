package com.example.delivery.data

import com.example.delivery.model.ItemMenu
import com.example.delivery.model.Restaurante
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RestauranteRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun uidLogado() = auth.currentUser?.uid ?: throw IllegalStateException("Não autenticado")

    // Cliente: lista restaurantes para escolher
    suspend fun listarRestaurantes(): List<Restaurante> =
        db.collection("restaurante").get().await()
            .toObjects(Restaurante::class.java)

    // RF02: cliente vê o menu do restaurante (só itens disponíveis)
    suspend fun listarMenu(restauranteId: String): List<ItemMenu> =
        db.collection("restaurante").document(restauranteId)
            .collection("itens")
            .whereEqualTo("disponivel", true)
            .get().await()
            .toObjects(ItemMenu::class.java)

    // Restaurante: vê todos os próprios itens (inclusive indisponíveis)
    suspend fun listarMeusItens(): List<ItemMenu> =
        db.collection("restaurante").document(uidLogado())
            .collection("itens")
            .get().await()
            .toObjects(ItemMenu::class.java)

    // RN01: só restaurante cria itens (as regras do Firestore também bloqueiam cliente)
    suspend fun criarItem(nome: String, descricao: String, preco: Double): String {
        require(preco > 0) { "Preço deve ser maior que zero" }
        val ref = db.collection("restaurante").document(uidLogado())
            .collection("itens")
            .add(ItemMenu(nome = nome, descricao = descricao, preco = preco))
            .await()
        return ref.id
    }

    suspend fun alterarDisponibilidade(itemId: String, disponivel: Boolean) {
        db.collection("restaurante").document(uidLogado())
            .collection("itens").document(itemId)
            .update("disponivel", disponivel)
            .await()
    }
}
