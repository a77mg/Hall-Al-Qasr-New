package com.example.repository

import com.example.models.Booking
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class BookingRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val _data = MutableStateFlow<List<Booking>>(emptyList())
    val data: StateFlow<List<Booking>> = _data.asStateFlow()

    private val _paginationState = MutableStateFlow<PaginationState<Booking>>(PaginationState.Idle)
    val paginationState: StateFlow<PaginationState<Booking>> = _paginationState.asStateFlow()

    private var lastDocumentSnapshot: DocumentSnapshot? = null
    private var isFetching = false
    private var hasMoreData = true
    
    private val limit = 50L

    suspend fun getBookings() {
        if (_data.value.isNotEmpty()) return
        refresh()
    }

    suspend fun refresh() {
        try {
            val snapshot = firestore.collection("bookings")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val fetched = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
            _data.value = fetched
            
            lastDocumentSnapshot = snapshot.documents.lastOrNull()
            hasMoreData = snapshot.documents.size == limit.toInt()
            _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
        } catch (e: Exception) {
            e.printStackTrace()
            _paginationState.value = PaginationState.Error(e.message ?: "حدث خطأ غير متوقع")
        }
    }

    suspend fun fetchBookingsPaginated(isInitialFetch: Boolean = false) {
        if (isFetching || (!hasMoreData && !isInitialFetch)) return

        if (!isInitialFetch && lastDocumentSnapshot == null) {
            hasMoreData = false
            return
        }

        if (isInitialFetch) {
            if (_data.value.isEmpty()) {
                _paginationState.value = PaginationState.Loading
                refresh()
            } else {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
            return
        }

        _paginationState.value = PaginationState.LoadingNextPage
        isFetching = true

        try {
            var query = firestore.collection("bookings")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit)

            lastDocumentSnapshot?.let {
                query = query.startAfter(it)
            }

            val snapshot = query.get().await()

            if (!snapshot.isEmpty) {
                lastDocumentSnapshot = snapshot.documents.last()
                
                val newBookings = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Booking::class.java) 
                    } catch (e: Exception) {
                        null
                    }
                }

                _data.value = _data.value + newBookings
                hasMoreData = snapshot.documents.size == limit.toInt()
            } else {
                hasMoreData = false
            }

            _paginationState.value = PaginationState.Success(
                data = _data.value,
                hasMore = hasMoreData
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _paginationState.value = PaginationState.ErrorNextPage(e.message ?: "حدث خطأ غير متوقع")
        } finally {
            isFetching = false
        }
    }

    suspend fun retry() {
        val currentState = _paginationState.value
        when (currentState) {
            is PaginationState.Error -> refresh()
            is PaginationState.ErrorNextPage -> fetchBookingsPaginated(isInitialFetch = false)
            else -> {}
        }
    }

    suspend fun fetchBookingsForMonth(yearMonth: String): List<Booking> {
        val start = "$yearMonth-01"
        val end = "$yearMonth-31"

        val snapshot = firestore.collection("bookings")
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .get()
            .await()

        val fetched = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
        
        val oldData = _data.value
        val newDataMap = oldData.associateBy { it.id }.toMutableMap()
        fetched.forEach { newDataMap[it.id] = it }
        _data.value = newDataMap.values.toList().sortedByDescending { it.date }
        
        return fetched
    }

    suspend fun createBooking(booking: Booking, userId: String): Result<Unit> {
        val oldData = _data.value
        return try {
            // Optimistic update
            _data.value = (listOf(booking) + oldData).sortedByDescending { it.date }
            if (_paginationState.value is PaginationState.Success) {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
            
            firestore.runTransaction { transaction ->
                val bookingRef = firestore.collection("bookings").document(booking.id)
                val snapshot = transaction.get(bookingRef)
                
                if (snapshot.exists()) {
                    throw Exception("هذا الموعد محجوز مسبقاً (تضارب في الحجز)")
                }
                
                transaction.set(bookingRef, booking)
                
                val auditRef = firestore.collection("auditLogs").document()
                val auditLog = com.example.models.AuditLog(
                    id = auditRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    actionType = "CREATE_BOOKING",
                    documentId = booking.id,
                    oldValue = null,
                    newValue = booking.toString()
                )
                transaction.set(auditRef, auditLog)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback
            _data.value = oldData
            if (_paginationState.value is PaginationState.Success) {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
            Result.failure(e)
        }
    }

    suspend fun deleteBooking(bookingId: String, userId: String): Result<Unit> {
        val oldData = _data.value
        val bookingToDelete = oldData.find { it.id == bookingId } ?: return Result.failure(Exception("الحجز غير موجود محلياً"))
        
        return try {
            // Optimistic update
            _data.value = oldData.filter { it.id != bookingId }
            if (_paginationState.value is PaginationState.Success) {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
            
            firestore.runTransaction { transaction ->
                val bookingRef = firestore.collection("bookings").document(bookingId)
                val snapshot = transaction.get(bookingRef)
                
                if (!snapshot.exists()) {
                    throw Exception("الحجز غير موجود")
                }
                
                val existingBooking = snapshot.toObject(Booking::class.java)
                transaction.delete(bookingRef)
                
                val auditRef = firestore.collection("auditLogs").document()
                val auditLog = com.example.models.AuditLog(
                    id = auditRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    actionType = "DELETE_BOOKING",
                    documentId = bookingId,
                    oldValue = existingBooking.toString(),
                    newValue = null
                )
                transaction.set(auditRef, auditLog)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback
            _data.value = oldData
            if (_paginationState.value is PaginationState.Success) {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
            Result.failure(e)
        }
    }

    suspend fun exportBookings(context: android.content.Context, uri: android.net.Uri): Result<Unit> {
        return com.example.engine.ReportExporter.exportBookings(context, uri, _data.value)
    }

    suspend fun importBookings(context: android.content.Context, uri: android.net.Uri, userId: String): Result<Unit> {
        val result = com.example.engine.DataImporter.importBookings(context, uri, userId)
        if (result.isSuccess) {
            val newBookings = result.getOrNull() ?: emptyList()
            val oldDataMap = _data.value.associateBy { it.id }.toMutableMap()
            newBookings.forEach { oldDataMap[it.id] = it }
            _data.value = oldDataMap.values.toList().sortedByDescending { it.date }
            if (_paginationState.value is PaginationState.Success) {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
        }
        return result.map { Unit }
    }

    suspend fun autoCleanup(): Int {
        return try {
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("status", com.example.models.BookingStatus.PENDING.name)
                .whereLessThan("createdAt", oneDayAgo)
                .get()
                .await()
                
            if (snapshot.isEmpty) return 0
            
            val batch = firestore.batch()
            val canceledIds = mutableSetOf<String>()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "status", com.example.models.BookingStatus.CANCELLED.name)
                canceledIds.add(doc.id)
            }
            batch.commit().await()
            
            // Update cache
            _data.value = _data.value.map { 
                if (canceledIds.contains(it.id)) it.copy(status = com.example.models.BookingStatus.CANCELLED) 
                else it 
            }
            if (_paginationState.value is PaginationState.Success) {
                _paginationState.value = PaginationState.Success(data = _data.value, hasMore = hasMoreData)
            }
            
            snapshot.size()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
