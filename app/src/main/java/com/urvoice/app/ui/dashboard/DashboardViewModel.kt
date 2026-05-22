package com.urvoice.app.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.urvoice.app.data.model.CallSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class DashboardStats(
    val totalToday: Int = 0,
    val completed: Int = 0,
    val active: Int = 0
)

data class DashboardUiState(
    val sessions: List<CallSession> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class DashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        attachRealtimeListener()
    }

    fun refresh() {
        attachRealtimeListener()
    }

    private fun attachRealtimeListener() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = DashboardUiState(
                isLoading = false,
                error = "Not signed in"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        listenerRegistration?.remove()
        Log.d("UrVoice", "Querying call_sessions with userId: $uid")
        // Sort in-memory to avoid requiring a Firestore composite index
        listenerRegistration = db.collection("call_sessions")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UrVoice", "Firestore error: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sessions"
                    )
                    return@addSnapshotListener
                }

                val docCount = snapshot?.documents?.size ?: 0
                Log.d("UrVoice", "Snapshot received — document count: $docCount")

                val sessions = (snapshot?.documents?.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(CallSession::class.java)
                    }.getOrElse { ex ->
                        Log.e("UrVoice", "toObject() failed for doc[${doc.id}]: ${ex.message}", ex)
                        null
                    }
                } ?: emptyList()).sortedByDescending { it.startTime }
                Log.d("UrVoice", "Mapped ${sessions.size} CallSession objects")

                _uiState.value = DashboardUiState(
                    sessions = sessions,
                    stats = computeStats(sessions),
                    isLoading = false,
                    error = null
                )
            }
    }

    private fun computeStats(sessions: List<CallSession>): DashboardStats {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val todaySessions = sessions.filter { it.startTime?.after(startOfDay) == true }

        return DashboardStats(
            totalToday = todaySessions.size,
            completed  = todaySessions.count { it.status == "completed" },
            active     = todaySessions.count { it.status == "active" }
        )
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
