package com.example.delivery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.delivery.data.AuthRepository
import com.example.delivery.model.Restaurante
import com.example.delivery.model.TipoUsuario
import com.example.delivery.model.Usuario
import com.example.delivery.ui.telas.LoginScreen
import com.example.delivery.ui.telas.MenuScreen
import com.example.delivery.ui.telas.MeusPedidosScreen
import com.example.delivery.ui.telas.PainelRestauranteScreen
import com.example.delivery.ui.telas.RestaurantesScreen
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
           MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

sealed class Tela {
    object Login : Tela()
    object Restaurantes : Tela()
    data class Menu(val restaurante: Restaurante) : Tela()
    object MeusPedidos : Tela()
    object PainelRestaurante : Tela()
}

@Composable
fun App() {
    val authRepo = remember { AuthRepository() }
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var tela by remember { mutableStateOf<Tela>(Tela.Login) }
    var iniciando by remember { mutableStateOf(true) }

    fun irParaHome(u: Usuario) {
        usuario = u
        tela = if (u.tipo == TipoUsuario.RESTAURANTE) Tela.PainelRestaurante else Tela.Restaurantes
    }

    fun sair() {
        authRepo.sair()
        usuario = null
        tela = Tela.Login
    }

    LaunchedEffect(Unit) {
        val uid = authRepo.usuarioAtualId
        if (uid != null) {
            try { irParaHome(authRepo.buscarPerfil(uid)) } catch (_: Exception) { authRepo.sair() }
        }
        iniciando = false
    }

    if (iniciando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    BackHandler(enabled = tela is Tela.Menu || tela is Tela.MeusPedidos) {
        tela = Tela.Restaurantes
    }

    val u = usuario
    when (val t = tela) {
        Tela.Login -> LoginScreen(onLogado = { irParaHome(it) })

        Tela.Restaurantes -> RestaurantesScreen(
            usuario = u!!,
            onAbrir = { tela = Tela.Menu(it) },
            onMeusPedidos = { tela = Tela.MeusPedidos },
            onSair = { sair() }
        )

        is Tela.Menu -> MenuScreen(
            restaurante = t.restaurante,
            usuario = u!!,
            onVoltar = { tela = Tela.Restaurantes },
            onPedidoFeito = { tela = Tela.MeusPedidos }
        )

        Tela.MeusPedidos -> MeusPedidosScreen(onVoltar = { tela = Tela.Restaurantes })

        Tela.PainelRestaurante -> PainelRestauranteScreen(usuario = u!!, onSair = { sair() })
    }
}
