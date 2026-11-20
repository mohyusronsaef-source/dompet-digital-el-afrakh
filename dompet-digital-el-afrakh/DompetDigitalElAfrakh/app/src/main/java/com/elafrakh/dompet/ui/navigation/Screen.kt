package com.elafrakh.dompet.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Pin : Screen("pin")
    data object InitialBalance : Screen("initial_balance")
    data object Dashboard : Screen("dashboard")
    data object AddIncome : Screen("add_income")
    data object AddExpense : Screen("add_expense")
    data object History : Screen("history")
    data object Report : Screen("report")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Beranda", Icons.Rounded.Home),
    BottomNavItem(Screen.AddIncome, "Pemasukan", Icons.Rounded.ArrowUpward),
    BottomNavItem(Screen.AddExpense, "Pengeluaran", Icons.Rounded.ArrowDownward),
    BottomNavItem(Screen.History, "Riwayat", Icons.Rounded.History),
    BottomNavItem(Screen.Report, "Laporan", Icons.Rounded.Assessment)
)
