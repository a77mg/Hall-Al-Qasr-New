package com.example.repository

import com.example.models.BookingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyStatistics(
    val todayConfirmedBookingsCount: Int = 0,
    val todayPaymentsReceived: Long = 0,
    val todayPendingRemaining: Long = 0
)

class StatisticsRepository(
    private val bookingRepo: BookingRepository = Repositories.bookings,
    private val paymentRepo: PaymentRepository = Repositories.payments
) {
    val dailyStatistics: Flow<DailyStatistics> = combine(
        bookingRepo.data,
        paymentRepo.data
    ) { bookings, payments ->
        // Format today's date safely, but note API level requirements.
        // We set minSdk=26, so java.time is fully available without desugaring.
        val todayStr = LocalDate.now().toString()

        val todayBookings = bookings.filter { it.date == todayStr }
        val todayConfirmedCount = todayBookings.count { it.status == BookingStatus.CONFIRMED }

        val todayPayments = payments.filter { it.date == todayStr }
        val todayPaymentsSum = todayPayments.sumOf { it.amount }

        val todayPendingRemaining = todayBookings.sumOf { it.liability }

        DailyStatistics(
            todayConfirmedBookingsCount = todayConfirmedCount,
            todayPaymentsReceived = todayPaymentsSum,
            todayPendingRemaining = todayPendingRemaining
        )
    }
}
