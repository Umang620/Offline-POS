package com.umang620.offline_pos.ui.register

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.umang620.offline_pos.data.local.AddOnEntity
import com.umang620.offline_pos.utils.BluetoothPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umang620.offline_pos.data.local.ProductEntity
import com.umang620.offline_pos.domain.model.CartItem
import com.umang620.offline_pos.ui.theme.GlassCard
import com.umang620.offline_pos.ui.theme.GlassSurface
import com.umang620.offline_pos.ui.theme.SuccessGreen
import com.umang620.offline_pos.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import com.umang620.offline_pos.utils.formatMoney
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val products by viewModel.filteredProducts.collectAsState(initial = emptyList())
    val activeAddOns by viewModel.activeAddOns.collectAsState(initial = emptyList())
    val cartItems by viewModel.cartItems.collectAsState()
    val editingOrder by viewModel.editingOrder.collectAsState()
    val orderNameState by viewModel.orderName.collectAsState()

    val isAddOnsEnabled by viewModel.isAddOnsEnabled.collectAsState()
    val isOrderTypesEnabled by viewModel.isOrderTypesEnabled.collectAsState()
    val isSurchargeEnabled by viewModel.isSurchargeEnabled.collectAsState()
    val surchargeNamePref by viewModel.surchargeName.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showNoPrinterDialog by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var selectedProductForAddOns by remember { mutableStateOf<ProductEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val totalAmount = cartItems.sumOf { it.subtotal }
    val totalItemCount = cartItems.sumOf { it.quantity }

    LaunchedEffect(Unit) {
        viewModel.checkoutEvent.collect { event ->
            when (event) {
                is CheckoutEvent.Success -> {
                    showCheckoutDialog = false
                    showCartSheet = false
                    val msg = if (event.isUnpaid) "Order #${event.orderId} Saved as UNPAID" else "Order #${event.orderId} Completed!"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                    val prefs = context.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)
                    val autoPrint = prefs.getBoolean("is_auto_print_enabled", true)
                    val address = prefs.getString("selected_printer_address", "") ?: ""

                    if (autoPrint) {
                        if (address.isBlank()) {
                            showNoPrinterDialog = true
                        } else {
                            scope.launch(Dispatchers.IO) {
                                val orderWithItems = viewModel.getOrderWithItemsById(event.orderId)
                                if (orderWithItems != null) {
                                    val result = BluetoothPrinterHelper.printSalesReceipt(context, orderWithItems)
                                    if (result.isFailure) {
                                        withContext(Dispatchers.Main) {
                                            showNoPrinterDialog = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is CheckoutEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (cartItems.isNotEmpty()) 84.dp else 0.dp)
        ) {
            if (editingOrder != null) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = WarningOrange.copy(alpha = 0.15f),
                    borderColor = WarningOrange.copy(alpha = 0.5f),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Editing Unpaid Order",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = editingOrder?.order?.orderNumber?.ifBlank { "Order #${editingOrder?.order?.id}" } ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { viewModel.cancelEditingOrder() }
                        ) {
                            Text("Cancel Edit", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Header / Search Bar
            SurfaceHeader(
                searchQuery = searchQuery,
                isGridView = isGridView,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onClearSearch = { viewModel.updateSearchQuery("") },
                onToggleLayout = { isGridView = !isGridView }
            )

            // Category Chips
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All Products", fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Product Catalog (Grid or List View)
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching products found" else "No products available in inventory",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 145.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onAddToCart = {
                                if (isAddOnsEnabled && activeAddOns.isNotEmpty()) {
                                    selectedProductForAddOns = product
                                } else {
                                    viewModel.addToCart(product)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductListRow(
                            product = product,
                            onAddToCart = {
                                if (isAddOnsEnabled && activeAddOns.isNotEmpty()) {
                                    selectedProductForAddOns = product
                                } else {
                                    viewModel.addToCart(product)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Cart Summary Floating Bar
        if (cartItems.isNotEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                elevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCartSheet = true }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "View Cart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$totalItemCount item${if (totalItemCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = { showCheckoutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCartSheet) {
        CartBottomSheet(
            cartItems = cartItems,
            sheetState = sheetState,
            onDismiss = { showCartSheet = false },
            onUpdateQuantity = { id, delta -> viewModel.updateCartQuantity(id, delta) },
            onRemoveItem = { id -> viewModel.removeFromCart(id) },
            onClearCart = { viewModel.clearCart() },
            onProceedToPayment = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showCartSheet = false
                    showCheckoutDialog = true
                }
            }
        )
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            totalAmount = totalAmount,
            initialOrderName = orderNameState,
            isEditing = editingOrder != null,
            isOrderTypesEnabled = isOrderTypesEnabled,
            isSurchargeEnabled = isSurchargeEnabled,
            surchargeName = surchargeNamePref,
            onDismiss = { showCheckoutDialog = false },
            onConfirmPayment = { method, isUnpaid, cashReceived, changeAmount, gcashRef, customerName, orderType, surchargeFee, surchargeName ->
                viewModel.processCheckout(
                    paymentMethod = method,
                    isUnpaid = isUnpaid,
                    cashReceived = cashReceived,
                    changeAmount = changeAmount,
                    gcashRefNumber = gcashRef,
                    customerName = customerName,
                    orderType = orderType,
                    surchargeFee = surchargeFee,
                    surchargeName = surchargeName
                )
            }
        )
    }

    selectedProductForAddOns?.let { product ->
        SelectAddOnsDialog(
            product = product,
            availableAddOns = activeAddOns,
            onDismiss = { selectedProductForAddOns = null },
            onConfirm = { selectedAddOns, itemNote ->
                viewModel.addToCart(product, selectedAddOns, itemNote)
                selectedProductForAddOns = null
            }
        )
    }

    if (showNoPrinterDialog) {
        AlertDialog(
            onDismissRequest = { showNoPrinterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("No Thermal Printer Connected", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-Print is enabled, but no thermal printer is connected or selected.")
                    Text("Would you like to turn off Auto-Print to stop this notification?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_auto_print_enabled", false).apply()
                        showNoPrinterDialog = false
                        Toast.makeText(context, "Auto-Print Disabled", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Turn Off Auto-Print", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoPrinterDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun SurfaceHeader(
    searchQuery: String,
    isGridView: Boolean,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleLayout: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Point of Sale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onToggleLayout) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search product name or category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    }
}

fun getCategoryColor(category: String): Color {
    val hash = Math.abs(category.hashCode())
    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF3B82F6), // Blue
        Color(0xFF06B6D4)  // Cyan
    )
    return colors[hash % colors.size]
}

@Composable
fun ProductListRow(
    product: ProductEntity,
    onAddToCart: () -> Unit
) {
    val catColor = remember(product.category) { getCategoryColor(product.category) }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (product.stockQuantity > 0) onAddToCart else null,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(catColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.category}${if (product.sku.isNotBlank()) " • ${product.sku}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (product.stockQuantity > 0) "Stock ${formatStockDisplay(product.stockQuantity)}" else "Out of stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stockQuantity > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = { if (product.stockQuantity > 0) onAddToCart() },
                    enabled = product.stockQuantity > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (product.stockQuantity > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

fun formatStockDisplay(quantity: Int): String {
    return when {
        quantity >= 1_000_000 -> String.format(Locale.US, "%.1fM", quantity / 1_000_000.0)
        quantity >= 100_000 -> "${quantity / 1000}k"
        else -> quantity.toString()
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onAddToCart: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (product.stockQuantity > 0) onAddToCart else null,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = product.category,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text(
                    text = formatMoney(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (product.stockQuantity > 0) "Stock ${formatStockDisplay(product.stockQuantity)}" else "Out of stock",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (product.stockQuantity > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearCart) {
                    Text("Clear Cart", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(cartItems) { index, item ->
                    CartItemRow(
                        cartItem = item,
                        onIncrease = { onUpdateQuantity(index, 1) },
                        onDecrease = { onUpdateQuantity(index, -1) },
                        onRemove = { onRemoveItem(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalAmount = cartItems.sumOf { it.subtotal }
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = formatMoney(totalAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onProceedToPayment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Proceed to Checkout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cartItem.product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                if (cartItem.addOnsSummary.isNotBlank()) {
                    Text(
                        cartItem.addOnsSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${formatMoney(cartItem.unitPriceWithAddOns)} × ${cartItem.quantity} = ${formatMoney(cartItem.subtotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }
                Text("${cartItem.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    totalAmount: Double,
    initialOrderName: String = "",
    isEditing: Boolean = false,
    isOrderTypesEnabled: Boolean = true,
    isSurchargeEnabled: Boolean = true,
    surchargeName: String = "Delivery Fee",
    onDismiss: () -> Unit,
    onConfirmPayment: (
        paymentMethod: String,
        isUnpaid: Boolean,
        cashReceived: Double?,
        changeAmount: Double?,
        gcashRef: String?,
        customerName: String,
        orderType: String?,
        surchargeFee: Double,
        surchargeName: String
    ) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Cash") }
    var selectedOrderType by remember { mutableStateOf("Dine In") }
    var surchargeFeeStr by remember { mutableStateOf("") }
    var cashReceivedStr by remember { mutableStateOf("") }
    var gcashRefStr by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf(initialOrderName) }

    val surchargeFee = if (isSurchargeEnabled && selectedOrderType == "Delivery") (surchargeFeeStr.toDoubleOrNull() ?: 0.0) else 0.0
    val grandTotal = totalAmount + surchargeFee
    val cashReceived = cashReceivedStr.toDoubleOrNull() ?: 0.0
    val changeAmount = cashReceived - grandTotal
    val isCashValid = selectedMethod != "Cash" || cashReceived >= grandTotal

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            borderColor = Color.White.copy(alpha = 0.90f),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Update Order / Checkout" else "Complete Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer / Order Name (Optional)") },
                    placeholder = { Text("e.g. Table 5, John, Drive-Thru...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Table / Location Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Table 1", "Table 2", "Table 3", "Table 4", "Table 5", "Take Out #1", "Drive-Thru").forEach { loc ->
                        OutlinedButton(
                            onClick = { customerName = loc },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(loc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Order Type Selector Section
                if (isOrderTypesEnabled) {
                    Text(
                        text = "Order Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Dine In", "Take Out", "Delivery").forEach { type ->
                            FilterChip(
                                selected = selectedOrderType == type,
                                onClick = { selectedOrderType = type },
                                label = { Text(type, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (isSurchargeEnabled && selectedOrderType == "Delivery") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = surchargeFeeStr,
                            onValueChange = { surchargeFeeStr = it },
                            label = { Text("$surchargeName (₱)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Discount Selector Section
                var selectedDiscountType by remember { mutableStateOf("None") }
                var customDiscountStr by remember { mutableStateOf("") }

                val discountAmount = when (selectedDiscountType) {
                    "Senior / PWD (20%)" -> totalAmount * 0.20
                    "10% Off" -> totalAmount * 0.10
                    "Custom ₱" -> (customDiscountStr.toDoubleOrNull() ?: 0.0).coerceAtMost(totalAmount)
                    else -> 0.0
                }

                Text(
                    text = "Apply Discount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("None", "Senior / PWD (20%)", "10% Off", "Custom ₱").forEach { disc ->
                        FilterChip(
                            selected = selectedDiscountType == disc,
                            onClick = { selectedDiscountType = disc },
                            label = { Text(disc, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (selectedDiscountType == "Custom ₱") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDiscountStr,
                        onValueChange = { customDiscountStr = it },
                        label = { Text("Custom Discount Amount (₱)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val grandTotal = (totalAmount + surchargeFee - discountAmount).coerceAtLeast(0.0)

                // Total Due Section
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    elevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Due",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatMoney(grandTotal),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (discountAmount > 0 || surchargeFee > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Subtotal ${formatMoney(totalAmount)}${if (discountAmount > 0) " • Discount -${formatMoney(discountAmount)}" else ""}${if (surchargeFee > 0) " • $surchargeName +${formatMoney(surchargeFee)}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Section
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMethod == "Cash",
                        onClick = { selectedMethod = "Cash" },
                        label = { Text("Cash", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedMethod == "GCash",
                        onClick = { selectedMethod = "GCash" },
                        label = { Text("GCash", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedMethod == "Cash") {
                    OutlinedTextField(
                        value = cashReceivedStr,
                        onValueChange = { cashReceivedStr = it },
                        label = { Text("Cash Received (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Cash Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(grandTotal, 100.0, 200.0, 500.0, 1000.0).forEach { amount ->
                            if (amount >= grandTotal) {
                                OutlinedButton(
                                    onClick = { cashReceivedStr = String.format(Locale.US, "%.0f", amount) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (amount == grandTotal) "Exact (${formatMoney(grandTotal)})" else "₱${amount.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cashReceivedStr.isNotBlank()) {
                        if (changeAmount >= 0) {
                            GlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = Color(0xFFDCFCE7).copy(alpha = 0.9f),
                                borderColor = SuccessGreen.copy(alpha = 0.5f),
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Change Due", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                        Text(
                                            text = formatMoney(changeAmount),
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        } else {
                            GlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                elevation = 1.dp
                            ) {
                                Text(
                                    text = "Short by ${formatMoney(-changeAmount)}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = gcashRefStr,
                        onValueChange = { gcashRefStr = it },
                        label = { Text("GCash Reference No. (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onConfirmPayment(
                            selectedMethod,
                            false,
                            if (selectedMethod == "Cash") cashReceived else null,
                            if (selectedMethod == "Cash") changeAmount else null,
                            if (selectedMethod == "GCash") gcashRefStr.ifBlank { null } else null,
                            customerName.trim(),
                            if (isOrderTypesEnabled) selectedOrderType else null,
                            surchargeFee,
                            surchargeName
                        )
                    },
                    enabled = isCashValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isEditing) "Complete & Pay Order" else "Complete Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onConfirmPayment(
                            "Unpaid",
                            true,
                            null,
                            null,
                            null,
                            customerName.trim(),
                            if (isOrderTypesEnabled) selectedOrderType else null,
                            surchargeFee,
                            surchargeName
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, WarningOrange)
                ) {
                    Text(
                        text = if (isEditing) "Update Unpaid Order" else "Save as UNPAID",
                        color = WarningOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SelectAddOnsDialog(
    product: ProductEntity,
    availableAddOns: List<AddOnEntity>,
    initialSelectedAddOns: List<AddOnEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (selectedAddOns: List<AddOnEntity>, itemNote: String) -> Unit
) {
    val addOnQuantityMap = remember {
        val map = mutableStateMapOf<Long, Int>()
        initialSelectedAddOns.groupBy { it.id }.forEach { (id, list) ->
            map[id] = list.size
        }
        map
    }

    var itemNote by remember { mutableStateOf("") }
    var addOnSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(availableAddOns) {
        availableAddOns.map { it.category }.distinct().sorted()
    }

    val filteredAddOns = remember(availableAddOns, addOnSearchQuery, selectedCategory) {
        availableAddOns.filter { addOn ->
            val matchesQuery = addOnSearchQuery.isBlank() ||
                    addOn.name.contains(addOnSearchQuery, ignoreCase = true) ||
                    addOn.category.contains(addOnSearchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || addOn.category.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    val totalAddOnsCount = addOnQuantityMap.values.sum()
    val addOnsTotal = addOnQuantityMap.entries.sumOf { (id, qty) ->
        val price = availableAddOns.find { it.id == id }?.price ?: 0.0
        price * qty
    }
    val itemTotal = product.price + addOnsTotal

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            borderColor = Color.White.copy(alpha = 0.90f),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customize Item",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = formatMoney(product.price),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar for Add-Ons
                OutlinedTextField(
                    value = addOnSearchQuery,
                    onValueChange = { addOnSearchQuery = it },
                    placeholder = { Text("Search add-ons or toppings...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Add-Ons") },
                    trailingIcon = {
                        if (addOnSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { addOnSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Category Filter Chips
                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                                label = { Text(category, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Add-Ons List with Quantity Stepper Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (filteredAddOns.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (addOnSearchQuery.isNotBlank()) "No add-ons matching \"$addOnSearchQuery\"" else "No add-ons available in this category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        filteredAddOns.forEach { addOn ->
                            val currentQty = addOnQuantityMap[addOn.id] ?: 0
                            val isSelected = currentQty > 0
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                },
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = addOn.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${addOn.category}${if (addOn.price > 0) " • +${formatMoney(addOn.price)} each" else " • Free"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    // Stepper Controls (- qty +)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (currentQty > 0) {
                                            IconButton(
                                                onClick = {
                                                    val newQty = currentQty - 1
                                                    if (newQty <= 0) addOnQuantityMap.remove(addOn.id)
                                                    else addOnQuantityMap[addOn.id] = newQty
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease Add-On", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$currentQty",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                addOnQuantityMap[addOn.id] = currentQty + 1
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase Add-On", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = itemNote,
                        onValueChange = { itemNote = it },
                        label = { Text("Item Note / Special Instructions (Optional)") },
                        placeholder = { Text("e.g. Extra hot, less sugar, no ice") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Total Price Summary & Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (totalAddOnsCount > 0) "$totalAddOnsCount Add-On${if (totalAddOnsCount > 1) "s" else ""} selected" else "Base Price",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMoney(itemTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                val selectedAddOnsList = mutableListOf<AddOnEntity>()
                                addOnQuantityMap.forEach { (addOnId, qty) ->
                                    val foundAddOn = availableAddOns.find { it.id == addOnId }
                                    if (foundAddOn != null && qty > 0) {
                                        repeat(qty) { selectedAddOnsList.add(foundAddOn) }
                                    }
                                }
                                onConfirm(selectedAddOnsList, itemNote)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to Cart", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
