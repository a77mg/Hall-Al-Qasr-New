package com.example.repository

import com.example.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun fetchCurrentUser() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                _currentUser.value = snapshot.toObject(User::class.java)?.copy(id = userId)
            } catch (e: Exception) {
                _currentUser.value = null
            }
        } else {
            _currentUser.value = null
        }
    }

    fun clearUser() {
        _currentUser.value = null
    }

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    suspend fun fetchAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val userList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
            _users.value = userList
            Result.success(userList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserStatus(userId: String, newStatus: com.example.models.UserStatus, performedByUserId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(userId)
                val snapshot = transaction.get(userRef)
                if (!snapshot.exists()) throw Exception("المستخدم غير موجود")
                
                val oldUser = snapshot.toObject(User::class.java)
                transaction.update(userRef, "status", newStatus.name)
                
                val auditRef = firestore.collection("auditLogs").document()
                val auditLog = com.example.models.AuditLog(
                    id = auditRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = performedByUserId,
                    actionType = "UPDATE_USER_STATUS",
                    documentId = userId,
                    oldValue = oldUser?.status?.name,
                    newValue = newStatus.name
                )
                transaction.set(auditRef, auditLog)
            }.await()
            
            _users.value = _users.value.map { if (it.id == userId) it.copy(status = newStatus) else it }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addUser(user: User, performedByUserId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val newUserRef = firestore.collection("users").document(user.id.ifEmpty { firestore.collection("users").document().id })
                val finalUser = user.copy(id = newUserRef.id)
                
                transaction.set(newUserRef, finalUser)
                
                val auditRef = firestore.collection("auditLogs").document()
                val auditLog = com.example.models.AuditLog(
                    id = auditRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = performedByUserId,
                    actionType = "CREATE_USER",
                    documentId = finalUser.id,
                    oldValue = null,
                    newValue = finalUser.toString()
                )
                transaction.set(auditRef, auditLog)
            }.await()
            
            fetchAllUsers()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
