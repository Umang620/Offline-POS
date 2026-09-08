package com.umang620.offline_pos.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.umang620.offline_pos.data.local.ExpenseEntity
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.repository.PosRepository
import com.umang620.offline_pos.utils.BluetoothPrinterHelper
import com.umang620.offline_pos.utils.PrinterDevice
import com.umang620.offline_pos.utils.PrinterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(context: Context, private val repository: PosRepository? = null) : ViewModel() {

    val allOrdersWithItems: Flow<List<OrderWithItems>>? = repository?.allOrdersWithItems
    val allExpenses: Flow<List<ExpenseEntity>>? = repository?.allExpenses

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)

    private val _managerPin = MutableStateFlow(prefs.getString("manager_pin", "") ?: "")
    val managerPin: StateFlow<String> = _managerPin.asStateFlow()

    private val _isPinRequired = MutableStateFlow(prefs.getBoolean("is_pin_required", true))
    val isPinRequired: StateFlow<Boolean> = _isPinRequired.asStateFlow()

    private val _isVoidPinRequired = MutableStateFlow(prefs.getBoolean("is_void_pin_required", false))
    val isVoidPinRequired: StateFlow<Boolean> = _isVoidPinRequired.asStateFlow()

    private val _isAutoPrintEnabled = MutableStateFlow(prefs.getBoolean("is_auto_print_enabled", true))
    val isAutoPrintEnabled: StateFlow<Boolean> = _isAutoPrintEnabled.asStateFlow()

    private val _defaultCashFloat = MutableStateFlow(prefs.getFloat("default_cash_float", 1000f).toDouble())
    val defaultCashFloat: StateFlow<Double> = _defaultCashFloat.asStateFlow()

    private val _selectedPrinterAddress = MutableStateFlow(prefs.getString("selected_printer_address", "") ?: "")
    val selectedPrinterAddress: StateFlow<String> = _selectedPrinterAddress.asStateFlow()

    private val _selectedPrinterName = MutableStateFlow(prefs.getString("selected_printer_name", "No Printer Selected") ?: "No Printer Selected")
    val selectedPrinterName: StateFlow<String> = _selectedPrinterName.asStateFlow()

    private val _selectedPrinterType = MutableStateFlow(
        PrinterType.valueOf(prefs.getString("selected_printer_type", PrinterType.BLUETOOTH.name) ?: PrinterType.BLUETOOTH.name)
    )
    val selectedPrinterType: StateFlow<PrinterType> = _selectedPrinterType.asStateFlow()

    private val _pairedPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PrinterDevice>> = _pairedPrinters.asStateFlow()

    init {
        refreshPairedPrinters(context)
    }

    fun refreshPairedPrinters(context: Context) {
        _pairedPrinters.value = BluetoothPrinterHelper.getAvailablePrinters(context)
    }

    fun selectPrinter(printer: PrinterDevice) {
        _selectedPrinterAddress.value = printer.address
        _selectedPrinterName.value = printer.name
        _selectedPrinterType.value = printer.type
        prefs.edit()
            .putString("selected_printer_address", printer.address)
            .putString("selected_printer_name", printer.name)
            .putString("selected_printer_type", printer.type.name)
            .apply()
    }

    fun executeTestPrint(context: Context): Result<String> {
        val address = _selectedPrinterAddress.value
        val type = _selectedPrinterType.value
        return BluetoothPrinterHelper.printTestReceipt(context, address, type)
    }

    fun setManagerPin(pin: String) {
        _managerPin.value = pin
        prefs.edit().putString("manager_pin", pin).apply()
        if (pin.length == 4) {
            setPinRequired(true)
        }
    }

    fun setPinRequired(required: Boolean) {
        _isPinRequired.value = required
        prefs.edit().putBoolean("is_pin_required", required).apply()
    }

    fun setVoidPinRequired(required: Boolean) {
        _isVoidPinRequired.value = required
        prefs.edit().putBoolean("is_void_pin_required", required).apply()
    }

    fun setAutoPrintEnabled(enabled: Boolean) {
        _isAutoPrintEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_print_enabled", enabled).apply()
    }

    fun setDefaultCashFloat(amount: Double) {
        _defaultCashFloat.value = amount
        prefs.edit().putFloat("default_cash_float", amount.toFloat()).apply()
    }

    fun verifyPin(input: String): Boolean {
        if (!_isPinRequired.value || _managerPin.value.isBlank()) return true
        return _managerPin.value == input.trim()
    }

    class Factory(private val context: Context, private val repository: PosRepository? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(context, repository) as T
        }
    }
}
