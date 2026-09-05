package com.umang620.offline_pos.ui.unpaid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.umang620.offline_pos.data.local.OrderWithItems
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UnpaidOrdersScreen(viewModel: UnpaidOrdersViewModel) {
    val unpaidOrders by viewModel.unpaidOrders.collectAsState()
    val unpaidTotal by viewModel.unpaidTotal.collectAsState()

    var selectedOrderToPay by remember { mutableStateOf<OrderWithItems?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Unpaid Orders",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Unpaid Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "₱%.2f", unpaidTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoneyOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (unpaidOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No unpaid orders found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(unpaidOrders, key = { it.order.id }) { orderWithItems ->
                    UnpaidOrderCard(
                        orderWithItems = orderWithItems,
                        onPayClick = { selectedOrderToPay = orderWithItems }
                    )
                }
            }
        }
    }

    selectedOrderToPay?.let { orderWithItems ->
        PayUnpaidOrderDialog(
            orderWithItems = orderWithItems,
            onDismiss = { selectedOrderToPay = null },
            onConfirmPay = { paymentMethod, cashReceived, changeAmount, gcashRef ->
                viewModel.payUnpaidOrder(
                    orderId = orderWithItems.order.id,
                    paymentMethod = paymentMethod,
                    cashReceived = cashReceived,
                    changeAmount = changeAmount,
                    gcashRefNumber = gcashRef
                )
                selectedOrderToPay = null
            }
        )
    }
}

@Composable
fun UnpaidOrderCard(
    orderWithItems: OrderWithItems,
    onPayClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val order = orderWithItems.order
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(order.timestamp) { dateFormat.format(Date(order.timestamp)) }
    val orderTitle = if (order.orderNumber.isNotBlank()) order.orderNumber else "Order #${order.id}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = orderTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${order.totalItems} items (Tap to preview)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "₱%.2f", order.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Now")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Show Less" else "Preview Items",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Order Items Breakdown:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    orderWithItems.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.productName}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format(Locale.US, "₱%.2f", item.subtotal),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PayUnpaidOrderDialog(
    orderWithItems: OrderWithItems,
    onDismiss: () -> Unit,
    onConfirmPay: (paymentMethod: String, cashReceived: Double?, changeAmount: Double?, gcashRef: String?) -> Unit
) {
    val totalAmount = orderWithItems.order.totalAmount
    val orderTitle = if (orderWithItems.order.orderNumber.isNotBlank()) orderWithItems.order.orderNumber else "Order #${orderWithItems.order.id}"

    var selectedMethod by remember { mutableStateOf("Cash") }
    var cashReceivedStr by remember { mutableStateOf("") }
    var gcashRefStr by remember { mutableStateOf("") }

    val cashReceived = cashReceivedStr.toDoubleOrNull() ?: 0.0
    val changeAmount = cashReceived - totalAmount
    val isCashValid = selectedMethod != "Cash" || cashReceived >= totalAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay $orderTitle") },
        text = {
            Column {
                Text(
                    text = String.format(Locale.US, "Amount Due: ₱%.2f", totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Itemized Preview inside Dialog
                Text("Items Preview:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                orderWithItems.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(String.format(Locale.US, "₱%.2f", item.subtotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Payment Method:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (selectedMethod == "Cash"),
                        onClick = { selectedMethod = "Cash" }
                    )
                    Text("Cash", modifier = Modifier.clickable { selectedMethod = "Cash" })
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = (selectedMethod == "GCash"),
                        onClick = { selectedMethod = "GCash" }
                    )
                    Text("GCash", modifier = Modifier.clickable { selectedMethod = "GCash" })
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedMethod == "Cash") {
                    OutlinedTextField(
                        value = cashReceivedStr,
                        onValueChange = { cashReceivedStr = it },
                        label = { Text("Cash Received (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (cashReceivedStr.isNotBlank()) {
                        if (changeAmount >= 0) {
                            Text(
                                text = String.format(Locale.US, "Change: ₱%.2f", changeAmount),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = String.format(Locale.US, "Insufficient Cash (Short by ₱%.2f)", -changeAmount),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = gcashRefStr,
                        onValueChange = { gcashRefStr = it },
                        label = { Text("GCash Ref No. (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPay(
                        selectedMethod,
                        if (selectedMethod == "Cash") cashReceived else null,
                        if (selectedMethod == "Cash") changeAmount else null,
                        if (selectedMethod == "GCash") gcashRefStr.ifBlank { null } else null
                    )
                },
                enabled = isCashValid
            ) {
                Text("Mark as Paid")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
