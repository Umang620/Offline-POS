package com.umang620.offline_pos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("pos_settings", MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", false)) }

            val toggleDarkMode = {
                val newMode = !isDarkMode
                isDarkMode = newMode
                prefs.edit().putBoolean("is_dark_mode", newMode).apply()
            }

            OfflinePOSTheme(darkTheme = isDarkMode) {
                MainAppScreen(
                    posViewModel = posViewModel,
                    unpaidOrdersViewModel = unpaidOrdersViewModel,
                    salesViewModel = salesViewModel,
                    expensesViewModel = expensesViewModel,
                    dailySummaryViewModel = dailySummaryViewModel,
                    inventoryViewModel = inventoryViewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = toggleDarkMode
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
    inventoryViewModel: InventoryViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(PosTab.REGISTER) }

    BoxWithConstraints {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                ) {
                    PosTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Compact Light/Dark Mode Toggle beside/below Inventory in Sidebar
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Light Mode" else "Dark Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    TabContent(selectedTab, innerPadding, posViewModel, unpaidOrdersViewModel, salesViewModel, expensesViewModel, dailySummaryViewModel, inventoryViewModel)
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        tonalElevation = 6.dp
                    ) {
                        PosTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = {
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                alwaysShowLabel = false
                            )
                        }

                        // Compact Light / Dark toggle in bottom bar on phone view
                        NavigationBarItem(
                            selected = false,
                            onClick = onToggleDarkMode,
                            icon = {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                    contentDescription = if (isDarkMode) "Light Mode" else "Dark Mode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = {
                                Text(
                                    text = if (isDarkMode) "Light" else "Dark",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            alwaysShowLabel = false
                        )
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
