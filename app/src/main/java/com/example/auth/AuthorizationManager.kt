package com.example.auth

import com.example.models.User
import com.example.models.UserRole

enum class Action {
    CREATE_BOOKING,
    UPDATE_BOOKING,
    DELETE_BOOKING,
    CREATE_PAYMENT,
    DELETE_PAYMENT,
    VIEW_REPORTS,
    MANAGE_STAFF
}

object AuthorizationManager {
    fun hasPermission(user: User?, action: Action): Boolean {
        if (user == null) return false
        
        // Superuser rule: Admin bypasses all checks
        if (user.role == UserRole.Admin) return true
        
        return when (action) {
            Action.CREATE_BOOKING, Action.UPDATE_BOOKING -> user.role in listOf(UserRole.Receptionist, UserRole.Manager)
            Action.DELETE_BOOKING -> user.role == UserRole.Manager
            Action.CREATE_PAYMENT -> user.role in listOf(UserRole.Accountant, UserRole.Receptionist, UserRole.Manager)
            Action.DELETE_PAYMENT -> user.role == UserRole.Manager
            Action.VIEW_REPORTS -> user.role in listOf(UserRole.Accountant, UserRole.Manager)
            Action.MANAGE_STAFF -> user.role == UserRole.Manager
        }
    }
}
