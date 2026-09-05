package com.umang620.offline_pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.umang620.offline_pos.data.local.PosDatabase
import com.umang620.offline_pos.data.repository.PosRepository
import com.umang620.offline_pos.ui.expenses.ExpensesScreen
import com.umang620.offline_pos.ui.expenses.ExpensesViewModel
import com.umang620.offline_pos.ui.inventory.InventoryScreen
import com.umang620.offline_pos.ui.inventory.InventoryViewModel
import com.umang620.offline_pos.ui.register.PosViewModel
import com.umang620.offline_pos.ui.register.RegisterScreen
import com.umang620.offline_pos.ui.sales.SalesHistoryScreen
import com.umang620.offline_pos.ui.sales.SalesViewModel
import com.umang620.offline_pos.ui.summary.DailySummaryScreen
import com.umang620.offline_pos.ui.summary.DailySummaryViewModel
import com.umang620.offline_pos.ui.theme.OfflinePOSTheme
import com.umang620.offline_pos.ui.unpaid.UnpaidOrdersScreen
import com.umang620.offline_pos.ui.unpaid.UnpaidOrdersViewModel

enum class PosTab(val title: String, val icon: ImageVector) {
    REGISTER("Register", Icons.Default.PointOfSale),
    UNPAID("Unpaid", Icons.Default.MoneyOff),
    SALES("Sales", Icons.AutoMirrored.Filled.TrendingUp),
    EXPENSES("Expenses", Icons.Default.Receipt),
    SUMMARY("Summary", Icons.Default.Assessment),
    INVENTORY("Inventory", Icons.Default.Inventory)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PosDatabase.getDatabase(applicationContext)
        val repository = PosRepository(
            productDao = database.productDao(),
            orderDao = database.orderDao(),
            expenseDao = database.expenseDao()
        )

        val posViewModel: PosViewModel by viewModels { PosViewModel.Factory(repository) }
        val unpaidOrdersViewModel: UnpaidOrdersViewModel by viewModels { UnpaidOrdersViewModel.Factory(repository) }
        val salesViewModel: SalesViewModel by viewModels { SalesViewModel.Factory(repository) }
        val expensesViewModel: ExpensesViewModel by viewModels { ExpensesViewModel.Factory(repository) }
        val dailySummaryViewModel: DailySummaryViewModel by viewModels { DailySummaryViewModel.Factory(repository) }
        val inventoryViewModel: InventoryViewModel by viewModels { InventoryViewModel.Factory(repository) }

        setContent {
            OfflinePOSTheme {
                MainAppScreen(
                    posViewModel = posViewModel,
                    unpaidOrdersViewModel = unpaidOrdersViewModel,
                    salesViewModel = salesViewModel,
                    expensesViewModel = expensesViewModel,
                    dailySummaryViewModel = dailySummaryViewModel,
                    inventoryViewModel = inventoryViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(
    posViewModel: PosViewModel,
    unpaidOrdersViewModel: UnpaidOrdersViewModel,
    salesViewModel: SalesViewModel,
    expensesViewModel: ExpensesViewModel,
    dailySummaryViewModel: DailySummaryViewModel,
    inventoryViewModel: InventoryViewModel
) {
    var selectedTab by remember { mutableStateOf(PosTab.REGISTER) }

    BoxWithConstraints {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    PosTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = (selectedTab == tab),
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    TabContent(selectedTab, innerPadding, posViewModel, unpaidOrdersViewModel, salesViewModel, expensesViewModel, dailySummaryViewModel, inventoryViewModel)
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        PosTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = (selectedTab == tab),
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = {
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                alwaysShowLabel = false // Helps fit 6 items on small screens
                            )
                        }
                    }
                }
            ) { innerPadding ->
                TabContent(selectedTab, innerPadding, posViewModel, unpaidOrdersViewModel, salesViewModel, expensesViewModel, dailySummaryViewModel, inventoryViewModel)
            }
        }
    }
}

@Composable
fun TabContent(
    selectedTab: PosTab,
    innerPadding: PaddingValues,
    posViewModel: PosViewModel,
    unpaidOrdersViewModel: UnpaidOrdersViewModel,
    salesViewModel: SalesViewModel,
    expensesViewModel: ExpensesViewModel,
    dailySummaryViewModel: DailySummaryViewModel,
    inventoryViewModel: InventoryViewModel
) {
    when (selectedTab) {
        PosTab.REGISTER -> {
            ScaffoldContentWrapper(innerPadding) {
                RegisterScreen(viewModel = posViewModel)
            }
        }
        PosTab.UNPAID -> {
            ScaffoldContentWrapper(innerPadding) {
                UnpaidOrdersScreen(viewModel = unpaidOrdersViewModel)
            }
        }
        PosTab.SALES -> {
            ScaffoldContentWrapper(innerPadding) {
                SalesHistoryScreen(viewModel = salesViewModel)
            }
        }
        PosTab.EXPENSES -> {
            ScaffoldContentWrapper(innerPadding) {
                ExpensesScreen(viewModel = expensesViewModel)
            }
        }
        PosTab.SUMMARY -> {
            ScaffoldContentWrapper(innerPadding) {
                DailySummaryScreen(viewModel = dailySummaryViewModel)
            }
        }
        PosTab.INVENTORY -> {
            ScaffoldContentWrapper(innerPadding) {
                InventoryScreen(viewModel = inventoryViewModel)
            }
        }
    }
}

@Composable
fun ScaffoldContentWrapper(
    paddingValues: PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        content()
    }
}
