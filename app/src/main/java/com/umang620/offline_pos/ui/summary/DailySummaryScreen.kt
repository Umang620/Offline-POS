package com.umang620.offline_pos.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.umang620.offline_pos.ui.theme.DangerRed
import com.umang620.offline_pos.ui.theme.GlassCard
import com.umang620.offline_pos.ui.theme.SuccessGreen
import com.umang620.offline_pos.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(viewModel: DailySummaryViewModel) {
    var selectedViewMode by remember { mutableStateOf("DATE") }

    val cashSales by viewModel.cashSales.collectAsState()
    val gcashSales by viewModel.gcashSales.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()

    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val gcashExpenses by viewModel.gcashExpenses.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()

    val cashOnHand by viewModel.cashOnHand.collectAsState()
    val gcashRecorded by viewModel.gcashRecorded.collectAsState()
    val unpaidTotal by viewModel.unpaidTotal.collectAsState()

    val allTimeTotalSales by viewModel.allTimeTotalSales.collectAsState()
    val allTimeCashSales by viewModel.allTimeCashSales.collectAsState()
    val allTimeGCashSales by viewModel.allTimeGCashSales.collectAsState()

    val allTimeTotalExpenses by viewModel.allTimeTotalExpenses.collectAsState()
    val allTimeCashExpenses by viewModel.allTimeCashExpenses.collectAsState()
    val allTimeGCashExpenses by viewModel.allTimeGCashExpenses.collectAsState()

    val allTimeCashOnHand by viewModel.allTimeCashOnHand.collectAsState()
    val allTimeGCashRecorded by viewModel.allTimeGCashRecorded.collectAsState()
    val allTimeUnpaidTotal by viewModel.allTimeUnpaidTotal.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState())
    ) {
        // All-Time Summary Overview Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            borderColor = MaterialTheme.colorScheme.primary,
            elevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All-Time Total Revenue Recorded",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "₱%.2f", allTimeTotalSales),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // View Mode Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedViewMode == "DATE",
                onClick = { selectedViewMode = "DATE" },
                label = { Text("Filter by Specific Date", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedViewMode == "ALL_TIME",
                onClick = { selectedViewMode = "ALL_TIME" },
                label = { Text("All-Time Total Summary", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedViewMode == "DATE") {
            // Header for Date View
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Date", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sales Summary Card
            SummarySectionCard(
                title = "Sales Summary (Paid)",
                totalLabel = "Total Sales on Selected Date",
                totalValue = totalSales,
                item1Label = "Cash Sales",
                item1Value = cashSales,
                item2Label = "GCash Sales",
                item2Value = gcashSales,
                icon = Icons.Default.PointOfSale,
                totalColor = SuccessGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expenses Summary Card
            SummarySectionCard(
                title = "Expenses Summary",
                totalLabel = "Total Expenses on Selected Date",
                totalValue = totalExpenses,
                item1Label = "Cash Expenses",
                item1Value = cashExpenses,
                item2Label = "GCash Expenses",
                item2Value = gcashExpenses,
                icon = Icons.Default.Receipt,
                totalColor = DangerRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Net Balances Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculated Balances (Selected Date)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    BalanceRow(
                        label = "Cash on Hand (Cash Sales - Cash Expenses)",
                        value = cashOnHand,
                        isHighlight = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BalanceRow(
                        label = "GCash Recorded (GCash Sales - GCash Expenses)",
                        value = gcashRecorded,
                        isHighlight = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unpaid Summary Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color(0xFFFEF3C7).copy(alpha = 0.85f),
                borderColor = Color(0xFFFDE68A),
                elevation = 2.dp
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
                            text = "Unpaid Orders Total on Selected Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "₱%.2f", unpaidTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.MoneyOff,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            // ALL TIME OVERVIEW MODE

            SummarySectionCard(
                title = "All-Time Sales Breakdown",
                totalLabel = "All-Time Revenue Recorded",
                totalValue = allTimeTotalSales,
                item1Label = "All-Time Cash Sales",
                item1Value = allTimeCashSales,
                item2Label = "All-Time GCash Sales",
                item2Value = allTimeGCashSales,
                icon = Icons.Default.PointOfSale,
                totalColor = SuccessGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            SummarySectionCard(
                title = "All-Time Expenses Breakdown",
                totalLabel = "All-Time Total Expenses",
                totalValue = allTimeTotalExpenses,
                item1Label = "All-Time Cash Expenses",
                item1Value = allTimeCashExpenses,
                item2Label = "All-Time GCash Expenses",
                item2Value = allTimeGCashExpenses,
                icon = Icons.Default.Receipt,
                totalColor = DangerRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All-Time Net Balances",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    BalanceRow(
                        label = "All-Time Cash Net (Cash Sales - Cash Expenses)",
                        value = allTimeCashOnHand,
                        isHighlight = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BalanceRow(
                        label = "All-Time GCash Net (GCash Sales - GCash Expenses)",
                        value = allTimeGCashRecorded,
                        isHighlight = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color(0xFFFEF3C7).copy(alpha = 0.85f),
                borderColor = Color(0xFFFDE68A),
                elevation = 2.dp
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
                            text = "All-Time Outstanding Unpaid Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "₱%.2f", allTimeUnpaidTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.MoneyOff,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
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
    totalColor: Color
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        elevation = 2.dp
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            BalanceRow(label = item1Label, value = item1Value)
            Spacer(modifier = Modifier.height(6.dp))
            BalanceRow(label = item2Label, value = item2Value)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            BalanceRow(label = totalLabel, value = totalValue, isHighlight = true, highlightColor = totalColor)
        }
    }
}

@Composable
fun BalanceRow(
    label: String,
    value: Double,
    isHighlight: Boolean = false,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format(Locale.US, "₱%.2f", value),
            style = if (isHighlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
