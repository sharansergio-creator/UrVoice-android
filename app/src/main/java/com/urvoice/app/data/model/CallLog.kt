package com.urvoice.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Call log entry for tracking incoming calls
 * Stores transcript, AI response, and call metadata
 */
data class CallLog(
    @DocumentId
    val callId: String? = null,

    @PropertyName("userId")
    val userId: String? = null,

    @PropertyName("callerId")
    val callerId: String? = null,

    @PropertyName("callerNumber")
    val callerNumber: String? = null,

    @PropertyName("transcript")
    val transcript: String? = null,

    @PropertyName("aiResponse")
    val aiResponse: String? = null,

    @PropertyName("language")
    val language: String? = null,

    @PropertyName("duration")
    val duration: Long? = null, // in seconds

    @PropertyName("timestamp")
    @ServerTimestamp
    val timestamp: Date? = null,

    @PropertyName("category")
    val category: CallCategory? = CallCategory.UNKNOWN
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        callId = null,
        userId = null,
        callerId = null,
        callerNumber = null,
        transcript = null,
        aiResponse = null,
        language = null,
        duration = null,
        timestamp = null,
        category = CallCategory.UNKNOWN
    )
}

/**
 * Category enum for call classification
 */
enum class CallCategory {
    @PropertyName("CUSTOMER")
    CUSTOMER,
    
    @PropertyName("SPAM")
    SPAM,
    
    @PropertyName("BUSINESS")
    BUSINESS,
    
    @PropertyName("UNKNOWN")
    UNKNOWN
}
