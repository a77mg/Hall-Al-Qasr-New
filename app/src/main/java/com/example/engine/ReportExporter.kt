package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.models.Booking
import com.example.models.Payment
import java.io.OutputStreamWriter

object ReportExporter {
    fun exportBookings(context: Context, uri: Uri, bookings: List<Booking>): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write("ID,Date,Shift,EventType,Status,TotalAmount,Deposit,Liability,CustomerId,Notes\n")
                    bookings.forEach { b ->
                        val notes = b.notes?.replace(",", " ")?.replace("\n", " ") ?: ""
                        writer.write("${b.id},${b.date},${b.shift.name},${b.eventType.name},${b.status.name},${b.totalAmount},${b.deposit},${b.liability},${b.customerId},$notes\n")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportPayments(context: Context, uri: Uri, payments: List<Payment>): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write("ID,BookingID,Amount,Date,PaymentType,ReceiptNumber,RecordedBy\n")
                    payments.forEach { p ->
                        val receipt = p.receiptNumber.replace(",", " ")
                        writer.write("${p.id},${p.bookingId},${p.amount},${p.date},${p.paymentType.name},$receipt,${p.recordedBy}\n")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
