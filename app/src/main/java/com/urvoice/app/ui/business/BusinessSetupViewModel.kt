package com.urvoice.app.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.urvoice.app.network.FetchBusinessContextRequest
import com.urvoice.app.network.UrVoiceApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BusinessHours(
    val day: String,
    val enabled: Boolean = false,
    val openTime: String = "09:00",
    val closeTime: String = "18:00"
)

sealed class FetchState {
    object Idle    : FetchState()
    object Loading : FetchState()
    object Success : FetchState()
    data class Error(val message: String) : FetchState()
}

val DAYS_OF_WEEK = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val BUSINESS_TYPES = listOf("Restaurant", "Hotel", "Salon", "Clinic", "Shop", "Other")
val QA_QUESTIONS = listOf(
    "What does your business do in one sentence?",
    "What are your main services or products?",
    "What is your price range?",
    "Do you accept walk-ins or appointments only?",
    "What should the AI never say to customers?"
)

private fun defaultHours(): List<BusinessHours> =
    DAYS_OF_WEEK.map { day ->
        BusinessHours(day = day, enabled = day !in listOf("Sat", "Sun"))
    }

private fun defaultQaAnswers(): Map<String, String> = QA_QUESTIONS.associateWith { "" }

class BusinessSetupViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object { private const val TAG = "UrVoice/SetupVM" }

    // Form state — exposed as MutableStateFlow so the Screen collects them directly
    val businessName = MutableStateFlow("")
    val businessType = MutableStateFlow("")
    val phoneNumber = MutableStateFlow(auth.currentUser?.phoneNumber ?: "")
    val businessHours = MutableStateFlow(defaultHours())
    val googleBusinessUrl = MutableStateFlow("")
    val websiteUrl = MutableStateFlow("")
    val about = MutableStateFlow("")
    val services = MutableStateFlow("")
    val pricing = MutableStateFlow("")
    val address = MutableStateFlow("")
    val qaAnswers = MutableStateFlow(defaultQaAnswers())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Idle)
    val fetchState: StateFlow<FetchState> = _fetchState.asStateFlow()

    private val _completionProgress = MutableStateFlow(0f)
    val completionProgress: StateFlow<Float> = _completionProgress.asStateFlow()

    init {
        loadExistingData()
    }

    // ──────────────────────────── field mutators ────────────────────────────

    fun onBusinessNameChange(value: String) {
        businessName.value = value
        _completionProgress.value = computeProgress()
    }

    fun onBusinessTypeChange(value: String) {
        businessType.value = value
        _completionProgress.value = computeProgress()
    }

    fun onPhoneNumberChange(value: String) {
        phoneNumber.value = value
        _completionProgress.value = computeProgress()
    }

    fun onHourToggle(day: String, enabled: Boolean) {
        businessHours.value = businessHours.value.map {
            if (it.day == day) it.copy(enabled = enabled) else it
        }
        _completionProgress.value = computeProgress()
    }

    fun onOpenTimeChange(day: String, time: String) {
        businessHours.value = businessHours.value.map {
            if (it.day == day) it.copy(openTime = time) else it
        }
    }

    fun onCloseTimeChange(day: String, time: String) {
        businessHours.value = businessHours.value.map {
            if (it.day == day) it.copy(closeTime = time) else it
        }
    }

    fun onGoogleBusinessUrlChange(value: String) {
        googleBusinessUrl.value = value
        _completionProgress.value = computeProgress()
    }

    fun onWebsiteUrlChange(value: String) {
        websiteUrl.value = value
        _completionProgress.value = computeProgress()
    }

    fun onAboutChange(value: String)    { about.value    = value }
    fun onServicesChange(value: String) { services.value = value }
    fun onPricingChange(value: String)  { pricing.value  = value }
    fun onAddressChange(value: String)  { address.value  = value }

    fun onQaAnswerChange(question: String, answer: String) {
        qaAnswers.value = qaAnswers.value.toMutableMap().also { it[question] = answer }
        _completionProgress.value = computeProgress()
    }

    // ──────────────────────────── Firestore ops ─────────────────────────────

    /** Reads all fields from Firestore into the form state. Can be called from a coroutine. */
    private suspend fun loadFromFirestore(uid: String) {
        val doc = db.collection("business_context").document(uid).get().await()
        if (doc.exists()) {
            doc.getString("businessName")?.let { businessName.value = it }
            doc.getString("businessType")?.let { businessType.value = it }
            // support both "phoneNumber" (saved by app) and "phone" (saved by auto-fetch)
            (doc.getString("phoneNumber") ?: doc.getString("phone"))
                ?.let { phoneNumber.value = it }
            doc.getString("googleBusinessUrl")?.let { googleBusinessUrl.value = it }
            doc.getString("websiteUrl")?.let { websiteUrl.value = it }
            doc.getString("about")?.let { about.value = it }
            doc.getString("services")?.let { services.value = it }
            doc.getString("pricing")?.let { pricing.value = it }
            // backend saves as "address", build_system_prompt reads "location"
            (doc.getString("address") ?: doc.getString("location"))?.let { address.value = it }

            @Suppress("UNCHECKED_CAST")
            val hoursData = doc.get("businessHours") as? List<Map<String, Any>>
            if (!hoursData.isNullOrEmpty()) {
                businessHours.value = hoursData.map { map ->
                    BusinessHours(
                        day       = map["day"]       as? String  ?: "",
                        enabled   = map["enabled"]   as? Boolean ?: false,
                        openTime  = map["openTime"]  as? String  ?: "09:00",
                        closeTime = map["closeTime"] as? String  ?: "18:00"
                    )
                }
            }

            @Suppress("UNCHECKED_CAST")
            val qa = doc.get("qaAnswers") as? Map<String, String>
            if (qa != null) {
                qaAnswers.value = defaultQaAnswers().toMutableMap().also { it.putAll(qa) }
            }
        }
        _completionProgress.value = computeProgress()
    }

    private fun loadExistingData() {
        val uid = auth.currentUser?.uid ?: run {
            _completionProgress.value = computeProgress()
            return
        }
        _saveSuccess.value = false   // reset stale success state from any previous save
        _isLoading.value = true
        viewModelScope.launch {
            try {
                loadFromFirestore(uid)
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchFromWeb(gbpUrl: String, websiteUrl: String) {
        val uid = auth.currentUser?.uid ?: run {
            android.util.Log.e(TAG, "fetchFromWeb: user not signed in")
            return
        }
        android.util.Log.d(TAG, "fetchFromWeb: uid=$uid gbpUrl=$gbpUrl websiteUrl=$websiteUrl")
        _fetchState.value = FetchState.Loading
        viewModelScope.launch {
            try {
                val response = UrVoiceApi.service.fetchBusinessContext(
                    FetchBusinessContextRequest(gbpUrl, websiteUrl, uid)
                )
                if (response.isSuccessful) {
                    android.util.Log.d(TAG, "fetchFromWeb: success (${response.code()})")
                    loadFromFirestore(uid)
                    _fetchState.value = FetchState.Success
                } else {
                    val body = response.errorBody()?.string() ?: "(no body)"
                    android.util.Log.e(TAG, "fetchFromWeb: HTTP ${response.code()} — $body")
                    _fetchState.value = FetchState.Error("Server error ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "fetchFromWeb: exception", e)
                _fetchState.value = FetchState.Error(e.message ?: "Request failed")
            }
        }
    }

    fun resetFetchState() {
        _fetchState.value = FetchState.Idle
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun save() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _error.value = "Not signed in"
            return
        }
        if (businessName.value.isBlank()) {
            _error.value = "Business name is required"
            return
        }
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "businessName" to businessName.value,
                    "businessType" to businessType.value,
                    "phoneNumber" to phoneNumber.value,
                    "businessHours" to businessHours.value.map { h ->
                        mapOf(
                            "day" to h.day,
                            "enabled" to h.enabled,
                            "openTime" to h.openTime,
                            "closeTime" to h.closeTime
                        )
                    },
                    "googleBusinessUrl" to googleBusinessUrl.value,
                    "websiteUrl" to websiteUrl.value,
                    "about" to about.value,
                    "services" to services.value,
                    "pricing" to pricing.value,
                    "address" to address.value,
                    "location" to address.value,
                    "qaAnswers" to qaAnswers.value,
                    "userId" to uid
                )
                db.collection("business_context").document(uid).set(data).await()
                // Only provision a number if user doesn't already have one
                val userDoc = db.collection("users")
                    .document(uid)
                    .get()
                    .await()
                val existingNumber = userDoc.getString("twilioNumber")
                if (existingNumber == null) {
                    provisionPhoneNumber(uid)
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = "Save failed: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ──────────────────────────── helpers ───────────────────────────────────

    private suspend fun provisionPhoneNumber(userId: String) {
        try {
            val response = UrVoiceApi.service.provisionNumber(
                mapOf("user_id" to userId, "country_code" to "US")
            )
            if (response.isSuccessful) {
                val body = response.body()
                val number = body?.get("phoneNumber") as? String
                if (number != null) {
                    db.collection("users")
                        .document(userId)
                        .update("twilioNumber", number)
                        .await()
                    android.util.Log.d(TAG, "Provisioned number: $number for user: $userId")
                }
            } else {
                android.util.Log.e(TAG, "Provision number failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "provisionPhoneNumber error: ${e.message}")
        }
    }

    private fun computeProgress(): Float {
        var filled = 0
        val total = 8
        if (businessName.value.isNotBlank()) filled++
        if (businessType.value.isNotBlank()) filled++
        if (phoneNumber.value.isNotBlank()) filled++
        if (businessHours.value.any { it.enabled }) filled++
        if (googleBusinessUrl.value.isNotBlank()) filled++
        if (websiteUrl.value.isNotBlank()) filled++
        // up to 2 extra points for answering Q&A
        filled += minOf(qaAnswers.value.values.count { it.isNotBlank() }, 2)
        return (filled.toFloat() / total).coerceIn(0f, 1f)
    }
}
