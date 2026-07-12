package com.example.repository

import com.example.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String): User? {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val userId = result.user?.uid ?: return null
        
        Repositories.users.fetchCurrentUser()
        return Repositories.users.currentUser.value
    }

    suspend fun logout() {
        auth.signOut()
        Repositories.users.clearUser()
    }
    
    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }
}
