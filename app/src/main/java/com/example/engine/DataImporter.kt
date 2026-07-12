package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.models.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.InputStreamReader

object DataImporter {
    
    suspend fun importBookings(
        context: Context, 
        uri: Uri, 
        userId: String,
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): Result<List<Booking>> {
        return try {
            val bookings = mutableListOf<Booking>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip header
                    var line: String? = reader.readLine()
                    var lineNum = 2
                    while (line != null) {
                        if (line.isNotBlank()) {
                            val tokens = line.split(",")
                            if (tokens.size < 10) throw Exception("تنسيق غير صحيح في السطر $lineNum")
                            
                            val id = tokens[0]
                            val date = tokens[1]
                            if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) throw Exception("تنسيق التاريخ غير صحيح في السطر $lineNum")
                            val shift = Shift.valueOf(tokens[2])
                            val eventType = EventType.valueOf(tokens[3])
                            val status = BookingStatus.valueOf(tokens[4])
                            val totalAmount = tokens[5].toLongOrNull() ?: throw Exception("مبلغ غير صحيح في السطر $lineNum")
                            val deposit = tokens[6].toLongOrNull() ?: throw Exception("دفعة غير صحيحة في السطر $lineNum")
                            val liability = tokens[7].toLongOrNull() ?: throw Exception("مديونية غير صحيحة في السطر $lineNum")
                            val customerId = tokens[8]
                            val notes = tokens.drop(9).joinToString(",").ifBlank { null }
                            
                            bookings.add(
                                Booking(
                                    id = id.ifBlank { "${date}_${shift.name}" },
                                    date = date,
                                    shift = shift,
                                    eventType = eventType,
                                    status = status,
                                    totalAmount = totalAmount,
                                    deposit = deposit,
                                    liability = liability,
                                    customerId = customerId,
                                    notes = notes
                                )
                            )
                        }
                        line = reader.readLine()
                        lineNum++
                    }
                }
            }
            
            val chunks = bookings.chunked(250)
            for (chunk in chunks) {
                val batch = firestore.batch()
                for (booking in chunk) {
                    val ref = firestore.collection("bookings").document(booking.id)
                    batch.set(ref, booking)
                    
                    val auditRef = firestore.collection("auditLogs").document()
                    val auditLog = AuditLog(
                        id = auditRef.id,
                        timestamp = System.currentTimeMillis(),
                        userId = userId,
                        actionType = "IMPORT_BOOKING",
                        documentId = booking.id,
                        oldValue = null,
                        newValue = booking.toString()
                    )
                    batch.set(auditRef, auditLog)
                }
                batch.commit().await()
            }
            
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun importPayments(
        context: Context, 
        uri: Uri, 
        userId: String,
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): Result<List<Payment>> {
         return try {
            val payments = mutableListOf<Payment>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip header
                    var line: String? = reader.readLine()
                    var lineNum = 2
                    while (line != null) {
                        if (line.isNotBlank()) {
                            val tokens = line.split(",")
                            if (tokens.size < 7) throw Exception("تنسيق غير صحيح في السطر $lineNum")
                            
                            val id = tokens[0].ifBlank { firestore.collection("payments").document().id }
                            val bookingId = tokens[1]
                            if (bookingId.isBlank()) throw Exception("معرف الحجز مفقود في السطر $lineNum")
                            val amount = tokens[2].toLongOrNull() ?: throw Exception("مبلغ غير صحيح في السطر $lineNum")
                            if (amount <= 0) throw Exception("يجب أن يكون المبلغ أكبر من الصفر في السطر $lineNum")
                            val date = tokens[3]
                            if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) throw Exception("تنسيق التاريخ غير صحيح في السطر $lineNum")
                            val paymentType = PaymentType.valueOf(tokens[4])
                            val receiptNumber = tokens[5]
                            val recordedBy = tokens[6].ifBlank { userId }
                            
                            payments.add(
                                Payment(
                                    id = id,
                                    bookingId = bookingId,
                                    amount = amount,
                                    date = date,
                                    paymentType = paymentType,
                                    receiptNumber = receiptNumber,
                                    recordedBy = recordedBy
                                )
                            )
                        }
                        line = reader.readLine()
                        lineNum++
                    }
                }
            }
            
            val bookingIds = payments.map { it.bookingId }.distinct()
            val bookingDocs = mutableMapOf<String, Boolean>()
            
            bookingIds.chunked(10).forEach { chunk ->
                val snapshot = firestore.collection("bookings").whereIn("id", chunk).get().await()
                snapshot.documents.forEach { doc ->
                    bookingDocs[doc.id] = true
                }
            }
            
            for ((index, payment) in payments.withIndex()) {
                val lineNum = index + 2
                if (bookingDocs[payment.bookingId] != true) {
                    throw Exception("معرف الحجز ${payment.bookingId} غير موجود في السطر $lineNum")
                }
            }
            
            val chunks = payments.chunked(250)
            for (chunk in chunks) {
                val chunkBatch = firestore.batch()
                for (payment in chunk) {
                    val ref = firestore.collection("payments").document(payment.id)
                    chunkBatch.set(ref, payment)
                    val auditRef = firestore.collection("auditLogs").document()
                    val auditLog = AuditLog(
                        id = auditRef.id,
                        timestamp = System.currentTimeMillis(),
                        userId = userId,
                        actionType = "IMPORT_PAYMENT",
                        documentId = payment.id,
                        oldValue = null,
                        newValue = payment.toString()
                    )
                    chunkBatch.set(auditRef, auditLog)
                }
                chunkBatch.commit().await()
            }
            
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
