package com.urvoice.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Business context information for AI assistant
 * Contains business details, services, pricing, and custom Q&A pairs
 */
data class BusinessContext(
    @DocumentId
    val userId: String? = null,
    
    @PropertyName("business_name")
    val businessName: String? = null,
    
    @PropertyName("business_type")
    val businessType: String? = null,
    
    @PropertyName("hours")
    val hours: String? = null,
    
    @PropertyName("services")
    val services: List<String>? = null,
    
    @PropertyName("pricing")
    val pricing: Map<String, String>? = null,
    
    @PropertyName("faqs")
    val faqs: List<FAQ>? = null,
    
    @PropertyName("custom_qa")
    val customQA: List<QAPair>? = null,
    
    @PropertyName("last_updated")
    @ServerTimestamp
    val lastUpdated: Date? = null
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        userId = null,
        businessName = null,
        businessType = null,
        hours = null,
        services = null,
        pricing = null,
        faqs = null,
        customQA = null,
        lastUpdated = null
    )
}

/**
 * FAQ data class for storing frequently asked questions
 */
data class FAQ(
    @PropertyName("question")
    val question: String? = null,
    
    @PropertyName("answer")
    val answer: String? = null
) {
    constructor() : this(null, null)
}

/**
 * Custom Question-Answer pair for business-specific information
 */
data class QAPair(
    @PropertyName("question")
    val question: String? = null,
    
    @PropertyName("answer")
    val answer: String? = null
) {
    constructor() : this(null, null)
    
    // Helper function to convert to Pair
    fun toPair(): Pair<String, String>? {
        return if (question != null && answer != null) {
            Pair(question, answer)
        } else null
    }
    
    companion object {
        // Helper function to create from Pair
        fun fromPair(pair: Pair<String, String>): QAPair {
            return QAPair(pair.first, pair.second)
        }
    }
}
