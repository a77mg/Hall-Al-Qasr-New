#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/viewmodel/DashboardViewModel.kt
package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.Booking
import com.example.models.Payment
import com.example.repository.BookingRepository
import com.example.repository.PaymentRepository
import com.example.repository.PresenceRepository
import com.example.repository.StatisticsRepository
import com.example.repository.DailyStatistics
import com.example.repository.Repositories
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardState(
    val totalRevenue: Long = 0,
    val occupancyRate: Float = 0f,
    val totalOutstanding: Long = 0,
    val onlineUsers: Int = 0,
    val error: String? = null,
    val isLoading: Boolean = false,
    val todayConfirmedBookingsCount: Int = 0,
    val todayPaymentsReceived: Long = 0,
    val todayPendingRemaining: Long = 0
)

class DashboardViewModel(
    private val bookingRepo: BookingRepository = Repositories.bookings,
    private val paymentRepo: PaymentRepository = Repositories.payments,
    private val presenceRepo: PresenceRepository = Repositories.presence,
    private val statsRepo: StatisticsRepository = Repositories.statistics
) : ViewModel() {

    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(false)

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                bookingRepo.getBookings()
                paymentRepo.getPayments()
            } catch (e: Exception) {
                _error.value = "حدث خطأ أثناء تحديث بيانات اللوحة: ${e.message}"
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepo.refresh()
                paymentRepo.refresh()
                _uiEvent.send(UiEvent.ShowSnackbar("تم التحديث بنجاح"))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowError("فشل التحديث، تأكد من الاتصال: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val dataFlow = combine(
        bookingRepo.data,
        paymentRepo.data,
        presenceRepo.onlineUsersCount,
        statsRepo.dailyStatistics
    ) { bookings, payments, onlineCount, dailyStats ->
        Triple(bookings, payments, Pair(onlineCount, dailyStats))
    }

    val state: StateFlow<DashboardState> = combine(
        dataFlow,
        _error,
        _isLoading
    ) { data, errorMsg, isLoading ->
        val bookings = data.first
        val payments = data.second
        val onlineCount = data.third.first
        val dailyStats = data.third.second
        
        val totalRevenue = payments.sumOf { it.amount }

        val currentMonth = LocalDate.now().monthValue
        val currentYear = LocalDate.now().year
        val daysInMonth = LocalDate.now().lengthOfMonth()

        val bookedDaysThisMonth = bookings.filter { 
            try {
                val date = LocalDate.parse(it.date)
                date.monthValue == currentMonth && date.year == currentYear
            } catch(e: Exception) {
                false
            }
        }.map { it.date }.distinct().count()

        val occupancyRate = if (daysInMonth > 0) (bookedDaysThisMonth.toFloat() / daysInMonth.toFloat()) * 100f else 0f
        
        val totalOutstanding = bookings.sumOf { it.liability }

        DashboardState(
            totalRevenue = totalRevenue,
            occupancyRate = occupancyRate,
            totalOutstanding = totalOutstanding,
            onlineUsers = onlineCount,
            error = errorMsg,
            isLoading = isLoading,
            todayConfirmedBookingsCount = dailyStats.todayConfirmedBookingsCount,
            todayPaymentsReceived = dailyStats.todayPaymentsReceived,
            todayPendingRemaining = dailyStats.todayPendingRemaining
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardState())
}
INNER_EOF
