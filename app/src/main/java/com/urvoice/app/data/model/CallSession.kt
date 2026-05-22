package com.urvoice.app.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Exchange(
    @PropertyName("transcript")
    val transcript: String? = null,

    @PropertyName("aiResponse")
    val aiResponse: String? = null,

    @PropertyName("language")
    val language: String? = null,

    @PropertyName("timestamp")
    val timestamp: String? = null  // ISO-8601 string from backend
) {
    constructor() : this(null, null, null, null)
}

data class CallSession(
    @PropertyName("sessionId")
    val sessionId: String? = null,

    @PropertyName("userId")
    val userId: String? = null,

    @PropertyName("callerNumber")
    val callerNumber: String? = null,

    @PropertyName("callerName")
    val callerName: String? = null,

    @PropertyName("category")
    val category: String? = null,

    @PropertyName("startTime")
    val startTime: Date? = null,

    @PropertyName("endTime")
    val endTime: Date? = null,

    @PropertyName("status")
    val status: String? = null,  // "active" or "completed"

    @PropertyName("totalExchanges")
    val totalExchanges: Int? = null,

    @PropertyName("exchanges")
    val exchanges: List<Exchange> = emptyList()
) {
    constructor() : this(null, null, null, null, null, null, null, null, null, emptyList())
}
