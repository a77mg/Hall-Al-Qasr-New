package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.User
import com.example.models.UserStatus
import com.example.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("الرجاء إدخال البريد الإلكتروني وكلمة المرور")
            return
        }

        _state.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val user = repository.login(email, password)
                if (user != null) {
                    if (user.status == UserStatus.SUSPENDED) {
                        repository.logout()
                        _state.value = LoginState.Error("حسابك معلق. يرجى مراجعة الإدارة.")
                    } else {
                        _state.value = LoginState.Success(user)
                    }
                } else {
                    _state.value = LoginState.Error("لم يتم العثور على بيانات المستخدم في النظام.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = LoginState.Error(e.message ?: "فشل تسجيل الدخول.")
            }
        }
    }
}
