package com.umang620.offline_pos.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun DailySummaryScreen(viewModel: DailySummaryViewModel) {
    val cashSales by viewModel.cashSales.collectAsState()
    val gcashSales by viewModel.gcashSales.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()

    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val gcashExpenses by viewModel.gcashExpenses.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()

    val cashOnHand by viewModel.cashOnHand.collectAsState()
    val gcashRecorded by viewModel.gcashRecorded.collectAsState()
    val unpaidTotal by viewModel.unpaidTotal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Daily Summary & Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sales Summary Card
        SummarySectionCard(
            title = "Sales Summary (Paid Only)",
            totalLabel = "Total Sales",
            totalValue = totalSales,
            item1Label = "Cash Sales",
            item1Value = cashSales,
            item2Label = "GCash Sales",
            item2Value = gcashSales,
            icon = Icons.Default.PointOfSale,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Expenses Summary Card
        SummarySectionCard(
            title = "Expenses Summary",
            totalLabel = "Total Expenses",
            totalValue = totalExpenses,
            item1Label = "Cash Expenses",
            item1Value = cashExpenses,
            item2Label = "GCash Expenses",
            item2Value = gcashExpenses,
            icon = Icons.Default.Receipt,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Net Balances & Cash on Hand Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculated Balances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                BalanceRow(
                    label = "Cash on Hand (Cash Sales - Cash Expenses)",
                    value = cashOnHand,
                    isHighlight = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                BalanceRow(
                    label = "GCash Recorded (GCash Sales - GCash Expenses)",
                    value = gcashRecorded,
                    isHighlight = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Unpaid Summary Card
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
                        text = "Unpaid Orders Total (Excluded from Sales)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "₱%.2f", unpaidTotal),
                        style = MaterialTheme.typography.headlineSmall,
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
    }
}

@Composable
fun SummarySectionCard(
    title: String,
    totalLabel: String,
    totalValue: Double,
    item1Label: String,
    item1Value: Double,
    item2Label: String,
    item2Value: Double,
    icon: ImageVector,
    containerColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = icon, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            BalanceRow(label = item1Label, value = item1Value)
            Spacer(modifier = Modifier.height(6.dp))
            BalanceRow(label = item2Label, value = item2Value)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            BalanceRow(label = totalLabel, value = totalValue, isHighlight = true)
        }
    }
}

@Composable
fun BalanceRow(
    label: String,
    value: Double,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = String.format(Locale.US, "₱%.2f", value),
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
