package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.repository.BookingRepository
import com.example.repository.PaginationState
import kotlinx.coroutines.launch

/**
 * ViewModel لإدارة حالة جلب وعرض بيانات الحجوزات مع التصفح.
 */
class BookingViewModel(private val repository: BookingRepository = BookingRepository()) : ViewModel() {

    val paginationState = repository.paginationState

    init {
        // جلب الصفحة الأولى عند التهيئة
        fetchInitial()
    }

    fun fetchInitial() {
        viewModelScope.launch {
            repository.fetchBookingsPaginated(isInitialFetch = true)
        }
    }

    fun fetchNextPage() {
        viewModelScope.launch {
            repository.fetchBookingsPaginated(isInitialFetch = false)
        }
    }

    fun retry() {
        viewModelScope.launch {
            repository.retry()
        }
    }
}
