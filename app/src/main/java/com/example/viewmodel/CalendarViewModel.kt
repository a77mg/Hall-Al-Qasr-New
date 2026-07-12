package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.Booking
import com.example.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarViewModel(
    private val repository: BookingRepository = com.example.repository.Repositories.bookings,
    private val userRepository: com.example.repository.UserRepository = com.example.repository.Repositories.users
) : ViewModel() {
    val bookings: StateFlow<List<Booking>> = repository.data

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    init {
        loadBookingsForMonth()
    }

    fun manualRefresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.refresh()
                val yearMonthStr = _currentMonth.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                repository.fetchBookingsForMonth(yearMonthStr)
                _uiEvent.send(UiEvent.ShowSnackbar("تم التحديث بنجاح"))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowError("فشل التحديث، تأكد من الاتصال"))
            } finally {
                _loading.value = false
            }
        }
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        loadBookingsForMonth()
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        loadBookingsForMonth()
    }

    fun loadBookingsForMonth() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val yearMonthStr = _currentMonth.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                repository.fetchBookingsForMonth(yearMonthStr)
            } catch (e: Exception) {
                _error.value = "فشل جلب الحجوزات: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createBooking(booking: Booking, userId: String, onSuccess: () -> Unit) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.CREATE_BOOKING)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لإضافة حجز."))
            }
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = repository.createBooking(booking, user?.id ?: userId)
                if (result.isSuccess) {
                    loadBookingsForMonth() // Refresh after successful booking
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف"
                    _uiEvent.send(UiEvent.ShowError(msg))
                    _error.value = msg
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteBooking(bookingId: String, onSuccess: () -> Unit) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.DELETE_BOOKING)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لحذف حجز."))
            }
            return
        }
        
        val userId = user?.id ?: return
        
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = repository.deleteBooking(bookingId, userId)
                if (result.isSuccess) {
                    loadBookingsForMonth()
                    _uiEvent.send(UiEvent.ShowSnackbar("تم حذف الحجز بنجاح"))
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف"
                    _uiEvent.send(UiEvent.ShowError(msg))
                    _error.value = msg
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun exportBookings(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.exportBookings(context, uri)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم تصدير الحجوزات بنجاح"))
                } else {
                    _uiEvent.send(UiEvent.ShowError(result.exceptionOrNull()?.message ?: "خطأ أثناء التصدير"))
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun importBookings(context: android.content.Context, uri: android.net.Uri) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.CREATE_BOOKING)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية."))
            }
            return
        }
        val currentUserId = user?.id ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.importBookings(context, uri, currentUserId)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم استيراد الحجوزات بنجاح"))
                    loadBookingsForMonth() // Optionally refresh
                } else {
                    _uiEvent.send(UiEvent.ShowError(result.exceptionOrNull()?.message ?: "خطأ أثناء الاستيراد"))
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun performAutoCleanup() {
        viewModelScope.launch {
            try {
                val cleanedCount = repository.autoCleanup()
                if (cleanedCount > 0) {
                    loadBookingsForMonth()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
