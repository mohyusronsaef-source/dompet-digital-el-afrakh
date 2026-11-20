package com.elafrakh.dompet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.ui.MainViewModel
import com.elafrakh.dompet.ui.navigation.BottomNavItems
import com.elafrakh.dompet.ui.navigation.Screen
import com.elafrakh.dompet.ui.screens.AddExpenseScreen
import com.elafrakh.dompet.ui.screens.AddIncomeScreen
import com.elafrakh.dompet.ui.screens.DashboardScreen
import com.elafrakh.dompet.ui.screens.HistoryScreen
import com.elafrakh.dompet.ui.screens.InitialBalanceScreen
import com.elafrakh.dompet.ui.screens.PinScreen
import com.elafrakh.dompet.ui.screens.ReportScreen
import com.elafrakh.dompet.ui.screens.SettingsScreen
import com.elafrakh.dompet.ui.screens.SplashScreen
import com.elafrakh.dompet.ui.theme.DompetElAfrakhTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            DompetElAfrakhTheme(darkTheme = isDarkMode) {
                DompetElAfrakhRoot(viewModel = viewModel, isDarkTheme = isDarkMode)
            }
        }
    }
}

@Composable
private fun DompetElAfrakhRoot(
    viewModel: MainViewModel,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val balanceSummary by viewModel.balanceSummary.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val last7DaysBuckets by viewModel.last7DaysBuckets.collectAsStateWithLifecycle()
    val hasPinSetup by viewModel.hasPinSetup.collectAsStateWithLifecycle()
    val hasInitialBalanceSetup by viewModel.hasInitialBalanceSetup.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    var splashFinished by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }

    // Auto-lock when app goes to background or screen locks (with exemption for system camera/gallery/SAF pickers)
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.lockOnBackground()
                Lifecycle.Event.ON_RESUME -> viewModel.clearAutoLockSuppression()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Enforce PIN lock screen whenever app becomes locked after splash
    LaunchedEffect(splashFinished, isUnlocked, hasPinSetup) {
        if (splashFinished && hasPinSetup && !isUnlocked && currentRoute != Screen.Pin.route) {
            navController.navigate(Screen.Pin.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val bottomNavRoutes = remember {
        setOf(
            Screen.Dashboard.route,
            Screen.AddIncome.route,
            Screen.AddExpense.route,
            Screen.History.route,
            Screen.Report.route
        )
    }
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    BottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onFinished = {
                            splashFinished = true
                            navController.navigate(Screen.Pin.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Pin.route) {
                    PinScreen(
                        isPinConfigured = hasPinSetup,
                        onSetupPin = { pin -> viewModel.setupFirstPin(pin) },
                        onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                        onResetPinWithRecovery = { code, newPin ->
                            viewModel.resetPinWithRecoveryCode(code, newPin)
                        },
                        onResetAppData = { viewModel.resetAllData() },
                        onPinSuccess = {
                            val nextRoute = if (hasInitialBalanceSetup) {
                                Screen.Dashboard.route
                            } else {
                                Screen.InitialBalance.route
                            }
                            navController.navigate(nextRoute) {
                                popUpTo(Screen.Pin.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.InitialBalance.route) {
                    InitialBalanceScreen(
                        onSaveInitialBalance = { amount ->
                            viewModel.saveInitialBalance(amount)
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.InitialBalance.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        summary = balanceSummary,
                        last7DaysBuckets = last7DaysBuckets,
                        recentTransactions = allTransactions,
                        isDarkTheme = isDarkTheme,
                        receiptStore = viewModel.receiptStore,
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onQuickAddIncome = {
                            navController.navigate(Screen.AddIncome.route)
                        },
                        onQuickAddExpense = {
                            navController.navigate(Screen.AddExpense.route)
                        },
                        onViewAllHistory = {
                            navController.navigate(Screen.History.route)
                        },
                        onTransactionClick = { tx ->
                            selectedTransactionForDetail = tx
                            navController.navigate(Screen.History.route)
                        }
                    )
                }

                composable(Screen.AddIncome.route) {
                    AddIncomeScreen(
                        isDarkTheme = isDarkTheme,
                        onBack = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onSaveIncome = { amount, note ->
                            viewModel.addIncome(amount, note)
                        },
                        onShowMessage = ::showMessage
                    )
                }

                composable(Screen.AddExpense.route) {
                    AddExpenseScreen(
                        currentBalance = balanceSummary.currentBalance,
                        isDarkTheme = isDarkTheme,
                        receiptStore = viewModel.receiptStore,
                        onSuppressAutoLock = { viewModel.suppressNextAutoLock() },
                        onBack = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onSaveExpense = { amount, note, uri ->
                            viewModel.addExpense(amount, note, uri)
                        },
                        onShowMessage = ::showMessage
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        transactions = allTransactions,
                        isDarkTheme = isDarkTheme,
                        receiptStore = viewModel.receiptStore,
                        initialSelectedTransaction = selectedTransactionForDetail,
                        onClearInitialSelected = { selectedTransactionForDetail = null },
                        onSuppressAutoLock = { viewModel.suppressNextAutoLock() },
                        onBack = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onUpdateTransaction = { existing, newAmount, newNote, newReceiptUri, removeExisting ->
                            viewModel.updateTransaction(
                                existing = existing,
                                newAmount = newAmount,
                                newNote = newNote,
                                newReceiptUri = newReceiptUri,
                                removeExistingReceipt = removeExisting
                            )
                        },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onShowMessage = ::showMessage
                    )
                }

                composable(Screen.Report.route) {
                    ReportScreen(
                        transactions = allTransactions,
                        isDarkTheme = isDarkTheme,
                        receiptStore = viewModel.receiptStore,
                        onSuppressAutoLock = { viewModel.suppressNextAutoLock() },
                        onBack = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onExportPdf = { uri, report ->
                            viewModel.exportPdf(context, uri, report)
                        },
                        onExportXlsx = { uri, report ->
                            viewModel.exportXlsx(context, uri, report)
                        },
                        onTransactionClick = { tx ->
                            selectedTransactionForDetail = tx
                            navController.navigate(Screen.History.route)
                        },
                        onShowMessage = ::showMessage
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        initialBalance = balanceSummary.initialBalance,
                        isDarkTheme = isDarkTheme,
                        onBack = { navController.popBackStack() },
                        onToggleDarkMode = { enabled -> viewModel.setDarkMode(enabled) },
                        onChangePin = { oldPin, newPin -> viewModel.changePin(oldPin, newPin) },
                        onUpdateInitialBalance = { amount -> viewModel.saveInitialBalance(amount) },
                        onSuppressAutoLock = { viewModel.suppressNextAutoLock() },
                        onCreateBackup = { uri -> viewModel.createBackupZip(context, uri) },
                        onRestoreBackup = { uri -> viewModel.restoreBackupZip(context, uri) },
                        onShowMessage = ::showMessage
                    )
                }
            }
        }
    }
}
