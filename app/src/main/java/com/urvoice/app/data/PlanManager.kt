package com.urvoice.app.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object PlanManager {
    private val _currentPlan = MutableStateFlow("free")
    val currentPlan: StateFlow<String> = _currentPlan

    val isPremium get() = _currentPlan.value == "premium"
    val isBasic get() = _currentPlan.value == "basic" || isPremium
    val isFree get() = _currentPlan.value == "free"

    suspend fun loadPlan() {
        try {
            val uid = Firebase.auth.currentUser?.uid ?: return
            val doc = Firebase.firestore
                .collection("subscriptions")
                .document(uid)
                .get()
                .await()
            val plan = doc.getString("plan") ?: "free"
            val status = doc.getString("status") ?: "inactive"
            _currentPlan.value = if (status == "active") plan else "free"
        } catch (e: Exception) {
            _currentPlan.value = "free"
        }
    }

    fun setPlan(plan: String) {
        _currentPlan.value = plan
    }
}
