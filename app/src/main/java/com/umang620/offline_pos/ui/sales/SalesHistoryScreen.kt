package com.umang620.offline_pos.ui.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.ui.theme.DangerRed
import com.umang620.offline_pos.ui.theme.GlassCard
import com.umang620.offline_pos.ui.theme.SecondaryText
import com.umang620.offline_pos.ui.theme.SuccessGreen
import com.umang620.offline_pos.utils.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(viewModel: SalesViewModel) {
    val orders by viewModel.ordersWithItems.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalOrders by viewModel.totalOrderCount.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var orderToVoid by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderToDelete by remember { mutableStateOf<OrderWithItems?>(null) }

    val isToday = remember(selectedDate) {
        val todayStart = SalesViewModel.getStartOfDay(System.currentTimeMillis())
        SalesViewModel.getStartOfDay(selectedDate) == todayStart
    }

    val dateLabel = remember(selectedDate, isToday) {
        val format = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        if (isToday) "Today (${format.format(Date(selectedDate))})" else format.format(Date(selectedDate))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        viewModel.updateSelectedDate(timestamp)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sales Log & Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select Date",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isToday) "Today" else "Filter Date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // KPI Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Total Revenue",
                value = formatMoney(totalRevenue),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Paid Orders",
                value = "$totalOrders",
                icon = Icons.Default.ShoppingBag,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transaction History (${orders.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isToday) "No sales recorded yet today." else "No sales records for this date.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(orders, key = { it.order.id }) { orderWithItems ->
                    OrderCard(
                        orderWithItems = orderWithItems,
                        onVoid = { orderToVoid = orderWithItems },
                        onDelete = { orderToDelete = orderWithItems }
                    )
                }
            }
        }
    }

    orderToVoid?.let { orderWithItems ->
        AlertDialog(
            onDismissRequest = { orderToVoid = null },
            title = { Text("Void Order #${orderWithItems.order.id}", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to VOID this transaction? It will remain in history as VOIDED but excluded from sales totals.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidOrder(orderWithItems.order.id)
                        orderToVoid = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm Void", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToVoid = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    orderToDelete?.let { orderWithItems ->
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Permanently Delete Order #${orderWithItems.order.id}", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to PERMANENTLY DELETE this transaction record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOrder(orderWithItems.order.id)
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Permanently Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    GlassCard(
        modifier = modifier,
        onClick = { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OrderCard(
    orderWithItems: OrderWithItems,
    onVoid: () -> Unit,
    onDelete: () -> Unit
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    val order = orderWithItems.order
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(order.timestamp) { dateFormat.format(Date(order.timestamp)) }
    val isVoided = order.status == "VOIDED"
    val isUnpaid = order.status == "UNPAID"
    val orderTitle = if (order.orderNumber.isNotBlank()) order.orderNumber else "Order #${order.id}"

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            title = {
                Text(
                    text = "Order Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = "Order Name/ID: $orderTitle", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: ${order.status}", 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = when {
                            isVoided -> SecondaryText
                            isUnpaid -> Color(0xFFB45309)
                            else -> SuccessGreen
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Date: $formattedDate", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Items: ${order.totalItems}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Payment Method: ${order.paymentMethod}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Amount: ${formatMoney(order.totalAmount)}", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (order.cashReceived != null && order.changeAmount != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Cash Received: ${formatMoney(order.cashReceived)}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Change: ${formatMoney(order.changeAmount)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!order.gcashRefNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "GCash Ref No: ${order.gcashRefNumber}", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Receipt Items:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    orderWithItems.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatMoney(item.subtotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                if (!isVoided) {
                    OutlinedButton(
                        onClick = {
                            showDetailsDialog = false
                            onVoid()
                        },
                        border = BorderStroke(1.dp, DangerRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Void", color = DangerRed)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    showDetailsDialog = false
                    onDelete()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Record", tint = SecondaryText)
                }
            }
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDetailsDialog = true },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = when {
            isVoided -> Color(0xFFF1F5F9).copy(alpha = 0.85f)
            isUnpaid -> Color(0xFFFEF3C7).copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        },
        borderColor = when {
            isVoided -> Color(0xFFCBD5E1)
            isUnpaid -> Color(0xFFFDE68A)
            else -> Color.White.copy(alpha = 0.85f)
        },
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = when {
                            isVoided -> SecondaryText
                            isUnpaid -> Color(0xFFB45309)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = orderTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isUnpaid) Color(0xFF451A03) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        when {
                                            isVoided -> Color(0xFFE2E8F0)
                                            isUnpaid -> Color(0xFFFDE68A)
                                            else -> Color(0xFFDCFCE7)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = order.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isVoided -> SecondaryText
                                        isUnpaid -> Color(0xFF78350F)
                                        else -> SuccessGreen
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUnpaid) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(order.totalAmount),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            isVoided -> SecondaryText
                            isUnpaid -> Color(0xFF451A03)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.paymentMethod} • ${order.totalItems} item${if (order.totalItems > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnpaid) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
