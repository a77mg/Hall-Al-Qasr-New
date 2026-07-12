package com.example.repository

import com.example.models.Customer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class CustomerRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val _data = MutableStateFlow<List<Customer>>(emptyList())
    val data: StateFlow<List<Customer>> = _data.asStateFlow()

    suspend fun getCustomers() {
        if (_data.value.isNotEmpty()) return
        refresh()
    }

    suspend fun refresh() {
        try {
            val snapshot = firestore.collection("customers").get().await()
            _data.value = snapshot.documents.mapNotNull { it.toObject(Customer::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addCustomer(customer: Customer): Result<Unit> {
        val oldData = _data.value
        return try {
            val docRef = firestore.collection("customers").document(customer.id.ifEmpty { firestore.collection("customers").document().id })
            val newCustomer = customer.copy(id = docRef.id)
            
            // Optimistic update
            _data.value = oldData + newCustomer
            
            docRef.set(newCustomer).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback
            _data.value = oldData
            Result.failure(e)
        }
    }
    
    suspend fun updateCustomer(customer: Customer): Result<Unit> {
        val oldData = _data.value
        return try {
            // Optimistic update
            _data.value = oldData.map { if (it.id == customer.id) customer else it }
            
            firestore.collection("customers").document(customer.id).set(customer).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback
            _data.value = oldData
            Result.failure(e)
        }
    }
}
