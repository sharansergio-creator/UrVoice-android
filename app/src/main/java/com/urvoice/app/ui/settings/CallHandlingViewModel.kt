package com.urvoice.app.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CallHandlingState(
    val answerMode:           String  = "BUSY_ONLY",
    val noAnswerDelaySeconds: Int     = 20,
    val scheduleStartHour:    Int     = 9,
    val scheduleEndHour:      Int     = 18,
    val forwardingNumber:     String  = "+16206596566",
    val isLoading:            Boolean = true,
    val isSaving:             Boolean = false,
    val saveSuccess:          Boolean = false,
    val error:                String? = null
)

class CallHandlingViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(CallHandlingState())
    val state: StateFlow<CallHandlingState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        val uid = auth.currentUser?.uid ?: run {
            _state.value = CallHandlingState(isLoading = false, error = "Not signed in")
            return
        }
        viewModelScope.launch {
            try {
                val doc = db.collection("call_settings").document(uid).get().await()
                _state.value = if (doc.exists()) {
                    CallHandlingState(
                        answerMode           = doc.getString("answerMode")                       ?: "BUSY_ONLY",
                        noAnswerDelaySeconds = (doc.getLong("noAnswerDelaySeconds") ?: 20L).toInt(),
                        scheduleStartHour    = (doc.getLong("scheduleStartHour")   ?: 9L).toInt(),
                        scheduleEndHour      = (doc.getLong("scheduleEndHour")     ?: 18L).toInt(),
                        forwardingNumber     = doc.getString("forwardingNumber")               ?: "+16206596566",
                        isLoading            = false
                    )
                } else {
                    _state.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("CallHandling", "load: ${e.message}")
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setAnswerMode(mode: String)     { _state.value = _state.value.copy(answerMode = mode, saveSuccess = false) }
    fun setNoAnswerDelay(seconds: Int)  { _state.value = _state.value.copy(noAnswerDelaySeconds = seconds) }
    fun setScheduleStartHour(hour: Int) { _state.value = _state.value.copy(scheduleStartHour = hour) }
    fun setScheduleEndHour(hour: Int)   { _state.value = _state.value.copy(scheduleEndHour = hour) }

    fun save() {
        val uid = auth.currentUser?.uid ?: return
        val s = _state.value
        _state.value = s.copy(isSaving = true, saveSuccess = false, error = null)
        viewModelScope.launch {
            try {
                db.collection("call_settings").document(uid).set(
                    mapOf(
                        "answerMode"           to s.answerMode,
                        "noAnswerDelaySeconds" to s.noAnswerDelaySeconds,
                        "scheduleStartHour"    to s.scheduleStartHour,
                        "scheduleEndHour"      to s.scheduleEndHour,
                        "forwardingNumber"     to s.forwardingNumber
                    )
                ).await()
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                Log.e("CallHandling", "save: ${e.message}")
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
