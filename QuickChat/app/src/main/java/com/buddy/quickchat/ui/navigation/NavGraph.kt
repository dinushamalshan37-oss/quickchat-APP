package com.buddy.quickchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.buddy.quickchat.ui.auth.LoginScreen
import com.buddy.quickchat.ui.auth.RegisterScreen
import com.buddy.quickchat.ui.home.HomeScreen
import com.buddy.quickchat.ui.chat.ChatScreen

@Composable
fun QuickChatNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Routes.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                onNavigateToChat = { chatId ->
                    navController.navigate(Routes.Chat.createRoute(chatId))
                }
            )
        }

        composable(Routes.Chat.route) {
            ChatScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
