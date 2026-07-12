package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.Payment
import com.example.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class PaymentViewModel(
    private val repository: PaymentRepository = com.example.repository.Repositories.payments,
    private val userRepository: com.example.repository.UserRepository = com.example.repository.Repositories.users
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    val payments: StateFlow<List<Payment>> = repository.data
    private val _closingDate = MutableStateFlow<String?>(null)

    init {
        fetchClosingDate()
    }

    fun manualRefresh(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.refresh(startDate, endDate)
                _uiEvent.send(UiEvent.ShowSnackbar("تم التحديث بنجاح"))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowError("فشل التحديث، تأكد من الاتصال"))
            } finally {
                _loading.value = false
            }
        }
    }

    private fun fetchClosingDate() {
        viewModelScope.launch {
            _closingDate.value = repository.getFinancialClosureDate()
        }
    }

    fun isEditable(date: LocalDate, userRole: String): Boolean {
        if (userRole == "Admin") return true
        val closureStr = _closingDate.value ?: return true
        return date.toString() > closureStr
    }

    fun processPayment(payment: Payment, userId: String, onSuccess: () -> Unit) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.CREATE_PAYMENT)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لإضافة دفعة."))
            }
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                if (payment.amount <= 0L) {
                    _error.value = "المبلغ يجب أن يكون أكبر من الصفر"
                    return@launch
                }
                
                val result = repository.createPayment(payment, user?.id ?: userId)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف أثناء الدفع"
                    _uiEvent.send(UiEvent.ShowError(msg))
                    _error.value = msg
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun deletePayment(paymentId: String, onSuccess: () -> Unit) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.DELETE_PAYMENT)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لحذف دفعة."))
            }
            return
        }

        val currentUserId = user?.id ?: return
        
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val result = repository.deletePayment(paymentId, currentUserId)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم حذف الدفعة بنجاح"))
                    onSuccess()
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف أثناء الحذف"
                    _uiEvent.send(UiEvent.ShowError(msg))
                    _error.value = msg
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun exportPayments(context: android.content.Context, uri: android.net.Uri) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.VIEW_REPORTS)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية."))
            }
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.exportPayments(context, uri)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم التصدير بنجاح"))
                } else {
                    _uiEvent.send(UiEvent.ShowError(result.exceptionOrNull()?.message ?: "خطأ أثناء التصدير"))
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun importPayments(context: android.content.Context, uri: android.net.Uri) {
        val user = userRepository.currentUser.value
        // Needs CREATE_PAYMENT and VIEW_REPORTS? Let's use VIEW_REPORTS for now or CREATE_PAYMENT
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.CREATE_PAYMENT)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية."))
            }
            return
        }
        val currentUserId = user?.id ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.importPayments(context, uri, currentUserId)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم الاستيراد بنجاح"))
                } else {
                    _uiEvent.send(UiEvent.ShowError(result.exceptionOrNull()?.message ?: "خطأ أثناء الاستيراد"))
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadReports(startDate: String? = null, endDate: String? = null) {
        val user = userRepository.currentUser.value
        if (!com.example.auth.AuthorizationManager.hasPermission(user, com.example.auth.Action.VIEW_REPORTS)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لعرض التقارير المالية."))
            }
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.refresh(startDate, endDate)
            } catch (e: Exception) {
                _error.value = "فشل جلب التقارير المالية"
            } finally {
                _loading.value = false
            }
        }
    }
}
