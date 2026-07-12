package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.User
import com.example.models.UserRole
import com.example.models.UserStatus
import com.example.repository.Repositories
import com.example.auth.AuthorizationManager
import com.example.auth.Action
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class StaffManagementViewModel(
    private val userRepository: com.example.repository.UserRepository = Repositories.users
) : ViewModel() {
    val users: StateFlow<List<User>> = userRepository.users

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        val currentUser = userRepository.currentUser.value
        if (!AuthorizationManager.hasPermission(currentUser, Action.MANAGE_STAFF)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لعرض الموظفين."))
            }
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                userRepository.fetchAllUsers()
            } catch (e: Exception) {
                _error.value = "فشل جلب الموظفين: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleUserStatus(user: User) {
        val currentUser = userRepository.currentUser.value
        if (!AuthorizationManager.hasPermission(currentUser, Action.MANAGE_STAFF)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لتعديل حالة الموظف."))
            }
            return
        }

        val currentUserId = currentUser?.id ?: return
        val newStatus = if (user.status == UserStatus.ACTIVE) UserStatus.SUSPENDED else UserStatus.ACTIVE
        
        viewModelScope.launch {
            try {
                val result = userRepository.updateUserStatus(user.id, newStatus, currentUserId)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم تحديث الحالة بنجاح"))
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف"
                    _uiEvent.send(UiEvent.ShowError(msg))
                    _error.value = msg
                }
            } catch (e: Exception) {
                _error.value = "فشل تحديث الحالة: ${e.message}"
            }
        }
    }

    fun addUser(name: String, email: String, role: UserRole) {
        val currentUser = userRepository.currentUser.value
        if (!AuthorizationManager.hasPermission(currentUser, Action.MANAGE_STAFF)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowError("عذراً، ليس لديك صلاحية لإضافة موظف."))
            }
            return
        }

        val currentUserId = currentUser?.id ?: return

        viewModelScope.launch {
            _loading.value = true
            try {
                val user = User(name = name, email = email, role = role, status = UserStatus.ACTIVE)
                val result = userRepository.addUser(user, currentUserId)
                if (result.isSuccess) {
                    _uiEvent.send(UiEvent.ShowSnackbar("تم إضافة الموظف بنجاح"))
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "حدث خطأ غير معروف"
                    _uiEvent.send(UiEvent.ShowError(msg))
                    _error.value = msg
                }
            } catch (e: Exception) {
                _error.value = "فشل إضافة الموظف: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
