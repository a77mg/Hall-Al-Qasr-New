package com.example.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("Admin")
    Admin,
    @SerialName("Manager")
    Manager,
    @SerialName("Accountant")
    Accountant,
    @SerialName("Receptionist")
    Receptionist
}

@Serializable
enum class UserStatus {
    @SerialName("ACTIVE")
    ACTIVE,
    @SerialName("SUSPENDED")
    SUSPENDED
}

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.Receptionist,
    val status: UserStatus = UserStatus.ACTIVE
)
