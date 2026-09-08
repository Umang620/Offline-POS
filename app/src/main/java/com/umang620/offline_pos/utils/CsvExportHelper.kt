package com.umang620.offline_pos.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.umang620.offline_pos.data.local.ExpenseEntity
import com.umang620.offline_pos.data.local.OrderWithItems
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    fun exportSalesToCsv(context: Context, orders: List<OrderWithItems>, periodLabel: String): Result<String> {
        return try {
            val sanitizeLabel = periodLabel.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "Sales_Report_${sanitizeLabel}.csv"

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sb = StringBuilder()

            // CSV Header
            sb.append("Order ID,Order Number,Date Time,Status,Payment Method,Total Items,Total Amount (PHP),Cash Received,Change Given,GCash Ref,Items Breakdown\n")

            // Rows
            orders.forEach { orderWithItems ->
                val order = orderWithItems.order
                val dateStr = dateFormat.format(Date(order.timestamp))
                val itemsStr = orderWithItems.items.joinToString(" | ") { "${it.quantity}x ${it.productName} (${formatMoney(it.subtotal)})" }

                sb.append("${order.id},")
                    .append("\"${escapeCsv(order.orderNumber)}\",")
                    .append("\"$dateStr\",")
                    .append("\"${order.status}\",")
                    .append("\"${order.paymentMethod}\",")
                    .append("${order.totalItems},")
                    .append("${order.totalAmount},")
                    .append("${order.cashReceived ?: 0.0},")
                    .append("${order.changeAmount ?: 0.0},")
                    .append("\"${escapeCsv(order.gcashRefNumber ?: "")}\",")
                    .append("\"${escapeCsv(itemsStr)}\"\n")
            }

            val csvContent = sb.toString()
            saveToDownloadsAndShare(context, fileName, csvContent, "Sales Report ($periodLabel)")
            Result.success("Saved to Downloads: $fileName")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to export Sales CSV: ${e.message}"))
        }
    }

    fun exportExpensesToCsv(context: Context, expenses: List<ExpenseEntity>, periodLabel: String): Result<String> {
        return try {
            val sanitizeLabel = periodLabel.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "Expenses_Report_${sanitizeLabel}.csv"

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val sb = StringBuilder()

            // CSV Header
            sb.append("Expense ID,Description,Category,Amount (PHP),Payment Method,Date Time\n")

            // Rows
            expenses.forEach { expense ->
                val dateStr = dateFormat.format(Date(expense.timestamp))

                sb.append("${expense.id},")
                    .append("\"${escapeCsv(expense.description)}\",")
                    .append("\"${escapeCsv(expense.category)}\",")
                    .append("${expense.amount},")
                    .append("\"${expense.paymentMethod}\",")
                    .append("\"$dateStr\"\n")
            }

            val csvContent = sb.toString()
            saveToDownloadsAndShare(context, fileName, csvContent, "Expenses Report ($periodLabel)")
            Result.success("Saved to Downloads: $fileName")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to export Expenses CSV: ${e.message}"))
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun saveToDownloadsAndShare(context: Context, fileName: String, csvContent: String, title: String) {
        // 1. Save directly to Downloads folder
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                val downloadFile = File(downloadsDir, fileName)
                downloadFile.writeText(csvContent, Charsets.UTF_8)
            }
        } catch (_: Exception) {}

        // Fallback to app external downloads directory if needed
        try {
            val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (extDir != null) {
                val extFile = File(extDir, fileName)
                extFile.writeText(csvContent, Charsets.UTF_8)
            }
        } catch (_: Exception) {}

        // 2. Write to cacheDir for FileProvider sharing
        val cacheFile = File(context.cacheDir, fileName)
        cacheFile.writeText(csvContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share/Open $title")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
