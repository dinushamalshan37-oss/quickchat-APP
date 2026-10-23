package com.buddy.quickchat.ui.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")
    object Home : Routes("home")
    object Chat : Routes("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object ScanQr : Routes("scan_qr")
    object MyQr : Routes("my_qr")
}
