package com.example.delivery.data

import com.example.delivery.model.Restaurante
import com.example.delivery.model.TipoUsuario
import com.example.delivery.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val usuarioAtualId: String?
        get() = auth.currentUser?.uid

    // RF01: criar conta (cliente ou restaurante)
    suspend fun cadastrar(
        nome: String,
        email: String,
        senha: String,
        tipo: String,
        descricaoRestaurante: String = ""
    ): Usuario {
        require(tipo == TipoUsuario.CLIENTE || tipo == TipoUsuario.RESTAURANTE) { "Tipo inválido" }

        val uid = auth.createUserWithEmailAndPassword(email, senha).await().user!!.uid

        // 1º grava o perfil com o tipo (RN01) — as regras de segurança dependem dele
        val usuario = Usuario(nome = nome, email = email, tipo = tipo)
        db.collection("usuarios").document(uid).set(usuario).await()

        // 2º se for restaurante, cria o perfil público que os clientes vão listar
        if (tipo == TipoUsuario.RESTAURANTE) {
            db.collection("restaurante").document(uid)
                .set(Restaurante(nome = nome, descricao = descricaoRestaurante))
                .await()
        }
        return usuario.copy(uid = uid)
    }

    // RF01: login — devolve o perfil para a tela decidir se abre área de cliente ou restaurante
    suspend fun entrar(email: String, senha: String): Usuario {
        val uid = auth.signInWithEmailAndPassword(email, senha).await().user!!.uid
        return buscarPerfil(uid)
    }

    suspend fun buscarPerfil(uid: String): Usuario {
        val snap = db.collection("usuarios").document(uid).get().await()
        return snap.toObject(Usuario::class.java)
            ?: throw IllegalStateException("Perfil não encontrado")
    }

    fun sair() = auth.signOut()
}
