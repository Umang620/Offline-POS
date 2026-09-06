package com.umang620.offline_pos.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.ProductSalesSummary
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class ReportPeriod(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Date Range")
}

class DailySummaryViewModel(private val repository: PosRepository) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    private val _customStartDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val customStartDate: StateFlow<Long> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val customEndDate: StateFlow<Long> = _customEndDate.asStateFlow()

    private val _selectedDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    fun updateSelectedDate(timestamp: Long) {
        val startOfDay = getStartOfDay(timestamp)
        _selectedDate.value = startOfDay
        _customStartDate.value = startOfDay
        _customEndDate.value = startOfDay
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    fun updateCustomStartDate(timestamp: Long) {
        _customStartDate.value = getStartOfDay(timestamp)
        if (_customEndDate.value < _customStartDate.value) {
            _customEndDate.value = _customStartDate.value
        }
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    fun updateCustomEndDate(timestamp: Long) {
        _customEndDate.value = getStartOfDay(timestamp)
        if (_customStartDate.value > _customEndDate.value) {
            _customStartDate.value = _customEndDate.value
        }
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _customStartDate.value = getStartOfDay(startMillis)
        _customEndDate.value = getStartOfDay(endMillis)
        _selectedPeriod.value = ReportPeriod.CUSTOM
    }

    companion object {
        fun getStartOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun getEndOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.timeInMillis
        }

        fun getStartOfWeek(timestamp: Long, firstDayOfWeek: Int = Calendar.MONDAY): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.firstDayOfWeek = firstDayOfWeek
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            var daysDiff = dayOfWeek - firstDayOfWeek
            if (daysDiff < 0) {
                daysDiff += 7
            }
            cal.add(Calendar.DAY_OF_MONTH, -daysDiff)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfWeek(startOfWeek: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = startOfWeek
            cal.add(Calendar.DAY_OF_MONTH, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        fun getStartOfMonth(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfMonth(timestamp: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        fun calculateDateRange(
            period: ReportPeriod,
            customStart: Long,
            customEnd: Long,
            referenceTime: Long = System.currentTimeMillis()
        ): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.timeInMillis = referenceTime

            return when (period) {
                ReportPeriod.TODAY -> {
                    Pair(getStartOfDay(referenceTime), getEndOfDay(referenceTime))
                }
                ReportPeriod.YESTERDAY -> {
                    val yesterday = referenceTime - 24 * 60 * 60 * 1000L
                    Pair(getStartOfDay(yesterday), getEndOfDay(yesterday))
                }
                ReportPeriod.THIS_WEEK -> {
                    val startOfWeek = getStartOfWeek(referenceTime)
                    val endOfWeek = getEndOfWeek(startOfWeek)
                    Pair(startOfWeek, endOfWeek)
                }
                ReportPeriod.LAST_WEEK -> {
                    val startOfThisWeek = getStartOfWeek(referenceTime)
                    val startOfLastWeek = startOfThisWeek - 7 * 24 * 60 * 60 * 1000L
                    val endOfLastWeek = startOfThisWeek - 1L
                    Pair(startOfLastWeek, endOfLastWeek)
                }
                ReportPeriod.THIS_MONTH -> {
                    val startOfMonth = getStartOfMonth(referenceTime)
                    val endOfMonth = getEndOfMonth(referenceTime)
                    Pair(startOfMonth, endOfMonth)
                }
                ReportPeriod.LAST_MONTH -> {
                    cal.timeInMillis = referenceTime
                    cal.add(Calendar.MONTH, -1)
                    val prevMonthTime = cal.timeInMillis
                    val startOfLastMonth = getStartOfMonth(prevMonthTime)
                    val endOfLastMonth = getEndOfMonth(prevMonthTime)
                    Pair(startOfLastMonth, endOfLastMonth)
                }
                ReportPeriod.ALL_TIME -> {
                    Pair(0L, Long.MAX_VALUE)
                }
                ReportPeriod.CUSTOM -> {
                    Pair(getStartOfDay(customStart), getEndOfDay(customEnd))
                }
            }
        }
    }

    val activeDateRange: StateFlow<Pair<Long, Long>> = combine(
        _selectedPeriod,
        _customStartDate,
        _customEndDate
    ) { period, customStart, customEnd ->
        calculateDateRange(period, customStart, customEnd)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateDateRange(ReportPeriod.TODAY, System.currentTimeMillis(), System.currentTimeMillis())
    )

    // All-time metrics
    val allTimeTotalSales: StateFlow<Double> = repository.totalRevenue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeCashSales: StateFlow<Double> = repository.cashSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeGCashSales: StateFlow<Double> = repository.gcashSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeTotalExpenses: StateFlow<Double> = repository.totalExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeCashExpenses: StateFlow<Double> = repository.cashExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeGCashExpenses: StateFlow<Double> = repository.gcashExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeUnpaidTotal: StateFlow<Double> = repository.unpaidSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeProductsSold: StateFlow<Int> = repository.allTimeProductsSold.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val allTimeCashOnHand: StateFlow<Double> = combine(allTimeCashSales, allTimeCashExpenses) { sales, expenses ->
        sales - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val allTimeGCashRecorded: StateFlow<Double> = combine(allTimeGCashSales, allTimeGCashExpenses) { sales, expenses ->
        sales - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Period-filtered metrics
    @OptIn(ExperimentalCoroutinesApi::class)
    val cashSales: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getCashSalesTotalByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val gcashSales: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getGCashSalesTotalByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSales: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getTotalRevenueByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val cashExpenses: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getCashExpensesTotalByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val gcashExpenses: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getGCashExpensesTotalByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalExpenses: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getTotalExpensesByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val unpaidTotal: StateFlow<Double> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getUnpaidSalesTotalByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val productsSold: StateFlow<Int> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getProductsSoldByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val productSalesSummary: StateFlow<List<ProductSalesSummary>> = activeDateRange.flatMapLatest { (start, end) ->
        repository.getProductSalesSummaryByDate(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cashOnHand: StateFlow<Double> = combine(cashSales, cashExpenses) { sales, expenses ->
        sales - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val gcashRecorded: StateFlow<Double> = combine(gcashSales, gcashExpenses) { sales, expenses ->
        sales - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailySummaryViewModel(repository) as T
        }
    }
}
