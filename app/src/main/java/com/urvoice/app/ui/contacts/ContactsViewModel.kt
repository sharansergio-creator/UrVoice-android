package com.urvoice.app.ui.contacts

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Contact(
    val phoneNumber: String = "",
    val name: String? = null,
    val type: String = "UNKNOWN",
    val totalCalls: Int = 0
)

data class ContactsUiState(
    val vip: List<Contact> = emptyList(),
    val customers: List<Contact> = emptyList(),
    val blocked: List<Contact> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ContactsViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private var listenerReg: ListenerRegistration? = null

    init { attachListener() }

    private fun attachListener() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = ContactsUiState(isLoading = false, error = "Not signed in")
            return
        }
        listenerReg?.remove()
        listenerReg = db.collection("contact_permissions")
            .document(uid)
            .collection("contacts")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("Contacts", "Listener error: ${err.message}")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
                    return@addSnapshotListener
                }
                val all = snap?.documents?.map { doc ->
                    Contact(
                        phoneNumber = doc.id,
                        name        = doc.getString("name"),
                        type        = doc.getString("type") ?: "UNKNOWN",
                        totalCalls  = (doc.getLong("totalCalls") ?: 0L).toInt()
                    )
                } ?: emptyList()
                _uiState.value = ContactsUiState(
                    vip       = all.filter { it.type == "VIP"      }.sortedBy { it.name ?: it.phoneNumber },
                    customers = all.filter { it.type == "CUSTOMER" }.sortedBy { it.name ?: it.phoneNumber },
                    blocked   = all.filter { it.type == "BLOCKED"  }.sortedBy { it.name ?: it.phoneNumber },
                    isLoading = false
                )
            }
    }

    fun updateContactType(phoneNumber: String, newType: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("contact_permissions").document(uid)
            .collection("contacts").document(phoneNumber)
            .update("type", newType)
            .addOnFailureListener { Log.e("Contacts", "updateContactType: ${it.message}") }
    }

    fun deleteContact(phoneNumber: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("contact_permissions").document(uid)
            .collection("contacts").document(phoneNumber)
            .delete()
            .addOnFailureListener { Log.e("Contacts", "deleteContact: ${it.message}") }
    }

    fun addContact(phoneNumber: String, name: String, type: String) {
        val uid   = auth.currentUser?.uid ?: return
        val phone = phoneNumber.trim()
        if (phone.isBlank()) return
        db.collection("contact_permissions").document(uid)
            .collection("contacts").document(phone)
            .set(mapOf("name" to name.trim(), "type" to type, "totalCalls" to 0))
            .addOnFailureListener { Log.e("Contacts", "addContact: ${it.message}") }
    }

    override fun onCleared() {
        super.onCleared()
        listenerReg?.remove()
    }
}
