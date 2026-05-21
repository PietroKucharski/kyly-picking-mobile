package com.kyly.picking.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kyly.picking.ui.enderecos.EnderecosAlternativosScreen
import com.kyly.picking.ui.finalizacao.FinalizacaoScreen
import com.kyly.picking.ui.login.LoginScreen
import com.kyly.picking.ui.menu.MenuScreen
import com.kyly.picking.ui.papeleta.PapeletaScreen
import com.kyly.picking.ui.picking.PickingScreen

sealed class AppDestination(val route: String) {
    object Login       : AppDestination("login")
    object Menu        : AppDestination("menu")
    object Papeleta    : AppDestination("papeleta")
    object Picking     : AppDestination("picking/{caixaCodigo}") {
        fun withArgs(codigo: String) = "picking/$codigo"
    }
    object Finalizacao : AppDestination("finalizacao/{tipo}") {
        fun withArgs(tipo: String) = "finalizacao/$tipo"
    }
    object Enderecos   : AppDestination("enderecos/{skuId}") {
        fun withArgs(skuId: String) = "enderecos/$skuId"
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = AppDestination.Login.route,
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(navController)
        }
        composable(AppDestination.Menu.route) {
            MenuScreen(navController)
        }
        composable(AppDestination.Papeleta.route) {
            PapeletaScreen(navController)
        }
        composable(
            route     = AppDestination.Picking.route,
            arguments = listOf(navArgument("caixaCodigo") { type = NavType.StringType }),
        ) { backStackEntry ->
            PickingScreen(
                navController = navController,
                caixaCodigo   = backStackEntry.arguments?.getString("caixaCodigo") ?: "",
            )
        }
        composable(
            route     = AppDestination.Finalizacao.route,
            arguments = listOf(navArgument("tipo") { type = NavType.StringType }),
        ) { backStackEntry ->
            FinalizacaoScreen(
                navController = navController,
                tipo          = backStackEntry.arguments?.getString("tipo") ?: "finalizada",
            )
        }
        composable(
            route     = AppDestination.Enderecos.route,
            arguments = listOf(navArgument("skuId") { type = NavType.StringType }),
        ) { backStackEntry ->
            EnderecosAlternativosScreen(
                navController = navController,
                skuId         = backStackEntry.arguments?.getString("skuId") ?: "",
            )
        }
    }
}
