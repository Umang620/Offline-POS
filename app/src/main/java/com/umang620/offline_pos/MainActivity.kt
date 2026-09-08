package com.umang620.offline_pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
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
import com.umang620.offline_pos.ui.settings.SettingsScreen
import com.umang620.offline_pos.ui.settings.SettingsViewModel
import com.umang620.offline_pos.ui.summary.DailySummaryScreen
import com.umang620.offline_pos.ui.summary.DailySummaryViewModel
import com.umang620.offline_pos.ui.theme.OfflinePOSTheme
import com.umang620.offline_pos.ui.unpaid.UnpaidOrdersScreen
import com.umang620.offline_pos.ui.unpaid.UnpaidOrdersViewModel
import com.umang620.offline_pos.utils.formatMoney

enum class PosTab(val title: String, val icon: ImageVector) {
    REGISTER("Register", Icons.Default.PointOfSale),
    UNPAID("Unpaid", Icons.Default.MoneyOff),
    SALES("Sales", Icons.AutoMirrored.Filled.TrendingUp),
    EXPENSES("Expenses", Icons.Default.Receipt),
    SUMMARY("Summary", Icons.Default.Assessment),
    INVENTORY("Inventory", Icons.Default.Inventory),
    SETTINGS("Settings", Icons.Default.Settings)
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
        val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory(applicationContext, repository) }

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
                var showSplashScreen by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplashScreen,
                    animationSpec = tween(durationMillis = 400),
                    label = "splashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onSplashFinished = { showSplashScreen = false }
                        )
                    } else {
                        MainAppScreen(
                            posViewModel = posViewModel,
                            unpaidOrdersViewModel = unpaidOrdersViewModel,
                            salesViewModel = salesViewModel,
                            expensesViewModel = expensesViewModel,
                            dailySummaryViewModel = dailySummaryViewModel,
                            inventoryViewModel = inventoryViewModel,
                            settingsViewModel = settingsViewModel,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = toggleDarkMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 750,
            easing = FastOutSlowInEasing
        ),
        label = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = tween(
            durationMillis = 750,
            easing = FastOutSlowInEasing
        ),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1100)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer(
                alpha = alphaAnim,
                scaleX = scaleAnim,
                scaleY = scaleAnim
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.umnx_logo),
                contentDescription = "UMNX Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Offline POS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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
    settingsViewModel: SettingsViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(PosTab.REGISTER) }
    val todayRevenue by salesViewModel.todayRevenue.collectAsState()

    BoxWithConstraints {
        val isWideScreen = maxWidth > 600.dp || maxHeight < 450.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NavigationRail(
                        containerColor = Color.Transparent,
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
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopHeaderBar(
                            todayRevenue = todayRevenue,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = onToggleDarkMode
                        )
                    }
                ) { innerPadding ->
                    TabContent(
                        selectedTab = selectedTab,
                        innerPadding = innerPadding,
                        posViewModel = posViewModel,
                        unpaidOrdersViewModel = unpaidOrdersViewModel,
                        salesViewModel = salesViewModel,
                        expensesViewModel = expensesViewModel,
                        dailySummaryViewModel = dailySummaryViewModel,
                        inventoryViewModel = inventoryViewModel,
                        settingsViewModel = settingsViewModel,
                        onSelectTab = { selectedTab = it }
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopHeaderBar(
                        todayRevenue = todayRevenue,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                    ) {
                        NavigationBar(
                            modifier = Modifier.width((PosTab.entries.size * 72).dp),
                            containerColor = Color.Transparent,
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
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                TabContent(
                    selectedTab = selectedTab,
                    innerPadding = innerPadding,
                    posViewModel = posViewModel,
                    unpaidOrdersViewModel = unpaidOrdersViewModel,
                    salesViewModel = salesViewModel,
                    expensesViewModel = expensesViewModel,
                    dailySummaryViewModel = dailySummaryViewModel,
                    inventoryViewModel = inventoryViewModel,
                    settingsViewModel = settingsViewModel,
                    onSelectTab = { selectedTab = it }
                )
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
    inventoryViewModel: InventoryViewModel,
    settingsViewModel: SettingsViewModel,
    onSelectTab: (PosTab) -> Unit = {}
) {
    when (selectedTab) {
        PosTab.REGISTER -> {
            ScaffoldContentWrapper(innerPadding) {
                RegisterScreen(viewModel = posViewModel)
            }
        }
        PosTab.UNPAID -> {
            ScaffoldContentWrapper(innerPadding) {
                UnpaidOrdersScreen(
                    viewModel = unpaidOrdersViewModel,
                    onEditOrder = { orderWithItems ->
                        posViewModel.loadUnpaidOrderForEditing(orderWithItems)
                        onSelectTab(PosTab.REGISTER)
                    }
                )
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
        PosTab.SETTINGS -> {
            ScaffoldContentWrapper(innerPadding) {
                SettingsScreen(viewModel = settingsViewModel)
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

@Composable
fun TopHeaderBar(
    todayRevenue: Double,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.umnx_logo),
                contentDescription = "POS Logo",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Offline POS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Today: ${formatMoney(todayRevenue)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
