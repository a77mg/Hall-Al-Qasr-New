package com.example.repository

import com.google.firebase.database.FirebaseDatabase

object Repositories {
    val users = UserRepository()
    val bookings = BookingRepository()
    val payments = PaymentRepository()
    val customers = CustomerRepository()
    val statistics = StatisticsRepository(bookings, payments)
    val presence = PresenceRepository(FirebaseDatabase.getInstance()).apply { initialize() }
}
