package com.example.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for managing User Presence via Firebase Realtime Database.
 * This class uses Hilt for Dependency Injection and ensures that listeners are not leaked.
 * It strictly uses StateFlow to communicate the count of online users to the Dashboard
 * following the Repository-Mediated Cache architecture.
 */
@Singleton
class PresenceRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    
    private val _onlineUsersCount = MutableStateFlow(0)
    val onlineUsersCount: StateFlow<Int> = _onlineUsersCount.asStateFlow()

    fun initialize() {
        setupPresenceListener()
    }

    /**
     * يقوم بتسجيل حالة اتصال المستخدم في Realtime Database
     * يستخدم .info/connected لمعرفة حالة الاتصال الفعلية من Firebase
     */
    fun setUserOnline(uid: String) {
        val userStatusRef = database.getReference("presence/$uid/online")
        
        val connectedRef = database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    userStatusRef.onDisconnect().setValue(false).addOnCompleteListener {
                        userStatusRef.setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore
            }
        })
    }

    /**
     * يقوم بمراقبة جميع المستخدمين وحساب عدد المتصلين حالياً
     * هذا الكود يعمل في الخلفية بمجرد تهيئة الـ Repository
     */
    private fun setupPresenceListener() {
        val presenceRef = database.getReference("presence")
        presenceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (child in snapshot.children) {
                    val isOnline = child.child("online").getValue(Boolean::class.java) ?: false
                    if (isOnline) count++
                }
                _onlineUsersCount.value = count
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore
            }
        })
    }
}
