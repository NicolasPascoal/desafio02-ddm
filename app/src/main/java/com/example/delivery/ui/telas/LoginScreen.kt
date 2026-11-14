package com.example.delivery.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.delivery.data.AuthRepository
import com.example.delivery.model.TipoUsuario
import com.example.delivery.model.Usuario
import kotlinx.coroutines.launch

// RF01: login e criação de conta / RN01: escolha entre Cliente e Restaurante
@Composable
fun LoginScreen(onLogado: (Usuario) -> Unit) {
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var modoCadastro by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoUsuario.CLIENTE) }
    var descricao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (modoCadastro) "Criar conta" else "Entrar",
            style = MaterialTheme.typography.headlineMedium
        )

        if (modoCadastro) {
            // RN01: tipo de usuário
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = tipo == TipoUsuario.CLIENTE, onClick = { tipo = TipoUsuario.CLIENTE })
                Text("Cliente")
                RadioButton(selected = tipo == TipoUsuario.RESTAURANTE, onClick = { tipo = TipoUsuario.RESTAURANTE })
                Text("Restaurante")
            }
            OutlinedTextField(
                value = nome, onValueChange = { nome = it },
                label = { Text(if (tipo == TipoUsuario.RESTAURANTE) "Nome do restaurante" else "Seu nome") },
                modifier = Modifier.fillMaxWidth()
            )
            if (tipo == TipoUsuario.RESTAURANTE) {
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição do restaurante") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        OutlinedTextField(
            value = email, onValueChange = { email = it.trim() },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = senha, onValueChange = { senha = it },
            label = { Text("Senha (mín. 6 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (carregando) {
            CircularProgressIndicator()
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    erro = null
                    if (email.isBlank() || senha.length < 6 || (modoCadastro && nome.isBlank())) {
                        erro = "Preencha todos os campos (senha com 6+ caracteres)"
                        return@Button
                    }
                    carregando = true
                    scope.launch {
                        try {
                            val usuario = if (modoCadastro) {
                                authRepo.cadastrar(nome, email, senha, tipo, descricao)
                            } else {
                                authRepo.entrar(email, senha)
                            }
                            onLogado(usuario)
                        } catch (e: Exception) {
                            erro = e.message ?: "Erro ao autenticar"
                        } finally {
                            carregando = false
                        }
                    }
                }
            ) { Text(if (modoCadastro) "Cadastrar" else "Entrar") }

            TextButton(onClick = { modoCadastro = !modoCadastro; erro = null }) {
                Text(if (modoCadastro) "Já tenho conta" else "Criar uma conta")
            }
        }
    }
}
