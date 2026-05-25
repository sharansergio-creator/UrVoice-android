package com.urvoice.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar

data class HourlyCall(val hour: Int, val count: Int)
data class DailyCall(val day: String, val count: Int)

data class AnalyticsState(
    val isLoading: Boolean = true,
    val totalCalls: Int = 0,
    val avgDurationSeconds: Int = 0,
    val peakHours: List<HourlyCall> = emptyList(),
    val dailyCalls: List<DailyCall> = emptyList(),
    val languageBreakdown: Map<String, Int> = emptyMap(),
    val callerCategories: Map<String, Int> = emptyMap(),
    val commonQuestions: List<String> = emptyList(),
    val commonQuestionsLoading: Boolean = false,
    val error: String? = null
)

class AnalyticsViewModel : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                _state.value = AnalyticsState(isLoading = true)
                val uid = Firebase.auth.currentUser?.uid ?: return@launch

                // Fetch last 30 days of call sessions
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                val sessions = Firebase.firestore
                    .collection("call_sessions")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.data }

                if (sessions.isEmpty()) {
                    _state.value = AnalyticsState(isLoading = false)
                    return@launch
                }

                // Total calls
                val totalCalls = sessions.size

                // Average duration
                val durations = sessions.mapNotNull { session ->
                    val start = (session["startTime"] as? com.google.firebase.Timestamp)?.seconds
                        ?: session["startTime"] as? Long
                    val end = (session["endTime"] as? com.google.firebase.Timestamp)?.seconds
                        ?: session["endTime"] as? Long
                    if (start != null && end != null && end > start) (end - start).toInt() else null
                }
                val avgDuration = if (durations.isNotEmpty()) durations.average().toInt() else 0

                // Peak hours (0-23)
                val hourCounts = mutableMapOf<Int, Int>()
                sessions.forEach { session ->
                    val startTime = (session["startTime"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: session["startTime"] as? Long
                        ?: return@forEach
                    val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
                }
                val peakHours = (0..23).map { hour ->
                    HourlyCall(hour, hourCounts[hour] ?: 0)
                }

                // Daily calls — last 7 days
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val dayCounts = mutableMapOf<Int, Int>()
                val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                sessions.forEach { session ->
                    val startTime = (session["startTime"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: session["startTime"] as? Long
                        ?: return@forEach
                    if (startTime < sevenDaysAgo) return@forEach
                    val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                    val day = cal.get(Calendar.DAY_OF_WEEK) - 1
                    dayCounts[day] = (dayCounts[day] ?: 0) + 1
                }
                val dailyCalls = (0..6).map { day ->
                    DailyCall(dayNames[day], dayCounts[day] ?: 0)
                }

                // Language breakdown
                val languageCounts = mutableMapOf<String, Int>()
                sessions.forEach { session ->
                    @Suppress("UNCHECKED_CAST")
                    val exchanges = session["exchanges"] as? List<Map<String, Any>> ?: return@forEach
                    exchanges.forEach { exchange ->
                        val lang = exchange["language"] as? String ?: "en-IN"
                        val displayLang = when {
                            lang.startsWith("kn") || lang == "kan" -> "Kannada"
                            lang.startsWith("hi") || lang == "hin" -> "Hindi"
                            lang.startsWith("ta") || lang == "tam" -> "Tamil"
                            lang.startsWith("te") -> "Telugu"
                            else -> "English"
                        }
                        languageCounts[displayLang] = (languageCounts[displayLang] ?: 0) + 1
                    }
                }

                // Caller categories
                val categoryCounts = mutableMapOf<String, Int>()
                sessions.forEach { session ->
                    val category = session["callerCategory"] as? String ?: "UNKNOWN"
                    categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
                }

                _state.value = AnalyticsState(
                    isLoading = false,
                    totalCalls = totalCalls,
                    avgDurationSeconds = avgDuration,
                    peakHours = peakHours,
                    dailyCalls = dailyCalls,
                    languageBreakdown = languageCounts,
                    callerCategories = categoryCounts,
                    commonQuestionsLoading = true
                )

                // Analyze common questions via Gemini
                analyzeCommonQuestions(sessions, uid)

            } catch (e: Exception) {
                _state.value = AnalyticsState(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }

    private suspend fun analyzeCommonQuestions(
        sessions: List<Map<String, Any>>,
        uid: String
    ) {
        try {
            val transcripts = mutableListOf<String>()
            sessions.takeLast(50).forEach { session ->
                @Suppress("UNCHECKED_CAST")
                val exchanges = session["exchanges"] as? List<Map<String, Any>> ?: return@forEach
                exchanges.forEach { exchange ->
                    val transcript = exchange["transcript"] as? String ?: return@forEach
                    if (transcript.length > 5 && !transcript.startsWith("(")) {
                        transcripts.add(transcript)
                    }
                }
            }

            if (transcripts.isEmpty()) {
                _state.value = _state.value.copy(
                    commonQuestionsLoading = false,
                    commonQuestions = listOf("Not enough call data yet")
                )
                return
            }

            val allTranscripts = transcripts.joinToString("\n") { "- $it" }
            val response = com.urvoice.app.network.UrVoiceApi.service.analyzeQuestions(
                mapOf("transcripts" to allTranscripts)
            )

            if (response.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                val questions = response.body()?.get("questions") as? List<String>
                if (!questions.isNullOrEmpty()) {
                    _state.value = _state.value.copy(
                        commonQuestionsLoading = false,
                        commonQuestions = questions
                    )
                    return
                }
            }

            _state.value = _state.value.copy(
                commonQuestionsLoading = false,
                commonQuestions = listOf("Could not analyze questions")
            )
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsVM", "analyzeQuestions error: ${e.message}", e)
            _state.value = _state.value.copy(
                commonQuestionsLoading = false,
                commonQuestions = listOf("Error: ${e.message?.take(50)}")
            )
        }
    }
}
