package com.example.repository

import com.example.models.Booking
import com.example.models.Payment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class PaymentRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val _data = MutableStateFlow<List<Payment>>(emptyList())
    val data: StateFlow<List<Payment>> = _data.asStateFlow()

    suspend fun getFinancialClosureDate(): String? {
        return try {
            val snapshot = firestore.collection("settings").document("financial").get().await()
            snapshot.getString("closingDate")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPayments() {
        if (_data.value.isNotEmpty()) return
        refresh()
    }

    suspend fun refresh(startDate: String? = null, endDate: String? = null) {
        try {
            var query = firestore.collection("payments").orderBy("date", Query.Direction.DESCENDING).limit(50)
            
            if (!startDate.isNullOrEmpty()) {
                query = query.whereGreaterThanOrEqualTo("date", startDate)
            }
            if (!endDate.isNullOrEmpty()) {
                query = query.whereLessThanOrEqualTo("date", endDate)
            }
            
            val snapshot = query.get().await()
            _data.value = snapshot.documents.mapNotNull { it.toObject(Payment::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun createPayment(payment: Payment, userId: String): Result<Unit> {
        val oldData = _data.value
        return try {
            val paymentRef = firestore.collection("payments").document(payment.id.ifEmpty { firestore.collection("payments").document().id })
            val finalPayment = payment.copy(id = paymentRef.id, recordedBy = userId)
            
            // Optimistic update
            _data.value = listOf(finalPayment) + oldData
            
            firestore.runTransaction { transaction ->
                val bookingRef = firestore.collection("bookings").document(payment.bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                
                if (!bookingSnapshot.exists()) {
                    throw Exception("الحجز غير موجود")
                }
                
                val booking = bookingSnapshot.toObject(Booking::class.java) ?: throw Exception("بيانات الحجز غير صالحة")
                
                if (payment.amount <= 0) {
                    throw Exception("المبلغ يجب أن يكون أكبر من الصفر")
                }
                
                val newLiability = booking.liability - payment.amount
                if (newLiability < 0) {
                    throw Exception("المبلغ المدفوع أكبر من المديونية المتبقية")
                }
                
                // Update booking
                transaction.update(bookingRef, "liability", newLiability)
                
                // Add payment
                transaction.set(paymentRef, finalPayment)
                
                // Add Audit Log
                val auditRef = firestore.collection("auditLogs").document()
                val auditLog = com.example.models.AuditLog(
                    id = auditRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    actionType = "CREATE_PAYMENT",
                    documentId = paymentRef.id,
                    oldValue = null,
                    newValue = finalPayment.toString()
                )
                transaction.set(auditRef, auditLog)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback
            _data.value = oldData
            Result.failure(e)
        }
    }

    suspend fun deletePayment(paymentId: String, userId: String): Result<Unit> {
        val oldData = _data.value
        val paymentToDelete = oldData.find { it.id == paymentId } ?: return Result.failure(Exception("الدفعة غير موجودة محلياً"))
        
        return try {
            // Optimistic update
            _data.value = oldData.filter { it.id != paymentId }
            
            firestore.runTransaction { transaction ->
                val paymentRef = firestore.collection("payments").document(paymentId)
                val snapshot = transaction.get(paymentRef)
                
                if (!snapshot.exists()) {
                    throw Exception("الدفعة غير موجودة")
                }
                
                val existingPayment = snapshot.toObject(Payment::class.java) ?: throw Exception("بيانات الدفعة غير صالحة")
                
                val bookingRef = firestore.collection("bookings").document(existingPayment.bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                
                if (bookingSnapshot.exists()) {
                    val booking = bookingSnapshot.toObject(Booking::class.java)
                    if (booking != null) {
                        val newLiability = booking.liability + existingPayment.amount
                        transaction.update(bookingRef, "liability", newLiability)
                    }
                }
                
                transaction.delete(paymentRef)
                
                val auditRef = firestore.collection("auditLogs").document()
                val auditLog = com.example.models.AuditLog(
                    id = auditRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    actionType = "DELETE_PAYMENT",
                    documentId = paymentId,
                    oldValue = existingPayment.toString(),
                    newValue = null
                )
                transaction.set(auditRef, auditLog)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback
            _data.value = oldData
            Result.failure(e)
        }
    }

    suspend fun exportPayments(context: android.content.Context, uri: android.net.Uri): Result<Unit> {
        return com.example.engine.ReportExporter.exportPayments(context, uri, _data.value)
    }

    suspend fun importPayments(context: android.content.Context, uri: android.net.Uri, userId: String): Result<Unit> {
        val result = com.example.engine.DataImporter.importPayments(context, uri, userId)
        if (result.isSuccess) {
            val newPayments = result.getOrNull() ?: emptyList()
            val oldDataMap = _data.value.associateBy { it.id }.toMutableMap()
            newPayments.forEach { oldDataMap[it.id] = it }
            _data.value = oldDataMap.values.toList().sortedByDescending { it.date }
        }
        return result.map { Unit }
    }

    suspend fun fetchPaymentsPaginated(startDate: String?, endDate: String?): List<Payment> {
        if (_data.value.isEmpty()) {
            refresh(startDate, endDate)
        }
        return _data.value
    }
}
